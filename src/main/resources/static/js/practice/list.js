import { authFetch, showAlert } from '/js/common.js';

// 연습을 끝내고 넘어오면 주소에 예매처가 실려 온다. 모르는 값이면 기본 종목을 연다.
const AGENCIES = ['I_TICKET_OLD', 'N_TICKET', 'M_TICKET'];
const asked = new URLSearchParams(location.search).get('agency');
const RANKING_TYPE = AGENCIES.includes(asked) ? asked : 'I_TICKET_OLD';   // 기본 노출 종목

// ── 전체 랭킹 상태 ──
const rankingState = {
    type: RANKING_TYPE,
    period: 'DAILY',
    cursor: null,
    hasNext: false,
    loading: false,
    rankOffset: 0
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

function switchHubView(view) {
    document.querySelectorAll('.hub-tab').forEach(t => t.classList.toggle('active', t.dataset.view === view));

    const practice = view === 'practice';
    document.getElementById('view-practice').classList.toggle('active', practice);
    document.getElementById('view-rank').classList.toggle('active', !practice);
    if (practice) return;

    const panel = view === 'ranking' ? 'ranking' : 'myrecord';
    document.querySelectorAll('.panel').forEach(p => p.classList.remove('active'));
    document.getElementById('panel-' + panel).classList.add('active');
    document.querySelector('.ranking-top-bar .period-tabs').hidden = panel !== 'ranking';

    if (panel === 'ranking') {
        if (!rankingLoaded) {
            rankingLoaded = true;
            loadRanking(true);
        }
        // 숨어 있는 동안에는 끝 감지 줄이 안 울린다. 열릴 때 다시 건다
        rearmTail('rank');
    } else {
        loadMyPanel();
        rearmTail('myrecord');
    }
}

// ── 종목(연습 타입) 전환 ──
// 전체 랭킹·내 기록 패널이 각자 agency-tabs 를 갖고 있어, 눌린 버튼이 속한 패널만 갱신한다.
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

/* 목록 끝에 고정으로 붙는 내 줄. 위 목록과 같은 기간 기준이라 순위가 어긋나지 않는다. */
async function loadMyRank() {
    const wrap = document.getElementById('myRankWrap');
    const row = document.getElementById('myRankRow');
    const detail = document.getElementById('myRankDetail');
    if (!wrap || !row) return;

    try {
        const res = await authFetch(
            `/api/practice/my-rank?type=${rankingState.type}&period=${rankingState.period}`);
        if (!res.ok) throw new Error('API error');
        const my = await res.json();

        if (my.rank == null) {
            wrap.style.display = 'none';
            return;
        }

        row.querySelector('.mr-rank').textContent = my.rank;
        row.querySelector('.mr-nick').textContent = my.nickname || '나';
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

// ── 전체 랭킹 ──
async function loadRanking(reset) {
    if (rankingState.loading) return;
    if (!reset && !rankingState.hasNext) return;

    if (reset) {
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
        const res = await fetch(url);
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
                const { tr, detailTr } = createRankRow(rank, item);
                tbody.appendChild(tr);
                tbody.appendChild(detailTr);
            });
            rankingState.rankOffset += data.data.length;
        }

        rankingState.hasNext = data.has_next;
        rankingState.cursor = data.has_next ? data.next_cursor : null;
        document.getElementById('rankingSentinel').hidden = !data.has_next;
        rearmTail('rank');

    } catch (e) {
        if (reset) {
            document.getElementById('rankingTableBody').innerHTML =
                '<tr class="rank-msg"><td colspan="4">불러오기에 실패했습니다.</td></tr>';
        }
    } finally {
        rankingState.loading = false;
    }
}

const RANK_MEDALS = { 1: '🥇', 2: '🥈', 3: '🥉' };

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

function createRankRow(rank, item) {
    const tr = document.createElement('tr');
    if (rank <= 3) tr.classList.add(`rank-${rank}`);

    const timeStr = (item.total_duration_ms / 1000).toFixed(3) + 's';

    const rankTd = document.createElement('td');
    rankTd.className = 'rank-num';
    rankTd.textContent = rank;

    const nameTd = document.createElement('td');
    nameTd.className = 'rank-name';
    nameTd.style.cssText = 'text-align:left;padding-left:30px;';

    if (RANK_MEDALS[rank]) {
        const wrap = document.createElement('span');
        wrap.className = 'rank-name-wrap';
        const medal = document.createElement('span');
        medal.className = 'rank-medal';
        medal.textContent = RANK_MEDALS[rank];
        wrap.appendChild(medal);
        wrap.appendChild(document.createTextNode(item.nickname));
        nameTd.appendChild(wrap);
    } else {
        nameTd.textContent = item.nickname;
    }

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

function renderMyStats(data) {
    const rankEl = document.getElementById('myRankValue');
    const rankSub = document.getElementById('myRankSub');
    const bestEl = document.getElementById('myBestRecord');
    const countEl = document.getElementById('myTotalCount');
    const secEl = document.getElementById('myImproveSec');
    const pctEl = document.getElementById('myImprovePct');

    if (!data || data.total_count === 0) {
        rankEl.textContent = '-';
        rankEl.className = 'stat-value highlight';
        rankSub.textContent = '이번 달 미참여';
        bestEl.textContent = '-';
        countEl.textContent = '-';
        secEl.textContent = '-';
        pctEl.style.display = 'none';
        return;
    }

    rankEl.className = 'stat-value highlight';
    rankEl.textContent = data.monthly_rank ? data.monthly_rank + '위' : '-';
    rankSub.textContent = data.monthly_rank ? '월간 기준' : '이번 달 미참여';
    bestEl.textContent = (data.best_ms / 1000).toFixed(3) + 's';
    countEl.innerHTML = data.total_count + '<span style="font-size:14px;color:#9a93b0;font-family:Pretendard;font-weight:500">회</span>';

    if (data.total_count >= 2 && data.first_ms && data.best_ms) {
        const improveSec = ((data.first_ms - data.best_ms) / 1000).toFixed(3);
        const improvePct = ((data.first_ms - data.best_ms) / data.first_ms * 100).toFixed(1);
        secEl.textContent = '-' + improveSec + 's';
        pctEl.textContent = improvePct + '% 단축';
        pctEl.style.display = 'inline-block';
    } else {
        secEl.textContent = '-';
        pctEl.style.display = 'none';
    }
}

function createRecordRows(r, attemptNum) {
    const timeStr = (r.total_duration_ms / 1000).toFixed(3) + 's';
    const dateStr = formatDate(r.started_at);

    const tr = document.createElement('tr');
    tr.className = 'record-row';

    const attemptTd = document.createElement('td');
    const attemptSpan = document.createElement('span');
    attemptSpan.className = 'attempt-num';
    attemptSpan.textContent = '#' + attemptNum;
    attemptTd.appendChild(attemptSpan);

    const dateTd = document.createElement('td');
    dateTd.className = 'date-text';
    dateTd.style.cssText = 'text-align:left;padding-left:20px;';
    dateTd.textContent = dateStr;

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

function formatDate(isoString) {
    if (!isoString) return '-';
    const d = new Date(isoString);
    const pad = n => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

// ── 기록 분포 그래프 ──
// 분포와 등급 컷은 종목당 한 벌이라 서버가 만든 것을 그대로 받아 쓴다.
// 등급도 상위 몇 %인지도 이 한 벌에서 나오므로 그래프와 내 기록 카드가 다른 값을 말할 수 없다.

const TIER_NAMES = ['SSS', 'SS', 'S', 'A', 'B', 'C', 'D', 'E', 'F'];
const TIER_KEYS = ['sss', 'ss', 's', 'a', 'b', 'c', 'd', 'e', 'f'];

let chartType = RANKING_TYPE;
const distCache = {};
const myBestCache = {};

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

async function fetchMyBestMs(type) {
    if (!(type in myBestCache)) {
        try {
            const res = await authFetch(`/api/practice/my-stats?type=${type}`);
            const data = await res.json();
            myBestCache[type] = data.total_count ? data.best_ms : null;
        } catch (e) {
            myBestCache[type] = null;
        }
    }
    return myBestCache[type];
}

/* 컷은 상위 p% 커트라인이다. 그 안에 들면 그 등급이고, 어느 컷에도 못 들면 마지막 등급이다. */
function tierIndexOf(cuts, ms) {
    for (let i = 0; i < cuts.length; i++) {
        if (ms <= cuts[i]) return i;
    }
    return cuts.length;
}

/* 컷 사이를 직선으로 이어 대략의 백분위를 낸다. 화면에 쓸 정도면 충분하다. */
function percentileOf(dist, ms) {
    const cuts = dist.tier_cut_ms;
    const pcts = dist.tier_percentiles;
    const i = tierIndexOf(cuts, ms);
    const loPct = i === 0 ? 0 : pcts[i - 1];
    const hiPct = i < pcts.length ? pcts[i] : 100;
    const loMs = i === 0 ? cuts[0] * 0.6 : cuts[i - 1];
    const hiMs = i < cuts.length ? cuts[i] : cuts[cuts.length - 1] * 1.5;
    const ratio = Math.min(1, Math.max(0, (ms - loMs) / (hiMs - loMs)));
    return Math.max(0.1, loPct + (hiPct - loPct) * ratio);
}

const toSec = ms => (ms / 1000).toFixed(2);
const withComma = n => n.toLocaleString('en-US');

async function drawChart(type) {
    const plot = document.getElementById('chartPlot');
    const hero = document.getElementById('chartHero');
    const foot = document.querySelector('#chartFoot .ce-msg');
    if (!plot) return;

    const [dist, myMs] = await Promise.all([fetchDistribution(type), fetchMyBestMs(type)]);
    // 종목을 빠르게 바꾸면 늦게 온 응답이 지금 화면을 덮는다
    if (type !== chartType) return;

    if (!dist || !dist.bins.length) {
        hero.className = 'ch-hero text';
        hero.innerHTML = '<b>아직 기록이 없어요</b>';
        plot.innerHTML = '';
        foot.textContent = '';
        return;
    }

    renderHistogram(plot, dist, myMs);

    if (myMs == null) {
        const peak = dist.bins.indexOf(Math.max(...dist.bins));
        const peakFrom = toSec(dist.bin_start_ms + peak * dist.bin_width_ms);
        const peakTo = toSec(dist.bin_start_ms + (peak + 1) * dist.bin_width_ms);
        hero.className = 'ch-hero text';
        hero.innerHTML = '<b>내 위치를 확인해보세요</b>'
            + `<span>지금까지 ${withComma(dist.total_users)}명이 기록을 남겼어요</span>`;
        foot.textContent = `가장 많은 구간은 ${peakFrom}~${peakTo}초예요`;
        return;
    }

    const pct = percentileOf(dist, myMs);
    const tier = tierIndexOf(dist.tier_cut_ms, myMs);
    hero.className = 'ch-hero';
    hero.innerHTML = `<b>상위 ${pct < 1 ? pct.toFixed(1) : Math.round(pct)}%</b>`
        + `<span class="tier tier--${TIER_KEYS[tier]}">${TIER_NAMES[tier]}</span>`;
    foot.textContent = '';
}

function renderHistogram(plot, dist, myMs) {
    const bins = dist.bins;
    const max = Math.max(...bins);
    const startMs = dist.bin_start_ms;
    const widthMs = dist.bin_width_ms;
    const endMs = startMs + bins.length * widthMs;

    // 마지막 칸보다 느린 기록은 그래프 밖이다. 막대는 안 물들이고 깃발만 오른쪽 끝에 세운다
    const myAt = myMs == null ? -1
        : Math.min(bins.length - 1, Math.max(0, Math.floor((myMs - startMs) / widthMs)));
    const myLeft = myMs == null ? 0
        : Math.min(100, Math.max(0, (myMs - startMs) / (endMs - startMs) * 100));

    const bars = bins.map((count, i) =>
        `<div class="bin${i === myAt ? ' me' : ''}" style="height:${(count / max * 100).toFixed(1)}%"></div>`
    ).join('');

    const flag = myMs == null ? ''
        : `<div class="me-flag" style="left:${myLeft.toFixed(1)}%;height:100%">`
          + `<span class="mf-label">나 ${toSec(myMs)}s</span>`
          + '<span class="mf-stem" style="flex:1"></span></div>';

    plot.innerHTML = `<div class="hist">${bars}${flag}</div>`
        + `<div class="hist-axis">${axisHtml(startMs, endMs)}</div>`;
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
