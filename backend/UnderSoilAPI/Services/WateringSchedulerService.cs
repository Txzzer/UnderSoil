using Microsoft.EntityFrameworkCore;
using UnderSoilAPI.Models;

namespace UnderSoilAPI.Services
{
    /// <summary>
    /// Every minute: opens the valve for due Pending schedules and closes it again once their duration has passed.
    ///
    /// Status flow: Pending -> Running (open sent) -> Completed (close sent).
    /// A Pending schedule whose whole window is already in the past (phone synced late, server was down)
    /// is marked Missed instead of watering at the wrong time.
    /// If an MQTT publish fails, the status is left unchanged so the next tick retries it.
    /// </summary>
    public class WateringSchedulerService : BackgroundService
    {
        private static readonly TimeSpan TickInterval = TimeSpan.FromSeconds(60);

        private readonly IServiceScopeFactory _scopeFactory;
        private readonly ValveCommandPublisher _valveCommands;
        private readonly ILogger<WateringSchedulerService> _logger;

        public WateringSchedulerService(
            IServiceScopeFactory scopeFactory,
            ValveCommandPublisher valveCommands,
            ILogger<WateringSchedulerService> logger)
        {
            _scopeFactory = scopeFactory;
            _valveCommands = valveCommands;
            _logger = logger;
        }

        protected override async Task ExecuteAsync(CancellationToken stoppingToken)
        {
            using var timer = new PeriodicTimer(TickInterval);
            do
            {
                try
                {
                    await ProcessSchedulesAsync(stoppingToken);
                }
                catch (OperationCanceledException) when (stoppingToken.IsCancellationRequested)
                {
                    break;
                }
                catch (Exception ex)
                {
                    _logger.LogError(ex, "Watering scheduler tick failed");
                }
            } while (await timer.WaitForNextTickAsync(stoppingToken));
        }

        private async Task ProcessSchedulesAsync(CancellationToken ct)
        {
            using var scope = _scopeFactory.CreateScope();
            var db = scope.ServiceProvider.GetRequiredService<UnderSoilContext>();
            long now = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();

            // 1. Finish running schedules whose duration has elapsed
            var running = await db.WateringSchedules
                .Where(s => s.Status == WateringSchedule.StatusRunning)
                .ToListAsync(ct);

            var finished = running.Where(s => s.EndTime <= now).ToList();
            if (finished.Count > 0)
            {
                // Don't close the valve while another overlapping schedule is still meant to be watering
                bool otherStillRunning = running.Any(s => s.EndTime > now);
                if (!otherStillRunning && !await TryPublishAsync(false, null, "close", finished[0].Id, ct))
                {
                    return; // retry next tick
                }

                foreach (var schedule in finished)
                {
                    schedule.Status = WateringSchedule.StatusCompleted;
                    _logger.LogInformation("Watering schedule {Id} completed", schedule.Id);
                }
                await db.SaveChangesAsync(ct);
            }

            // 2. Start pending schedules that are due
            var due = await db.WateringSchedules
                .Where(s => s.Status == WateringSchedule.StatusPending && s.StartTime <= now)
                .OrderBy(s => s.StartTime)
                .ToListAsync(ct);

            foreach (var schedule in due)
            {
                if (schedule.EndTime <= now)
                {
                    schedule.Status = WateringSchedule.StatusMissed;
                    _logger.LogWarning("Watering schedule {Id} missed: its window ended before it could start", schedule.Id);
                    await db.SaveChangesAsync(ct);
                    continue;
                }

                // Only water for what's left of the window (the tick can be up to a minute late)
                int remainingMinutes = (int)Math.Ceiling((schedule.EndTime - now) / 60_000.0);
                if (!await TryPublishAsync(true, remainingMinutes, "open", schedule.Id, ct))
                {
                    return; // MQTT is down; leave Pending and retry next tick
                }

                schedule.Status = WateringSchedule.StatusRunning;
                await db.SaveChangesAsync(ct);
                _logger.LogInformation("Watering schedule {Id} started for {Minutes} min (sector {SectorId})",
                    schedule.Id, remainingMinutes, schedule.SectorId);
            }
        }

        private async Task<bool> TryPublishAsync(bool open, int? durationMinutes, string action, string scheduleId, CancellationToken ct)
        {
            try
            {
                await _valveCommands.PublishAsync(open, durationMinutes, ct);
                return true;
            }
            catch (Exception ex) when (ex is not OperationCanceledException)
            {
                _logger.LogWarning(ex, "Failed to send valve {Action} command for schedule {Id}; will retry", action, scheduleId);
                return false;
            }
        }
    }
}
