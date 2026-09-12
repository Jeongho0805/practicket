import { authFetch, showAlert } from '/js/common.js';

// 연습을 끝내고 넘어오면 주소에 예매처가 실려 온다. 그 종목의 랭킹 탭을 열고, 모르는 값이면 기본 종목의 연습하기를 연다.
const AGENCIES = ['I_TICKET_OLD', 'N_TICKET', 'M_TICKET'];
const asked = new URLSearchParams(location.search).get('agency');
const RANKING_TYPE = AGENCIES.includes(asked) ? asked : 'I_TICKET_OLD';   // 기본 노출 종목

// ── 랭킹 상태 ──
const rankingState = {
    type: RANKING_TYPE,
    period: 'DAILY',
    cursor: null,
    hasNext: false,
    loading: false,
    rankOffset: 0,
    /* 종목·기간을 바꿀 때마다 오른다. 늦게 온 옛 응답이 새 화면을 덮지 않게 번호가 다르면 버린다 */
    seq: 0
};

// ── 내 기록 상태 ──
const myState = {
    type: RANKING_TYPE,
    cursorId: null,
    hasNext: false,
    loading: false,
    totalCount: 0,
    recordOffset: 0
};

const PERIOD_MAP = { '일간': 'DAILY', '주간': 'WEEKLY', '월간': 'MONTHLY', '전체': 'ALL_TIME' };

// ── I-Ticket 카드 클릭 시 닉네임 검증 후 이동 ──
async function goToITicket(event) {
    event.preventDefault();
    try {
        const clientRes = await authFetch('/api/client');
        const clientData = await clientRes.json();
        if (!clientData.name) {
            await showAlert({ title: '닉네임을 설정해주세요', msg: '랭킹 기록을 남기려면 닉네임이 필요합니다.\n우측 상단에서 닉네임 설정 후 다시 시도해주세요.' });
            return;
        }
    } catch (e) {
        console.error('[Practicket] Failed to fetch client info:', e);
    }
    window.location.href = '/practice/i-ticket/intro';
}

async function goToNTicket(event) {
    event.preventDefault();
    try {
        const clientRes = await authFetch('/api/client');
        const clientData = await clientRes.json();
        if (!clientData.name) {
            await showAlert({ title: '닉네임을 설정해주세요', msg: '랭킹 기록을 남기려면 닉네임이 필요합니다.\n우측 상단에서 닉네임 설정 후 다시 시도해주세요.' });
            return;
        }
    } catch (e) {
        console.error('[Practicket] Failed to fetch client info:', e);
    }
    window.location.href = '/practice/n-ticket/intro';
}

async function goToMTicket(event) {
    event.preventDefault();
    try {
        const clientRes = await authFetch('/api/client');
        const clientData = await clientRes.json();
        if (!clientData.name) {
            await showAlert({ title: '닉네임을 설정해주세요', msg: '랭킹 기록을 남기려면 닉네임이 필요합니다.\n우측 상단에서 닉네임 설정 후 다시 시도해주세요.' });
            return;
        }
    } catch (e) {
        console.error('[Practicket] Failed to fetch client info:', e);
    }
    window.location.href = '/practice/m-ticket/intro';
}

// ── 뷰 전환 ──
// 목록은 그 탭을 처음 열 때 부른다. 첫 화면이 연습하기라 랭킹까지 미리 부를 이유가 없다.
let rankingLoaded = false;

/* 모바일 카드 행의 순위 칸은 고정 폭이다. 네 자리부터는 자간만 아주 조금 줄여
   숫자가 다음 열을 밀지 않게 한다. 공개 랭킹·내 순위·내 기록이 같은 규칙을 쓴다. */
function mobileNumberDigitsClass(value) {
    const digits = String(Math.abs(Number(value) || 0)).length;
    return digits >= 5 ? 'mobile-num-digits-5plus' : `mobile-num-digits-${digits}`;
}

/* PRACTICE_MID는 목록 탭에서만 보이는 단일 슬롯이다.
   연습하기에서는 숨겨 보관하고 랭킹·내 기록 첫 행으로 옮긴다.
   어느 목록이 언제 다시 그려지든 자리는 항상 지금 켜진 탭을 따라간다 — 늦게 끝난 요청이
   다른 탭으로 가져가면 보고 있는 화면에서 광고가 사라진다. */
const PRACTICE_MID_TARGETS = {
    ranking: { tbodyId: 'rankingTableBody', rowSelector: '.rank-row-clickable' },
    myrecord: { tbodyId: 'myRecordTableBody', rowSelector: '.record-row' },
};

function notifyVisiblePracticeMid() {
    window.fillVisibleAdSlots?.();
    window.reportVisibleAdBanners?.();
}

/* 슬롯은 광고 행 안에 있어 행을 지우면 문서에서 떨어져 나간다. 지우기 전에 잡아 둔 참조로만 옮긴다 */
let practiceMidHost = null;

function restorePracticeMidHome() {
    const home = document.getElementById('practiceMidHome');
    if (home && practiceMidHost) home.appendChild(practiceMidHost);
}

/* tbody 를 통째로 비우기 전에 그 안에 있는 슬롯만 홈으로 꺼낸다. 다른 탭에 있으면 건드리지 않는다 */
function parkPracticeMid(tbodyId) {
    const tbody = document.getElementById(tbodyId);
    if (practiceMidHost && tbody && tbody.contains(practiceMidHost)) restorePracticeMidHome();
}

function activeHubView() {
    return document.querySelector('.hub-tab.active')?.dataset.view;
}

function syncPracticeMidInfeed() {
    practiceMidHost ??= document.getElementById('practiceMidHost');
    if (!practiceMidHost) return;
    document.querySelectorAll('tr.practice-mid-row').forEach(row => row.remove());
    const target = PRACTICE_MID_TARGETS[activeHubView()];
    const tbody = target && document.getElementById(target.tbodyId);
    if (!tbody) {
        restorePracticeMidHome();
        return;
    }

    // 광고는 1위/최신 기록 바로 위에 둔다. 빈 목록도 안내 행 위에 남긴다.
    const tr = document.createElement('tr');
    tr.className = 'practice-mid-row';
    const td = document.createElement('td');
    td.colSpan = 4;
    td.appendChild(practiceMidHost);
    tr.appendChild(td);

    const firstItem = tbody.querySelector(target.rowSelector) || tbody.querySelector('tr');
    if (firstItem) tbody.insertBefore(tr, firstItem);
    else tbody.appendChild(tr);
    notifyVisiblePracticeMid();
}

function switchHubView(view) {
    document.querySelectorAll('.hub-tab').forEach(t => t.classList.toggle('active', t.dataset.view === view));

    const practice = view === 'practice';
    document.getElementById('view-practice').classList.toggle('active', practice);
    document.getElementById('view-rank').classList.toggle('active', !practice);
    if (practice) {
        syncPracticeMidInfeed();
        return;
    }

    const panel = view === 'ranking' ? 'ranking' : 'myrecord';
    document.querySelectorAll('.panel').forEach(p => p.classList.remove('active'));
    document.getElementById('panel-' + panel).classList.add('active');
    document.querySelector('.ranking-top-bar .period-tabs').hidden = panel !== 'ranking';

    if (panel === 'ranking') {
        if (!rankingLoaded) {
            rankingLoaded = true;
            loadRanking(true);
        } else {
            syncPracticeMidInfeed();
        }
        // 숨어 있는 동안에는 끝 감지 줄이 안 울린다. 열릴 때 다시 건다
        rearmTail('rank');
    } else {
        loadMyPanel();
        rearmTail('myrecord');
    }
}

// ── 종목(연습 타입) 전환 ──
// 랭킹·내 기록 패널이 각자 agency-tabs 를 갖고 있어, 눌린 버튼이 속한 패널만 갱신한다.
function selectAgency(btn) {
    const type = btn.dataset.agency;
    const tabs = btn.closest('.agency-tabs');
    tabs.querySelectorAll('.agency-tab').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');

    if (btn.closest('#panel-ranking')) {
        rankingState.type = type;
        loadRanking(true);
    } else {
        myState.type = type;
        loadMyPanel();
    }
}

function selectPeriod(btn) {
    btn.closest('.period-tabs').querySelectorAll('.period-tab').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    rankingState.period = PERIOD_MAP[btn.textContent.trim()];
    loadRanking(true);
}

/* 표 머리 위에 고정으로 붙는 내 줄. 위 목록과 같은 기간 기준이라 순위가 어긋나지 않는다. */
async function loadMyRank() {
    const wrap = document.getElementById('myRankWrap');
    const row = document.getElementById('myRankRow');
    const detail = document.getElementById('myRankDetail');
    if (!wrap || !row) return;
    const seq = rankingState.seq;

    try {
        const [res, dist] = await Promise.all([
            authFetch(`/api/practice/my-rank?type=${rankingState.type}&period=${rankingState.period}`),
            fetchDistribution(rankingState.type),
        ]);
        if (seq !== rankingState.seq) return;
        if (!res.ok) throw new Error('API error');
        const my = await res.json();

        if (my.rank == null) {
            wrap.style.display = 'none';
            return;
        }

        const myRank = row.querySelector('.mr-rank');
        myRank.textContent = my.rank;
        myRank.classList.remove('mobile-num-digits-1', 'mobile-num-digits-2',
            'mobile-num-digits-3', 'mobile-num-digits-4', 'mobile-num-digits-5plus');
        myRank.classList.add(mobileNumberDigitsClass(my.rank));
        row.querySelector('.mr-nick').textContent = my.nickname || '나';
        applyTierBadge(row.querySelector('.mr-tier'), dist, my.best_ms, rankingState.type, true);
        row.querySelector('.mr-time').innerHTML =
            (my.best_ms / 1000).toFixed(3) + 's<span class="badge-arrow">&#9660;</span>';
        row.querySelector('.rank-split').replaceWith(createSplitBar(my));

        detail.innerHTML = `<div class="rank-exp-inner">${segmentListHtml(my)}</div>`;
        detail.classList.add('hidden');
        row.classList.remove('open');
        row.classList.add('clickable');
        /* 종목·기간을 바꾸면 이 함수가 다시 돈다. 리스너가 겹치지 않게 매번 새로 건다. */
        row.onclick = () => {
            const isOpen = row.classList.contains('open');
            closeAllDetails();
            if (isOpen) return;
            row.classList.add('open');
            detail.classList.remove('hidden');
        };

        wrap.style.display = 'block';
    } catch (e) {
        wrap.style.display = 'none';
    }
}

/* 목록 끝 감지 줄. 스크롤 칸(root) 안으로 들어오면 다음 페이지를 부른다.
   목록이 짧아 줄이 계속 보이면 옵저버가 다시 울리지 않으므로, 한 페이지 붙일 때마다 다시 건다. */
const tails = {};

function setupTail(key, sentinelId, rootSelector, loadMore) {
    const el = document.getElementById(sentinelId);
    const root = document.querySelector(rootSelector);
    if (!el || !root) return;

    const io = new IntersectionObserver(
        entries => { if (entries.some(e => e.isIntersecting)) loadMore(); },
        { root, rootMargin: '120px' }
    );
    tails[key] = { el, io };
    io.observe(el);
}

function rearmTail(key) {
    const tail = tails[key];
    if (!tail || tail.el.hidden) return;
    tail.io.unobserve(tail.el);
    tail.io.observe(tail.el);
}

// ── 랭킹 ──
async function loadRanking(reset) {
    if (!reset && (rankingState.loading || !rankingState.hasNext)) return;
    const seq = reset ? ++rankingState.seq : rankingState.seq;

    if (reset) {
        parkPracticeMid('rankingTableBody');
        rankingState.cursor = null;
        rankingState.hasNext = false;
        rankingState.rankOffset = 0;
        document.getElementById('rankingTableBody').innerHTML =
            '<tr class="rank-msg"><td colspan="4">불러오는 중...</td></tr>';
        document.getElementById('rankingSentinel').hidden = true;
        loadMyRank();
    }

    rankingState.loading = true;

    let url = `/api/practice/rank?type=${rankingState.type}&period=${rankingState.period}&limit=20`;
    if (rankingState.cursor) {
        url += `&cursorTotalDurationMs=${rankingState.cursor.total_duration_ms}&cursorId=${rankingState.cursor.id}`;
    }

    try {
        const [res, dist] = await Promise.all([fetch(url), fetchDistribution(rankingState.type)]);
        if (seq !== rankingState.seq) return;
        if (!res.ok) throw new Error('API error');
        const data = await res.json();

        const tbody = document.getElementById('rankingTableBody');
        if (reset) tbody.innerHTML = '';

        if (data.data.length === 0 && reset) {
            tbody.innerHTML =
                '<tr class="rank-msg"><td colspan="4">아직 기록이 없습니다.</td></tr>';
        } else {
            data.data.forEach((item, i) => {
                const rank = rankingState.rankOffset + i + 1;
                const { tr, detailTr } = createRankRow(rank, item, dist);
                tbody.appendChild(tr);
                tbody.appendChild(detailTr);
            });
            rankingState.rankOffset += data.data.length;
        }

        rankingState.hasNext = data.has_next;
        rankingState.cursor = data.has_next ? data.next_cursor : null;
        document.getElementById('rankingSentinel').hidden = !data.has_next;
        syncPracticeMidInfeed();
        rearmTail('rank');

    } catch (e) {
        if (reset && seq === rankingState.seq) {
            document.getElementById('rankingTableBody').innerHTML =
                '<tr class="rank-msg"><td colspan="4">불러오기에 실패했습니다.</td></tr>';
        }
    } finally {
        if (seq === rankingState.seq) rankingState.loading = false;
    }
}

const SEGMENT_LABELS = ['반응', '대기열', '보안문자', '좌석 선택'];

/* 구간 합이 곧 총 시간이다(서버가 좌석을 나머지로 낸다). 보안문자 이전 기록은
   captcha_ms 가 0 이라 세 칸으로 그려진다. */
function segmentsOf(item) {
    return [item.reaction_time_ms, item.queue_wait_ms, item.captcha_ms, item.seat_selection_ms]
        .map(ms => Number(ms) || 0);
}

function createSplitBar(item, className = 'rank-split') {
    const bar = document.createElement('span');
    bar.className = className;

    const segments = segmentsOf(item);
    const sum = segments.reduce((a, b) => a + b, 0);
    if (!sum) return bar;

    segments.forEach((ms, i) => {
        const seg = document.createElement('i');
        seg.className = `s${i + 1}`;
        seg.style.width = (ms / sum * 100).toFixed(1) + '%';
        bar.appendChild(seg);
    });
    return bar;
}

/* 펼쳤을 때 나오는 구간 목록. 막대와 같은 순서·같은 색이라야 둘이 이어진다. */
function segmentListHtml(item) {
    const segments = segmentsOf(item);
    const sum = segments.reduce((a, b) => a + b, 0);
    if (!sum) return '';

    return segments.map((ms, i) => ms
        ? `<span class="exp-seg"><i class="lg-dot lg${i + 1}"></i>${SEGMENT_LABELS[i]}`
          + `<b>${(ms / 1000).toFixed(3)}s</b><em>${Math.round(ms / sum * 100)}%</em></span>`
        : '').join('');
}

function createSegmentRow(item, colspan) {
    const tr = document.createElement('tr');
    tr.className = 'rank-detail hidden';
    tr.innerHTML = `<td colspan="${colspan}"><div class="rank-exp-inner">${segmentListHtml(item)}</div></td>`;
    return tr;
}

/* 랭킹 줄·내 순위 줄·내 기록 줄이 한 아코디언으로 묶인다. 여러 개가 동시에 열리면
   목록이 밀려 어느 줄의 상세인지 알기 어려워진다. */
function closeAllDetails() {
    document.querySelectorAll('.rank-row-clickable.open, .record-row.open, .my-rank-row.open')
        .forEach(r => r.classList.remove('open'));
    document.querySelectorAll('.rank-detail:not(.hidden), .my-rank-detail:not(.hidden)')
        .forEach(r => r.classList.add('hidden'));
}

function bindDetailToggle(mainRow, detailRow) {
    mainRow.addEventListener('click', () => {
        const isOpen = mainRow.classList.contains('open');
        closeAllDetails();
        if (isOpen) return;
        mainRow.classList.add('open');
        detailRow.classList.remove('hidden');
        setTimeout(() => mainRow.scrollIntoView({ behavior: 'smooth', block: 'nearest' }), 50);
    });
}

/* 등급은 기록 초에 붙는다. 컷이 전체 기간 분포 하나라 기간 탭을 바꿔도 같은 기록이면 같은 등급이다. */
function applyTierBadge(el, dist, ms, type, mine = false) {
    if (!dist || !dist.tier_cut_ms.length || ms == null) {
        el.hidden = true;
        return;
    }
    const index = tierIndexOf(dist.tier_cut_ms, ms);
    // 등급 색만 갈아 끼운다. mr-tier 같은 자리 표시 클래스를 지우면 다음 갱신에서 요소를 못 찾는다
    [...el.classList].filter(c => c.startsWith('tier--')).forEach(c => el.classList.remove(c));
    el.classList.add('tier', 'tier-sm', `tier--${TIER_KEYS[index]}`);
    el.textContent = TIER_NAMES[index];
    el.hidden = false;
    bindTierPop(el, { type, index, ms, mine });
}

/* 배지 하나에 카드 하나. 줄 클릭(펼침)과 겹치지 않게 전파를 끊는다. */
function bindTierPop(el, info) {
    el.dataset.tier = info.index;
    el.dataset.type = info.type;
    el.dataset.mine = info.mine ? '1' : '';
    el.dataset.group = info.group ? '1' : '';
    if (info.ms != null) el.dataset.ms = info.ms;
    if (el.dataset.popBound) return;
    el.dataset.popBound = '1';
    el.addEventListener('click', e => {
        e.stopPropagation();
        toggleTierPop(el);
    });
}

const tierPop = document.getElementById('tierPop');

function closeTierPop() {
    if (!tierPop) return;
    tierPop.hidden = true;
    tierPop.anchorEl = null;
}

async function toggleTierPop(anchor) {
    if (!tierPop) return;
    if (!tierPop.hidden && tierPop.anchorEl === anchor) {
        closeTierPop();
        return;
    }
    const dist = await fetchDistribution(anchor.dataset.type);
    if (!dist || !dist.tier_cut_ms.length) return;

    const index = Number(anchor.dataset.tier);
    const mine = anchor.dataset.mine === '1';
    const ms = anchor.dataset.ms ? Number(anchor.dataset.ms) : null;
    tierPop.innerHTML = tierPopHtml(dist, index, mine, ms, anchor.dataset.group === '1');
    tierPop.anchorEl = anchor;
    tierPop.hidden = false;
    placeTierPop(anchor);
}

function tierPopHtml(dist, index, mine, ms, group) {
    const cuts = dist.tier_cut_ms;
    const pcts = dist.tier_percentiles;
    const cutText = i => i < cuts.length ? (cuts[i] / 1000).toFixed(2) + 's 이내' : '그보다 느림';
    const pctText = i => i < pcts.length ? `상위 ${pcts[i]}%` : '나머지';

    const rows = TIER_NAMES.map((name, i) =>
        `<div class="ts-row${mine && i === index ? ' ts-row--me' : ''}">`
        + `<span class="tier tier--${TIER_KEYS[i]}">${name}</span>`
        + `<span class="ts-pct">${pctText(i)}</span>`
        + `<span class="ts-cut">${cutText(i)}</span></div>`).join('');

    let next = '';
    if (mine && ms != null) {
        next = index > 0
            ? `<b>${TIER_NAMES[index - 1]}</b> 까지 <b>${((ms - cuts[index - 1]) / 1000).toFixed(2)}초</b>`
            : '가장 높은 등급이에요';
        next = `<div class="ts-next">${next}</div>`;
    }

    const headName = group ? `${TIER_NAMES[index]} 이상` : TIER_NAMES[index];
    return `<div class="ts-head"><span class="tier tier--${TIER_KEYS[index]}">${headName}</span>`
        + `<span class="ts-lab">${mine ? '내 등급' : '이 기록의 등급'}</span>`
        + `<span class="ts-crit">${pctText(index)} · ${cutText(index)}</span></div>`
        + next + rows;
}

/* 아래에 자리가 없으면 위로 붙인다. 폰은 양옆 12px 만 남기고 꼬리표만 배지를 가리킨다. */
function placeTierPop(anchor) {
    const a = anchor.getBoundingClientRect();
    const gap = 10;
    const mobile = window.matchMedia('(max-width: 768px)').matches;
    const width = mobile ? window.innerWidth - 24 : 300;
    const left = mobile ? 12 : Math.max(8, Math.min(window.innerWidth - width - 8, a.left + a.width / 2 - 24));
    tierPop.style.width = width + 'px';
    tierPop.style.left = left + 'px';
    tierPop.style.setProperty('--ax', (a.left + a.width / 2 - left - 6) + 'px');

    const h = tierPop.offsetHeight;
    const below = a.bottom + gap + h <= window.innerHeight - 8;
    tierPop.classList.toggle('above', !below);
    tierPop.style.top = (below ? a.bottom + gap : Math.max(8, a.top - gap - h)) + 'px';
}

document.addEventListener('click', e => {
    if (!tierPop || tierPop.hidden || e.target.closest('#tierPop')) return;
    closeTierPop();
});
document.addEventListener('keydown', e => { if (e.key === 'Escape') closeTierPop(); });
/* 목록이 스크롤되면 배지가 움직여 카드가 허공에 남는다. 그냥 닫는다. */
document.addEventListener('scroll', closeTierPop, true);

function createRankRow(rank, item, dist) {
    const tr = document.createElement('tr');
    if (rank <= 3) tr.classList.add(`rank-${rank}`);

    const timeStr = (item.total_duration_ms / 1000).toFixed(3) + 's';

    const rankTd = document.createElement('td');
    rankTd.className = `rank-num ${mobileNumberDigitsClass(rank)}`;
    rankTd.textContent = rank;

    const nameTd = document.createElement('td');
    nameTd.className = 'rank-name';
    nameTd.style.cssText = 'text-align:left;padding-left:30px;';

    const wrap = document.createElement('span');
    wrap.className = 'rank-name-wrap';
    const tier = document.createElement('span');
    applyTierBadge(tier, dist, item.total_duration_ms, rankingState.type);
    const nick = document.createElement('span');
    nick.className = 'rank-nick';
    nick.textContent = item.nickname;
    wrap.appendChild(tier);
    wrap.appendChild(nick);
    nameTd.appendChild(wrap);

    const splitTd = document.createElement('td');
    splitTd.appendChild(createSplitBar(item));

    const timeTd = document.createElement('td');
    const badge = document.createElement('span');
    badge.className = 'time-badge';
    badge.innerHTML = timeStr + '<span class="badge-arrow">&#9660;</span>';
    timeTd.appendChild(badge);

    tr.appendChild(rankTd);
    tr.appendChild(nameTd);
    tr.appendChild(splitTd);
    tr.appendChild(timeTd);
    tr.classList.add('rank-row-clickable');

    const detailTr = createSegmentRow(item, 4);
    bindDetailToggle(tr, detailTr);

    return { tr, detailTr };
}

// ── 내 기록 패널 ──
async function loadMyPanel() {
    await loadMyStats();
    loadMyRecords(true);
}

async function loadMyStats() {
    try {
        const res = await authFetch(`/api/practice/my-stats?type=${myState.type}`);
        if (!res.ok) throw new Error('stats error');
        const data = await res.json();
        renderMyStats(data);
        myState.totalCount = data.total_count ?? 0;
    } catch (e) {
        renderMyStats(null);
    }
}

async function loadMyRecords(reset) {
    if (myState.loading) return;
    if (!reset && !myState.hasNext) return;

    if (reset) {
        parkPracticeMid('myRecordTableBody');
        myState.cursorId = null;
        myState.hasNext = false;
        myState.recordOffset = 0;
        document.getElementById('myRecordTableBody').innerHTML =
            '<tr class="rank-msg"><td colspan="4">불러오는 중...</td></tr>';
        document.getElementById('myRecordSentinel').hidden = true;
    }

    myState.loading = true;

    let url = `/api/practice/my-records?type=${myState.type}&limit=20`;
    if (myState.cursorId) url += `&cursorId=${myState.cursorId}`;

    try {
        const res = await authFetch(url);
        if (!res.ok) throw new Error('records error');
        const data = await res.json();

        const tbody = document.getElementById('myRecordTableBody');
        if (reset) tbody.innerHTML = '';

        if (data.data.length === 0 && reset) {
            tbody.innerHTML =
                '<tr class="rank-msg"><td colspan="4">아직 기록이 없습니다. 연습을 시작해 보세요!</td></tr>';
        } else {
            data.data.forEach((r, i) => {
                const attemptNum = myState.totalCount - myState.recordOffset - i;
                const { tr: mainRow, detailTr } = createRecordRows(r, attemptNum);
                tbody.appendChild(mainRow);
                tbody.appendChild(detailTr);
            });
            myState.recordOffset += data.data.length;
        }

        myState.hasNext = data.has_next;
        myState.cursorId = data.has_next ? data.next_cursor : null;
        document.getElementById('myRecordSentinel').hidden = !data.has_next;
        syncPracticeMidInfeed();
        rearmTail('myrecord');

    } catch (e) {
        if (reset) {
            document.getElementById('myRecordTableBody').innerHTML =
                '<tr class="rank-msg"><td colspan="4">불러오기에 실패했습니다.</td></tr>';
        }
    } finally {
        myState.loading = false;
    }
}

/* 등급은 전체 기간 분포의 컷으로, 상위 %·순위는 전체 기간 등수 API 로 낸다. 그래프와 같은 값을 읽는다. */
async function renderMyStats(data) {
    const tierEl = document.getElementById('myTier');
    const pctEl = document.getElementById('myPercentile');
    const rankEl = document.getElementById('myOverallRank');
    const bestEl = document.getElementById('myBestRecord');

    const bestMs = data && data.total_count ? data.best_ms : null;
    delete myRankCache[myState.type];
    closeTierPop();

    if (bestMs == null) {
        tierEl.className = 'tier tier--f';
        tierEl.textContent = '-';
        tierEl.disabled = true;
        pctEl.textContent = '-';
        rankEl.textContent = '-';
        bestEl.textContent = '-';
        return;
    }

    bestEl.textContent = (bestMs / 1000).toFixed(3) + 's';

    const [dist, my] = await Promise.all([fetchDistribution(myState.type), fetchMyRank(myState.type)]);
    if (!dist || !dist.tier_cut_ms.length || !my) {
        tierEl.textContent = '-';
        pctEl.textContent = '-';
        rankEl.textContent = '-';
        return;
    }

    const index = tierIndexOf(dist.tier_cut_ms, bestMs);

    tierEl.className = `tier tier--${TIER_KEYS[index]}`;
    tierEl.textContent = TIER_NAMES[index];
    tierEl.disabled = false;
    pctEl.textContent = fmtPct(my.rank / my.total_users * 100) + '%';
    rankEl.textContent = withComma(my.rank) + '위';
    bindTierPop(tierEl, { type: myState.type, index, ms: bestMs, mine: true });
}

function createRecordRows(r, attemptNum) {
    const timeStr = (r.total_duration_ms / 1000).toFixed(3) + 's';

    const tr = document.createElement('tr');
    tr.className = 'record-row';

    const attemptTd = document.createElement('td');
    const attemptSpan = document.createElement('span');
    attemptSpan.className = `attempt-num ${mobileNumberDigitsClass(attemptNum)}`;
    attemptSpan.textContent = '#' + attemptNum;
    attemptTd.appendChild(attemptSpan);

    const dateTd = document.createElement('td');
    dateTd.className = 'date-text';
    dateTd.style.cssText = 'text-align:left;padding-left:20px;';
    dateTd.innerHTML = whenCellHtml(r.started_at);

    const splitTd = document.createElement('td');
    splitTd.appendChild(createSplitBar(r, 'row-split'));

    const timeTd = document.createElement('td');
    const badge = document.createElement('span');
    badge.className = 'time-badge';
    badge.innerHTML = timeStr + '<span class="badge-arrow">&#9660;</span>';
    timeTd.appendChild(badge);

    tr.appendChild(attemptTd);
    tr.appendChild(dateTd);
    tr.appendChild(splitTd);
    tr.appendChild(timeTd);

    const detailTr = createSegmentRow(r, 4);
    bindDetailToggle(tr, detailTr);

    return { tr, detailTr };
}

/* 한 덩어리로 붙으면 어느 쪽도 안 읽힌다. 눈이 날짜를 먼저 타도록 무게를 달리 준다. */
function whenCellHtml(isoString) {
    if (!isoString) return '-';
    const d = new Date(isoString);
    const pad = n => String(n).padStart(2, '0');
    const dayGap = Math.round((startOfDay(new Date()) - startOfDay(d)) / 86400000);
    const day = dayGap === 0 ? '오늘'
        : dayGap === 1 ? '어제'
        : `${d.getMonth() + 1}월 ${d.getDate()}일`;
    return `<span class="rec-day">${day}</span>`
        + `<span class="rec-time">${pad(d.getHours())}:${pad(d.getMinutes())}</span>`;
}

function startOfDay(d) {
    return new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();
}

// ── 기록 분포 그래프 ──
// 분포와 등급 컷은 종목당 한 벌이라 서버가 만든 것을 그대로 받아 쓴다.
// 등급도 상위 몇 %인지도 이 한 벌에서 나오므로 그래프와 내 기록 카드가 다른 값을 말할 수 없다.

const TIER_NAMES = ['SSS', 'SS', 'S', 'A', 'B', 'C', 'D', 'E', 'F'];
const TIER_KEYS = ['sss', 'ss', 's', 'a', 'b', 'c', 'd', 'e', 'f'];

let chartType = RANKING_TYPE;
const distCache = {};
const myRankCache = {};

async function fetchDistribution(type) {
    if (!(type in distCache)) {
        try {
            const res = await fetch(`/api/practice/distribution?type=${type}`);
            distCache[type] = res.ok ? await res.json() : null;
        } catch (e) {
            distCache[type] = null;
        }
    }
    return distCache[type];
}

/* 전체 기간 등수. 기록이 없으면 null 이다. */
async function fetchMyRank(type) {
    if (!(type in myRankCache)) {
        try {
            const res = await authFetch(`/api/practice/my-rank?type=${type}&period=ALL_TIME`);
            const data = await res.json();
            myRankCache[type] = data.rank == null ? null : data;
        } catch (e) {
            myRankCache[type] = null;
        }
    }
    return myRankCache[type];
}

/* 컷은 상위 p% 커트라인이다. 그 안에 들면 그 등급이고, 어느 컷에도 못 들면 마지막 등급이다. */
function tierIndexOf(cuts, ms) {
    for (let i = 0; i < cuts.length; i++) {
        if (ms <= cuts[i]) return i;
    }
    return cuts.length;
}

const fmtPct = pct => pct < 1 ? pct.toFixed(1) : String(Math.round(pct));

const toSec = ms => (ms / 1000).toFixed(2);
const withComma = n => n.toLocaleString('en-US');

async function drawChart(type) {
    const plot = document.getElementById('chartPlot');
    const hero = document.getElementById('chartHero');
    if (!plot) return;

    const [dist, my] = await Promise.all([fetchDistribution(type), fetchMyRank(type)]);
    const myMs = my ? my.best_ms : null;
    // 종목을 빠르게 바꾸면 늦게 온 응답이 지금 화면을 덮는다
    if (type !== chartType) return;

    if (!dist || !dist.bins.length) {
        hero.className = 'ch-hero text';
        hero.innerHTML = '<b>아직 기록이 없어요</b>';
        plot.innerHTML = '';
        return;
    }

    renderHistogram(plot, dist, myMs);

    if (myMs == null) {
        hero.className = 'ch-hero text';
        hero.innerHTML = '<b>내 위치를 확인해보세요</b>'
            + `<span>지금까지 ${withComma(dist.total_users)}명이 기록을 남겼어요</span>`;
        return;
    }

    const tier = tierIndexOf(dist.tier_cut_ms, myMs);
    hero.className = 'ch-hero';
    hero.innerHTML = `<b>상위 ${fmtPct(my.rank / my.total_users * 100)}%</b>`
        + `<span class="tier tier--${TIER_KEYS[tier]}">${TIER_NAMES[tier]}</span>`;
    bindTierPop(hero.querySelector('.tier'), { type, index: tier, ms: myMs, mine: true });
}

/* 막대 대신 면 곡선. 칸 가운데를 점으로 잡고 이웃과 가로 중간에서 꺾어 잇는다.
   세로는 늘려 그리므로(preserveAspectRatio none) 선 굵기는 non-scaling 으로 고정한다. */
const CURVE_W = 1000;
const CURVE_H = 100;

function renderHistogram(plot, dist, myMs) {
    const bins = dist.bins;
    const max = Math.max(...bins);
    const startMs = dist.bin_start_ms;
    const widthMs = dist.bin_width_ms;
    const endMs = startMs + bins.length * widthMs;
    const xOf = ms => (ms - startMs) / (endMs - startMs);
    const yOf = count => CURVE_H - count / max * (CURVE_H - 2);

    const pts = bins.map((c, i) => [(i + 0.5) / bins.length * CURVE_W, yOf(c)]);
    let line = `M0,${yOf(bins[0])} L${pts[0][0]},${pts[0][1]}`;
    for (let i = 0; i < pts.length - 1; i++) {
        const [x0, y0] = pts[i], [x1, y1] = pts[i + 1];
        const cx = (x0 + x1) / 2;
        line += ` C${cx},${y0} ${cx},${y1} ${x1},${y1}`;
    }
    line += ` L${CURVE_W},${yOf(bins[bins.length - 1])}`;
    const area = `${line} L${CURVE_W},${CURVE_H} L0,${CURVE_H} Z`;

    const cutLines = dist.tier_cut_ms.filter(c => c > startMs && c < endMs).map(c =>
        `<line x1="${(xOf(c) * CURVE_W).toFixed(1)}" y1="0" x2="${(xOf(c) * CURVE_W).toFixed(1)}" y2="${CURVE_H}" class="ch-cut"/>`).join('');

    // 마지막 칸보다 느린 기록은 그래프 밖이다. 점은 오른쪽 끝 바닥에 세운다
    let me = '';
    if (myMs != null) {
        const at = Math.floor((myMs - startMs) / widthMs);
        const inside = at >= 0 && at < bins.length;
        const left = Math.min(100, Math.max(0, xOf(myMs) * 100));
        const top = inside ? yOf(bins[at]) : CURVE_H;
        me = `<div class="ch-me" style="left:${left.toFixed(1)}%;top:calc(${top.toFixed(1)}% - 22px)">`
            + `<span class="mf-label">나 ${toSec(myMs)}s</span><i class="ch-dot"></i><span class="mf-stem"></span></div>`;
    }

    plot.innerHTML = '<div class="ch-area"><div class="ch-bands"></div>'
        + `<svg class="ch-svg" viewBox="0 0 ${CURVE_W} ${CURVE_H}" preserveAspectRatio="none" aria-hidden="true">`
        + '<defs><linearGradient id="chFill" x1="0" y1="0" x2="0" y2="1">'
        + '<stop offset="0" stop-color="#8f7fd0" stop-opacity=".55"/><stop offset="1" stop-color="#8f7fd0" stop-opacity=".05"/>'
        + '</linearGradient></defs>'
        + `<path d="${area}" fill="url(#chFill)"/>${cutLines}<path d="${line}" class="ch-line"/></svg>`
        + `<div class="ch-overlay">${me}<div class="ch-cursor" hidden><span class="ch-cur-label"></span><i class="ch-cur-dot"></i><span class="ch-cur-line"></span></div></div></div>`
        + `<div class="ch-axis">${axisHtml(startMs, endMs)}</div>`;

    renderTierBands(plot.querySelector('.ch-bands'), dist, startMs, endMs);
    bindChartCursor(plot.querySelector('.ch-area'), dist, startMs, endMs);
}

/* 커서 자리의 등급·초·사람 수·상위 %. 상위 % 는 그 자리까지 칸 인원을 더한 값이다.
   마우스는 올리기만, 폰은 끌기로 같은 동작이다. */
function bindChartCursor(area, dist, startMs, endMs) {
    const cursor = area.querySelector('.ch-cursor');
    const label = cursor.querySelector('.ch-cur-label');
    const dot = cursor.querySelector('.ch-cur-dot');
    const bins = dist.bins;
    const widthMs = dist.bin_width_ms;
    const max = Math.max(...bins);
    const sec = ms => String(Number((ms / 1000).toFixed(2)));
    const before = [0];
    bins.forEach(c => before.push(before[before.length - 1] + c));

    const move = clientX => {
        const rect = area.getBoundingClientRect();
        const ratio = Math.min(1, Math.max(0, (clientX - rect.left) / rect.width));
        const ms = startMs + ratio * (endMs - startMs);
        const at = Math.min(bins.length - 1, Math.floor((ms - startMs) / widthMs));
        const lo = startMs + at * widthMs;
        const faster = before[at] + bins[at] * Math.min(1, (ms - lo) / widthMs);
        const pct = Math.max(0.1, faster / dist.total_users * 100);
        const tier = tierIndexOf(dist.tier_cut_ms, ms);
        label.innerHTML = `<span class="tier tier-xs tier--${TIER_KEYS[tier]}">${TIER_NAMES[tier]}</span>`
            + `<em>${sec(lo)}~${sec(lo + widthMs)}초</em>${withComma(bins[at])}명 · 상위 ${fmtPct(pct)}%`;
        dot.style.top = (CURVE_H - bins[at] / max * (CURVE_H - 2)).toFixed(1) + '%';
        cursor.style.left = (ratio * 100).toFixed(2) + '%';
        cursor.hidden = false;
        cursor.classList.toggle('flip', ratio * rect.width + 6 + label.offsetWidth > rect.width);
    };
    const hide = () => { cursor.hidden = true; };

    area.addEventListener('mousemove', e => move(e.clientX));
    area.addEventListener('mouseleave', hide);
    area.addEventListener('touchstart', e => move(e.touches[0].clientX), { passive: true });
    area.addEventListener('touchmove', e => move(e.touches[0].clientX), { passive: true });
    area.addEventListener('touchend', hide);
}

/* 등급 구간 가운데에 배지. 폭이 40px 도 안 되는 구간은 배지가 겹치니 건너뛰되,
   S 이상 셋 중 하나라도 그렇게 빠지면 셋을 S+ 하나로 묶어 A 왼쪽 경계에 세운다.
   하나만 빼면 SSS 와 S+ 가 나란히 서서 사람이 적은 종목에서 겹쳐 보인다. */
function renderTierBands(host, dist, startMs, endMs) {
    const edges = [startMs, ...dist.tier_cut_ms, endMs];
    const pxPerMs = host.clientWidth / (endMs - startMs);
    const widthPx = i => (Math.min(endMs, edges[i + 1]) - Math.max(startMs, edges[i])) * pxPerMs;
    const sPlus = [0, 1, 2].some(i => widthPx(i) < 40);
    for (let i = 0; i < TIER_NAMES.length; i++) {
        const lo = Math.max(startMs, edges[i]);
        const hi = Math.min(endMs, edges[i + 1]);
        if (widthPx(i) < 40 || (sPlus && i <= 2)) continue;
        const badge = document.createElement('span');
        badge.className = `tier tier-sm tier--${TIER_KEYS[i]}`;
        badge.textContent = TIER_NAMES[i];
        badge.style.left = ((lo + hi) / 2 - startMs) / (endMs - startMs) * 100 + '%';
        bindTierPop(badge, { type: chartType, index: i, ms: null, mine: false });
        host.appendChild(badge);
    }
    if (!sPlus) return;

    // S 구간은 늘 좁아 S+ 가 왼쪽으로 삐져나온다. 카드 여백까지만 허용하고 뒤 배지들을 그만큼 민다
    const badge = document.createElement('span');
    badge.className = 'tier tier-sm tier--s ch-splus';
    badge.textContent = 'S+';
    bindTierPop(badge, { type: chartType, index: 2, ms: null, mine: false, group: true });
    host.appendChild(badge);
    const card = host.closest('.chart-card');
    const gutter = card ? parseFloat(getComputedStyle(card).paddingLeft) - 2 : 0;
    const w = badge.offsetWidth;
    const left = Math.max(-gutter, (edges[3] - startMs) * pxPerMs - 4 - w);
    badge.style.left = left + 'px';
    let edge = left + w + 4;
    for (const b of host.querySelectorAll('.tier:not(.ch-splus)')) {
        const half = b.offsetWidth / 2;
        if (b.offsetLeft - half >= edge) break;
        b.style.left = (edge + half) + 'px';
        edge += b.offsetWidth + 4;
    }
}

/* 눈금은 구간 경계에 맞춰 절대 위치로 찍는다. 균등 분할하면 표시된 초와 막대가 어긋난다. */
function axisHtml(startMs, endMs) {
    const stepMs = Math.max(1000, Math.round((endMs - startMs) / 4000) * 1000);
    const marks = [];
    for (let ms = Math.ceil(startMs / stepMs) * stepMs; ms <= endMs; ms += stepMs) {
        const left = (ms - startMs) / (endMs - startMs) * 100;
        marks.push(`<span style="left:${left.toFixed(1)}%">${Math.round(ms / 1000)}s</span>`);
    }
    return marks.join('');
}

// ── 전역 노출 (onclick 속성용) ──
window.goToITicket = goToITicket;
window.goToNTicket = goToNTicket;
window.goToMTicket = goToMTicket;
window.selectAgency = selectAgency;
window.selectPeriod = selectPeriod;

// ── 초기 로드 ──
document.addEventListener('DOMContentLoaded', () => {
    // 상태만 바꾸면 탭은 기본 종목에 켜진 채 남아 표와 어긋난다.
    document.querySelectorAll('.agency-tab').forEach(b => {
        b.classList.toggle('active', b.dataset.agency === RANKING_TYPE);
    });
    document.querySelectorAll('.ch-chip').forEach(b => {
        b.classList.toggle('on', b.dataset.agency === RANKING_TYPE);
        b.addEventListener('click', () => {
            chartType = b.dataset.agency;
            document.querySelectorAll('.ch-chip').forEach(c => c.classList.toggle('on', c === b));
            drawChart(chartType);
        });
    });
    document.querySelectorAll('.hub-tab').forEach(b => {
        b.addEventListener('click', () => switchHubView(b.dataset.view));
    });

    setupTail('rank', 'rankingSentinel', '.ranking-table-body', () => loadRanking(false));
    setupTail('myrecord', 'myRecordSentinel', '.my-record-table-body', () => loadMyRecords(false));

    drawChart(chartType);
    window.addEventListener('resize', () => drawChart(chartType));
});

    if (AGENCIES.includes(asked)) switchHubView('ranking');
