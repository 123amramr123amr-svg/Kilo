/* محرك تخطيط المذاكرة الذكي — يحسب خطة أسبوعية تراعي وقت النوم ومواعيد الدروس والتحصيل */

function t2m(t){ const p=String(t||'').split(':'); return (parseInt(p[0])||0)*60 + (parseInt(p[1])||0); }
function m2t(m){ m=((m%1440)+1440)%1440; const h=Math.floor(m/60), mm=m%60; return String(h).padStart(2,'0')+':'+String(mm).padStart(2,'0'); }
function fmtMin(m){ const h=Math.floor(m/60), mm=m%60; return h>0 ? (mm? h+'س '+mm+'د' : h+'ساعة') : mm+'د'; }

function subjectStats(s){
  const tot = s.units.reduce((n,u)=>n+u.lessons.length,0);
  let read = 0, qscores = [];
  s.units.forEach(u=>u.lessons.forEach(l=>{
    const p = (S.progress[l.id]||{});
    if(p.done) read++;
    if(typeof p.quiz === 'number') qscores.push(p.quiz);
  }));
  const avg = qscores.length ? qscores.reduce((a,b)=>a+b,0)/qscores.length : null;
  const pref = (S.planPrefs && typeof S.planPrefs[s.id]==='number') ? S.planPrefs[s.id] : 3;
  return {sub:s, tot, read, avg, pref};
}

function pickLesson(sub, exclude, dayExclude){
  exclude = exclude || new Set();
  dayExclude = dayExclude || new Set();
  const scoreOf = (l)=>{
    const p = (S.progress[l.id]||{});
    const q = (typeof p.quiz==='number') ? p.quiz : null;
    if(q===null) return 100 + (!p.done?10:0);
    if(q<60) return 120 + (60-q);        // درس ضعيف = أعلى أولوية للمراجعة الفورية
    if(q<80) return 40;
    return !p.done ? 30 : 5;
  };
  const passes = [
    l => !exclude.has(l.id) && !dayExclude.has(l.id),  // غير مخططة أبدًا وليس في هذا اليوم
    l => !exclude.has(l.id),                            // غير مخططة أسبوعًا (قد تكررت اليوم)
    l => !dayExclude.has(l.id),                         // مراجعة: ليست في هذا اليوم
    l => true                                           // مراجعة حرة
  ];
  for(const pass of passes){
    let best=null, bestScore=-1;
    sub.units.forEach(u=>u.lessons.forEach(l=>{
      if(!pass(l)) return;
      const sc = scoreOf(l);
      if(sc>bestScore){ bestScore=sc; best=l; }
    }));
    if(best) return best;
  }
  return sub.units[0].lessons[0];
}

function freeIntervals(dayIdx){
  const st = S.settings;
  let start = t2m(st.wake), end = t2m(st.sleep);
  if(end<=start) end += 1440; // نوم بعد منتصف الليل
  const blocks = [[start,end]];
  const cut = (a,b)=>{
    for(let i=blocks.length-1;i>=0;i--){
      const [s,e]=blocks[i];
      if(b<=s||a>=e) continue;
      blocks.splice(i,1);
      if(a>s) blocks.push([s,Math.min(a,e)]);
      if(b<e) blocks.push([Math.max(b,s),e]);
    }
  };
  // دروس المدرسة
  (S.schedule||[]).filter(x=>x.day===dayIdx).forEach(x=>cut(t2m(x.start),t2t2end(x)));
  // فسحة غداء وعشاء
  cut(t2m(st.lunch||'13:00'), t2m(st.lunch||'13:00')+60);
  cut(t2m(st.dinner||'19:30'), t2m(st.dinner||'19:30')+45);
  return blocks.sort((a,b)=>a[0]-b[0]);
}
function t2t2end(x){ return t2m(x.end); }

function focusScore(mins){
  const h = mins/60;
  if(h>=8&&h<12) return 3;      // ذروة تركيز صباحية
  if(h>=16&&h<21) return 2;     // مساء
  if(h>=7&&h<8) return 1.5;
  if(h>=12&&h<16) return 1;     // بعد الظهر
  return 0.7;                    // متأخر
}

function buildPlan(){
  const st = S.settings;
  const days = (S.planDays && S.planDays.length ? S.planDays : [6,0,1,2,3,4]);
  const len = st.sessionLen || 45;
  const breakLen = st.breakLen || 10;
  const subjects = allSubjects().map(subjectStats).filter(x=>x.read<x.tot || x.avg===null || x.avg<95);
  if(!subjects.length) return {empty:true, days:[], summary:'', tips:[]};

  // وزن كل مادة: أولوية + نسبة غير المقروءة + ضعف التحصيل + قرب الامتحان
  let totalW = 0;
  subjects.forEach(x=>{
    let w = x.pref * 2;
    w += ((x.tot-x.read)/x.tot) * 4;
    if(x.avg!==null && x.avg<60) w += (60-x.avg)/60 * 5;
    if(x.avg===null) w += 2;
    x.w = w; totalW += w;
  });

  // عدد الحصص المتاحة أسبوعيًا (مع حد معقول لكل يوم)
  const dailyCap = st.dailyCap || 6;
  let slots = [];
  days.forEach(d=>{
    const daySlots = [];
    freeIntervals(d).forEach(([s,e])=>{
      let t=s;
      while(t+len<=e){
        daySlots.push({day:d, start:t, end:t+len, focus:focusScore(t)});
        t += len + breakLen;
      }
    });
    // اختيار أفضل الحصص في اليوم مع توزيعها على اليوم (لا نحشرها كلها صباحًا)
    daySlots.sort((a,b)=>b.focus-a.focus);
    const picked = daySlots.slice(0, dailyCap).sort((a,b)=>a.start-b.start);
    slots = slots.concat(picked);
  });
  if(!slots.length) return {empty:true, days:[], summary:'', tips:[]};

  // توزيع الحصص على المواد
  let alloc = subjects.map(x=>({x, n: Math.max(1, Math.round(x.w/totalW*slots.length))}));
  let sum = alloc.reduce((a,b)=>a+b.n,0);
  while(sum>slots.length){ const big = alloc.reduce((a,b)=>a.n>b.n?a:b); if(big.n>1){big.n--;sum--;} else break; }
  while(sum<slots.length){ const small = alloc.reduce((a,b)=>a.n<b.n?a:b); small.n++; sum++; }

  // ترتيب الحصص: الأصعب في أوقات التركيز العالي (تناوب بين المواد)
  slots.sort((a,b)=>b.focus-a.focus || a.start-b.start);
  const queue = [];
  alloc.forEach(a=>{ for(let i=0;i<a.n;i++) queue.push(a.x); });
  // توزيع متناوب: نأخذ من المواد بالتناوب لتجنب تكرار مادة متتالية
  const ordered = [];
  const buckets = alloc.map(a=>({x:a.x, left:a.n}));
  let lastId = null;
  while(buckets.some(b=>b.left>0)){
    let cand = buckets.filter(b=>b.left>0 && b.x.sub.id!==lastId).sort((a,b)=>b.x.w-a.x.w);
    if(!cand.length) cand = buckets.filter(b=>b.left>0).sort((a,b)=>b.x.w-a.x.w);
    const pick = cand[0];
    ordered.push(pick.x); pick.left--; lastId = pick.x.sub.id;
  }

  const planDays = {};
  const today = new Date();
  const dayNames = ['الأحد','الاثنين','الثلاثاء','الأربعاء','الخميس','الجمعة','السبت'];
  const plannedMap = {};
  const dayPlannedMap = {};
  let i=0;
  slots.forEach(slot=>{
    const sub = ordered[i % ordered.length]; i++;
    const planned = plannedMap[sub.sub.id] || (plannedMap[sub.sub.id] = new Set());
    const dayPlanned = dayPlannedMap[sub.sub.id+'-'+slot.day] || (dayPlannedMap[sub.sub.id+'-'+slot.day] = new Set());
    const lesson = pickLesson(sub.sub, planned, dayPlanned);
    planned.add(lesson.id);
    dayPlanned.add(lesson.id);
    const isRev = (S.progress[lesson.id] && S.progress[lesson.id].quiz>=60);
    const entry = {
      day: slot.day, dayName: dayNames[slot.day],
      start: m2t(slot.start), end: m2t(slot.end),
      subject: sub.sub.name, subjectId: sub.sub.id, icon: sub.sub.icon, color: sub.sub.color,
      kind: isRev ? 'مراجعة' : 'درس جديد',
      lessonId: lesson.id, lessonTitle: lesson.title,
      unitName: (sub.sub.units.find(u=>u.lessons.some(l=>l.id===lesson.id))||{}).name || ''
    };
    (planDays[slot.day] = planDays[slot.day] || []).push(entry);
  });
  // ترتيب حصص كل يوم زمنيًا للعرض
  Object.keys(planDays).forEach(d=>{ planDays[d].sort((a,b)=>t2m(a.start)-t2m(b.start)); });

  // فحص قرب الامتحان
  let examNote = '';
  if(st.examDate){
    const diff = Math.ceil((new Date(st.examDate) - today)/86400000);
    if(diff>=0 && diff<=14) examNote = '⚠️ الامتحان بعد '+diff+' يوم — الخطة تركّز على المراجعة والاختبارات.';
  }

  const totalMin = slots.reduce((a,s)=>a+(s.end-s.start),0);
  const summary = 'خطة أسبوعية: '+slots.length+' حصة مذاكرة ('+fmtMin(totalMin)+') في '+days.length+' أيام — تراعي نومك من '+m2t(t2m(st.sleep))+' إلى '+m2t(t2m(st.wake))+'.';
  const tips = [
    'نم 7-8 ساعات على الأقل — النوم يثبّت الذاكرة، والخطة تنتهي قبل موعد نومك بـ30 دقيقة.',
    'ابدأ أصعب مادة في أوقات التركيز (8-11 صباحًا و5-9 مساءً) كما رتّبتها الخطة.',
    'بعد كل حصة: 10 دقائق مراجعة سريعة بأسلوب الاسترجاع (اكتب ما تذكرته دون النظر).',
    'كرّر المادة بعد يوم ثم بعد أسبوع (التكرار المتباعد) — التطبيق يختار دروسك الأضعف تلقائيًا.',
    'قبل الامتحان: اعمل اختبار "المنهج كامل" تحت ضغط الوقت.',
    examNote
  ].filter(Boolean);

  return {createdAt: new Date().toISOString(), daysArr: days, planDays, summary, tips, slotsCount: slots.length};
}

/* توليد خطة بالذكاء الاصطناعي عبر API (اختياري) */
async function generateAIPlan(endpoint, apiKey, model){
  const stats = allSubjects().map(subjectStats);
  const data = {
    student: S.settings.name || 'طالب الثالثة الثانوي',
    branch: S.branch,
    wake: S.settings.wake, sleep: S.settings.sleep,
    studyDays: (S.planDays||[6,0,1,2,3,4]).map(d=>['الأحد','الاثنين','الثلاثاء','الأربعاء','الخميس','الجمعة','السبت'][d]).join('، '),
    examDate: S.settings.examDate || 'غير محدد',
    sessionMinutes: S.settings.sessionLen || 45,
    subjects: stats.map(x=>({
      subject: x.sub.name,
      lessonsTotal: x.tot, lessonsDone: x.read,
      avgQuiz: x.avg===null ? 'لا يوجد' : Math.round(x.avg),
      priority: x.pref,
      weakestLessons: x.sub.units.flatMap(u=>u.lessons.map(l=>({l:l.title, q:(S.progress[l.id]||{}).quiz}))).filter(o=>o.q===undefined||o.q<60).slice(0,5)
    }))
  };
  const prompt = 'أنت مدرّب أكاديمي خبير للثانوية العامة المصرية. أنشئ خطة مذاكرة أسبوعية مفصّلة بالعربية للطالب التالي بناءً على بياناته. اذكر لكل يوم: التوقيت، المادة، الدرس/الموضوع، نوع الحصة (درس جديد/مراجعة/اختبار)، مع نصائح نوم وتركيز. راعِ: وقت الاستيقاظ '+data.wake+' ووقت النوم '+data.sleep+'، وأولوية المواد الأضعف، والتكرار المتباعد.\n\nالبيانات:\n'+JSON.stringify(data,null,1);
  const res = await fetch(endpoint, {
    method:'POST',
    headers:{'Content-Type':'application/json','Authorization':'Bearer '+apiKey},
    body: JSON.stringify({model: model||'gpt-4o-mini', messages:[{role:'user',content:prompt}], temperature:0.4})
  });
  if(!res.ok) throw new Error('خطأ في الاتصال بالخدمة ('+res.status+') — تأكد من المفتاح والرابط');
  const j = await res.json();
  const text = j.choices && j.choices[0] && j.choices[0].message && j.choices[0].message.content;
  if(!text) throw new Error('لم تُرجع الخدمة خطة — جرّب نموذجًا آخر');
  return text;
}
