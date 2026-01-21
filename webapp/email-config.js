const form = document.getElementById('config-form');
const tableBody = document.getElementById('table-body');
const statusEl = document.getElementById('status');
const deleteBtn = document.getElementById('delete-btn');
const resetBtn = document.getElementById('reset-btn');

const fields = [
  'alias', 'protocol', 'server', 'port', 'username', 'password',
  'clientId', 'locationId', 'assignToGroup', 'ticketType',
  'defaultPostedBy', 'active', 'addReply',
];

const input = (id) => document.getElementById(id);

const setStatus = (message, isError = false) => {
  statusEl.textContent = message;
  statusEl.style.color = isError ? '#d32f2f' : '#1b5e20';
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

const loadConfigs = async () => {
  const configs = await fetchJson('api');
  tableBody.innerHTML = '';
  configs.forEach((config) => {
    const row = document.createElement('tr');
    row.innerHTML = `
      <td>${config.id}</td>
      <td>${config.alias}</td>
      <td>${config.server}</td>
      <td>${config.protocol}</td>
      <td>${config.port ?? ''}</td>
      <td><span class="badge">${config.active ? 'Active' : 'Inactive'}</span></td>
    `;
    row.addEventListener('click', () => fillForm(config));
    tableBody.appendChild(row);
  });
};

const fillForm = (config) => {
  input('config-id').value = config.id;
  fields.forEach((field) => {
    input(field).value = config[field] ?? '';
  });
  deleteBtn.disabled = !config.id;
};

const resetForm = () => {
  form.reset();
  input('config-id').value = '';
  deleteBtn.disabled = true;
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
  }
});

deleteBtn.addEventListener('click', async () => {
  const id = input('config-id').value;
  if (!id) return;
  try {
    await fetchJson(id, { method: 'DELETE' });
    setStatus('Configuration deactivated.');
    await loadConfigs();
    resetForm();
  } catch (error) {
    setStatus(error.message, true);
  }
});

resetBtn.addEventListener('click', resetForm);

loadConfigs().catch((error) => setStatus(error.message, true));
