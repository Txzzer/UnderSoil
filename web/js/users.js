// UnderSoil Admin — user management
// Loading, adding, editing, and removing users, via modals + toasts
// instead of an inline always-visible form and native confirm() popups.

(function () {
  const session = requireAdminSession();

  // Do not make API requests without an admin session.
  if (!session) return;

  const tableBody = document.getElementById("users-table-body");
  const userCount = document.getElementById("user-count");

  const modalOverlay = document.getElementById("user-modal-overlay");
  const form = document.getElementById("user-form");
  const formTitle = document.getElementById("form-title");
  const formMessage = document.getElementById("form-message");
  const saveButton = document.getElementById("save-user-btn");

  const idInput = document.getElementById("u-id");
  const nameInput = document.getElementById("u-name");
  const roleInput = document.getElementById("u-role");
  const emailInput = document.getElementById("u-email");
  const passwordInput = document.getElementById("u-password");

  const confirmOverlay = document.getElementById("confirm-modal-overlay");
  const confirmMessage = document.getElementById("confirm-message");
  const confirmDeleteBtn = document.getElementById("confirm-delete-btn");

  let users = [];
  let pendingDeleteUser = null;

  function getToken() {
    return sessionStorage.getItem("undersoil_token");
  }

  // showToast() comes from auth.js — shared across every page.

  function setMessage(element, message, isError = false) {
    element.textContent = message;
    element.style.color = isError ? "#b42318" : "";
  }

  // ---------- API ----------
  async function apiRequest(path, options = {}) {
    const token = getToken();

    if (!token) {
      logout();
      throw new Error(
        "Your session has expired. Please sign in again."
      );
    }

    const response = await fetch(`${API_BASE}${path}`, {
      ...options,
      headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${token}`,
        ...(options.headers || {})
      }
    });

    if (response.status === 401 || response.status === 403) {
      throw new Error(
        response.status === 401
          ? "Your session has expired or the token was rejected. Please sign in again."
          : "Access denied. An administrator account is required."
      );
    }

    if (!response.ok) {
      const detail = await response.text();
      throw new Error(
        detail || `The server returned error ${response.status}.`
      );
    }

    if (response.status === 204) return null;

    const text = await response.text();
    return text ? JSON.parse(text) : null;
  }

  function makeCell(value) {
    const cell = document.createElement("td");
    cell.textContent = value ?? "";
    return cell;
  }

  // ---------- Table ----------
  function renderUsers() {
    tableBody.replaceChildren();

    userCount.textContent =
      `${users.length} ${users.length === 1 ? "person" : "people"} with accounts on UnderSoil`;

    if (users.length === 0) {
      const row = document.createElement("tr");
      const cell = makeCell("No users found.");
      cell.colSpan = 4;
      row.appendChild(cell);
      tableBody.appendChild(row);
      return;
    }

    users.forEach(user => {
      const row = document.createElement("tr");

      row.appendChild(makeCell(user.name));
      row.appendChild(makeCell(user.role));
      row.appendChild(makeCell(user.email));

      const actionsCell = document.createElement("td");
      const actions = document.createElement("div");
      actions.className = "row-actions";

      // EDIT BUTTON — opens the modal pre-filled
      const editButton = document.createElement("button");
      editButton.type = "button";
      editButton.className = "icon-btn";
      editButton.textContent = "Edit";
      editButton.addEventListener("click", () => openModal("edit", user));

      // REMOVE BUTTON — opens the confirm modal
      const removeButton = document.createElement("button");
      removeButton.type = "button";
      removeButton.className = "icon-btn danger";
      removeButton.textContent = "Remove";
      removeButton.addEventListener("click", () => openConfirmModal(user));

      actions.append(editButton, removeButton);
      actionsCell.appendChild(actions);
      row.appendChild(actionsCell);

      tableBody.appendChild(row);
    });
  }

  // GET: Load users from the API.
  async function loadUsers() {
    tableBody.replaceChildren();

    const row = document.createElement("tr");
    const cell = makeCell("Loading users...");
    cell.colSpan = 4;
    row.appendChild(cell);
    tableBody.appendChild(row);

    try {
      users = await apiRequest("/api/Users");
      renderUsers();
    } catch (error) {
      userCount.textContent = "Unable to load users";
      tableBody.replaceChildren();

      const errorRow = document.createElement("tr");
      const errorCell = makeCell(error.message);
      errorCell.colSpan = 4;
      errorRow.appendChild(errorCell);
      tableBody.appendChild(errorRow);

      showToast(error.message, "error");
    }
  }

  // ---------- Add / edit modal ----------
  function openModal(mode, user) {
    form.reset();
    setMessage(formMessage, "");

    if (mode === "edit") {
      idInput.value = user.id;
      nameInput.value = user.name ?? "";
      emailInput.value = user.email ?? "";
      roleInput.value = user.role ?? "Farmer";

      // Never populate the password with the existing hash.
      passwordInput.value = "";
      passwordInput.required = false;

      formTitle.textContent = "Edit user";
      saveButton.textContent = "Update user";
    } else {
      idInput.value = "";
      passwordInput.required = true;

      formTitle.textContent = "Add user";
      saveButton.textContent = "Save user";
    }

    modalOverlay.hidden = false;
    nameInput.focus();
  }

  function closeModal() {
    modalOverlay.hidden = true;
    form.reset();
    setMessage(formMessage, "");
  }

  document.getElementById("add-user-btn")
    .addEventListener("click", () => openModal("add"));
  document.getElementById("modal-close-btn")
    .addEventListener("click", closeModal);
  document.getElementById("cancel-user-form")
    .addEventListener("click", closeModal);

  // Click outside the modal box closes it.
  modalOverlay.addEventListener("click", event => {
    if (event.target === modalOverlay) closeModal();
  });

  // ---------- Delete confirm modal ----------
  function openConfirmModal(user) {
    pendingDeleteUser = user;
    confirmMessage.textContent =
      `Are you sure you want to remove ${user.name} (${user.email})? This action cannot be undone.`;
    confirmOverlay.hidden = false;
  }

  function closeConfirmModal() {
    confirmOverlay.hidden = true;
    pendingDeleteUser = null;
    confirmDeleteBtn.disabled = false;
    confirmDeleteBtn.textContent = "Remove";
  }

  document.getElementById("confirm-close-btn")
    .addEventListener("click", closeConfirmModal);
  document.getElementById("cancel-delete-btn")
    .addEventListener("click", closeConfirmModal);

  confirmOverlay.addEventListener("click", event => {
    if (event.target === confirmOverlay) closeConfirmModal();
  });

  // DELETE: Remove a user after confirmation.
  confirmDeleteBtn.addEventListener("click", async () => {
    if (!pendingDeleteUser) return;
    const user = pendingDeleteUser;

    confirmDeleteBtn.disabled = true;
    confirmDeleteBtn.textContent = "Removing...";

    try {
      await apiRequest(`/api/Users/${encodeURIComponent(user.id)}`, {
        method: "DELETE"
      });

      // If the deleted user is currently open in the edit modal, close it too.
      if (String(idInput.value) === String(user.id)) {
        closeModal();
      }

      closeConfirmModal();
      showToast(`${user.name} was removed.`, "success");
      await loadUsers();
    } catch (error) {
      // Includes the last-admin safeguard message from the backend, if that's
      // why the delete was rejected.
      confirmDeleteBtn.disabled = false;
      confirmDeleteBtn.textContent = "Remove";
      showToast(error.message, "error");
    }
  });

  // Escape closes whichever modal is currently open.
  document.addEventListener("keydown", event => {
    if (event.key !== "Escape") return;
    if (!confirmOverlay.hidden) closeConfirmModal();
    else if (!modalOverlay.hidden) closeModal();
  });

  // ---------- Create and update ----------
  form.addEventListener("submit", async event => {
    event.preventDefault();

    const isEditing = idInput.value !== "";
    const id = idInput.value;

    const name = nameInput.value.trim();
    const email = emailInput.value.trim();
    const password = passwordInput.value;
    const role = roleInput.value;

    if (!name || !email) {
      setMessage(
        formMessage,
        "Please enter the user's name and email address.",
        true
      );
      return;
    }

    // Password is required only when adding a user.
    if (!isEditing && !password) {
      setMessage(
        formMessage,
        "A password is required when adding a user.",
        true
      );
      return;
    }

    if (password && password.length < 8) {
      setMessage(
        formMessage,
        "The password must contain at least 8 characters.",
        true
      );
      return;
    }

    if (role !== "Farmer" && role !== "Admin") {
      setMessage(formMessage, "Please select a valid role.", true);
      return;
    }

    const requestBody = { name, email, role };
    if (password) requestBody.password = password;

    saveButton.disabled = true;
    saveButton.textContent = isEditing ? "Updating..." : "Saving...";

    try {
      if (isEditing) {
        // PUT: Update an existing user.
        await apiRequest(
          `/api/Users/${encodeURIComponent(id)}`,
          {
            method: "PUT",
            body: JSON.stringify(requestBody)
          }
        );

        closeModal();
        showToast("User updated successfully.", "success");
      } else {
        // POST: Create a new user.
        await apiRequest("/api/Users", {
          method: "POST",
          body: JSON.stringify({ ...requestBody, password })
        });

        closeModal();
        showToast("User created successfully.", "success");
      }

      await loadUsers();
    } catch (error) {
      // Stays inline in the modal (not a toast) since it's tied to the form
      // the user is actively filling in — e.g. "That email is already
      // registered," or the last-admin safeguard message on a role change.
      setMessage(formMessage, error.message, true);
    } finally {
      saveButton.disabled = false;
      saveButton.textContent = idInput.value ? "Update user" : "Save user";
    }
  });

  // Initial page load.
  loadUsers();
})();
