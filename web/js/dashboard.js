// Farm overview page — pulls sector count, device online count, and active
// alerts from the backend. The "Disease scans this week" and "Water used
// today" tiles stay static for now — there's no backend endpoint for scan
// totals or daily water totals yet.

const DASHBOARD_ONLINE_THRESHOLD_MS = 10 * 60 * 1000;

(async function () {
  loadSectors();
  loadDeviceCount();
  loadAlerts();
  loadScanCount();
  loadWaterToday();
})();

async function loadScanCount() {
  const el = document.getElementById("fact-scans");
  const ONE_WEEK_MS = 7 * 24 * 60 * 60 * 1000;

  try {
    const logs = await apiGet("/api/scanlogs");
    const cutoff = Date.now() - ONE_WEEK_MS;

    const thisWeek = logs.filter(log => {
      const dateStr = log.dateStr ?? log.DateStr;
      if (!dateStr) return false;
      const parsed = new Date(dateStr).getTime();
      return !isNaN(parsed) && parsed >= cutoff;
    });

    el.textContent = thisWeek.length;
  } catch (err) {
    el.textContent = "—";
  }
}

// Needs /api/wateringschedules, which needs WateringSchedules added as a
// DbSet + its table created first — see WateringSchedulesController.cs.
// Fails quietly (stays "—") until that's in place.
async function loadWaterToday() {
  const el = document.getElementById("fact-water");

  try {
    const schedules = await apiGet("/api/wateringschedules");

    const now = new Date();
    const startOfDay = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();

    const total = schedules
      .filter(w => {
        const startTime = w.startTime ?? w.StartTime;
        return startTime && startTime >= startOfDay;
      })
      .reduce((sum, w) => {
        const actual = w.actualVolume ?? w.ActualVolume ?? 0;
        const expected = w.expectedVolume ?? w.ExpectedVolume ?? 0;
        return sum + (actual > 0 ? actual : expected);
      }, 0);

    el.innerHTML = `${Math.round(total).toLocaleString()} <span>L</span>`;
  } catch (err) {
    el.textContent = "—";
  }
}

// Drives both the "Sectors under watch" tile and the Farm plan card — one
// fetch, two uses, since they're the same data.
async function loadSectors() {
  const countEl = document.getElementById("fact-sectors");
  const canvas = document.getElementById("plan-canvas");

  try {
    const sectors = await apiGet("/api/sectors");
    countEl.textContent = sectors.length;

    if (!sectors.length) {
      canvas.innerHTML = "<p>No sectors mapped yet.</p>";
      return;
    }

    // The Sector model only has a name and crop list right now — no flow
    // rate or health status — so these plots show what's actually there
    // instead of faking a status badge that isn't backed by real data.
    canvas.innerHTML = sectors.map(s => {
      const name = s.name ?? s.Name ?? "—";
      const crops = s.cropNames ?? s.CropNames ?? "—";
      const synced = s.isSynced ?? s.IsSynced;
      return `
        <div class="plot">
          <div class="plot-head"><h3>${escapeHtml(name)}</h3><span class="plot-tag">${synced ? "Synced" : "Pending sync"}</span></div>
          <p class="plot-meta">Crop: <b>${escapeHtml(crops)}</b></p>
          <div class="mainline"></div>
        </div>
      `;
    }).join("");
  } catch (err) {
    countEl.textContent = "—";
    canvas.innerHTML = `<p>Couldn't load sectors — ${escapeHtml(err.message)}</p>`;
  }
}

async function loadDeviceCount() {
  const el = document.getElementById("fact-devices");
  try {
    const readings = await apiGet("/api/v1/Telemetry/latest-per-device");
    const online = readings.filter(r => {
      const ts = r.timestamp ?? r.Timestamp;
      return ts && Date.now() - new Date(ts).getTime() < DASHBOARD_ONLINE_THRESHOLD_MS;
    }).length;
    el.innerHTML = `${online} <span>of ${readings.length}</span>`;
  } catch (err) {
    el.textContent = "—";
  }
}

async function loadAlerts() {
  const list = document.getElementById("alerts-list");
  const banner = document.getElementById("dashboard-banner");

  try {
    const alerts = await apiGet("/api/alerts");

    if (!alerts.length) {
      list.innerHTML = `<li>No active alerts.</li>`;
      banner.style.display = "none";
      return;
    }

    banner.style.display = "";
    banner.innerHTML = `<strong>${alerts.length} active alert${alerts.length === 1 ? "" : "s"}</strong> — ${escapeHtml(firstAlertText(alerts[0]))}`;

    list.innerHTML = alerts.map(a => {
      const desc = a.alertDesc ?? a.AlertDesc ?? "Alert";
      const deviceId = a.deviceId ?? a.DeviceId ?? "";
      const when = a.dateTime ?? a.DateTime;
      return `
        <li>
          <div>
            <div style="margin-top:6px;">${escapeHtml(deviceId ? `${deviceId} — ${desc}` : desc)}</div>
          </div>
          <span class="when">${when ? timeAgo(when) : "—"}</span>
        </li>
      `;
    }).join("");
  } catch (err) {
    list.innerHTML = `<li>Couldn't load alerts — ${escapeHtml(err.message)}</li>`;
    banner.style.display = "none";
    showToast(`Couldn't load alerts — ${err.message}`, "error");
  }
}

function firstAlertText(alert) {
  const desc = alert.alertDesc ?? alert.AlertDesc ?? "Check the alerts list for details.";
  const deviceId = alert.deviceId ?? alert.DeviceId ?? "";
  return deviceId ? `${deviceId} — ${desc}` : desc;
}

function escapeHtml(str) {
  const div = document.createElement("div");
  div.textContent = str;
  return div.innerHTML;
}
