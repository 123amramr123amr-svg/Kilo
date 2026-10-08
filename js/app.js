/* منارة — تطبيق مجموع منهج الصف الثالث الثانوي 2027 */
const $ = s => document.querySelector(s);
const $$ = s => [...document.querySelectorAll(s)];
const LS = 'manara-t3-2027-v1';
const DAY_NAMES = ['الأحد','الاثنين','الثلاثاء','الأربعاء','الخميس','الجمعة','السبت'];

function defState(){
  return {
    branch:'science',
    progress:{},
    schedule:[],
    alarms:[],
    planDays:[6,0,1,2,3,4],
    planPrefs:{},
    settings:{name:'', wake:'06:30', sleep:'23:30', examDate:'', sessionLen:45, breakLen:10, lunch:'13:00', dinner:'19:30', bedtimeRemind:true, dailyCap:6},
    ai:{endpoint:'https://api.openai.com/v1/chat/completions', key:'', model:'gpt-4o-mini'},
    log:[]
  };
}
let S = load();
function load(){ try{ const s=Object.assign(defState(), JSON.parse(localStorage.getItem(LS)||'{}')); s.settings=Object.assign(defState().settings, s.settings||{}); return s; }catch(e){ return defState(); } }
function save(){ localStorage.setItem(LS, JSON.stringify(S)); }

/* ---------- helpers ---------- */
function allSubjects(){ return window.CURRICULUM.filter(s=>s.branch==='both'||s.branch===S.branch); }
function findSubject(id){ return window.CURRICULUM.find(s=>s.id===id); }
function findUnit(uid){ for(const s of window.CURRICULUM){ const u=s.units.find(u=>u.id===uid); if(u) return {sub:s, unit:u}; } return null; }
function findLesson(lid){ for(const s of window.CURRICULUM){ for(const u of s.units){ const l=u.lessons.find(l=>l.id===lid); if(l) return {sub:s, unit:u, lesson:l}; } } return null; }
function esc(s){ return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;'); }
function toast(msg){ const t=$('#toast'); t.textContent=msg; t.classList.add('show'); clearTimeout(t._tm); t._tm=setTimeout(()=>t.classList.remove('show'),2800); }
function subjectProgress(s){
  const tot = s.units.reduce((n,u)=>n+u.lessons.length,0);
  const read = s.units.reduce((n,u)=>n+u.lessons.filter(l=>S.progress[l.id]&&S.progress[l.id].done).length,0);
  return {tot, read, pct: tot?Math.round(read/tot*100):0};
}
function nextLesson(){
  const subs = allSubjects();
  let best=null, score=-1;
  subs.forEach(s=>s.units.forEach(u=>u.lessons.forEach(l=>{
    const p=S.progress[l.id]||{};
    let sc;
    if(p.quiz==null && !p.done) sc=100;
    else if(p.quiz!=null && p.quiz<60) sc=90+(60-p.quiz);
    else if(p.quiz!=null && p.quiz<80) sc=40;
    else if(!p.done) sc=30;
    else sc=1;
    if(sc>score){ score=sc; best=l; }
  })));
  return best;
}

/* ---------- audio ---------- */
let AC=null;
function tone(f=880,d=.2,type='sine',vol=.18){
  try{
    AC = AC || new (window.AudioContext||window.webkitAudioContext)();
    const o=AC.createOscillator(), g=AC.createGain();
    o.type=type; o.frequency.value=f;
    g.gain.setValueAtTime(vol, AC.currentTime);
    g.gain.exponentialRampToValueAtTime(.001, AC.currentTime+d);
    o.connect(g); g.connect(AC.destination);
    o.start(); o.stop(AC.currentTime+d);
  }catch(e){}
}
function alarmSound(){ [0,1,2,3,4].forEach(i=>setTimeout(()=>tone(i%2?990:660,.32,'triangle',.22), i*380)); }

/* ---------- router ---------- */
function route(){
  const h = location.hash.slice(2);
  const parts = h.split('/');
  const view = $('#view');
  window.scrollTo(0,0);
  $$('#main-nav a').forEach(a=>a.classList.remove('active'));
  const navMap = {'':'home','subject':'home','lesson':'home','quiz':'home','plans':'plans','timer':'timer','alarms':'alarms','schedule':'schedule'};
  const navKey = navMap[parts[0]]||'home';
  const navEl = $('#main-nav a[data-nav="'+navKey+'"]'); if(navEl) navEl.classList.add('active');
  if(!h) return viewHome();
  if(parts[0]==='subject') return viewSubject(parts[1]);
  if(parts[0]==='lesson') return viewLesson(parts[1]);
  if(parts[0]==='quiz') return viewQuiz(parts[1], decodeURIComponent(parts[2]||''), parts[3]);
  if(parts[0]==='plans') return viewPlans();
  if(parts[0]==='timer') return viewTimer();
  if(parts[0]==='alarms') return viewAlarms();
  if(parts[0]==='schedule') return viewSchedule();
  viewHome();
}
window.addEventListener('hashchange', route);

/* ---------- HOME ---------- */
function viewHome(){
  const subs = allSubjects();
  let totRead=0, totAll=0;
  subs.forEach(s=>{ const p=subjectProgress(s); totRead+=p.read; totAll+=p.tot; });
  const overall = totAll?Math.round(totRead/totAll*100):0;
  const nl = nextLesson();
  const nlInfo = nl ? findLesson(nl.id) : null;
  const today = new Date().getDay();
  const todaySessions = (S.plan && S.plan.planDays && S.plan.planDays[today]) || [];
  const q = (location.hash.split('?')[1]||'');
  $('#view').innerHTML = `
  <div class="hero">
    <div class="ring" style="background:conic-gradient(var(--accent) ${overall*3.6}deg, var(--card) 0)">
      <div style="text-align:center"><b>${overall}%</b><small>مُنجَز</small></div>
    </div>
    <div>
      <h2>أهلاً ${S.settings.name?esc(S.settings.name):'بطل الثانوية'} 👋</h2>
      <p>منارة — مجموع منهج الصف الثالث الثانوي (2027): شرح مبسط لكل مادة ووحدة ودرس، اختبارات لكل درس ووحدة ومنهج كامل، وخطط مذاكرة ذكية تراعي وقت نومك.</p>
      <div style="display:flex;gap:10px;flex-wrap:wrap;margin-top:12px">
        <a class="btn big" href="${nl?('#/lesson/'+nl.id):'#/'}">⚡ ابدأ المذاكرة الآن</a>
        <a class="btn big sec" href="#/plans">📅 خطتي الذكية</a>
      </div>
    </div>
  </div>

  <div class="form-row" style="align-items:center">
    <div class="field">
      <label>الشعبة</label>
      <select id="branchSel">
        <option value="science" ${S.branch==='science'?'selected':''}>علمي علوم</option>
        <option value="arts" ${S.branch==='arts'?'selected':''}>أدبي</option>
      </select>
    </div>
    <div class="field" style="flex:1;min-width:220px">
      <label>ابحث عن درس أو مادة</label>
      <input class="search" style="margin:0" id="searchBox" placeholder="مثال: النهايات، كولوم، البلاغة...">
    </div>
  </div>
  <div id="searchResults"></div>

  ${todaySessions.length?`<h2 class="section-title">⏰ خطة اليوم (${DAY_NAMES[today]})</h2>
  <div>${todaySessions.map(se=>`
    <div class="session" style="border-right-color:${se.color||'var(--accent)'}">
      <span class="time">${se.start} - ${se.end}</span>
      <span class="what"><b>${esc(se.subject)} — ${esc(se.lessonTitle)}</b><small>${esc(se.unitName||'')} · ${se.kind}</small></span>
      <a class="btn sm sec" href="#/lesson/${se.lessonId}">فتح الدرس</a>
    </div>`).join('')}</div>`:''}

  <h2 class="section-title">المواد ${S.branch==='science'?'(الشعبة العلمية)':'(الشعبة الأدبية)'}</h2>
  <div class="grid cols-3">
    ${subs.map(s=>{
      const p=subjectProgress(s);
      const weak = s.units.flatMap(u=>u.lessons).filter(l=>{const q=(S.progress[l.id]||{}).quiz; return q!=null&&q<60;}).length;
      return `<a class="card hover" href="#/subject/${s.id}" style="border-top:4px solid ${s.color}">
        <h3>${s.icon} ${s.name}</h3>
        <p>${s.desc}</p>
        <div style="display:flex;justify-content:space-between;align-items:center;margin:10px 0 6px">
          <span class="chip">${p.read}/${p.tot} درس</span>
          ${weak?`<span class="badge bad">${weak} دروس ضعيفة</span>`:''}
        </div>
        <div class="progress"><i style="width:${p.pct}%"></i></div>
        <div style="display:flex;justify-content:space-between;margin-top:8px">
          <small style="color:var(--muted)">${p.pct}%</small>
          <small style="color:var(--accent)">ابدأ ←</small>
        </div>
      </a>`;
    }).join('')}
  </div>`;

  $('#branchSel').onchange = e=>{ S.branch=e.target.value; save(); route(); };
  $('#searchBox').oninput = e=>{
    const v=e.target.value.trim();
    const box=$('#searchResults');
    if(!v){ box.innerHTML=''; return; }
    const res=[];
    allSubjects().forEach(s=>{
      if(s.name.includes(v)) res.push({type:'subject', s});
      s.units.forEach(u=>u.lessons.forEach(l=>{ if(l.title.includes(v)) res.push({type:'lesson', s, u, l}); }));
    });
    box.innerHTML = res.length ? `<h2 class="section-title">نتائج البحث (${res.length})</h2>
      <div class="grid cols-2">${res.slice(0,30).map(r=> r.type==='subject'
        ? `<a class="card hover" href="#/subject/${r.s.id}"><h3>${r.s.icon} ${r.s.name}</h3><p>مادة كاملة — ${r.s.desc}</p></a>`
        : `<a class="card hover" href="#/lesson/${r.l.id}" style="border-right:4px solid ${r.s.color}"><h3>${r.s.icon} ${r.l.title}</h3><p>${r.s.name} · ${esc(r.u.name)}</p></a>`).join('')}</div>`
      : `<div class="empty">لا نتائج لـ "${esc(v)}" — جرّب كلمة أخرى</div>`;
  };
}

/* ---------- SUBJECT ---------- */
function viewSubject(sid){
  const s = findSubject(sid);
  if(!s) return viewHome();
  const p = subjectProgress(s);
  $('#view').innerHTML = `
  <div class="crumbs"><a href="#/">الرئيسية</a> ‹ <span>${s.name}</span></div>
  <div class="hero" style="border-right:6px solid ${s.color}">
    <div style="flex:1">
      <h2>${s.icon} ${s.name}</h2>
      <p>${s.desc}</p>
      <div style="display:flex;gap:10px;align-items:center;flex-wrap:wrap;margin-top:10px">
        <span class="chip">${p.read}/${p.tot} درس مقروء</span>
        <span class="chip">${p.pct}%</span>
        <a class="btn sm" href="#/quiz/final/${s.id}">🎓 اختبار المنهج كامل</a>
      </div>
      <div class="progress" style="margin-top:12px"><i style="width:${p.pct}%"></i></div>
    </div>
  </div>
  ${s.units.map((u,ui)=>{
    const utot=u.lessons.length, uread=u.lessons.filter(l=>S.progress[l.id]&&S.progress[l.id].done).length;
    const uexam = (s.uexam && s.uexam[u.id]) ? s.uexam[u.id].length : 0;
    return `<h2 class="section-title">${esc(u.name)} <span class="chip">${uread}/${utot}</span></h2>
    <div class="grid cols-2">
      ${u.lessons.map(l=>{
        const prog=S.progress[l.id]||{};
        const badge = prog.quiz!=null ? (prog.quiz>=80?`<span class="badge ok">ممتاز ${prog.quiz}%</span>`:prog.quiz>=60?`<span class="badge warn">جيد ${prog.quiz}%</span>`:`<span class="badge bad">ضعيف ${prog.quiz}%</span>`) : '';
        return `<div class="card hover" style="border-right:4px solid ${s.color};cursor:pointer" onclick="location.hash='#/lesson/${l.id}'">
          <div style="display:flex;justify-content:space-between;align-items:flex-start;gap:8px">
            <div><h3>${ui+1}.${u.lessons.indexOf(l)+1} ${l.title}</h3><p>${l.p.length} نقطة أساسية · ${l.q.length} أسئلة</p></div>
            ${prog.done?'<span class="badge ok">✓ مقروء</span>':''}
          </div>
          <div style="display:flex;gap:6px;margin-top:10px;flex-wrap:wrap">
            ${badge}
            <a class="btn sm" href="#/quiz/lesson/${l.id}" onclick="event.stopPropagation()">اختبار الدرس</a>
          </div>
        </div>`;
      }).join('')}
      ${uexam?`<div class="card hover" style="border-right:4px solid var(--info);cursor:pointer" onclick="location.hash='#/quiz/unit/${u.id}'">
        <h3>📝 امتحان الوحدة</h3><p>${uexam} أسئلة تغطي كل الدرس — تحت ضغط الوقت</p>
        <div style="margin-top:10px"><a class="btn sm sec" href="#/quiz/unit/${u.id}">ابدأ امتحان الوحدة</a></div>
      </div>`:''}
    </div>`;
  }).join('')}`;
}

/* ---------- LESSON ---------- */
function viewLesson(lid){
  const info = findLesson(lid);
  if(!info) return viewHome();
  const {sub, unit, lesson} = info;
  const prog = S.progress[lid] || {};
  const idx = unit.lessons.findIndex(l=>l.id===lid);
  const prev = unit.lessons[idx-1], next = unit.lessons[idx+1];
  if(!prog.done){ S.progress[lid] = Object.assign({}, prog, {done:true}); save(); }
  $('#view').innerHTML = `
  <div class="crumbs">
    <a href="#/">الرئيسية</a> ‹ <a href="#/subject/${sub.id}">${sub.name}</a> ‹ <a href="#/subject/${sub.id}">${esc(unit.name)}</a> ‹ <span>${lesson.title}</span>
  </div>
  <div class="hero" style="border-right:6px solid ${sub.color}">
    <div style="flex:1">
      <h2>${sub.icon} ${lesson.title}</h2>
      <p>${esc(unit.name)} — ${sub.name}</p>
      <div style="display:flex;gap:8px;flex-wrap:wrap;margin-top:10px">
        <a class="btn sm" href="#/quiz/lesson/${lid}">📝 اختبار الدرس (${lesson.q.length} أسئلة)</a>
        ${prev?`<a class="btn sm sec" href="#/lesson/${prev.id}">← الدرس السابق</a>`:''}
        ${next?`<a class="btn sm sec" href="#/lesson/${next.id}">الدرس التالي ←</a>`:''}
        <a class="btn sm sec" href="#/subject/${sub.id}">العودة للمادة</a>
      </div>
    </div>
  </div>
  <h2 class="section-title">📖 الشرح المبسط</h2>
  <ul class="lesson-points">
    ${lesson.p.map(pt=>`<li>${pt}</li>`).join('')}
  </ul>
  <h2 class="section-title">💡 أمثلة محلولة</h2>
  ${lesson.ex.map(ex=>`<div class="ex-box">${ex}</div>`).join('')}
  <h2 class="section-title">📝 اختبر نفسك</h2>
  <div class="card">
    <p>بعد قراءة الشرح، اعمل اختبار الدرس لقياس فهمك — النتائج تُحفظ وتُستخدم في إنشاء خطتك الذكية.</p>
    <div style="display:flex;gap:10px;flex-wrap:wrap;margin-top:10px">
      <a class="btn" href="#/quiz/lesson/${lid}">ابدأ اختبار الدرس</a>
      <a class="btn sec" href="#/quiz/unit/${unit.id}">امتحان الوحدة</a>
    </div>
  </div>`;
}

/* ---------- QUIZ ENGINE ---------- */
let QZ = null;
function viewQuiz(type, id){
  let title='', list=[], subName='', icon='📝';
  if(type==='lesson'){
    const info=findLesson(id); if(!info) return viewHome();
    title='اختبار الدرس: '+info.lesson.title; list=info.lesson.q; subName=info.sub.name; icon=info.sub.icon;
  } else if(type==='unit'){
    const info=findUnit(id); if(!info) return viewHome();
    const s=info.sub; title='امتحان الوحدة: '+info.unit.name;
    list=(s.uexam&&s.uexam[id])||[]; subName=s.name; icon=s.icon;
    if(!list.length) list=info.unit.lessons.flatMap(l=>l.q);
  } else if(type==='final'){
    const s=findSubject(id); if(!s) return viewHome();
    title='اختبار المنهج كامل: '+s.name; list=s.final||[]; subName=s.name; icon=s.icon;
  }
  if(!list.length) return $('#view').innerHTML='<div class="empty">لا توجد أسئلة لهذا الاختبار</div>';
  QZ = {type, id, title, list, subName, icon, idx:0, score:0, answers:[], locked:false, startT:Date.now()};
  renderQuestion();
}
function renderQuestion(){
  const q = QZ.list[QZ.idx];
  const pct = Math.round(QZ.idx/QZ.list.length*100);
  $('#view').innerHTML = `
  <div class="crumbs"><a href="#/">الرئيسية</a> ‹ <span>${esc(QZ.subName)}</span> ‹ <span>${esc(QZ.title)}</span></div>
  <div class="progress" style="margin-bottom:14px"><i style="width:${pct}%"></i></div>
  <div style="display:flex;justify-content:space-between;color:var(--muted);font-size:.85rem;margin-bottom:10px">
    <span>السؤال ${QZ.idx+1} من ${QZ.list.length}</span><span>النتيجة: ${QZ.score}/${QZ.idx}</span>
  </div>
  <div class="quiz-q">
    <h3>${esc(q.q)}</h3>
    ${q.o.map((o,i)=>`<button class="opt" data-i="${i}">${String.fromCharCode(65+i)}. ${esc(o)}</button>`).join('')}
    <div id="qWhy"></div>
    <div style="display:flex;justify-content:space-between;gap:10px;margin-top:16px;flex-wrap:wrap">
      <span></span>
      <button class="btn" id="qNext" disabled>${QZ.idx<QZ.list.length-1?'السؤال التالي ←':'عرض النتيجة 🎯'}</button>
    </div>
  </div>`;
  $$('#view .opt').forEach(btn=>{
    btn.onclick = ()=>{
      if(QZ.locked) return; QZ.locked=true;
      const i=parseInt(btn.dataset.i);
      const ok = i===q.a;
      QZ.answers.push({i, ok});
      if(ok){ QZ.score++; tone(880,.15,'sine',.15); } else { tone(220,.3,'sawtooth',.12); }
      $$('#view .opt').forEach((b,bi)=>{
        b.disabled=true;
        if(bi===q.a) b.classList.add('correct');
        else if(bi===i) b.classList.add('wrong');
      });
      $('#qWhy').innerHTML = `<div class="why">${ok?'✓ إجابة صحيحة':'✗ الإجابة الصحيحة: '+String.fromCharCode(65+q.a)} — ${q.w||''}</div>`;
      $('#qNext').disabled=false;
    };
  });
  $('#qNext').onclick = ()=>{ QZ.idx++; QZ.locked=false; if(QZ.idx<QZ.list.length) renderQuestion(); else renderResult(); };
}
function renderResult(){
  const pct = Math.round(QZ.score/QZ.list.length*100);
  const timeS = Math.round((Date.now()-QZ.startT)/1000);
  const grade = pct>=90?'ممتاز 🏆':pct>=80?'جيد جدًا 🌟':pct>=60?'جيد 👍':pct>=40?'مقبول — راجع الشرح 📖':'ضعيف — ادرس الدرس أولًا 📚';
  // حفظ النتيجة
  if(QZ.type==='lesson'){
    const prev=(S.progress[QZ.id]||{}).quiz||0;
    S.progress[QZ.id]=Object.assign({},S.progress[QZ.id],{done:true,quiz:Math.max(prev,pct)});
  } else if(QZ.type==='unit'){
    const u=findUnit(QZ.id);
    if(u) u.unit.lessons.forEach(l=>{
      const prev=(S.progress[l.id]||{}).quiz||0;
      S.progress[l.id]=Object.assign({},S.progress[l.id],{done:true,quiz:Math.max(prev,pct)});
    });
  }
  save();
  const wrong = QZ.list.map((q,i)=>({q,i,a:QZ.answers[i]})).filter(x=>!x.a.ok);
  $('#view').innerHTML = `
  <div class="crumbs"><a href="#/">الرئيسية</a> ‹ <span>${esc(QZ.subName)}</span> ‹ <span>النتيجة</span></div>
  <div class="hero" style="border-right:6px solid ${pct>=60?'var(--ok)':'var(--bad)'}">
    <div class="ring" style="background:conic-gradient(${pct>=60?'var(--ok)':'var(--bad)'} ${pct*3.6}deg, var(--card) 0)">
      <div style="text-align:center"><b>${pct}%</b><small>${QZ.score}/${QZ.list.length}</small></div>
    </div>
    <div>
      <h2>${grade}</h2>
      <p>${esc(QZ.title)} — الوقت المستغرق: ${Math.floor(timeS/60)}د ${timeS%60}ث</p>
      <div style="display:flex;gap:10px;flex-wrap:wrap;margin-top:12px">
        <a class="btn" href="#/quiz/${QZ.type}/${encodeURIComponent(QZ.id)}">🔄 إعادة الاختبار</a>
        ${QZ.type==='lesson'?`<a class="btn sec" href="#/lesson/${QZ.id}">📖 مراجعة الشرح</a>`:''}
        <a class="btn sec" href="#/subject/${QZ.type==='final'?QZ.id:(QZ.type==='unit'?findUnit(QZ.id).sub.id:findLesson(QZ.id).sub.id)}">المادة</a>
      </div>
    </div>
  </div>
  ${wrong.length?`<h2 class="section-title">❌ مراجعة الأخطاء (${wrong.length})</h2>
  ${wrong.map(x=>`<div class="quiz-q" style="border-right:4px solid var(--bad)">
    <h3>${esc(x.q.q)}</h3>
    <div style="color:var(--muted);font-size:.9rem">إجابتك: ${String.fromCharCode(65+x.a.i)} — الصحيحة: <b style="color:var(--ok)">${String.fromCharCode(65+x.q.a)}</b></div>
    <div class="why">${x.q.w||''}</div>
    <a class="btn sm sec" href="#/lesson/${x.q.lessonId||''}" style="visibility:hidden">مراجعة</a>
  </div>`).join('')}`:'<div class="empty">🎉 كل الأسئلة صحيحة! أنت مستعد للاختبار التالي.</div>'}`;
  // ربط أخطاء الدروس بالمراجعة
  if(QZ.type==='lesson'){
    const links=$$('#view .quiz-q a[style*="hidden"]');
    links.forEach(a=>{ a.setAttribute('href','#/lesson/'+QZ.id); a.style.visibility='visible'; a.textContent='📖 مراجعة الدرس'; });
  } else {
    $$('#view .quiz-q a[style*="hidden"]').forEach(a=>a.remove());
  }
}

/* ---------- PLANS ---------- */
function viewPlans(){
  const st=S.settings;
  const subs=allSubjects();
  $('#view').innerHTML = `
  <div class="crumbs"><a href="#/">الرئيسية</a> ‹ <span>خطة المذاكرة</span></div>
  <h2 class="page-title">📅 منشئ خطة المذاكرة الذكية</h2>
  <div class="card">
    <h3>بياناتك (تُحفظ محليًا على جهازك فقط)</h3>
    <div class="form-row">
      <div class="field"><label>اسم الطالب</label><input id="pName" value="${esc(st.name)}" placeholder="اسمك"></div>
      <div class="field"><label>وقت الاستيقاظ ☀️</label><input type="time" id="pWake" value="${st.wake}"></div>
      <div class="field"><label>وقت النوم 🌙</label><input type="time" id="pSleep" value="${st.sleep}"></div>
      <div class="field"><label>موعد امتحان الثانوية</label><input type="date" id="pExam" value="${st.examDate||''}"></div>
    </div>
    <div class="form-row">
      <div class="field"><label>مدة الحصة (دقيقة)</label><input type="number" id="pLen" value="${st.sessionLen}" min="15" max="120" step="5"></div>
      <div class="field"><label>الاستراحة (دقيقة)</label><input type="number" id="pBreak" value="${st.breakLen}" min="0" max="30" step="5"></div>
      <div class="field"><label>حصص اليوم الأقصى</label><input type="number" id="pDaily" value="${st.dailyCap||6}" min="1" max="12"></div>
      <div class="field"><label>استراحة غداء</label><input type="time" id="pLunch" value="${st.lunch}"></div>
      <div class="field"><label>استراحة عشاء</label><input type="time" id="pDinner" value="${st.dinner}"></div>
    </div>
    <div class="form-row" style="align-items:flex-start">
      <div class="field" style="flex:1">
        <label>أيام الدراسة الأسبوعية</label>
        <div style="display:flex;gap:6px;flex-wrap:wrap">
          ${DAY_NAMES.map((d,i)=>`<label class="chip" style="cursor:pointer;padding:6px 12px"><input type="checkbox" class="pDay" value="${i}" ${S.planDays.includes(i)?'checked':''} style="accent-color:var(--accent)"> ${d}</label>`).join('')}
        </div>
      </div>
    </div>
    <h3 style="margin-top:14px">أولويات المواد (1 = منخفضة، 5 = قصوى) — المواد الضعيفة تُرفع تلقائيًا</h3>
    <div class="form-row">
      ${subs.map(s=>{
        const p=subjectProgress(s); const pref=(S.planPrefs[s.id]??3);
        return `<div class="field" style="min-width:150px"><label>${s.icon} ${s.name} <span class="badge ${p.pct>=80?'ok':p.pct>=40?'warn':'bad'}">${p.pct}%</span></label>
        <select class="pPref" data-sid="${s.id}">${[1,2,3,4,5].map(n=>`<option value="${n}" ${n===pref?'selected':''}>${n} — ${['منخفضة','متوسطة','جيدة','عالية','قصوى'][n-1]}</option>`).join('')}</select></div>`;
      }).join('')}
    </div>
    <div style="display:flex;gap:10px;flex-wrap:wrap;margin-top:16px">
      <button class="btn big" id="buildPlan">🧠 إنشاء الخطة الذكية</button>
      <button class="btn big sec" id="aiPlan">🤖 توليد بالذكاء الاصطناعي (API)</button>
    </div>
    <div id="aiForm" style="display:none;margin-top:14px">
      <div class="note">التوليد بالذكاء الاصطناعي اختياري ويحتاج مفتاح API (مثل OpenAI) — بياناتك تُرسل فقط عند الضغط، ولن يعمل بدون مفتاح. الخطة الذكية المحلية تعمل بدون إنترنت بالكامل.</div>
      <div class="form-row">
        <div class="field" style="flex:1"><label>رابط API</label><input id="aiEndpoint" value="${esc(S.ai.endpoint)}"></div>
        <div class="field"><label>Model</label><input id="aiModel" value="${esc(S.ai.model)}"></div>
        <div class="field"><label>مفتاح API</label><input type="password" id="aiKey" value="${esc(S.ai.key)}" placeholder="sk-..."></div>
      </div>
    </div>
  </div>
  <div id="planOutput" style="margin-top:20px">
    ${S.plan && !S.plan.empty ? renderPlan(S.plan) : '<div class="empty">📌 اضغط "إنشاء الخطة الذكية" لإنشاء خطتك الأسبوعية — تُحسب من وقت نومك، مواعيد دروسك، وتحصيلك الحالي.</div>'}
  </div>`;

  $('#buildPlan').onclick = ()=>{
    collectPlanInputs();
    const plan = buildPlan();
    if(plan.empty){ toast('لا توجد حصص متاحة — عدّل أوقاتك أو أيامك'); return; }
    S.plan = plan; save();
    $('#planOutput').innerHTML = renderPlan(plan);
    tone(880,.2); tone(1100,.25);
    toast('تم إنشاء خطتك الذكية ✅');
  };
  $('#aiPlan').onclick = ()=>{
    const f=$('#aiForm'); f.style.display = f.style.display==='none'?'block':'none';
    if(f.style.display==='block' && !$('#aiRun')){
      const runAI = async ()=>{
        const btn=$('#aiRun');
        btn.disabled=true; btn.textContent='⏳ جارٍ التوليد...';
        try{
          const text = await generateAIPlan($('#aiEndpoint').value, $('#aiKey').value, $('#aiModel').value);
          $('#aiOut').textContent = text;
          $('#aiOut').style.display='block';
          S.ai={endpoint:$('#aiEndpoint').value, key:$('#aiKey').value, model:$('#aiModel').value}; save();
        }catch(e){ $('#aiOut').textContent='⚠️ '+e.message; $('#aiOut').style.display='block'; }
        btn.disabled=false; btn.textContent='🤖 توليد الخطة الآن';
      };
      f.insertAdjacentHTML('beforeend', `<div class="form-row"><button class="btn" id="aiRun">🤖 توليد الخطة الآن</button></div><pre class="ai-out" id="aiOut" style="display:none"></pre>`);
      $('#aiRun').onclick = runAI;
    }
  };
}
function collectPlanInputs(){
  S.settings.name=$('#pName').value;
  S.settings.wake=$('#pWake').value||'06:30';
  S.settings.sleep=$('#pSleep').value||'23:30';
  S.settings.examDate=$('#pExam').value||'';
  S.settings.sessionLen=parseInt($('#pLen').value)||45;
  S.settings.breakLen=parseInt($('#pBreak').value)||10;
  S.settings.dailyCap=parseInt($('#pDaily').value)||6;
  S.settings.lunch=$('#pLunch').value||'13:00';
  S.settings.dinner=$('#pDinner').value||'19:30';
  S.planDays=$$('.pDay').filter(c=>c.checked).map(c=>parseInt(c.value));
  $$('.pPref').forEach(sel=>{ S.planPrefs[sel.dataset.sid]=parseInt(sel.value); });
  save();
}
function renderPlan(plan){
  const egOrder = [6,0,1,2,3,4,5]; // السبت..الجمعة
  const dayOrder = (S.planDays.length?S.planDays:Object.keys(plan.planDays).map(Number)).slice().sort((a,b)=>egOrder.indexOf(a)-egOrder.indexOf(b));
  return `
  <h2 class="page-title">📋 خطتك الأسبوعية</h2>
  <div class="note">${esc(plan.summary)}</div>
  ${dayOrder.map(d=>{
    const ses=plan.planDays[d]||[];
    if(!ses.length) return '';
    return `<div class="plan-day">
      <h3><span>📆 ${DAY_NAMES[d]}</span><span class="chip">${ses.length} حصص · ${fmtMin(ses.reduce((a,s)=>a+(t2m(s.end)-t2m(s.start)),0))}</span></h3>
      ${ses.map(s=>`<div class="session" style="border-right-color:${s.color}">
        <span class="time">${s.start} - ${s.end}</span>
        <span class="what"><b>${s.icon} ${esc(s.subject)} — ${esc(s.lessonTitle)}</b><small>${esc(s.unitName)} · ${s.kind}</small></span>
        <a class="btn sm" href="#/lesson/${s.lessonId}">فتح الدرس</a>
      </div>`).join('')}
    </div>`;
  }).join('')}
  <h2 class="section-title">💡 نصائح الخطة</h2>
  <ul class="tips">${plan.tips.map(t=>`<li>${esc(t)}</li>`).join('')}</ul>
  <div style="display:flex;gap:10px;flex-wrap:wrap" class="no-print">
    <button class="btn sec" id="copyPlan">📋 نسخ نص الخطة</button>
    <button class="btn sec" onclick="window.print()">🖨 طباعة الخطة</button>
  </div>`;
}
document.addEventListener('click', e=>{
  if(e.target && e.target.id==='copyPlan'){
    let txt='خطة المذاكرة الأسبوعية — منارة\n'+S.plan.summary+'\n\n';
    const egOrder = [6,0,1,2,3,4,5];
    const dayOrder = (S.planDays.length?S.planDays:Object.keys(S.plan.planDays).map(Number)).slice().sort((a,b)=>egOrder.indexOf(a)-egOrder.indexOf(b));
    dayOrder.forEach(d=>{
      const ses=S.plan.planDays[d]||[];
      if(!ses.length) return;
      txt+='== '+DAY_NAMES[d]+' ==\n';
      ses.forEach(s=>{ txt+=s.start+' - '+s.end+' | '+s.subject+' | '+s.lessonTitle+' ('+s.kind+')\n'; });
      txt+='\n';
    });
    txt+='نصائح:\n'+S.plan.tips.map(t=>'- '+t).join('\n');
    navigator.clipboard && navigator.clipboard.writeText(txt).then(()=>toast('تم نسخ الخطة ✅')).catch(()=>toast('تعذر النسخ'));
  }
});

/* ---------- TIMER ---------- */
let TM=null;
function viewTimer(){
  $('#view').innerHTML = `
  <h2 class="page-title">⏲ تايمر المذاكرة</h2>
  <div class="card" style="text-align:center">
    <div class="timer-display" id="tDisplay">25:00</div>
    <div id="tLabel" style="color:var(--muted);margin-bottom:14px">جلسة مذاكرة (بومودورو)</div>
    <div class="timer-controls">
      <button class="btn" id="tStart">▶ بدء</button>
      <button class="btn sec" id="tPause">⏸ إيقاف مؤقت</button>
      <button class="btn sec" id="tReset">↺ إعادة</button>
    </div>
    <div style="display:flex;gap:8px;justify-content:center;flex-wrap:wrap;margin:18px 0">
      ${[['بومودورو 25/5',25,5],['تركيز عميق 50/10',50,10],['مراجعة سريعة 15/3',15,3],['امتحان 45/0',45,0]].map(p=>`<button class="btn sm sec tPreset" data-w="${p[1]}" data-b="${p[2]}">${p[0]}</button>`).join('')}
    </div>
    <div class="form-row" style="justify-content:center">
      <div class="field"><label>دقائق العمل</label><input type="number" id="tWork" value="25" min="1" max="180"></div>
      <div class="field"><label>دقائق الاستراحة</label><input type="number" id="tRest" value="5" min="0" max="60"></div>
    </div>
    <div class="chip" id="tCycles">دورات مكتملة اليوم: ${(S.log||[]).filter(l=>l.type==='timer'&&l.date===new Date().toDateString()).length}</div>
  </div>
  <h2 class="section-title">📓 سجل جلسات اليوم</h2>
  <div class="card" id="tLog">${renderTimerLog()}</div>`;
  let remain=25*60, total=25*60, rest=5*60, mode='work', timer=null, cycles=0;
  const disp=$('#tDisplay'), lbl=$('#tLabel');
  const paint=()=>{ const m=Math.floor(remain/60), s=remain%60; disp.textContent=String(m).padStart(2,'0')+':'+String(s).padStart(2,'0'); document.title=disp.textContent+' — منارة'; };
  const stop=()=>{ clearInterval(timer); timer=null; $('#tStart').textContent='▶ بدء'; };
  const finish=()=>{
    alarmSound();
    if(mode==='work'){
      cycles++;
      S.log.push({type:'timer', date:new Date().toDateString(), mins:Math.round(total/60)});
      save();
      $('#tCycles').textContent='دورات مكتملة اليوم: '+((S.log||[]).filter(l=>l.type==='timer'&&l.date===new Date().toDateString()).length);
      $('#tLog').innerHTML=renderTimerLog();
      mode='rest'; remain=rest; lbl.textContent = rest>0?'استراحة — قف وتحرك قليلاً 🌿':'استراحة قصيرة';
      toast('انتهت جلسة المذاكرة! خذ استراحة 🌿');
    } else {
      mode='work'; remain=total; lbl.textContent='جلسة مذاكرة (بومودورو)';
      toast('ابدأ جلسة جديدة 💪');
    }
    document.title='منارة';
  };
  $('#tStart').onclick=()=>{
    if(timer){ stop(); return; }
    $('#tStart').textContent='⏹ إيقاف';
    timer=setInterval(()=>{
      if(remain<=1){ finish(); return; }
      remain--; paint();
    },1000);
  };
  $('#tPause').onclick=()=>{ if(timer){ stop(); toast('إيقاف مؤقت'); } };
  $('#tReset').onclick=()=>{ stop(); mode='work'; remain=total; paint(); };
  $$('.tPreset').forEach(b=>b.onclick=()=>{
    stop(); mode='work';
    total=parseInt(b.dataset.w)*60; rest=parseInt(b.dataset.b)*60; remain=total;
    $('#tWork').value=b.dataset.w; $('#tRest').value=b.dataset.b;
    lbl.textContent='جلسة مذاكرة ('+b.dataset.w+' دقيقة)'; paint();
  });
  $('#tWork').onchange=e=>{ stop(); mode='work'; total=Math.max(1,parseInt(e.target.value)||25)*60; remain=total; paint(); };
  $('#tRest').onchange=e=>{ rest=Math.max(0,parseInt(e.target.value)||0)*60; };
  paint();
}
function renderTimerLog(){
  const logs=(S.log||[]).filter(l=>l.type==='timer'&&l.date===new Date().toDateString());
  if(!logs.length) return '<div class="empty">لا جلسات مسجلة اليوم — ابدأ أول بومودورو!</div>';
  const tot=logs.reduce((a,l)=>a+l.mins,0);
  return `<p style="color:var(--muted)">${logs.length} جلسة · إجمالي ${fmtMin(tot)} مذاكرة مركزة اليوم 👏</p>`;
}

/* ---------- ALARMS ---------- */
let alarmTimer=null;
function viewAlarms(){
  $('#view').innerHTML = `
  <h2 class="page-title">⏰ منبّه المذاكرة</h2>
  <div class="card">
    <h3>منبّه جديد</h3>
    <div class="form-row">
      <div class="field"><label>الوقت</label><input type="time" id="aTime" value="08:00"></div>
      <div class="field" style="flex:1"><label>الوصف (مثال: مذاكرة رياضيات — النهايات)</label><input id="aLabel" placeholder="مذاكرة..."></div>
      <div class="field"><label>&nbsp;</label><button class="btn" id="aAdd">إضافة المنبّه</button></div>
    </div>
    <div class="form-row" style="align-items:flex-start">
      <div class="field" style="flex:1">
        <label>أيام التكرار</label>
        <div style="display:flex;gap:6px;flex-wrap:wrap">
          ${DAY_NAMES.map((d,i)=>`<label class="chip" style="cursor:pointer;padding:6px 12px"><input type="checkbox" class="aDay" value="${i}" checked style="accent-color:var(--accent)"> ${d}</label>`).join('')}
        </div>
      </div>
    </div>
    <label style="display:flex;gap:8px;align-items:center;margin-top:8px;color:var(--muted);font-size:.9rem">
      <input type="checkbox" id="aBed" ${S.settings.bedtimeRemind?'checked':''} style="accent-color:var(--accent)">
      تنبيه "استعد للنوم" قبل 30 دقيقة من وقت نومي (${esc(S.settings.sleep)}) — للنوم المبكر يزيد التركيز
    </label>
    <div style="display:flex;gap:8px;margin-top:14px">
      <button class="btn sec" id="aTest">🔔 تجربة الصوت</button>
      <button class="btn sec" id="aNotif">🔔 تفعيل إشعارات المتصفح</button>
    </div>
  </div>
  <h2 class="section-title">المنبّهات المحفوظة</h2>
  <div id="alarmList">${renderAlarms()}</div>
  <div class="note">💡 المنبّه يعمل عندما يكون التطبيق مفتوحًا في المتصفح — يمكنك تركه مفتوحًا في تبويب أثناء المذاكرة. الإشعارات تظهر حتى لو كان التبويب في الخلفية.</div>`;
  $('#aAdd').onclick=()=>{
    const time=$('#aTime').value, label=$('#aLabel').value||'مذاكرة';
    const days=$$('.aDay').filter(c=>c.checked).map(c=>parseInt(c.value));
    if(!time){ toast('حدد الوقت أولًا'); return; }
    S.alarms.push({id:Date.now(), time, label, days, enabled:true});
    S.settings.bedtimeRemind=$('#aBed').checked;
    save();
    $('#aLabel').value='';
    $('#alarmList').innerHTML=renderAlarms();
    tone(880,.2);
    toast('تمت إضافة المنبّه ✅');
    if('Notification' in window && Notification.permission==='default') Notification.requestPermission();
  };
  $('#aTest').onclick=alarmSound;
  $('#aNotif').onclick=()=>{
    if(!('Notification' in window)) return toast('المتصفح لا يدعم الإشعارات');
    Notification.requestPermission().then(p=>toast(p==='granted'?'الإشعارات مفعّلة ✅':'لم تُفعّل الإشعارات'));
  };
  startAlarmTick();
}
function renderAlarms(){
  if(!S.alarms.length) return '<div class="empty">لا منبّهات بعد — أضف أول منبّه مذاكرة 👆</div>';
  return S.alarms.map(a=>{
    const days=(a.days||[]).map(d=>DAY_NAMES[d]).join('، ')||'يوميًا';
    return `<div class="alarm-item ${a.enabled?'':'off'}">
      <span class="t">${a.time}</span>
      <span style="flex:1"><b>${esc(a.label)}</b><br><small style="color:var(--muted)">${days}</small></span>
      <button class="btn sm sec aToggle" data-id="${a.id}">${a.enabled?'إيقاف مؤقت':'تشغيل'}</button>
      <button class="btn sm danger aDel" data-id="${a.id}">حذف</button>
    </div>`;
  }).join('');
}
document.addEventListener('click', e=>{
  const t=e.target.closest('.aToggle'), d=e.target.closest('.aDel');
  if(t){ const a=S.alarms.find(x=>x.id==t.dataset.id); if(a){ a.enabled=!a.enabled; save(); $('#alarmList').innerHTML=renderAlarms(); } }
  if(d){ S.alarms=S.alarms.filter(x=>x.id!=d.dataset.id); save(); $('#alarmList').innerHTML=renderAlarms(); toast('تم حذف المنبّه'); }
});
function startAlarmTick(){
  clearInterval(alarmTimer);
  let lastMin='';
  alarmTimer=setInterval(()=>{
    const now=new Date();
    const hm=String(now.getHours()).padStart(2,'0')+':'+String(now.getMinutes()).padStart(2,'0');
    if(hm===lastMin) return; lastMin=hm;
    const day=now.getDay();
    // منبّهات المستخدم
    S.alarms.forEach(a=>{
      if(a.enabled && a.time===hm && (a.days||[]).includes(day)){
        alarmSound();
        if('Notification' in window && Notification.permission==='granted') new Notification('⏰ منارة — '+a.label, {body:'حان وقت: '+a.label+' ('+a.time+')'});
        toast('⏰ '+a.label);
      }
    });
    // تنبيه النوم قبل 30 دقيقة
    if(S.settings.bedtimeRemind){
      const bed=t2m(S.settings.sleep)-30;
      const cur=now.getHours()*60+now.getMinutes();
      if(cur===bed){ alarmSound(); toast('🌙 استعد للنوم خلال 30 دقيقة — نومك يزيد تركيزك غدًا'); }
    }
  },15000);
}

/* ---------- SCHEDULE ---------- */
function viewSchedule(){
  const subs=allSubjects();
  $('#view').innerHTML = `
  <h2 class="page-title">🏫 مواعيد الدروس (جدول المدرسة)</h2>
  <div class="note">أضف مواعيد دروسك في المدرسة — الخطة الذكية تتجنبها تلقائيًا وتبني مذاكرتك في الأوقات الحرة.</div>
  <div class="card">
    <h3>إضافة موعد درس</h3>
    <div class="form-row">
      <div class="field"><label>اليوم</label><select id="sDay">${DAY_NAMES.map((d,i)=>`<option value="${i}">${d}</option>`).join('')}</select></div>
      <div class="field"><label>من</label><input type="time" id="sStart" value="08:00"></div>
      <div class="field"><label>إلى</label><input type="time" id="sEnd" value="09:00"></div>
      <div class="field"><label>المادة</label><select id="sSubject">${subs.map(s=>`<option value="${s.name}">${s.icon} ${s.name}</option>`).join('')}</select></div>
      <div class="field"><label>&nbsp;</label><button class="btn" id="sAdd">إضافة</button></div>
    </div>
  </div>
  <h2 class="section-title">الجدول الأسبوعي</h2>
  <div id="schedList">${renderSchedule()}</div>`;
  $('#sAdd').onclick=()=>{
    const day=parseInt($('#sDay').value), start=$('#sStart').value, end=$('#sEnd').value, subject=$('#sSubject').value;
    if(!start||!end){ toast('حدد المواعيد'); return; }
    if(t2m(end)<=t2m(start)){ toast('وقت النهاية يجب أن يكون بعد البداية'); return; }
    S.schedule.push({id:Date.now(), day, start, end, subject});
    save();
    $('#schedList').innerHTML=renderSchedule();
    toast('تمت إضافة موعد الدرس ✅');
  };
}
function renderSchedule(){
  if(!S.schedule.length) return '<div class="empty">لا مواعيد بعد — أضف جدول دروسك ليتم تجنبه في الخطة.</div>';
  return DAY_NAMES.map((d,di)=>{
    const items=S.schedule.filter(x=>x.day===di).sort((a,b)=>t2m(a.start)-t2m(b.start));
    if(!items.length) return '';
    return `<div class="plan-day"><h3><span>📆 ${d}</span><span class="chip">${items.length} دروس</span></h3>
      ${items.map(x=>`<div class="session">
        <span class="time">${x.start} - ${x.end}</span>
        <span class="what"><b class="subj">${esc(x.subject)}</b><small>درس في المدرسة</small></span>
        <button class="btn sm danger sDel" data-id="${x.id}">حذف</button>
      </div>`).join('')}</div>`;
  }).join('') || '<div class="empty">لا مواعيد.</div>';
}
document.addEventListener('click', e=>{
  const d=e.target.closest('.sDel');
  if(d){ S.schedule=S.schedule.filter(x=>x.id!=d.dataset.id); save(); const el=$('#schedList'); if(el) el.innerHTML=renderSchedule(); toast('تم حذف الموعد'); }
});

/* ---------- boot ---------- */
route();
startAlarmTick();
setInterval(()=>{ // تحديث خطة اليوم عند منتصف الليل
  const now=new Date();
  if(route._day===undefined) route._day=now.getDate();
  if(route._day!==now.getDate()){ route._day=now.getDate(); if(location.hash===''||location.hash==='#/') route(); }
},60000);
