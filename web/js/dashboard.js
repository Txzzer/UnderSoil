// Farm overview page — pulls sector count, device online count, and active
// alerts from the backend. The "Disease scans this week" and "Water used
// today" tiles stay static for now — there's no backend endpoint for scan
// totals or daily water totals yet.

const DASHBOARD_ONLINE_THRESHOLD_MS = 10 * 60 * 1000;

(async function () {
  loadSectorCount();
  loadDeviceCount();
  loadAlerts();
})();

async function loadSectorCount() {
  const el = document.getElementById("fact-sectors");
  try {
    const sectors = await apiGet("/api/sectors");
    el.textContent = sectors.length;
  } catch (err) {
    el.textContent = "—";
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
