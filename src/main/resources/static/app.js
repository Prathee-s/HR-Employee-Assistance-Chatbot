const token = localStorage.getItem('token');
if (!token) location.href = 'login.html';
const employee = JSON.parse(localStorage.getItem('employee') || '{}');

const API = '/api';
const chat = document.getElementById('chat');
const form = document.getElementById('form');
const input = document.getElementById('input');
const sendBtn = document.getElementById('send');

document.getElementById('who').textContent = employee.name ? `Hi, ${employee.name}` : '';
if (employee.role === 'HR') { const l = document.getElementById('hrLink'); l.style.display = 'inline'; l.href = 'hr.html'; }
document.getElementById('logout').onclick = () => { localStorage.clear(); location.href = 'login.html'; };

// One session id per browser; kept so a page refresh resumes the same conversation.
let sessionId = localStorage.getItem('sessionId');
if (!sessionId) { sessionId = crypto.randomUUID(); localStorage.setItem('sessionId', sessionId); }

function addMessage(role, text, opts = {}) {
  const div = document.createElement('div');
  div.className = 'msg ' + role;
  div.textContent = text;                       // textContent => no XSS from LLM output
  if (opts.sources && opts.sources.length) {
    const s = document.createElement('span');
    s.className = 'src'; s.textContent = '📋 Policy: ' + opts.sources.join(', ');
    div.appendChild(s);
  }
  chat.appendChild(div);
  chat.scrollTop = chat.scrollHeight;
  return div;
}

async function send(text) {
  addMessage('user', text);
  const typing = addMessage('bot typing', 'HRBot is typing...');
  sendBtn.disabled = true;
  try {
    const res = await fetch(API + '/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
      body: JSON.stringify({ sessionId, message: text })
    });
    typing.remove();
    if (res.status === 401) { localStorage.clear(); location.href = 'login.html'; return; }
    const data = await res.json();
    if (!res.ok) { addMessage('bot', '⚠️ ' + (data.error || 'Something went wrong.')); return; }
    addMessage('bot', data.reply, { sources: data.sources });
  } catch (e) {
    typing.remove();
    addMessage('bot', '⚠️ Could not reach the server. Please try again.');
  } finally { sendBtn.disabled = false; input.focus(); }
}

form.addEventListener('submit', e => {
  e.preventDefault();
  const text = input.value.trim();
  if (!text) return;
  input.value = '';
  document.getElementById('chips').style.display = 'none';
  send(text);
});
document.querySelectorAll('.chip').forEach(c => c.onclick = () => { input.value = c.textContent; form.requestSubmit(); });

document.getElementById('newChat').onclick = () => {
  sessionId = crypto.randomUUID(); localStorage.setItem('sessionId', sessionId);
  chat.innerHTML = ''; document.getElementById('chips').style.display = 'flex'; greet();
};

function greet() { addMessage('bot', `Hi ${employee.name || ''}! I'm HRBot. Ask me about your leave balance, attendance, holidays, or company policies.`); }

// restore history on load
(async () => {
  try {
    const res = await fetch(`${API}/chat/${sessionId}/history`, { headers: { 'Authorization': 'Bearer ' + token } });
    if (res.status === 401) { localStorage.clear(); location.href = 'login.html'; return; }
    const msgs = res.ok ? await res.json() : [];
    if (!msgs || !msgs.length) return greet();
    document.getElementById('chips').style.display = 'none';
    msgs.forEach(m => addMessage(m.role === 'user' ? 'user' : 'bot', m.content));
  } catch { greet(); }
})();
