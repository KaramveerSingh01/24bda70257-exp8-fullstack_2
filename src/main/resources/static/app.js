const eventsBody = document.getElementById('eventsBody');
const dlqBox = document.getElementById('dlqBox');
const messageBox = document.getElementById('messageBox');

async function fetchJson(url, options) {
  const response = await fetch(url, options);
  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || 'Request failed');
  }
  return response.json();
}

function renderEvents(events) {
  if (!events.length) {
    eventsBody.innerHTML = '<tr><td colspan="5" class="empty">No events yet.</td></tr>';
    return;
  }

  eventsBody.innerHTML = events
    .slice().reverse()
    .map(event => `
      <tr>
        <td>${event.eventId}</td>
        <td>${event.message}</td>
        <td>${event.attempts}</td>
        <td><span class="status ${event.status}">${event.status}</span></td>
        <td>${event.retryCount}</td>
      </tr>
    `)
    .join('');
}

function renderDlq(items) {
  if (!items.length) {
    dlqBox.innerHTML = '<p class="empty">No dead-lettered events.</p>';
    return;
  }

  dlqBox.innerHTML = items.map(item => `
    <div class="dlq-item">
      <div>
        <strong>${item.eventId}</strong><br/>
        <small>${item.message}</small>
      </div>
      <button data-id="${item.eventId}">Replay</button>
    </div>
  `).join('');

  document.querySelectorAll('[data-id]').forEach(button => {
    button.addEventListener('click', async () => {
      try {
        const result = await fetchJson(`/api/dlq/${button.dataset.id}/replay`, { method: 'POST' });
        messageBox.textContent = `Replay sent: ${result.eventId}`;
        await refresh();
      } catch (error) {
        messageBox.textContent = error.message;
      }
    });
  });
}

async function refresh() {
  try {
    const [stats, events, dlq] = await Promise.all([
      fetchJson('/api/stats'),
      fetchJson('/api/events'),
      fetchJson('/api/dlq')
    ]);

    document.getElementById('published').textContent = stats.published;
    document.getElementById('processed').textContent = stats.processed;
    document.getElementById('retries').textContent = stats.retries;
    document.getElementById('deadLetters').textContent = stats.deadLetters;

    renderEvents(events);
    renderDlq(dlq);
  } catch (error) {
    messageBox.textContent = 'Could not connect to backend: ' + error.message;
  }
}

document.getElementById('eventForm').addEventListener('submit', async (event) => {
  event.preventDefault();
  const eventId = document.getElementById('eventId').value.trim();
  const message = document.getElementById('message').value.trim();
  const failAttempts = Number(document.getElementById('failAttempts').value);

  if (!message) {
    messageBox.textContent = 'Please enter a message';
    return;
  }

  try {
    const result = await fetchJson('/api/events', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ eventId, message, failAttempts })
    });

    document.getElementById('eventId').value = result.eventId;
    messageBox.textContent = `Published ${result.eventId} with status ${result.status}`;
    await refresh();
  } catch (error) {
    messageBox.textContent = error.message;
  }
});

refresh();
setInterval(refresh, 2000);
