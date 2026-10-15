// UnderSoil Admin — shared auth helpers
// Loaded on every page. Talks to the backend's /api/Auth/login endpoint
// and keeps the logged-in admin's session in sessionStorage (cleared when
// the browser tab closes — on a shared lab PC that's safer than localStorage,
// which would leave the session signed in indefinitely).

const API_BASE = "http://localhost:5196"; // change this if the backend runs on a different port

// TODO confirm this against the backend / Users table — "Admin" is a guess
// based on the "Farmer" role seen in a decoded token. If the real value is
// different (e.g. "Administrator"), update this one line.
const ADMIN_ROLE = "Admin";

const SESSION_TOKEN_KEY = "undersoil_token";
const SESSION_USER_KEY = "undersoil_user";

// Decodes a JWT's payload (no signature check — the backend already verified
// it; this is just so the frontend can read the role/name/exp claims).
function decodeJwt(token) {
  try {
    const payload = token.split(".")[1];
    const padded = payload.replace(/-/g, "+").replace(/_/g, "/") + "===".slice((payload.length + 3) % 4);
    return JSON.parse(atob(padded));
  } catch (e) {
    return null;
  }
}

// Calls POST /api/Auth/login. Throws an Error with a human-readable message
// on failure; resolves to { token, userId, name } on success.
async function loginRequest(email, password) {
  let res;
  try {
    res = await fetch(`${API_BASE}/api/Auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, password })
    });
  } catch (e) {
    throw new Error("Can't reach the server. Is the backend running?");
  }
  if (!res.ok) {
    if (res.status === 401 || res.status === 400) {
      throw new Error("Incorrect email or password.");
    }
    throw new Error("Something went wrong signing in. Try again.");
  }
  return res.json();
}

function saveSession(token, claims) {
  sessionStorage.setItem(SESSION_TOKEN_KEY, token);
  sessionStorage.setItem(SESSION_USER_KEY, JSON.stringify({
    userId: claims.sub,
    name: claims.name,
    email: claims.email,
    role: claims.role
  }));
}

function clearSession() {
  sessionStorage.removeItem(SESSION_TOKEN_KEY);
  sessionStorage.removeItem(SESSION_USER_KEY);
}

// Returns { token, user } if a valid, unexpired session exists, else null.
function getSession() {
  const token = sessionStorage.getItem(SESSION_TOKEN_KEY);
  const userRaw = sessionStorage.getItem(SESSION_USER_KEY);
  if (!token || !userRaw) return null;

  const claims = decodeJwt(token);
  if (!claims || !claims.exp || Date.now() >= claims.exp * 1000) {
    clearSession();
    return null;
  }
  return { token, user: JSON.parse(userRaw) };
}

// Call this at the top of every protected admin page. Redirects to the
// login page if there's no valid session, or if the logged-in user isn't
// an admin (e.g. a farmer account trying to open the web dashboard).
function requireAdminSession() {
  const session = getSession();
  if (!session || session.user.role !== ADMIN_ROLE) {
    clearSession();
    window.location.href = "login.html";
    return null;
  }
  return session;
}

function logout() {
  clearSession();
  window.location.href = "login.html";
}

// Shared GET helper for authenticated admin reads. Returns parsed JSON on
// success; throws an Error with a human-readable message on failure. On a
// 401/403 it clears the session and bounces to login, same as an expired
// token would.
async function apiGet(path) {
  const session = getSession();
  if (!session) {
    window.location.href = "login.html";
    throw new Error("Session expired.");
  }

  let res;
  try {
    res = await fetch(`${API_BASE}${path}`, {
      headers: { Authorization: `Bearer ${session.token}` }
    });
  } catch (e) {
    throw new Error("Can't reach the server. Is the backend running?");
  }

  if (res.status === 401 || res.status === 403) {
    clearSession();
    window.location.href = "login.html";
    throw new Error("Session expired.");
  }
  if (!res.ok) {
    throw new Error(`Request failed (${res.status}).`);
  }
  return res.json();
}

// Shared helper for authenticated writes (POST/PUT/DELETE). `body`, if
// given, is JSON-stringified. Returns parsed JSON for responses with a
// body, or null for a 204 No Content. Throws an Error with a
// human-readable message on failure.
async function apiRequest(path, method, body) {
  const session = getSession();
  if (!session) {
    window.location.href = "login.html";
    throw new Error("Session expired.");
  }

  let res;
  try {
    res = await fetch(`${API_BASE}${path}`, {
      method,
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${session.token}`
      },
      body: body !== undefined ? JSON.stringify(body) : undefined
    });
  } catch (e) {
    throw new Error("Can't reach the server. Is the backend running?");
  }

  if (res.status === 401 || res.status === 403) {
    clearSession();
    window.location.href = "login.html";
    throw new Error("Session expired.");
  }
  if (res.status === 204) return null;

  if (!res.ok) {
    let message = `Request failed (${res.status}).`;
    try {
      const data = await res.json();
      if (data?.message) message = data.message;
    } catch (e) { /* body wasn't JSON — keep the default message */ }
    throw new Error(message);
  }

  try {
    return await res.json();
  } catch (e) {
    return null;
  }
}

// Shared toast notifications. Every page that wants to use this needs a
// <div class="toast-container" id="toast-container"></div> in its HTML and
// css/modals.css linked (that's where the toast styling lives).
function showToast(message, type = "success") {
  const container = document.getElementById("toast-container");
  if (!container) return; // page didn't opt in — fail quietly rather than throw

  const toast = document.createElement("div");
  toast.className = `toast ${type}`;
  toast.setAttribute("role", "status");

  const text = document.createElement("span");
  text.textContent = message;

  const close = document.createElement("button");
  close.className = "toast-close";
  close.type = "button";
  close.setAttribute("aria-label", "Dismiss");
  close.textContent = "×";
  close.addEventListener("click", () => toast.remove());

  toast.append(text, close);
  container.appendChild(toast);

  setTimeout(() => toast.remove(), 4500);
}

// Rough relative-time formatter ("Just now", "5 min ago", "3 hr ago") for
// device/alert timestamps, so we're not just dumping raw ISO strings.
function timeAgo(isoString) {
  const then = new Date(isoString).getTime();
  if (isNaN(then)) return isoString;
  const diffMs = Date.now() - then;
  const mins = Math.floor(diffMs / 60000);
  if (mins < 1) return "Just now";
  if (mins === 1) return "1 min ago";
  if (mins < 60) return `${mins} min ago`;
  const hrs = Math.floor(mins / 60);
  if (hrs === 1) return "1 hr ago";
  if (hrs < 24) return `${hrs} hr ago`;
  const days = Math.floor(hrs / 24);
  return days === 1 ? "1 day ago" : `${days} days ago`;
}
