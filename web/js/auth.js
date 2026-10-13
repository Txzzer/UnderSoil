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
