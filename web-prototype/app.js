// ═══════════════════════════════════════════════════════
// Living Patient Memory — Web Prototype Logic
// ═══════════════════════════════════════════════════════

const state = {
  currentScreen: 'onboarding',
  onboardingSlide: 1,
  user: {
    name: 'Alex',
    email: 'alex@example.com',
  },
  devices: {
    watch: false,
    thermometer: true,
    bp: false,
    scale: false,
  },
  trackings: [],
  selectedTracking: null,

  // New tracking wizard state
  newTracking: {
    step: 1,
    description: '',
    appointmentDate: '',
    doctorName: '',
    rules: {
      temperature: true,
      pain: true,
      photos: true,
      smartwatch: false,
      bloodPressure: false,
      custom: '',
    },
    aiPlan: null,
  },

  // Daily routine state
  routineStep: 1,
  routineData: {
    painLevel: 3,
    hasRedness: false,
    hasDischarge: false,
    notes: '',
  },
};

// ═══ DRAWER ═══
function toggleDrawer() {
  const drawer = document.getElementById('mainDrawer');
  const overlay = document.getElementById('drawerOverlay');
  if (drawer.classList.contains('open')) {
    drawer.classList.remove('open');
    overlay.classList.remove('visible');
  } else {
    drawer.classList.add('open');
    overlay.classList.add('visible');
    renderDrawerList();
  }
}

function renderDrawerList() {
  const list = document.getElementById('drawerTrackingsList');
  if (!list) return;
  
  if (state.trackings.length === 0) {
    list.innerHTML = `<div style="font-size:13px; color:var(--gray-400); padding:12px;">No active trackings.</div>`;
    return;
  }
  
  list.innerHTML = state.trackings.map((t, i) => `
    <div class="drawer-item ${state.selectedTracking === i && state.currentScreen === 'detail' ? 'active' : ''}" onclick="openTracking(${i}); toggleDrawer()">
      <div style="flex:1; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;">${t.title}</div>
      <div class="tracking-badge ${t.isActive ? '' : 'tracking-badge--done'}" style="font-size:9px; padding:2px 6px;">
        ${t.isActive ? `${t.daysLeft}d` : 'Done'}
      </div>
    </div>
  `).join('');
}

// ═══ NAVIGATION ═══
function goTo(screenId) {
  document.querySelectorAll('.screen').forEach(s => s.classList.remove('active'));
  const target = document.getElementById('screen-' + screenId);
  if (target) target.classList.add('active');

  document.querySelectorAll('.pov-nav button').forEach(b => {
    b.classList.toggle('active', b.dataset.screen === screenId);
  });

  state.currentScreen = screenId;

  // Screen-specific init
  if (screenId === 'home') updateHome();
  if (screenId === 'new-tracking') renderNewTrackingStep();
  if (screenId === 'detail') updateDetail();
  if (screenId === 'routine') { state.routineStep = 1; renderRoutineStep(); }
  if (screenId === 'profile') updateProfile();
}

// ═══ ONBOARDING ═══
const onboardingNext = document.getElementById('onboardingNext');
onboardingNext?.addEventListener('click', () => {
  if (state.onboardingSlide < 2) {
    state.onboardingSlide++;
    updateOnboardingSlide();
  } else {
    goTo('home');
  }
});

function updateOnboardingSlide() {
  document.querySelectorAll('.onboarding-slide').forEach(s => s.classList.remove('active'));
  document.querySelectorAll('.onboarding-dots .dot').forEach(d => d.classList.remove('active'));

  const slide = document.querySelector(`.onboarding-slide[data-slide="${state.onboardingSlide}"]`);
  const dot = document.querySelector(`.dot[data-dot="${state.onboardingSlide}"]`);
  if (slide) slide.classList.add('active');
  if (dot) dot.classList.add('active');

  if (state.onboardingSlide === 2) {
    onboardingNext.textContent = 'Start a new tracking';
  } else {
    onboardingNext.textContent = 'Next';
  }
}

// ═══ HOME ═══
function updateHome() {
  const emptyState = document.getElementById('emptyState');
  const trackingList = document.getElementById('trackingList');
  const countdownCard = document.getElementById('countdownCard');
  const todayNudge = document.getElementById('todayNudge');
  const nameEl = document.getElementById('homeUserName');

  if (nameEl) nameEl.textContent = state.user.name;

  const initials = state.user.name.split(' ').map(w => w[0]).join('').toUpperCase();
  const avatar = document.getElementById('homeAvatar');
  if (avatar) avatar.textContent = initials;

  if (state.trackings.length === 0) {
    emptyState.style.display = 'flex';
    trackingList.style.display = 'none';
    countdownCard.style.display = 'none';
    todayNudge.style.display = 'none';
  } else {
    emptyState.style.display = 'none';
    trackingList.style.display = 'block';

    // Show countdown for first active tracking
    const active = state.trackings.find(t => t.isActive);
    if (active) {
      countdownCard.style.display = 'block';
      todayNudge.style.display = 'block';
      const apptDate = new Date(active.appointmentDate);
      document.getElementById('countdownDate').textContent =
        apptDate.toLocaleDateString('en-US', { month: 'long', day: 'numeric', year: 'numeric' }) +
        (active.doctorName ? ` — ${active.doctorName}` : '');
      updateCountdown(apptDate);
    } else {
      countdownCard.style.display = 'none';
      todayNudge.style.display = 'none';
    }

    // Render tracking cards
    trackingList.innerHTML = state.trackings.map((t, i) => `
      <div class="tracking-card" onclick="openTracking(${i})">
        <div class="tracking-card-header">
          <div>
            <div class="tracking-card-title">${t.title}</div>
            <div class="tracking-card-sub">Day ${t.currentDay} of ${t.totalDays}</div>
          </div>
          <button class="btn-icon" style="color:var(--gray-400)" onclick="event.stopPropagation(); deleteTrackingIndex(${i})">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="20" height="20"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path></svg>
          </button>
        </div>
        <div class="progress-bar"><div class="progress-fill" style="width:${t.progress}%"></div></div>
        <div class="tracking-card-footer">
          <span>${new Date(t.appointmentDate).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}</span>
          <span>${t.isActive ? 'View plan →' : 'View summary →'}</span>
        </div>
      </div>
    `).join('');
  }
}

function updateCountdown(targetDate) {
  const now = new Date();
  const diff = targetDate - now;
  if (diff <= 0) return;
  const days = Math.floor(diff / (1000 * 60 * 60 * 24));
  const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
  const mins = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
  document.getElementById('cdDays').textContent = days;
  document.getElementById('cdHours').textContent = String(hours).padStart(2, '0');
  document.getElementById('cdMins').textContent = String(mins).padStart(2, '0');
}

function openTracking(index) {
  state.selectedTracking = index;
  goTo('detail');
}

function deleteTrackingIndex(index) {
  state.trackings.splice(index, 1);
  updateHome();
  showToast('Tracking deleted');
}

// ═══ NEW TRACKING WIZARD ═══
function renderNewTrackingStep() {
  const step = state.newTracking.step;
  const container = document.getElementById('newTrackingContent');
  const indicator = document.getElementById('stepIndicator');
  const count = document.getElementById('stepCount');

  // Update step bars
  indicator.innerHTML = [1, 2, 3, 4].map(i =>
    `<div class="step-bar ${i <= step ? 'done' : ''}"></div>`
  ).join('');
  count.textContent = `Step ${step} of 4`;

  switch (step) {
    case 1: renderStep1(container); break;
    case 2: renderStep2(container); break;
    case 3: renderStep3(container); break;
    case 4: renderStep4(container); break;
  }
}

function renderStep1(el) {
  el.innerHTML = `
    <h2 style="font-size:22px; font-weight:800; margin-bottom:8px;">Describe your situation</h2>
    <p style="font-size:14px; color:var(--gray-500); margin-bottom:20px; line-height:1.6;">
      Tell us what you want to track. Be as specific as you'd like — the more detail, the better your personalized plan.
    </p>
    <div class="field">
      <label for="symptomInput">What's going on?</label>
      <textarea id="symptomInput" placeholder="e.g. I have a wound on my left knee that was stitched 3 days ago. Some redness around the edges, moderate pain. I want to track the healing before my follow-up appointment..."
      >${state.newTracking.description}</textarea>
      <div class="hint">Describe symptoms, location, duration, concerns</div>
    </div>
    <div class="field">
      <label for="doctorInput">Doctor's name (optional)</label>
      <input type="text" id="doctorInput" placeholder="e.g. Dr. Martinez" value="${state.newTracking.doctorName}" />
    </div>
    <button class="btn-primary btn-primary--accent" onclick="nextWizardStep()" id="step1Btn" ${state.newTracking.description ? '' : 'disabled'}>
      Continue
    </button>
  `;

  const textarea = document.getElementById('symptomInput');
  const btn = document.getElementById('step1Btn');
  textarea?.addEventListener('input', () => {
    state.newTracking.description = textarea.value;
    btn.disabled = !textarea.value.trim();
  });
  document.getElementById('doctorInput')?.addEventListener('input', (e) => {
    state.newTracking.doctorName = e.target.value;
  });
}

function renderStep2(el) {
  const today = new Date();
  const minDate = new Date(today.getTime() + 86400000).toISOString().split('T')[0];
  const maxDate = new Date(today.getTime() + 180 * 86400000).toISOString().split('T')[0];

  el.innerHTML = `
    <h2 style="font-size:22px; font-weight:800; margin-bottom:8px;">When is your appointment?</h2>
    <p style="font-size:14px; color:var(--gray-500); margin-bottom:20px; line-height:1.6;">
      Enter the exact date of your next medical appointment. We'll build a tracking plan that prepares everything your doctor needs by that day.
    </p>
    <div class="field">
      <label for="apptDateInput">Appointment date</label>
      <div class="date-input-wrapper">
        <input type="date" id="apptDateInput" min="${minDate}" max="${maxDate}" value="${state.newTracking.appointmentDate}" />
      </div>
      <div class="hint">Your plan will be optimized for this date</div>
    </div>
    <div class="info-card">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>
      <p>You can change the date later at any time from the tracking detail screen.</p>
    </div>
    <div style="display:flex; gap:10px;">
      <button class="btn-secondary" style="flex:1;" onclick="prevWizardStep()">Back</button>
      <button class="btn-primary btn-primary--accent" style="flex:2;" onclick="nextWizardStep()" id="step2Btn" ${state.newTracking.appointmentDate ? '' : 'disabled'}>
        Continue
      </button>
    </div>
  `;

  const dateInput = document.getElementById('apptDateInput');
  const btn = document.getElementById('step2Btn');
  dateInput?.addEventListener('change', () => {
    state.newTracking.appointmentDate = dateInput.value;
    btn.disabled = !dateInput.value;
  });
}

function renderStep3(el) {
  const r = state.newTracking.rules;
  el.innerHTML = `
    <h2 style="font-size:22px; font-weight:800; margin-bottom:8px;">Your tracking rules</h2>
    <p style="font-size:14px; color:var(--gray-500); margin-bottom:20px; line-height:1.6;">
      Choose what you want to monitor. Toggle on the measurements you can do — the AI will adapt your plan accordingly.
    </p>

    <div class="toggle-row" onclick="toggleRule(this, 'temperature')">
      <div class="toggle-row-left">
        <div>
          <div class="toggle-label">Temperature</div>
          <div class="toggle-sublabel">I take my temperature with a device</div>
        </div>
      </div>
      <div class="toggle ${r.temperature ? 'on' : ''}" data-rule="temperature"><div class="toggle-knob"></div></div>
    </div>

    <div class="toggle-row" onclick="toggleRule(this, 'pain')">
      <div class="toggle-row-left">
        <div>
          <div class="toggle-label">Pain tracking</div>
          <div class="toggle-sublabel">Daily pain level and location</div>
        </div>
      </div>
      <div class="toggle ${r.pain ? 'on' : ''}" data-rule="pain"><div class="toggle-knob"></div></div>
    </div>

    <div class="toggle-row" onclick="toggleRule(this, 'photos')">
      <div class="toggle-row-left">
        <div>
          <div class="toggle-label">Daily photos</div>
          <div class="toggle-sublabel">Guided photo capture of the area</div>
        </div>
      </div>
      <div class="toggle ${r.photos ? 'on' : ''}" data-rule="photos"><div class="toggle-knob"></div></div>
    </div>

    <div class="toggle-row" onclick="toggleRule(this, 'smartwatch')">
      <div class="toggle-row-left">
        <div>
          <div class="toggle-label">Smartwatch data</div>
          <div class="toggle-sublabel">Heart rate, steps, sleep</div>
        </div>
      </div>
      <div class="toggle ${r.smartwatch ? 'on' : ''}" data-rule="smartwatch"><div class="toggle-knob"></div></div>
    </div>

    <div class="toggle-row" onclick="toggleRule(this, 'bloodPressure')">
      <div class="toggle-row-left">
        <div>
          <div class="toggle-label">Blood pressure</div>
          <div class="toggle-sublabel">Manual or connected BP readings</div>
        </div>
      </div>
      <div class="toggle ${r.bloodPressure ? 'on' : ''}" data-rule="bloodPressure"><div class="toggle-knob"></div></div>
    </div>

    <div class="field" style="margin-top:12px;">
      <label for="customRule">Anything else to track?</label>
      <input type="text" id="customRule" placeholder="e.g. appetite, swelling, medication times…" value="${r.custom}" />
    </div>

    <div style="display:flex; gap:10px; margin-top:8px;">
      <button class="btn-secondary" style="flex:1;" onclick="prevWizardStep()">Back</button>
      <button class="btn-primary btn-primary--accent" style="flex:2;" onclick="nextWizardStep()">
        Generate my plan
      </button>
    </div>
  `;

  document.getElementById('customRule')?.addEventListener('input', (e) => {
    state.newTracking.rules.custom = e.target.value;
  });
}

function renderStep4(el) {
  // Simulate AI generating a plan
  const appt = new Date(state.newTracking.appointmentDate);
  const now = new Date();
  const totalDays = Math.max(1, Math.ceil((appt - now) / 86400000));
  const r = state.newTracking.rules;

  // Build plan items based on rules
  const planItems = [];
  if (r.photos) planItems.push({ title: 'Guided photo capture', desc: 'Morning — consistent framing for comparison' });
  if (r.pain) planItems.push({ title: 'Pain & symptom check-in', desc: 'Evening — pain scale, infection signs' });
  if (r.temperature) planItems.push({ title: 'Temperature log', desc: 'Every 8 hours while symptoms are present' });
  if (r.smartwatch) planItems.push({ title: 'Automatic health data', desc: 'Heart rate, steps, sleep — pulled daily' });
  if (r.bloodPressure) planItems.push({ title: 'Blood pressure reading', desc: 'Morning and evening' });
  if (r.custom) planItems.push({ title: r.custom, desc: 'Custom tracking — daily entry' });

  state.newTracking.aiPlan = { totalDays, planItems };

  el.innerHTML = `
    <div class="ai-card">
      <div class="ai-card-header">
        <span>AI-Generated Plan</span>
      </div>
      <div class="ai-card-title">${totalDays}-day personalized tracking</div>
      <div class="ai-card-body">
        Based on your description, appointment date, and tracking rules, here's your optimized plan${state.newTracking.doctorName ? ` for ${state.newTracking.doctorName}` : ''}.
      </div>
    </div>

    <div class="section-label" style="margin-top:0">Daily routine (under 2 minutes)</div>
    <div class="plan-list">
      ${planItems.map(item => `
        <div class="plan-item" style="padding-left:16px;">
          <div class="plan-text">
            <h4>${item.title}</h4>
            <p>${item.desc}</p>
          </div>
        </div>
      `).join('')}
    </div>

    <div class="info-card" style="margin-top:16px;">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>
      <p>On appointment day, your doctor will receive a complete briefing with all collected data, trends, and photos — ready in under 2 minutes.</p>
    </div>

    <div style="display:flex; gap:10px; margin-top:16px;">
      <button class="btn-secondary" style="flex:1;" onclick="prevWizardStep()">Back</button>
      <button class="btn-primary btn-primary--accent" style="flex:2;" onclick="startTracking()">
        Start my tracking
      </button>
    </div>
  `;
}

function nextWizardStep() {
  if (state.newTracking.step < 4) {
    state.newTracking.step++;
    renderNewTrackingStep();
  }
}

function prevWizardStep() {
  if (state.newTracking.step > 1) {
    state.newTracking.step--;
    renderNewTrackingStep();
  }
}

function toggleRule(row, key) {
  state.newTracking.rules[key] = !state.newTracking.rules[key];
  const toggle = row.querySelector('.toggle');
  toggle.classList.toggle('on', state.newTracking.rules[key]);
}

function startTracking() {
  const nt = state.newTracking;
  const appt = new Date(nt.appointmentDate);
  const now = new Date();
  const totalDays = Math.max(1, Math.ceil((appt - now) / 86400000));

  // Generate a title from the description
  const desc = nt.description.toLowerCase();
  let title = 'Health Tracking';
  if (desc.includes('wound') || desc.includes('cut') || desc.includes('stitch')) title = 'Wound Monitoring';
  else if (desc.includes('fever') || desc.includes('temperature')) title = 'Fever Tracking';
  else if (desc.includes('rash') || desc.includes('skin') || desc.includes('eczema')) title = 'Skin Follow-up';
  else if (desc.includes('pain') || desc.includes('knee') || desc.includes('back') || desc.includes('sprain')) title = 'Pain & Recovery';
  else if (desc.includes('surgery') || desc.includes('operation')) title = 'Post-Surgery Recovery';

  const tracking = {
    title,
    description: nt.description,
    doctorName: nt.doctorName,
    appointmentDate: nt.appointmentDate,
    rules: { ...nt.rules },
    plan: nt.aiPlan,
    totalDays,
    currentDay: 1,
    daysLeft: totalDays - 1,
    progress: Math.round((1 / totalDays) * 100),
    isActive: true,
    createdAt: new Date().toISOString(),
    messages: [
      { type: 'user', date: 'Day 1 - Morning', text: 'Started the tracking plan.' },
    ]
  };

  state.trackings.push(tracking);
  state.selectedTracking = state.trackings.length - 1;

  // Reset wizard
  state.newTracking = {
    step: 1, description: '', appointmentDate: '', doctorName: '',
    rules: { temperature: true, pain: true, photos: true, smartwatch: false, bloodPressure: false, custom: '' },
    aiPlan: null,
  };

  showToast('Tracking started! Your first routine is ready.');
  goTo('detail');
}

// ═══ TRACKING DETAIL ═══
function updateDetail() {
  const t = state.trackings[state.selectedTracking];
  if (!t) return;

  document.getElementById('detailTitle').textContent = t.title;
  document.getElementById('detApptDate').textContent =
    new Date(t.appointmentDate).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });

  // Build timeline
  const vt = document.getElementById('vtContainer');
  if (!vt) return;
  let html = '';
  t.messages.forEach((m, idx) => {
    const side = (idx % 2 === 0) ? 'left' : 'right';
    html += `
      <div class="vt-node ${side}">
        <div class="vt-dot"></div>
        <div class="vt-bubble">
          <div class="vt-date">${m.date || 'Record'}</div>
          <div class="vt-text">${m.text}</div>
        </div>
      </div>
    `;
  });
  
  // Render Current Pending Card
  if (t.isActive && !t.routineDoneToday) {
    html += `
      <div class="vt-input-wrapper">
        <div class="vt-input-card">
          <h3>Evening Check-in</h3>
          <p>What is your current pain level?</p>
          <div style="display:flex; gap:8px;">
            <input type="range" min="0" max="10" value="3" style="flex:1" id="vtQuickPain" />
            <span style="font-weight:bold; width:20px; text-align:right;" id="vtQuickPainVal">3</span>
          </div>
          <div class="vt-chat-row">
            <input type="text" id="vtChatInput" placeholder="Or type a message..." />
            <button onclick="submitVtForm()">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg>
            </button>
          </div>
        </div>
      </div>
    `;
  } else {
    html += `
      <div class="vt-input-wrapper">
        <div style="background:var(--gray-100); padding:12px 24px; border-radius:20px; font-size:13px; color:var(--gray-500); font-weight:600;">
          All caught up for today!
        </div>
      </div>
    `;
  }
  
  vt.innerHTML = html;
  
  const painSlider = document.getElementById('vtQuickPain');
  if (painSlider) {
    painSlider.oninput = (e) => {
      document.getElementById('vtQuickPainVal').textContent = e.target.value;
    };
  }
  
  // Scroll to bottom
  window.scrollTo(0, document.body.scrollHeight);
}

function submitVtForm() {
  const t = state.trackings[state.selectedTracking];
  if (!t) return;
  
  const chatInput = document.getElementById('vtChatInput').value.trim();
  const pain = document.getElementById('vtQuickPain').value;
  
  let msgText = chatInput ? chatInput : `Pain level recorded at ${pain}/10.`;
  
  t.messages.push({
    type: 'user',
    text: msgText,
    date: `Day ${t.currentDay} - Evening`
  });
  
  t.routineDoneToday = true;
  t.currentDay = Math.min(t.currentDay + 1, t.totalDays);
  t.daysLeft = Math.max(0, t.daysLeft - 1);
  
  // Simulate AI response
  setTimeout(() => {
    t.messages.push({ type: 'ai', date: `Day ${t.currentDay - 1} - Assistant`, text: "I've noted that down in your medical journal." });
    updateDetail();
  }, 1000);
}

function toggleManageModal() {
  const modal = document.getElementById('manageModal');
  modal.classList.toggle('visible');
}

function showDateEditModal() {
  document.getElementById('manageModal').classList.remove('visible');
  const modal = document.getElementById('dateEditModal');
  modal.classList.add('visible');
  const t = state.trackings[state.selectedTracking];
  if (t) {
    document.getElementById('editApptDate').value = t.appointmentDate;
  }
}

function hideDateEditModal() {
  document.getElementById('dateEditModal').classList.remove('visible');
}

function saveNewDate() {
  const newDate = document.getElementById('editApptDate').value;
  if (!newDate) return;
  const t = state.trackings[state.selectedTracking];
  if (t) {
    t.appointmentDate = newDate;
    const now = new Date();
    const appt = new Date(newDate);
    t.totalDays = Math.max(1, Math.ceil((appt - new Date(t.createdAt)) / 86400000));
    t.daysLeft = Math.max(0, Math.ceil((appt - now) / 86400000));
    t.progress = Math.round((t.currentDay / t.totalDays) * 100);
  }
  hideDateEditModal();
  updateDetail();
  showToast('Appointment date updated');
}

function deleteTracking() {
  if (state.selectedTracking !== null) {
    state.trackings.splice(state.selectedTracking, 1);
    state.selectedTracking = null;
  }
  document.getElementById('manageModal').classList.remove('visible');
  showToast('Tracking deleted');
  goTo('home');
}

// ═══ DAILY ROUTINE ═══
function renderRoutineStep() {
  const step = state.routineStep;
  const container = document.getElementById('routineContent');
  const t = state.trackings[state.selectedTracking];
  const rules = t?.rules || state.newTracking.rules;

  // Count total steps based on rules
  const steps = [];
  if (rules.photos) steps.push('photo');
  if (rules.pain) steps.push('checkin');
  if (rules.temperature) steps.push('temperature');
  steps.push('done');
  const totalSteps = steps.length;
  const currentStepType = steps[step - 1] || 'done';

  // Update step indicator
  const indicator = document.getElementById('routineStepIndicator');
  indicator.innerHTML = steps.map((_, i) =>
    `<div class="step-bar ${i < step ? 'done' : ''}"></div>`
  ).join('');
  document.getElementById('routineStepCount').textContent = `Step ${Math.min(step, totalSteps)} of ${totalSteps}`;

  switch (currentStepType) {
    case 'photo':
      container.innerHTML = `
        <div class="routine-step">
          <div class="routine-step-header">
            <div class="routine-step-num">1</div>
            <div class="routine-step-title">Take your daily photo</div>
          </div>
          <div style="background:var(--gray-200); border-radius:var(--radius); height:280px; display:flex; align-items:center; justify-content:center; flex-direction:column; gap:12px; margin-bottom:16px;">
            <div style="width:200px; height:200px; border:2px dashed var(--gray-400); border-radius:var(--radius); display:flex; align-items:center; justify-content:center; opacity:.6;">
              <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="var(--gray-400)" stroke-width="1.5"><path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/><circle cx="12" cy="13" r="4"/></svg>
            </div>
            <span style="font-size:13px; color:var(--gray-500);">Camera preview — align with frame</span>
          </div>
          <p style="font-size:13px; color:var(--gray-500); margin-bottom:16px;">Position the area to track inside the frame. Keep the same distance and angle as previous days.</p>
          <button class="btn-primary btn-primary--accent" onclick="nextRoutineStep()">Take photo</button>
        </div>
      `;
      break;

    case 'checkin':
      container.innerHTML = `
        <div class="routine-step">
          <div class="routine-step-header">
            <div class="routine-step-num">${step}</div>
            <div class="routine-step-title">Pain & symptom check-in</div>
          </div>

          <div style="margin-bottom:20px;">
            <label style="font-size:14px; font-weight:600; margin-bottom:8px; display:block;">Pain level today</label>
            <div class="slider-wrapper">
              <div class="slider-labels"><span>None</span><span>Severe</span></div>
              <input type="range" min="0" max="10" value="${state.routineData.painLevel}" id="painSlider" />
              <div class="slider-value" id="painValue">${state.routineData.painLevel} / 10</div>
            </div>
          </div>

          <div style="margin-bottom:16px;">
            <label style="font-size:14px; font-weight:600; margin-bottom:8px; display:block;">Is redness spreading?</label>
            <div class="yn-group">
              <button class="yn-btn ${!state.routineData.hasRedness ? 'selected' : ''}" onclick="setRedness(false)">No</button>
              <button class="yn-btn ${state.routineData.hasRedness ? 'selected' : ''}" onclick="setRedness(true)">Yes</button>
            </div>
          </div>

          <div style="margin-bottom:16px;">
            <label style="font-size:14px; font-weight:600; margin-bottom:8px; display:block;">Any discharge or unusual odor?</label>
            <div class="yn-group">
              <button class="yn-btn ${!state.routineData.hasDischarge ? 'selected' : ''}" onclick="setDischarge(false)">No</button>
              <button class="yn-btn ${state.routineData.hasDischarge ? 'selected' : ''}" onclick="setDischarge(true)">Yes</button>
            </div>
          </div>

          <div class="field" style="margin-top:16px;">
            <label for="routineNotes">Anything else to note?</label>
            <input type="text" id="routineNotes" placeholder="e.g. took ibuprofen at 3pm, slept poorly…" />
          </div>

          <button class="btn-primary btn-primary--accent" onclick="nextRoutineStep()">Save & continue</button>
        </div>
      `;

      const slider = document.getElementById('painSlider');
      slider?.addEventListener('input', () => {
        state.routineData.painLevel = parseInt(slider.value);
        document.getElementById('painValue').textContent = slider.value + ' / 10';
      });
      break;

    case 'temperature':
      container.innerHTML = `
        <div class="routine-step">
          <div class="routine-step-header">
            <div class="routine-step-num">${step}</div>
            <div class="routine-step-title">Temperature reading</div>
          </div>
          <div class="field">
            <label for="tempInput">Current temperature</label>
            <div style="display:flex; gap:8px; align-items:center;">
              <input type="number" id="tempInput" placeholder="37.2" step="0.1" style="flex:1;" />
              <span style="font-size:18px; font-weight:700; color:var(--gray-500);">°C</span>
            </div>
          </div>
          <div class="field">
            <label>How did you measure?</label>
            <div class="yn-group">
              <button class="yn-btn selected" onclick="selectMeasure(this)">Oral</button>
              <button class="yn-btn" onclick="selectMeasure(this)">Ear</button>
              <button class="yn-btn" onclick="selectMeasure(this)">Forehead</button>
            </div>
          </div>
          <button class="btn-primary btn-primary--accent" style="margin-top:12px;" onclick="nextRoutineStep()">Save & continue</button>
        </div>
      `;
      break;

    case 'done':
    default:
      // Update tracking progress
      if (t) {
        t.currentDay = Math.min(t.currentDay + 1, t.totalDays);
        t.daysLeft = Math.max(0, t.daysLeft - 1);
        t.progress = Math.round((t.currentDay / t.totalDays) * 100);
      }
      goTo('routine-done');
      break;
  }
}

function nextRoutineStep() {
  state.routineStep++;
  renderRoutineStep();
}

function setRedness(val) {
  state.routineData.hasRedness = val;
  document.querySelectorAll('.yn-group')[0]?.querySelectorAll('.yn-btn').forEach((btn, i) => {
    btn.classList.toggle('selected', (i === 0 && !val) || (i === 1 && val));
  });
}

function setDischarge(val) {
  state.routineData.hasDischarge = val;
  document.querySelectorAll('.yn-group')[1]?.querySelectorAll('.yn-btn').forEach((btn, i) => {
    btn.classList.toggle('selected', (i === 0 && !val) || (i === 1 && val));
  });
}

function selectMeasure(btn) {
  btn.parentElement.querySelectorAll('.yn-btn').forEach(b => b.classList.remove('selected'));
  btn.classList.add('selected');
}

// ═══ PROFILE ═══
function updateProfile() {
  const nameInput = document.getElementById('profileNameInput');
  if (nameInput) {
    nameInput.value = state.user.name;
    nameInput.onchange = (e) => {
      state.user.name = e.target.value;
      updateProfileAvatar();
    };
  }
  updateProfileAvatar();
}

function updateProfileAvatar() {
  const initials = (state.user.name || 'A').split(' ').map(w => w[0] || '').join('').toUpperCase();
  const avatarLg = document.getElementById('profileAvatarLg');
  if (avatarLg) avatarLg.textContent = initials;
}

function toggleDevice(row) {
  const toggle = row.querySelector('.toggle');
  const device = toggle.dataset.device;
  if (device) {
    state.devices[device] = !state.devices[device];
    toggle.classList.toggle('on', state.devices[device]);
    showToast(state.devices[device] ? 'Device connected' : 'Device disconnected');
  }
}

// ═══ TOAST ═══
let toastTimeout;
function showToast(message) {
  const toast = document.getElementById('toast');
  toast.textContent = message;
  toast.classList.add('visible');
  clearTimeout(toastTimeout);
  toastTimeout = setTimeout(() => toast.classList.remove('visible'), 2500);
}

// ═══ POV PANEL NAVIGATION ═══
document.querySelectorAll('.pov-nav button').forEach(btn => {
  btn.addEventListener('click', () => {
    const screen = btn.dataset.screen;
    // Don't show routine screen directly from nav anymore
    if (screen === 'routine') return showToast('Routines are now handled in the chat detail screen');
    goTo(screen);
  });
});

// ═══ INIT ═══
updateHome();
