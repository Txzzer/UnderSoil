
 // UnderSoil Admin — user management
 // Supports loading, adding, editing, and removing users.

(function () {
  const session = requireAdminSession();

  // Do not make API requests without an admin session.
  if (!session) return;

  const tableBody = document.getElementById("users-table-body");
  const userCount = document.getElementById("user-count");
  const tableMessage = document.getElementById("table-message");
  const form = document.getElementById("user-form");
  const formTitle = document.getElementById("form-title");
  const formMessage = document.getElementById("form-message");
  const saveButton = document.getElementById("save-user-btn");

  const idInput = document.getElementById("u-id");
  const nameInput = document.getElementById("u-name");
  const roleInput = document.getElementById("u-role");
  const emailInput = document.getElementById("u-email");
  const passwordInput = document.getElementById("u-password");

  let users = [];

  function getToken() {
    return sessionStorage.getItem("undersoil_token");
  }

  function setMessage(element, message, isError = false) {
    element.textContent = message;
    element.style.color = isError ? "#b42318" : "";
  }

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

  // Display all users and create their action buttons.
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

      // EDIT BUTTON
      const editButton = document.createElement("button");
      editButton.type = "button";
      editButton.className = "icon-btn";
      editButton.textContent = "Edit";

      editButton.addEventListener("click", () => {
        editUser(user);
      });

      // REMOVE BUTTON
      const removeButton = document.createElement("button");
      removeButton.type = "button";
      removeButton.className = "icon-btn danger";
      removeButton.textContent = "Remove";

      removeButton.addEventListener("click", () => {
        deleteUser(user, removeButton);
      });

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
      setMessage(tableMessage, "");
    } catch (error) {
      userCount.textContent = "Unable to load users";
      tableBody.replaceChildren();

      const errorRow = document.createElement("tr");
      const errorCell = makeCell(error.message);
      errorCell.colSpan = 4;
      errorRow.appendChild(errorCell);
      tableBody.appendChild(errorRow);

      setMessage(tableMessage, error.message, true);
    }
  }

  // EDIT: Populate the form with the selected user's details.
  function editUser(user) {
    idInput.value = user.id;
    nameInput.value = user.name ?? "";
    emailInput.value = user.email ?? "";
    roleInput.value = user.role ?? "Farmer";

    // Never populate the password with the existing password hash.
    passwordInput.value = "";
    passwordInput.required = false;

    formTitle.textContent = "Edit user";
    saveButton.textContent = "Update user";

    setMessage(
      formMessage,
      "Update the user's details. Leave the password blank to keep it unchanged."
    );

    setMessage(tableMessage, "");

    // Bring the form into view.
    document.getElementById("edit-user").scrollIntoView({
      behavior: "smooth",
      block: "start"
    });
  }

  // DELETE: Remove a user after confirmation.
  async function deleteUser(user, button) {
    const confirmed = window.confirm(
      `Are you sure you want to remove ${user.name} (${user.email})? This action cannot be undone.`
    );

    if (!confirmed) return;

    button.disabled = true;
    const originalText = button.textContent;
    button.textContent = "Removing...";

    try {
      await apiRequest(`/api/Users/${encodeURIComponent(user.id)}`, {
        method: "DELETE"
      });

      // If the deleted user is currently being edited, clear the form.
      if (String(idInput.value) === String(user.id)) {
        clearForm();
      }

      setMessage(
        tableMessage,
        `${user.name} was removed successfully.`
      );

      await loadUsers();
    } catch (error) {
      setMessage(tableMessage, error.message, true);
      button.disabled = false;
      button.textContent = originalText;
    }
  }

  // Reset the form to Add mode.
  function clearForm() {
    form.reset();
    idInput.value = "";

    formTitle.textContent = "Add user";
    saveButton.textContent = "Save user";
    passwordInput.required = true;

    setMessage(formMessage, "");
  }

  // CREATE and UPDATE: Handle form submission.
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
      setMessage(
        formMessage,
        "Please select a valid role.",
        true
      );
      return;
    }

    // Build the request body.
    const requestBody = {
      name,
      email,
      role
    };

    // Include the password only if one was provided.
    if (password) {
      requestBody.password = password;
    }

    saveButton.disabled = true;
    saveButton.textContent = isEditing
      ? "Updating..."
      : "Saving...";

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

        clearForm();

        setMessage(
          formMessage,
          "User updated successfully."
        );

        setMessage(
          tableMessage,
          "User details updated successfully."
        );
      } else {
        // POST: Create a new user.
        await apiRequest("/api/Users", {
          method: "POST",
          body: JSON.stringify({
            ...requestBody,
            password
          })
        });

        clearForm();

        setMessage(
          formMessage,
          "User created successfully."
        );

        setMessage(
          tableMessage,
          "User created successfully."
        );
      }

      await loadUsers();
    } catch (error) {
      setMessage(formMessage, error.message, true);
    } finally {
      saveButton.disabled = false;

      // Keep the appropriate button label if a request failed.
      saveButton.textContent = idInput.value
        ? "Update user"
        : "Save user";
    }
  });

  // CLEAR: Return to Add mode.
  document.getElementById("clear-user-form")
    .addEventListener("click", clearForm);

  // Initial page load.
  loadUsers();
})();