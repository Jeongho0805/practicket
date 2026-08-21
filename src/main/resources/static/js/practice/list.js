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

const PERIOD_MAP = { '일간': 'DAILY', '주간': 'WEEKLY', '월간': 'MONTHLY' };

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

// ── 탭 전환 ──
function switchMainTab(target, btn) {
    document.querySelectorAll('.view-mode-tab').forEach(t => t.classList.remove('active'));
    btn.classList.add('active');
    document.querySelectorAll('.panel').forEach(p => p.classList.remove('active'));
    document.getElementById('panel-' + target).classList.add('active');
    document.querySelector('.ranking-top-bar .period-tabs').hidden = target !== 'ranking';
    if (target === 'myrecord') loadMyPanel();
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
    const row = document.getElementById('myRankRow');
    if (!row) return;

    try {
        const res = await authFetch(
            `/api/practice/my-rank?type=${rankingState.type}&period=${rankingState.period}`);
        if (!res.ok) throw new Error('API error');
        const my = await res.json();

        if (my.rank == null) {
            row.style.display = 'none';
            return;
        }

        row.querySelector('.mr-rank').textContent = my.rank;
        row.querySelector('.mr-nick').textContent = my.nickname || '나';
        row.querySelector('.mr-time').textContent = (my.best_ms / 1000).toFixed(3) + 's';
        row.querySelector('.rank-split').replaceWith(createSplitBar(my));
        row.style.display = 'grid';
    } catch (e) {
        row.style.display = 'none';
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
                tbody.appendChild(createRankRow(rank, item));
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

/* 구간 합이 곧 총 시간이다(서버가 좌석을 나머지로 낸다). 보안문자 이전 기록은
   captcha_ms 가 0 이라 세 칸으로 그려진다. */
function createSplitBar(item) {
    const bar = document.createElement('span');
    bar.className = 'rank-split';

    const segments = [item.reaction_time_ms, item.queue_wait_ms, item.captcha_ms, item.seat_selection_ms]
        .map(ms => Number(ms) || 0);
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
    badge.className = 'time-record-badge';
    badge.textContent = timeStr;
    timeTd.appendChild(badge);

    tr.appendChild(rankTd);
    tr.appendChild(nameTd);
    tr.appendChild(splitTd);
    tr.appendChild(timeTd);

    return tr;
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
            '<tr class="rank-msg"><td colspan="3">불러오는 중...</td></tr>';
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
                '<tr class="rank-msg"><td colspan="3">아직 기록이 없습니다. 연습을 시작해 보세요!</td></tr>';
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
                '<tr class="rank-msg"><td colspan="3">불러오기에 실패했습니다.</td></tr>';
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
    countEl.innerHTML = data.total_count + '<span style="font-size:14px;color:#94a3b8;font-family:Pretendard;font-weight:500">회</span>';

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

    const timeTd = document.createElement('td');
    const badge = document.createElement('span');
    badge.className = 'time-badge';
    badge.innerHTML = timeStr + '<span class="badge-arrow">&#9660;</span>';
    timeTd.appendChild(badge);

    tr.appendChild(attemptTd);
    tr.appendChild(dateTd);
    tr.appendChild(timeTd);

    const detailTr = document.createElement('tr');
    detailTr.className = 'detail-row hidden';
    detailTr.innerHTML = `
        <td colspan="3">
            <div class="detail-title-row">상세 소요 시간</div>
            <div class="detail-inner">
                <div class="detail-card">
                    <div class="detail-label">반응 속도</div>
                    <div class="detail-value">${(r.reaction_time_ms / 1000).toFixed(3)}s</div>
                </div>
                <div class="detail-card">
                    <div class="detail-label">대기열 소요 시간</div>
                    <div class="detail-value">${(r.queue_wait_ms / 1000).toFixed(3)}s</div>
                </div>
                <div class="detail-card">
                    <div class="detail-label">좌석 선택 속도</div>
                    <div class="detail-value">${(r.seat_selection_ms / 1000).toFixed(3)}s</div>
                </div>
                <div class="detail-card">
                    <div class="detail-label">대기열 초기 순번</div>
                    <div class="detail-value neutral">#${r.queue_initial_rank}번</div>
                </div>
            </div>
        </td>
    `;

    tr.addEventListener('click', () => toggleDetail(tr, detailTr));

    return { tr, detailTr };
}

function toggleDetail(mainRow, detailRow) {
    const isOpen = mainRow.classList.contains('open');
    document.querySelectorAll('.record-row.open').forEach(r => r.classList.remove('open'));
    document.querySelectorAll('.detail-row:not(.hidden)').forEach(r => r.classList.add('hidden'));
    if (!isOpen) {
        mainRow.classList.add('open');
        detailRow.classList.remove('hidden');
        setTimeout(() => mainRow.scrollIntoView({ behavior: 'smooth', block: 'nearest' }), 50);
    }
}

function formatDate(isoString) {
    if (!isoString) return '-';
    const d = new Date(isoString);
    const pad = n => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

// ── 전역 노출 (onclick 속성용) ──
window.goToITicket = goToITicket;
window.goToNTicket = goToNTicket;
window.goToMTicket = goToMTicket;
window.switchMainTab = switchMainTab;
window.selectAgency = selectAgency;
window.selectPeriod = selectPeriod;

// ── 초기 로드 ──
document.addEventListener('DOMContentLoaded', () => {
    // 상태만 바꾸면 탭은 기본 종목에 켜진 채 남아 표와 어긋난다.
    document.querySelectorAll('.agency-tab').forEach(b => {
        b.classList.toggle('active', b.dataset.agency === RANKING_TYPE);
    });

    setupTail('rank', 'rankingSentinel', '.ranking-table-body', () => loadRanking(false));
    setupTail('myrecord', 'myRecordSentinel', '.my-record-table-body', () => loadMyRecords(false));
    loadRanking(true);
});
