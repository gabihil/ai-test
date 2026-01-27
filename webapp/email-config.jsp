<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!doctype html>
<html lang="en">
  <head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <title>Email Config Manager</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/email-config.css" />
    <script defer src="<%= request.getContextPath() %>/email-config.js"></script>
  </head>
  <body>
    <div class="container">
      <header>
        <div>
          <h1>Email Config Manager</h1>
          <p>Manage mail parser settings stored in a NoSQL collection.</p>
        </div>
        <div class="header-actions">
          <button type="button" id="refresh-btn" class="secondary">Refresh</button>
        </div>
      </header>

      <div class="grid">
        <section class="card">
          <div class="card-title">
            <div>
              <h2>Editor</h2>
              <p class="subtitle">Create or update a mail configuration profile.</p>
            </div>
            <span class="pill" id="form-mode">New</span>
          </div>
          <form id="config-form">
            <input type="hidden" id="config-id" />
            <div class="row">
              <div>
                <label for="alias">Alias</label>
                <input id="alias" required />
              </div>
              <div>
                <label for="protocol">Protocol</label>
                <input id="protocol" required />
              </div>
            </div>
            <div class="row">
              <div>
                <label for="server">Server</label>
                <input id="server" required />
              </div>
              <div>
                <label for="port">Port</label>
                <input id="port" type="number" min="1" />
              </div>
            </div>
            <div class="row">
              <div>
                <label for="username">Username</label>
                <input id="username" required />
              </div>
              <div>
                <label for="password">Password</label>
                <input id="password" type="password" />
              </div>
            </div>
            <div class="row">
              <div>
                <label for="clientId">Client ID</label>
                <input id="clientId" type="number" />
              </div>
              <div>
                <label for="locationId">Location ID</label>
                <input id="locationId" type="number" />
              </div>
            </div>
            <div class="row">
              <div>
                <label for="assignToGroup">Assign To Group</label>
                <input id="assignToGroup" type="number" />
              </div>
              <div>
                <label for="ticketType">Ticket Type</label>
                <input id="ticketType" type="number" />
              </div>
            </div>
            <div class="row">
              <div>
                <label for="defaultPostedBy">Default Posted By</label>
                <input id="defaultPostedBy" type="number" />
              </div>
              <div>
                <label for="active">Active</label>
                <select id="active">
                  <option value="true" selected>Active</option>
                  <option value="false">Inactive</option>
                </select>
              </div>
            </div>
            <div class="row">
              <div>
                <label for="addReply">Add Reply</label>
                <select id="addReply">
                  <option value="true">Yes</option>
                  <option value="false" selected>No</option>
                </select>
              </div>
            </div>
            <div class="actions">
              <button type="submit" class="primary">Save</button>
              <button type="button" id="reset-btn" class="secondary">Reset</button>
              <button type="button" id="delete-btn" class="danger" disabled>Deactivate</button>
            </div>
          </form>
          <div class="status" id="status" role="status" aria-live="polite"></div>
        </section>

        <section class="card">
          <div class="card-title">
            <div>
              <h2>Configurations</h2>
              <p class="subtitle">Browse, filter, and select entries to edit.</p>
            </div>
            <div class="stats">
              <span class="stat-label">Total</span>
              <span class="stat-value" id="config-count">0</span>
            </div>
          </div>
          <div class="toolbar">
            <label class="search">
              <span>Search</span>
              <input id="search-input" placeholder="Alias, server, protocol..." />
            </label>
            <div class="toolbar-meta" id="last-refresh">Last synced: --</div>
          </div>
          <div class="table-wrapper" aria-live="polite">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Alias</th>
                  <th>Server</th>
                  <th>Protocol</th>
                  <th>Port</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody id="table-body"></tbody>
            </table>
            <div class="empty-state" id="empty-state">
              <h3>No configurations found</h3>
              <p>Adjust your search or create a new configuration using the editor.</p>
            </div>
          </div>
        </section>
      </div>
    </div>
  </body>
</html>
