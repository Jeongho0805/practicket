/* ============================================================
   i-ticket-new 좌석선택 (신버전) — 좌석 화면 전용
   실제 NOL/인터파크 대기실(대기순서 감소) + 좌석 + 완료/매진 모달
   (인트로는 /practice/i-ticket-new/intro → sessionStorage 로 인계)
   ------------------------------------------------------------
   타이밍 정합성(서버 검증 대응):
   - /start 는 인트로 "연습 시작하기" 시점에 호출됨 (서버가 startAt 기록)
   - reaction 은 인트로에서 측정되어 pkt.reactionTimeMs 로 넘어옴
   - 여기서 queue + seat 를 측정, total = reaction + queue + seat
   - seat 측정은 대기열 종료(캡차 포함)부터 시작해 공백 없이 이어붙인다.
   ============================================================ */
import { authFetch, showAlert } from '/js/common.js';

const INTRO_URL = '/practice/i-ticket-new/intro';

/* ===================== 인트로 인계 (세션/반응시간) ===================== */
const sessionId = sessionStorage.getItem('pkt.sessionId');
if (!sessionId) {
  window.location.replace(INTRO_URL);
}

/* ===================== 화면 전환 ===================== */
function showScreen(id){
  document.querySelectorAll('.screen').forEach(s=>s.classList.toggle('active', s.id===id));
}

/* ===================== 타이밍 측정 ===================== */
const T = {
  reaction: parseInt(sessionStorage.getItem('pkt.reactionTimeMs') || '0', 10),
  queueStart:0, queue:0, seatStart:0, seat:0, queueInitialRank:0
};
const now = ()=>performance.now();
const fmt = ms => (ms/1000).toFixed(2);

/* ===================== 등급 / 가격 ===================== */
const GRADE = {
  R:{ name:'R석', price:154000, color:'#7a68a8' },
  S:{ name:'S석', price:143000, color:'#2e9e57' },
};
const won = n => n.toLocaleString('ko-KR') + '원';

/* ===================== 1) 대기실 (실제 NOL/인터파크 형식) ===================== */
// 초기 대기순서: 반응 느릴수록 큼 (0.1초 단위, 최대 30단계) → 5,000 ~ 200,000
// 초당 20,000 감소 → 최대 10초 내 통과 (OLD QueueManager 와 동일 상수)
const QUEUE = { MIN:5000, MAX:200000, DEQ:20000, MAX_REACTION:3000 };

function initQueue(){
  const loading=document.getElementById('qLoading');
  const wrap=document.getElementById('qWrap');
  loading.style.display='block';
  wrap.style.display='none';
  setTimeout(()=>{
    loading.style.display='none';
    wrap.style.display='flex';
    startQueue();
  }, 800);
}

function startQueue(){
  const step = Math.min(Math.floor(Math.min(T.reaction, QUEUE.MAX_REACTION)/100), 30);
  const initialQueue = Math.round(QUEUE.MIN + (step/30)*(QUEUE.MAX - QUEUE.MIN));
  T.queueInitialRank = initialQueue;

  const countEl=document.getElementById('queueCount');
  const fillEl=document.getElementById('queueFill');
  const timeEl=document.getElementById('queueTimeLeft');

  T.queueStart=now();
  const t0=now();
  const render=()=>{
    const elapsedSec=(now()-t0)/1000;
    const cur=Math.max(0, Math.floor(initialQueue - elapsedSec*QUEUE.DEQ));
    countEl.textContent=cur.toLocaleString();
    const pct=initialQueue>0 ? Math.min(100, ((initialQueue-cur)/initialQueue)*100) : 100;
    fillEl.style.width=pct+'%';
    fillEl.classList.toggle('urgent', cur<=5000 && cur>0);
    timeEl.textContent=Math.ceil(cur/QUEUE.DEQ)+'초';
    if(cur<=0){
      clearInterval(iv);
      T.queue=now()-T.queueStart;
      enterSeat();
    }
  };
  render();
  const iv=setInterval(render,200);
}

function enterSeat(){
  // 좌석 측정 시작 = 대기열 종료(캡차 포함) → total 이 서버 실경과와 일치
  T.seatStart=now();
  startSeatTimer(150); // 세션 TTL(180s) 안에서 안전하게 2:30
  showScreen('screen-seat');
  document.getElementById('captchaOverlay').style.display='flex';
  document.getElementById('captchaInput').focus();
}

/* ===================== 2) 좌석 데이터/렌더 (단일 구역) ===================== */
const SEAT_GAP=3, SEAT_R=1;
const BLOCK={ id:'main', label:'1층', x:24, y:40, cols:16, rows:13, rSeatRows:5, soldRate:.26 };
const STRUCTS=[
  { type:'STAGE', x:24+(16-1)*SEAT_GAP/2-26, y:14, w:52, h:10, fs:6, label:'STAGE' },
];
const MAX_SEL=2;

let rng=20260208;
function rand(){ rng=(rng*1103515245+12345)&0x7fffffff; return rng/0x7fffffff; }

const seats={};
function buildSeatData(){
  for(let r=0;r<BLOCK.rows;r++){
    for(let c=0;c<BLOCK.cols;c++){
      const id=`${BLOCK.id}:${r+1}:${c+1}`;
      const grade = r < BLOCK.rSeatRows ? 'R' : 'S';
      seats[id]={ id, r:r+1, c:c+1, grade,
        cx:BLOCK.x+c*SEAT_GAP, cy:BLOCK.y+r*SEAT_GAP,
        state: rand()<BLOCK.soldRate ? 'sold':'avail' };
    }
  }
}

const NS='http://www.w3.org/2000/svg';
const svg=document.getElementById('seatmap');
let zoomLayer;
const SOLD='#edeff3';

function buildMap(){
  let minX=1e9,minY=1e9,maxX=-1e9,maxY=-1e9;
  const acc=(x,y)=>{minX=Math.min(minX,x);minY=Math.min(minY,y);maxX=Math.max(maxX,x);maxY=Math.max(maxY,y);};
  for(const s of Object.values(seats)) acc(s.cx,s.cy);
  for(const st of STRUCTS){ acc(st.x,st.y); acc(st.x+st.w,st.y+st.h); }
  const pad=14;
  svg.setAttribute('viewBox',[minX-pad, minY-pad-6, (maxX-minX)+pad*2, (maxY-minY)+pad*2+10].join(' '));
  svg.setAttribute('preserveAspectRatio','xMidYMid meet');

  zoomLayer=document.createElementNS(NS,'g');
  zoomLayer.setAttribute('id','zoomLayer');

  for(const st of STRUCTS){
    const rect=document.createElementNS(NS,'rect');
    rect.setAttribute('class','struct-box');
    rect.setAttribute('x',st.x);rect.setAttribute('y',st.y);
    rect.setAttribute('width',st.w);rect.setAttribute('height',st.h);rect.setAttribute('rx',2);
    zoomLayer.appendChild(rect);
    const t=document.createElementNS(NS,'text');
    t.setAttribute('class','struct-text');
    t.setAttribute('x',st.x+st.w/2);t.setAttribute('y',st.y+st.h/2);
    t.setAttribute('font-size',st.fs);
    t.style.letterSpacing='2px';
    t.textContent=st.label;
    zoomLayer.appendChild(t);
  }

  const lab=document.createElementNS(NS,'text');
  lab.setAttribute('class','blk-label');
  lab.setAttribute('x', BLOCK.x+(BLOCK.cols-1)*SEAT_GAP/2);
  lab.setAttribute('y', BLOCK.y-4);
  lab.textContent=BLOCK.label+' 지정석';
  zoomLayer.appendChild(lab);

  const frag=document.createDocumentFragment();
  for(const s of Object.values(seats)){
    const c=document.createElementNS(NS,'circle');
    c.setAttribute('id','s_'+s.id);
    c.setAttribute('cx',s.cx);c.setAttribute('cy',s.cy);c.setAttribute('r',SEAT_R);
    c.dataset.id=s.id;
    paintSeat(s,c);
    frag.appendChild(c);
  }
  zoomLayer.appendChild(frag);

  const note=document.createElementNS(NS,'text');
  note.setAttribute('class','seat-note');
  note.setAttribute('x', BLOCK.x+(BLOCK.cols-1)*SEAT_GAP/2);
  note.setAttribute('y', 8);
  note.textContent='전석 지정석 · 선택한 좌석으로 입장합니다';
  zoomLayer.appendChild(note);

  svg.appendChild(zoomLayer);
}

function paintSeat(s, circle){
  circle=circle||document.getElementById('s_'+s.id);
  if(!circle) return;
  const gcol=GRADE[s.grade].color;
  if(s.state==='sold'){
    circle.setAttribute('fill',SOLD);    circle.setAttribute('fill-opacity','1');
    circle.setAttribute('stroke',SOLD);  circle.setAttribute('stroke-width',0.3);
  }else if(s.state==='selected'){
    circle.setAttribute('fill',gcol); circle.setAttribute('fill-opacity','1');
    circle.setAttribute('stroke',gcol);circle.setAttribute('stroke-width',0.8);
  }else{
    circle.setAttribute('fill',gcol); circle.setAttribute('fill-opacity','0.35');
    circle.setAttribute('stroke',gcol);circle.setAttribute('stroke-width',0.3);
  }
  circle.style.cursor = s.state==='sold' ? 'default':'pointer';

  const checkId='chk_'+s.id;
  const exist=document.getElementById(checkId);
  if(s.state==='selected'){
    if(!exist){
      const t=document.createElementNS(NS,'text');
      t.setAttribute('id',checkId);
      t.setAttribute('class','seat-check');
      t.setAttribute('x',s.cx);t.setAttribute('y',s.cy+0.05);
      t.setAttribute('font-size','2');
      t.setAttribute('text-anchor','middle');
      t.setAttribute('dominant-baseline','central');
      t.setAttribute('fill','#fff');t.setAttribute('font-weight','900');
      t.textContent='✓';
      zoomLayer.appendChild(t);
    }
  }else if(exist){ exist.remove(); }
}

/* ===================== 선택 로직 ===================== */
const selected=[];
function onSeatClick(id){
  if(document.getElementById('captchaOverlay').style.display!=='none') return;
  const s=seats[id]; if(!s) return;
  if(s.state==='sold'){ showToast('이미 판매된 좌석입니다'); return; }
  if(s.state==='selected'){
    s.state='avail'; paintSeat(s);
    const i=selected.indexOf(id); if(i>=0) selected.splice(i,1);
  }else{
    if(selected.length>=MAX_SEL){ showToast(`최대 ${MAX_SEL}매까지 선택할 수 있습니다`); return; }
    s.state='selected'; paintSeat(s); selected.push(id);
  }
  renderSelected();
}
function renderSelected(){
  document.getElementById('selCount').textContent=selected.length;
  document.getElementById('btnClear').style.display = selected.length ? 'block' : 'none';
  const list=document.getElementById('selList');
  const done=document.getElementById('btnDone');
  const totalEl=document.getElementById('selTotal');
  if(selected.length===0){
    list.innerHTML='<div class="sel-empty">선택한 좌석이 없습니다.</div>';
    done.disabled=true; totalEl.textContent='0원'; return;
  }
  done.disabled=false;
  let total=0;
  list.innerHTML='';
  selected.forEach(id=>{
    const s=seats[id]; const g=GRADE[s.grade]; total+=g.price;
    const loc=`${BLOCK.label} ${s.r}열 ${s.c}번`;
    const el=document.createElement('div'); el.className='sel-item';
    el.innerHTML=`<div class="info"><div class="g">${g.name}</div><div class="loc">${loc}</div></div>`+
                 `<div class="price">${won(g.price)}</div><button class="x" title="삭제">✕</button>`;
    el.querySelector('.x').addEventListener('click',()=>{
      s.state='avail'; paintSeat(s);
      const i=selected.indexOf(id); if(i>=0) selected.splice(i,1);
      renderSelected();
    });
    list.appendChild(el);
  });
  totalEl.textContent=won(total);
}

/* ===================== 줌 / 팬 ===================== */
let scale=1, tx=0, ty=0;
const MIN_S=1, MAX_S=16;
const SEAT_ZOOM_MIN=3;
function applyTransform(){ zoomLayer.setAttribute('transform',`translate(${tx} ${ty}) scale(${scale})`); }
function clientToVB(x,y){ const p=svg.createSVGPoint(); p.x=x;p.y=y; return p.matrixTransform(svg.getScreenCTM().inverse()); }
function zoomAt(cx,cy,factor){
  const p=clientToVB(cx,cy);
  const ns=Math.max(MIN_S,Math.min(MAX_S,scale*factor));
  if(ns===scale) return;
  tx=p.x-(p.x-tx)*(ns/scale); ty=p.y-(p.y-ty)*(ns/scale);
  scale=ns; applyTransform();
}
svg.addEventListener('wheel',e=>{ e.preventDefault(); zoomAt(e.clientX,e.clientY,e.deltaY<0?1.18:1/1.18); },{passive:false});
document.getElementById('zoomIn').onclick =()=>{ const r=svg.getBoundingClientRect(); zoomAt(r.left+r.width/2,r.top+r.height/2,1.4); };
document.getElementById('zoomOut').onclick=()=>{ const r=svg.getBoundingClientRect(); zoomAt(r.left+r.width/2,r.top+r.height/2,1/1.4); };
document.getElementById('zoomFit').onclick=()=>{ scale=1;tx=0;ty=0;applyTransform(); };

let panning=false,moved=false,sx=0,sy=0,stx=0,sty=0;
svg.addEventListener('pointerdown',e=>{ panning=true;moved=false;sx=e.clientX;sy=e.clientY;stx=tx;sty=ty;svg.classList.add('grabbing'); });
svg.addEventListener('pointermove',e=>{
  if(!panning) return;
  const dx=e.clientX-sx, dy=e.clientY-sy;
  if(Math.abs(dx)+Math.abs(dy)>3) moved=true;
  const m=svg.getScreenCTM(); tx=stx+dx/m.a; ty=sty+dy/m.d; applyTransform();
});
svg.addEventListener('pointerup',()=>{ panning=false; svg.classList.remove('grabbing'); });
window.addEventListener('pointerup',()=>{ panning=false; svg.classList.remove('grabbing'); });
svg.addEventListener('click',e=>{
  if(moved) return;
  const t=(e.target && e.target.tagName==='circle') ? e.target : document.elementFromPoint(e.clientX, e.clientY);
  if(!(t&&t.tagName==='circle'&&t.dataset.id)) return;
  if(scale < SEAT_ZOOM_MIN){
    zoomAt(e.clientX, e.clientY, SEAT_ZOOM_MIN/scale);
    showToast('좌석을 확대했어요. 좌석을 눌러 선택하세요');
    return;
  }
  onSeatClick(t.dataset.id);
});

/* ===================== 실시간 좌석 빠짐 + 매진 판정 ===================== */
let soldoutShown=false;
setInterval(()=>{
  if(!document.getElementById('screen-seat').classList.contains('active')) return;
  if(document.getElementById('captchaOverlay').style.display!=='none') return;
  if(soldoutShown) return;
  const avail=Object.values(seats).filter(s=>s.state==='avail');
  if(!avail.length){
    if(selected.length===0) showSoldout();
    return;
  }
  const n=1+Math.floor(rand()*3);
  for(let i=0;i<n;i++){
    const s=avail[Math.floor(rand()*avail.length)];
    if(!s||s.state!=='avail') continue;
    s.state='sold'; paintSeat(s);
  }
},1200);

function showSoldout(){
  if(soldoutShown) return;
  soldoutShown=true;
  if(seatTimerIv) clearInterval(seatTimerIv);
  document.getElementById('sReaction').textContent=fmt(Math.round(T.reaction))+'초';
  document.getElementById('sQueue').textContent=fmt(Math.round(T.queue))+'초';
  document.getElementById('sRank').textContent=T.queueInitialRank.toLocaleString()+'번';
  document.getElementById('soldoutModal').style.display='flex';
}

/* ===================== 좌석 선택 제한시간 타이머 ===================== */
let seatTimerIv=null;
function startSeatTimer(sec){
  const el=document.getElementById('seatTimer');
  let left=sec;
  const tick=()=>{
    const m=Math.floor(left/60), s=left%60;
    el.textContent=`${m}:${String(s).padStart(2,'0')}`;
    if(left<=0){
      clearInterval(seatTimerIv);
      showToast('좌석 선택 시간이 만료되었습니다. 다시 시도해주세요.');
      setTimeout(()=>{ window.location.href = INTRO_URL; }, 1600);
      return;
    }
    left--;
  };
  tick();
  seatTimerIv=setInterval(tick,1000);
}

/* ===================== 전체삭제 / 완료 ===================== */
document.getElementById('btnClear').onclick=()=>{
  selected.slice().forEach(id=>{ seats[id].state='avail'; paintSeat(seats[id]); });
  selected.length=0; renderSelected();
};

document.getElementById('btnDone').onclick=async ()=>{
  if(!selected.length) return;
  if(seatTimerIv) clearInterval(seatTimerIv);
  T.seat=now()-T.seatStart;

  const reaction=Math.round(T.reaction), queue=Math.round(T.queue), seat=Math.round(T.seat);
  const total=reaction+queue+seat;
  const s=seats[selected[0]];
  const extra = selected.length>1 ? ` 외 ${selected.length-1}석` : '';

  document.getElementById('resultSeat').textContent=`${GRADE[s.grade].name} ${BLOCK.label} ${s.r}열 ${s.c}번${extra}`;
  document.getElementById('resultTotal').textContent=fmt(total);
  document.getElementById('rReaction').textContent=fmt(reaction)+'초';
  document.getElementById('rQueue').textContent=fmt(queue)+'초';
  document.getElementById('rSeat').textContent=fmt(seat)+'초';
  document.getElementById('rRank').textContent=T.queueInitialRank.toLocaleString()+'번';

  const rankBar=document.getElementById('resultRankBar');
  rankBar.style.display='none';

  if(sessionId){
    try{
      const res = await authFetch('/api/practice/complete', {
        method:'POST',
        headers:{ 'Content-Type':'application/json' },
        body: JSON.stringify({
          session_id: sessionId,
          total_duration_ms: total,
          reaction_time_ms: reaction,
          queue_wait_ms: queue,
          seat_selection_ms: seat,
          queue_initial_rank: T.queueInitialRank
        })
      });
      if(res.ok){
        const data = await res.json();
        if(data.percentile != null && data.total_users >= 2){
          document.getElementById('resultRankVal').innerHTML =
            `상위 ${data.percentile}%<span class="pb-sub">/ ${data.total_users.toLocaleString()}명 중 ${data.my_rank}위</span>`;
          rankBar.style.display='flex';
        }
      }else{
        const err = await res.json().catch(()=>({}));
        showToast(err.message || '기록 저장에 실패했습니다.');
      }
    }catch(e){
      console.error('[Practicket] complete 실패:', e);
      showToast('네트워크 오류로 기록이 저장되지 않았습니다.');
    }finally{
      sessionStorage.removeItem('pkt.sessionId');
      sessionStorage.removeItem('pkt.reactionTimeMs');
    }
  }

  document.getElementById('resultModal').style.display='flex';
};

document.getElementById('btnAgain').onclick=()=>{ window.location.href = INTRO_URL; };
document.getElementById('btnRank').onclick=()=>{ window.location.href='/practice'; };
document.getElementById('btnSoldAgain').onclick=()=>{ window.location.href = INTRO_URL; };
document.getElementById('btnSoldRank').onclick=()=>{ window.location.href='/practice'; };
document.getElementById('btnSeatBack').onclick=()=>{ window.location.href='/practice'; };

/* ===================== 등급별 가격 팝오버 ===================== */
const gradePop=document.getElementById('gradePop');
document.getElementById('gradeFab').onclick=(e)=>{ e.stopPropagation(); gradePop.classList.toggle('open'); };
document.getElementById('gradePopClose').onclick=()=>gradePop.classList.remove('open');
document.addEventListener('click',(e)=>{
  if(!gradePop.contains(e.target) && e.target.id!=='gradeFab') gradePop.classList.remove('open');
});

/* ===================== 캡차 (canvas 실시간 생성) ===================== */
const capCanvas=document.getElementById('captchaCanvas');
const capCtx=capCanvas.getContext('2d');
let captchaAnswer='';
const CAP_THEMES=[
  {bg:['#7c1f1f','#9c2b2b'], fg:['#1ea83a','#37c24a','#2e9e57','#49d06a']},
  {bg:['#1f2a5c','#26357c'], fg:['#ffffff','#ffe14d','#9be37a','#7fd4ff']},
  {bg:['#1e4d2b','#2a6b3a'], fg:['#ffe14d','#ffffff','#bfe86a']},
  {bg:['#46206a','#5e2a7a'], fg:['#ffffff','#ffd24d','#9be37a']},
];
function drawCaptcha(){
  const chars='ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
  const W=capCanvas.width, H=capCanvas.height;
  captchaAnswer='';
  const th=CAP_THEMES[Math.floor(Math.random()*CAP_THEMES.length)];
  const g=capCtx.createLinearGradient(0,0,W,H);
  g.addColorStop(0,th.bg[0]); g.addColorStop(1,th.bg[1]);
  capCtx.fillStyle=g; capCtx.fillRect(0,0,W,H);
  const dot=['#d9c84a','#6fcf57','#e08a3c','#ffffff','#e0d24a'];
  for(let i=0;i<300;i++){
    capCtx.fillStyle=dot[Math.floor(Math.random()*dot.length)];
    capCtx.globalAlpha=0.45+Math.random()*0.45;
    capCtx.fillRect(Math.random()*W, Math.random()*H, 1.5, 1.5);
  }
  capCtx.globalAlpha=1;
  const n=6, pad=30, step=(W-pad*2)/(n-1);
  const fg=th.fg;
  for(let i=0;i<n;i++){
    const ch=chars[Math.floor(Math.random()*chars.length)];
    captchaAnswer+=ch;
    capCtx.save();
    capCtx.translate(pad+i*step, H/2+(Math.random()*12-6));
    capCtx.rotate((Math.random()*32-16)*Math.PI/180);
    capCtx.font='bold '+(30+Math.random()*8)+'px "Courier New",monospace';
    capCtx.fillStyle=fg[Math.floor(Math.random()*fg.length)];
    capCtx.textAlign='center'; capCtx.textBaseline='middle';
    capCtx.fillText(ch,0,0);
    capCtx.restore();
  }
  const ln=['rgba(110,90,220,.7)','rgba(80,160,230,.6)','rgba(90,200,120,.6)'];
  for(let k=0;k<2;k++){
    capCtx.beginPath();
    capCtx.strokeStyle=ln[k%ln.length]; capCtx.lineWidth=1.6;
    capCtx.moveTo(0, Math.random()*H);
    capCtx.bezierCurveTo(W*0.3,Math.random()*H, W*0.6,Math.random()*H, W, Math.random()*H);
    capCtx.stroke();
  }
}
document.getElementById('captchaRefresh').onclick=()=>{ drawCaptcha(); document.getElementById('captchaInput').value=''; };
document.getElementById('captchaVoice').onclick=()=>showToast('음성안내는 연습모드입니다.');
document.getElementById('captchaCancel').onclick=()=>{ window.location.href = INTRO_URL; };
document.getElementById('captchaSubmit').onclick=()=>{
  const v=document.getElementById('captchaInput').value.trim().toUpperCase();
  if(v.length<6){ showToast('문자를 입력해주세요'); return; }
  if(v!==captchaAnswer){ showToast('문자가 일치하지 않습니다. 다시 입력해주세요'); drawCaptcha(); document.getElementById('captchaInput').value=''; return; }
  document.getElementById('captchaOverlay').style.display='none';
  // 좌석 측정은 대기열 종료부터 이미 진행 중(캡차 시간 포함)
};
document.getElementById('captchaInput').addEventListener('keydown',e=>{ if(e.key==='Enter') document.getElementById('captchaSubmit').click(); });

/* ===================== 모바일 바텀시트 / 토스트 ===================== */
document.getElementById('sheetHandle').onclick=()=>document.getElementById('sidePane').classList.toggle('collapsed');
let toastT;
function showToast(msg){ const t=document.getElementById('toast'); t.textContent=msg; t.classList.add('show'); clearTimeout(toastT); toastT=setTimeout(()=>t.classList.remove('show'),1800); }

/* ===================== 시작 ===================== */
if(sessionId){
  buildSeatData();
  buildMap();
  renderSelected();
  drawCaptcha();
  initQueue();
}
