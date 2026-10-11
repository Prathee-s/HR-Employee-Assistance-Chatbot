const token = localStorage.getItem('token');
const employee = JSON.parse(localStorage.getItem('employee') || '{}');
if (!token || employee.role !== 'HR') location.href = 'login.html';
document.getElementById('logout').onclick = () => { localStorage.clear(); location.href = 'login.html'; };

const auth = { headers: { 'Authorization': 'Bearer ' + token } };
const cell = txt => { const td = document.createElement('td'); td.textContent = txt; return td; };

async function loadLeaves() {
  const res = await fetch('/api/hr/leave/pending', auth);
  const rows = document.getElementById('leaveRows');
  rows.innerHTML = '';
  if (!res.ok) { rows.innerHTML = '<tr><td colspan="7">Could not load.</td></tr>'; return; }
  const list = await res.json();
  if (!list || !list.length) { rows.innerHTML = '<tr><td colspan="7">No pending requests 🎉</td></tr>'; return; }
  list.forEach(l => {
    const tr = document.createElement('tr');
    tr.appendChild(cell(l.id));
    tr.appendChild(cell(l.employeeId));
    tr.appendChild(cell(l.leaveType));
    tr.appendChild(cell(`${l.startDate} → ${l.endDate}`));
    tr.appendChild(cell(l.days));
    tr.appendChild(cell(l.reason || '-'));
    const actionsTd = document.createElement('td');
    const approve = document.createElement('button'); approve.className = 'action-btn approve'; approve.textContent = 'Approve';
    const reject = document.createElement('button'); reject.className = 'action-btn reject'; reject.textContent = 'Reject';
    const decide = async (status) => { await fetch(`/api/hr/leave/${l.id}/decision?status=${status}`, { method: 'PATCH', ...auth }); loadLeaves(); };
    approve.onclick = () => decide('APPROVED');
    reject.onclick = () => decide('REJECTED');
    actionsTd.append(approve, reject);
    tr.appendChild(actionsTd);
    rows.appendChild(tr);
  });
}

async function loadEscalations() {
  const res = await fetch('/api/hr/escalations', auth);
  const rows = document.getElementById('escRows');
  rows.innerHTML = '';
  if (!res.ok) { rows.innerHTML = '<tr><td colspan="6">Could not load.</td></tr>'; return; }
  const list = await res.json();
  if (!list || !list.length) { rows.innerHTML = '<tr><td colspan="6">No escalations 🎉</td></tr>'; return; }
  list.forEach(e => {
    const tr = document.createElement('tr');
    tr.appendChild(cell(e.id));
    tr.appendChild(cell(e.employeeId));
    tr.appendChild(cell(e.query));
    const sTd = document.createElement('td');
    const badge = document.createElement('span'); badge.className = 'badge ' + e.status; badge.textContent = e.status;
    sTd.appendChild(badge); tr.appendChild(sTd);
    tr.appendChild(cell(new Date(e.createdAt).toLocaleString()));
    const actionsTd = document.createElement('td');
    if (e.status !== 'RESOLVED') {
      const resolve = document.createElement('button'); resolve.className = 'action-btn approve'; resolve.textContent = 'Mark resolved';
      resolve.onclick = async () => { await fetch(`/api/hr/escalations/${e.id}/resolve`, { method: 'PATCH', ...auth }); loadEscalations(); };
      actionsTd.appendChild(resolve);
    }
    tr.appendChild(actionsTd);
    rows.appendChild(tr);
  });
}

loadLeaves(); loadEscalations();
