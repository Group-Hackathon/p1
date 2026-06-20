const journeyData = {
  days: [
    {
      day: 1, date: 'Jun 13', status: 'completed', time: '08:32',
      temp: 37.8, feeling: 6,
      note: 'Wound is sore.',
      aiText: 'Fever detected. Monitor closely.',
    },
    {
      day: 2, date: 'Jun 14', status: 'completed', time: '09:10',
      temp: 37.5, feeling: 5,
      note: 'Slept better last night.',
      aiText: 'Temperature improving. Keep wound clean.',
    },
    {
      day: 3, date: 'Jun 15', status: 'completed', time: '08:45',
      temp: 37.2, feeling: 4,
      note: 'Looking less red today.',
      aiText: 'Good progress. Redness is reducing.',
    },
    {
      day: 4, date: 'Jun 16', status: 'completed', time: '08:20',
      temp: 37.0, feeling: 3,
      note: 'Able to walk easily now.',
      aiText: 'Normal temp range. Keep walking.',
    },
    {
      day: 5, date: 'Jun 17', status: 'completed', time: '09:05',
      temp: 36.8, feeling: 2,
      note: 'Almost completely normal.',
      aiText: 'Excellent recovery trajectory.',
    },
    { day: 6, date: 'Jun 18', status: 'today' },
    { day: 7, date: 'Jun 19', status: 'future' },
    { day: 8, date: 'Jun 20', status: 'future' },
  ],
};

function renderTimeline() {
  const container = document.getElementById('timelineContainer');
  let html = '';

  const reversedDays = [...journeyData.days].reverse();

  reversedDays.forEach(day => {
    let sideClass = day.day % 2 !== 0 ? 'left-side' : 'right-side';
    
    if (day.status === 'completed') {
      html += `
        <div class="tl-day-block completed ${sideClass}">
          <div style="position:absolute; top:0; bottom:0; left:50%; width:4px; background:var(--black); transform:translateX(-50%); z-index:1;"></div>
          <div class="tl-content-wrapper">
             <div class="tl-dot"></div>
             <div class="tl-content-card">
                <div class="tl-date-header">Day ${day.day} — ${day.date}</div>
                <div class="tl-metrics">
                   <div class="metric"><span class="metric-val">${day.temp.toFixed(1)}</span><span class="metric-unit">°C</span></div>
                   <div class="metric"><span class="metric-val">${day.feeling}</span><span class="metric-unit">/10 PAIN</span></div>
                </div>
                <div class="tl-note">${day.note}</div>
                <div class="tl-ai-box">
                   <div class="tl-ai-header">Assistant</div>
                   <div class="tl-ai-text">${day.aiText}</div>
                </div>
             </div>
          </div>
        </div>
      `;
    } else if (day.status === 'today') {
      html += `
        <div class="tl-day-block center-side today">
          <div style="position:absolute; top:0; height:50%; left:50%; border-left:2px dashed var(--gray-400); transform:translateX(-50%); z-index:1;"></div>
          <div style="position:absolute; bottom:0; height:50%; left:50%; width:4px; background:var(--black); transform:translateX(-50%); z-index:1;"></div>
          <div class="tl-content-wrapper">
            <div class="today-card">
              <div class="today-badge">TODAY — Day ${day.day}</div>
              <div class="today-title">Action Required</div>
              <div class="inline-input-group">
                 <label>Add a note or measurement</label>
                 <input type="text" class="inline-input" placeholder="E.g. Temp is 37.1..." />
              </div>
              <button class="btn-primary" onclick="alert('Saving measurement...')">Submit Check-in</button>
            </div>
          </div>
        </div>
      `;
    } else if (day.status === 'future') {
      html += `
        <div class="tl-day-block ${sideClass} future">
          <div style="position:absolute; top:0; bottom:0; left:50%; border-left:2px dashed var(--gray-400); transform:translateX(-50%); z-index:1;"></div>
          <div class="tl-content-wrapper">
             <div class="tl-dot"></div>
             <div class="tl-content-card">
                <div class="tl-date-header">Day ${day.day}</div>
                <div class="tl-future-text">Scheduled</div>
             </div>
          </div>
        </div>
      `;
    }
  });

  // STARTING POINT (Oldest event) at the bottom
  html += `
    <div class="tl-day-block endpoint">
      <div style="position:absolute; top:0; height:50%; left:50%; width:4px; background:var(--black); transform:translateX(-50%); z-index:1;"></div>
      <div class="tl-content-wrapper">
         <div class="tl-dot"></div>
         <div class="tl-endpoint-label">Started Tracking — Jun 13</div>
      </div>
    </div>
  `;

  container.innerHTML = html;
  
  // Auto scroll to today
  setTimeout(() => {
    const todayCard = document.querySelector('.tl-day-block.center-side');
    if(todayCard) {
      container.scrollTo({
        top: todayCard.offsetTop - 120,
        behavior: 'smooth'
      });
    }
  }, 100);
}

function openPdfPreview() {
  document.getElementById('pdfModal').classList.add('active');
  drawChart();
}

function closePdfPreview() {
  document.getElementById('pdfModal').classList.remove('active');
}

function drawChart() {
  const canvas = document.getElementById('previewChart');
  const ctx = canvas.getContext('2d');
  
  const dpr = window.devicePixelRatio || 1;
  const rect = canvas.getBoundingClientRect();
  canvas.width = rect.width * dpr;
  canvas.height = rect.height * dpr;
  ctx.scale(dpr, dpr);
  const w = rect.width;
  const h = rect.height;
  
  ctx.clearRect(0,0,w,h);
  
  const temps = journeyData.days.filter(d => d.temp).map(d => d.temp);
  const minT = 36.5, maxT = 38.0;
  
  const points = temps.map((t, i) => ({
    x: 10 + (i / (temps.length - 1)) * (w - 20),
    y: h - 10 - ((t - minT) / (maxT - minT)) * (h - 20)
  }));
  
  ctx.beginPath();
  ctx.moveTo(points[0].x, points[0].y);
  for(let i=1; i<points.length; i++) {
    const xc = (points[i-1].x + points[i].x) / 2;
    const yc = (points[i-1].y + points[i].y) / 2;
    ctx.quadraticCurveTo(points[i-1].x, points[i-1].y, xc, yc);
  }
  ctx.lineTo(points[points.length-1].x, points[points.length-1].y);
  
  ctx.strokeStyle = '#000';
  ctx.lineWidth = 3;
  ctx.lineCap = 'round';
  ctx.lineJoin = 'round';
  ctx.stroke();
  
  points.forEach(p => {
    ctx.beginPath();
    ctx.arc(p.x, p.y, 5, 0, Math.PI*2);
    ctx.fillStyle = '#fff';
    ctx.fill();
    ctx.lineWidth = 2;
    ctx.stroke();
  });
}

// Initialize
renderTimeline();
