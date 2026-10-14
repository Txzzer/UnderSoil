// Devices page — read-only, data owned by the farmer's mobile app / ESP32s.
// Pulls GET /api/telemetry/latest (most recent reading per device) and
// renders it into the table. "Online" is derived client-side from how
// recent the last reading is, since the backend doesn't track a live
// connection state — anything seen in the last 10 minutes counts as online.

const ONLINE_THRESHOLD_MS = 10 * 60 * 1000;

(async function () {
  const body = document.getElementById("devices-body");
  const subtitle = document.getElementById("devices-subtitle");

  try {
    const readings = await apiGet("/api/telemetry/latest");

    if (!readings.length) {
      body.innerHTML = `<tr><td colspan="7">No devices have reported in yet.</td></tr>`;
      subtitle.textContent = "No devices seen yet";
      return;
    }

    const onlineCount = readings.filter(isOnline).length;
    subtitle.textContent = `${onlineCount} of ${readings.length} devices online`;

    body.innerHTML = readings
      .slice()
      .sort((a, b) => (a.deviceId || a.DeviceId || "").localeCompare(b.deviceId || b.DeviceId || ""))
      .map(r => {
        const deviceId = r.deviceId ?? r.DeviceId ?? "—";
        const flow = r.flowRateLitersPerMin ?? r.FlowRateLitersPerMin;
        const valveOpen = r.valveIsOpen ?? r.ValveIsOpen;
        const temp = r.temperatureCelsius ?? r.TemperatureCelsius;
        const humidity = r.humidityPercentage ?? r.HumidityPercentage;
        const timestamp = r.timestamp ?? r.Timestamp;
        const online = isOnline(r);

        return `
          <tr>
            <td>${escapeHtml(deviceId)}</td>
            <td>${flow != null ? flow.toFixed(1) + " L/min" : "—"}</td>
            <td>${valveOpen ? "Open" : "Closed"}</td>
            <td>${temp != null ? temp.toFixed(1) + "°C" : "—"}</td>
            <td>${humidity != null ? humidity.toFixed(0) + "%" : "—"}</td>
            <td>${online ? '<span class="status-pill ok">Online</span>' : '<span class="status-pill offline">Offline</span>'}</td>
            <td>${timestamp ? timeAgo(timestamp) : "—"}</td>
          </tr>
        `;
      })
      .join("");
  } catch (err) {
    body.innerHTML = `<tr><td colspan="7">Couldn't load devices — ${escapeHtml(err.message)}</td></tr>`;
    subtitle.textContent = "Couldn't load devices";
  }
})();

function isOnline(reading) {
  const timestamp = reading.timestamp ?? reading.Timestamp;
  if (!timestamp) return false;
  return Date.now() - new Date(timestamp).getTime() < ONLINE_THRESHOLD_MS;
}

function escapeHtml(str) {
  const div = document.createElement("div");
  div.textContent = str;
  return div.innerHTML;
}
