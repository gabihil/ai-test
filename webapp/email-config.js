const form = document.getElementById('config-form');
const tableBody = document.getElementById('table-body');
const statusEl = document.getElementById('status');
const deleteBtn = document.getElementById('delete-btn');
const resetBtn = document.getElementById('reset-btn');
const refreshBtn = document.getElementById('refresh-btn');
const searchInput = document.getElementById('search-input');
const configCount = document.getElementById('config-count');
const emptyState = document.getElementById('empty-state');
const formMode = document.getElementById('form-mode');
const lastRefresh = document.getElementById('last-refresh');

const fields = [
  'alias', 'protocol', 'server', 'port', 'username', 'password',
  'clientId', 'locationId', 'assignToGroup', 'ticketType',
  'defaultPostedBy', 'active', 'addReply',
];

const input = (id) => document.getElementById(id);
let cachedConfigs = [];

const setStatus = (message, isError = false) => {
  statusEl.textContent = message;
  statusEl.classList.toggle('error', isError);
  statusEl.classList.toggle('success', !isError);
};

const fetchJson = async (url, options = {}) => {
  const response = await fetch(url, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || 'Request failed');
  }
  return data;
};

const setLoading = (isLoading) => {
  document.body.classList.toggle('loading', isLoading);
};

const updateLastRefresh = () => {
  const timestamp = new Date().toLocaleTimeString();
  lastRefresh.textContent = `Last synced: ${timestamp}`;
};

const renderRows = (configs) => {
  tableBody.innerHTML = '';
  configs.forEach((config) => {
    const row = document.createElement('tr');
    const appendCell = (value, label) => {
      const cell = document.createElement('td');
      cell.textContent = value ?? '';
      cell.dataset.label = label;
      row.appendChild(cell);
    };

    appendCell(config.id, 'ID');
    appendCell(config.alias, 'Alias');
    appendCell(config.server, 'Server');
    appendCell(config.protocol, 'Protocol');
    appendCell(config.port ?? '', 'Port');

    const statusCell = document.createElement('td');
    statusCell.dataset.label = 'Status';
    const badge = document.createElement('span');
    badge.className = config.active ? 'badge' : 'badge inactive';
    badge.textContent = config.active ? 'Active' : 'Inactive';
    statusCell.appendChild(badge);
    row.appendChild(statusCell);
    row.addEventListener('click', () => fillForm(config));
    tableBody.appendChild(row);
  });

  configCount.textContent = configs.length;
  emptyState.style.display = configs.length ? 'none' : 'block';
};

const applySearch = () => {
  const term = searchInput.value.trim().toLowerCase();
  if (!term) {
    renderRows(cachedConfigs);
    return;
  }
  const filtered = cachedConfigs.filter((config) => (
    [config.alias, config.server, config.protocol, config.id]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(term))
  ));
  renderRows(filtered);
};

const loadConfigs = async () => {
  setLoading(true);
  try {
    const configs = await fetchJson('api');
    cachedConfigs = configs;
    renderRows(configs);
    updateLastRefresh();
  } finally {
    setLoading(false);
  }
};

const fillForm = (config) => {
  input('config-id').value = config.id;
  fields.forEach((field) => {
    input(field).value = config[field] ?? '';
  });
  deleteBtn.disabled = !config.id;
  formMode.textContent = 'Editing';
  document.querySelectorAll('tbody tr').forEach((row) => {
    row.classList.toggle('active', row.firstChild?.textContent == config.id);
  });
};

const resetForm = () => {
  form.reset();
  input('config-id').value = '';
  deleteBtn.disabled = true;
  formMode.textContent = 'New';
  document.querySelectorAll('tbody tr').forEach((row) => {
    row.classList.remove('active');
  });
};

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  const payload = {};
  fields.forEach((field) => {
    payload[field] = input(field).value;
  });

  const numericFields = [
    'assignToGroup', 'clientId', 'defaultPostedBy',
    'locationId', 'port', 'ticketType',
  ];
  numericFields.forEach((field) => {
    payload[field] = payload[field] ? Number(payload[field]) : null;
  });
  payload.active = payload.active === 'true';
  payload.addReply = payload.addReply === 'true';

  const id = input('config-id').value;
  try {
    setLoading(true);
    if (id) {
      await fetchJson(id, { method: 'PUT', body: JSON.stringify(payload) });
      setStatus('Configuration updated.');
    } else {
      await fetchJson('', { method: 'POST', body: JSON.stringify(payload) });
      setStatus('Configuration created.');
    }
    await loadConfigs();
    resetForm();
  } catch (error) {
    setStatus(error.message, true);
  } finally {
    setLoading(false);
  }
});

deleteBtn.addEventListener('click', async () => {
  const id = input('config-id').value;
  if (!id) return;
  try {
    setLoading(true);
    await fetchJson(id, { method: 'DELETE' });
    setStatus('Configuration deactivated.');
    await loadConfigs();
    resetForm();
  } catch (error) {
    setStatus(error.message, true);
  } finally {
    setLoading(false);
  }
});

resetBtn.addEventListener('click', resetForm);
refreshBtn.addEventListener('click', () => loadConfigs().catch((error) => setStatus(error.message, true)));
searchInput.addEventListener('input', applySearch);

loadConfigs().catch((error) => setStatus(error.message, true));
