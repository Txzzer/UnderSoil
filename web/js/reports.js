// Reports page — disease scan history from /api/scanlogs, and water use by
// sector (this week) from /api/wateringschedules + /api/sectors (to map
// SectorId to a readable name).
//
// NOTE: /api/wateringschedules only exists once WateringSchedules is added
// as a DbSet on UnderSoilContext and the table is created in the DB — see
// WateringSchedulesController.cs. Until then this card will show a
// "couldn't load" message, which is expected.

(async function () {
  loadScanHistory();
  loadWaterBySector();
})();

async function loadScanHistory() {
  const body = document.getElementById("scans-body");

  try {
    const logs = await apiGet("/api/scanlogs");

    if (!logs.length) {
      body.innerHTML = `<tr><td colspan="5">No scans logged yet.</td></tr>`;
      return;
    }

    body.innerHTML = logs.map(log => {
      const date = log.dateStr ?? log.DateStr ?? "—";
      const crop = log.cropName ?? log.CropName ?? "—";
      const diagnosis = log.diagnosis ?? log.Diagnosis ?? "—";
      const confidenceRaw = log.confidence ?? log.Confidence;
      const notes = log.notes ?? log.Notes ?? "";

      // Confidence might be stored as a 0–1 fraction or already a percent —
      // treat anything at or under 1 as a fraction.
      const confidencePct = confidenceRaw == null
        ? "—"
        : `${Math.round(confidenceRaw <= 1 ? confidenceRaw * 100 : confidenceRaw)}%`;

      const isHealthy = diagnosis.toLowerCase().includes("healthy");
      const pillClass = isHealthy ? "ok" : "alert";

      return `
        <tr>
          <td>${escapeHtml(date)}</td>
          <td>${escapeHtml(crop)}</td>
          <td><span class="status-pill ${pillClass}">${escapeHtml(diagnosis)}</span></td>
          <td>${confidencePct}</td>
          <td>${escapeHtml(notes)}</td>
        </tr>
      `;
    }).join("");
  } catch (err) {
    body.innerHTML = `<tr><td colspan="5">Couldn't load scan history — ${escapeHtml(err.message)}</td></tr>`;
    showToast(`Couldn't load scan history — ${err.message}`, "error");
  }
}

async function loadWaterBySector() {
  const list = document.getElementById("water-bars");
  const ONE_WEEK_MS = 7 * 24 * 60 * 60 * 1000;

  try {
    const [sectors, schedules] = await Promise.all([
      apiGet("/api/sectors"),
      apiGet("/api/wateringschedules")
    ]);

    const sectorNames = {};
    sectors.forEach(s => {
      sectorNames[s.id ?? s.Id] = s.name ?? s.Name ?? "Unnamed sector";
    });

    const cutoff = Date.now() - ONE_WEEK_MS;
    const totals = {};

    schedules.forEach(w => {
      const startTime = w.startTime ?? w.StartTime;
      if (!startTime || startTime < cutoff) return;

      const sectorId = w.sectorId ?? w.SectorId;
      // Use the actual recorded volume once a schedule has run; fall back
      // to the expected volume for schedules still pending/running.
      const actual = w.actualVolume ?? w.ActualVolume ?? 0;
      const expected = w.expectedVolume ?? w.ExpectedVolume ?? 0;
      const volume = actual > 0 ? actual : expected;

      totals[sectorId] = (totals[sectorId] || 0) + volume;
    });

    const entries = Object.entries(totals);

    if (!entries.length) {
      list.innerHTML = `<li>No watering recorded this week.</li>`;
      return;
    }

    const max = Math.max(...entries.map(([, v]) => v));

    list.innerHTML = entries
      .sort((a, b) => b[1] - a[1])
      .map(([sectorId, volume]) => {
        const name = sectorNames[sectorId] || sectorId;
        const pct = max > 0 ? Math.round((volume / max) * 100) : 0;
        return `
          <li>
            <span>${escapeHtml(name)}</span>
            <span class="track"><span class="fill" style="width:${pct}%;"></span></span>
            <span class="val">${Math.round(volume)} L</span>
          </li>
        `;
      }).join("");
  } catch (err) {
    list.innerHTML = `<li>Couldn't load water use — ${escapeHtml(err.message)}</li>`;
    showToast(`Couldn't load water use — ${err.message}`, "error");
  }
}

function escapeHtml(str) {
  const div = document.createElement("div");
  div.textContent = str;
  return div.innerHTML;
}
