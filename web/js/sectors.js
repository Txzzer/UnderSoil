// Sectors & crops page — read-only, data owned by the farmer's mobile app.
// Pulls GET /api/sectors and renders it into the table.

(async function () {
  const body = document.getElementById("sectors-body");
  const subtitle = document.getElementById("sectors-subtitle");

  try {
    const sectors = await apiGet("/api/sectors");

    if (!sectors.length) {
      body.innerHTML = `<tr><td colspan="3">No sectors yet.</td></tr>`;
      subtitle.textContent = "No sectors mapped yet";
      return;
    }

    subtitle.textContent = `${sectors.length} sector${sectors.length === 1 ? "" : "s"} mapped across your farm`;

    body.innerHTML = sectors.map(s => `
      <tr>
        <td>${escapeHtml(s.name || s.Name || "—")}</td>
        <td>${escapeHtml(s.cropNames || s.CropNames || "—")}</td>
        <td>${(s.isSynced ?? s.IsSynced) ? '<span class="status-pill ok">Synced</span>' : '<span class="status-pill amber">Pending sync</span>'}</td>
      </tr>
    `).join("");
  } catch (err) {
    body.innerHTML = `<tr><td colspan="3">Couldn't load sectors — ${escapeHtml(err.message)}</td></tr>`;
    subtitle.textContent = "Couldn't load sectors";
  }
})();

function escapeHtml(str) {
  const div = document.createElement("div");
  div.textContent = str;
  return div.innerHTML;
}
