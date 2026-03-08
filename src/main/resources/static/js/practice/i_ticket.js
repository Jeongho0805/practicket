import { authFetch } from '/js/common.js';

// ════════════════════════════════════════
// Queue System
// ════════════════════════════════════════

const QueueConfig = {
    MIN_QUEUE: 5_000,
    MAX_QUEUE: 200_000,
    MAX_REACTION_MS: 3_000,        // 3초 이상은 최대 대기로 처리
    FIXED_DEQUEUE_PER_SEC: 20_000, // MAX_QUEUE / 10초 → 항상 10초 이내 통과
    STORAGE_KEY: 'iq.queue.payload'
};

const QueueManager = {
    payload: null,
    intervalId: null,
    dom: {},

    init() {
        this.createQueueDOM();

        const stored = sessionStorage.getItem(QueueConfig.STORAGE_KEY);
        if (stored) {
            try {
                this.payload = JSON.parse(stored);
            } catch (e) {
                console.error('Queue Payload Corrupted', e);
                this.createFallbackPayload();
            }
        } else {
            console.warn('No Queue Payload Found. Creating Fallback.');
            this.createFallbackPayload();
        }

        const now = Date.now();
        if (now - this.payload.createdAtMs > 15 * 60 * 1000) {
            console.warn('Queue Payload Expired. Resetting.');
            sessionStorage.removeItem(QueueConfig.STORAGE_KEY);
            this.createFallbackPayload();
        }

        if (this.payload.status === 'PASSED') {
            this.removeQueueDOM();
            if (window.matchMedia('(max-width: 768px)').matches && sessionStorage.getItem('captcha_solved') !== 'true') {
                setTimeout(() => MobileDateScreen.show(), 0);
            }
            return;
        }

        this.renderPhase('LOADING');

        if (!this.payload.queueStartAtMs) {
            setTimeout(() => {
                this.startQueue();
            }, this.payload.introLoadingMs || 800);
        } else {
            this.startQueue();
        }
    },

    createFallbackPayload() {
        const now = Date.now();
        const reactionTimeMs = parseInt(sessionStorage.getItem('pkt.reactionTimeMs') || '0');
        const step = Math.min(Math.floor(reactionTimeMs / 100), 30); // 0.1초 단위, 최대 30단계
        const initialQueue = Math.round(
            QueueConfig.MIN_QUEUE + (step / 30) * (QueueConfig.MAX_QUEUE - QueueConfig.MIN_QUEUE)
        );

        sessionStorage.setItem('pkt.queueInitialRank', initialQueue.toString());

        this.payload = {
            version: 2,
            createdAtMs: now,
            introClickedAtMs: now,
            introLoadingMs: 800,
            queueStartAtMs: null,
            initialQueue: initialQueue,
            dequeuePerSec: QueueConfig.FIXED_DEQUEUE_PER_SEC,
            status: 'INTRO_LOADING'
        };
        this.savePayload();
    },

    savePayload() {
        sessionStorage.setItem(QueueConfig.STORAGE_KEY, JSON.stringify(this.payload));
    },

    createQueueDOM() {
        if (document.getElementById('queue-overlay')) return;

        const overlay = document.createElement('div');
        overlay.id = 'queue-overlay';
        overlay.className = 'queue-overlay';
        overlay.innerHTML = `
            <div id="queue-loading" class="queue-loading-container" style="display:none;">
                <div style="width: 50px; height: 50px; border: 5px solid #e0e0e0; border-top: 5px solid #448aff; border-radius: 50%; animation: spin 1s linear infinite; margin: 0 auto 20px;"></div>
                <div style="font-size: 18px; font-weight: bold; color: #333;">예매 정보를 불러오는 중입니다.</div>
                <style>@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }</style>
            </div>

            <div id="queue-card-container" class="queue-container" style="display:none;">
                <div class="queue-header-group">
                    <h1 class="queue-header-title">접속 인원이 많아 대기 중입니다.</h1>
                    <h2 class="queue-header-subtitle">조금만 기다려주세요.</h2>
                    <p class="queue-concert-name">2026 G-DRAGON 'FAM' MEETING</p>
                </div>
                <div class="queue-card">
                    <div class="queue-my-order-label">나의 대기순서</div>
                    <div id="queue-count" class="queue-number-display">---</div>
                    <div class="queue-progress-track">
                        <div id="queue-progress" class="queue-progress-fill"></div>
                    </div>
                    <div class="queue-info-grid">
                        <div class="queue-info-item">
                            <span class="q-label">예상 대기시간</span>
                            <span id="queue-time-left" class="q-value">계산 중...</span>
                        </div>
                        <div class="queue-info-item right">
                            <span class="q-label">상태</span>
                            <span class="q-status-badge">대기 중</span>
                        </div>
                    </div>
                </div>
                <div class="queue-footer-desc">
                    <p>잠시만 기다려주시면, 예매하기 페이지로 연결됩니다.</p>
                    <p>새로고침 하거나 재접속 하시면 대기순서가 초기화 되어 대기시간이 더 길어집니다.</p>
                </div>
            </div>
        `;
        document.body.appendChild(overlay);
        document.documentElement.style.overflow = 'hidden';
        document.body.style.overflow = 'hidden';

        this.dom = {
            overlay: overlay,
            loading: document.getElementById('queue-loading'),
            cardContainer: document.getElementById('queue-card-container'),
            count: document.getElementById('queue-count'),
            progress: document.getElementById('queue-progress'),
            timeLeft: document.getElementById('queue-time-left')
        };
    },

    removeQueueDOM() {
        const overlay = document.getElementById('queue-overlay');
        if (overlay) overlay.remove();
        document.documentElement.style.overflow = '';
        document.body.style.overflow = '';
    },

    renderPhase(phase) {
        if (!this.dom.overlay) return;

        if (phase === 'LOADING') {
            this.dom.loading.style.display = 'block';
            this.dom.cardContainer.style.display = 'none';
        } else if (phase === 'QUEUE') {
            this.dom.loading.style.display = 'none';
            this.dom.cardContainer.style.display = 'flex';
        }
    },

    startQueue() {
        if (!this.payload.queueStartAtMs) {
            this.payload.queueStartAtMs = Date.now();
            this.payload.status = 'WAITING';
            this.savePayload();
        }

        sessionStorage.setItem('pkt.queueWaitStartMs', this.payload.queueStartAtMs.toString());

        this.renderPhase('QUEUE');
        this.updateLoop();
        this.intervalId = setInterval(() => this.updateLoop(), 200);
    },

    updateLoop() {
        const now = Date.now();
        const elapsedSec = (now - this.payload.queueStartAtMs) / 1000;
        let currentQueue = Math.max(0, Math.floor(
            this.payload.initialQueue - (elapsedSec * this.payload.dequeuePerSec)
        ));

        this.renderQueue(currentQueue);

        if (currentQueue <= 0) {
            this.finishQueue();
        }
    },

    renderQueue(num) {
        if (!this.dom.count) return;

        this.dom.count.innerText = num.toLocaleString();

        const total = this.payload.initialQueue;
        const percent = total > 0 ? Math.min(100, Math.max(0, ((total - num) / total) * 100)) : 100;
        this.dom.progress.style.width = percent + '%';

        const secondsLeft = this.payload.dequeuePerSec > 0 ? Math.ceil(num / this.payload.dequeuePerSec) : 0;
        this.dom.timeLeft.innerText = secondsLeft + '초';

        if (num <= 5000 && num > 0) {
            this.dom.progress.classList.add('urgent');
        } else {
            this.dom.progress.classList.remove('urgent');
        }
    },

    finishQueue() {
        if (this.intervalId) {
            clearInterval(this.intervalId);
        }

        this.payload.status = 'PASSED';
        this.savePayload();

        const queueWaitStart = parseInt(sessionStorage.getItem('pkt.queueWaitStartMs') || '0');
        if (queueWaitStart) {
            sessionStorage.setItem('pkt.queueWaitMs', (Date.now() - queueWaitStart).toString());
        }
        sessionStorage.setItem('pkt.seatSelectionStartMs', Date.now().toString());

        const afterQueue = () => {
            this.removeQueueDOM();
            if (window.matchMedia('(max-width: 768px)').matches && sessionStorage.getItem('captcha_solved') !== 'true') {
                MobileDateScreen.show();
            }
        };

        if (this.dom.overlay) {
            this.dom.overlay.classList.add('finished');
            setTimeout(afterQueue, 700);
        } else {
            afterQueue();
        }
    }
};

// ════════════════════════════════════════
// Mobile Date / Captcha Screens (모바일 전용)
// ════════════════════════════════════════

const MobileDateScreen = {
    overlay: null,

    show() {
        document.documentElement.style.overflow = 'hidden';
        document.body.style.overflow = 'hidden';

        if (this.overlay) {
            this.overlay.style.display = 'flex';
            return;
        }

        const el = document.createElement('div');
        el.id = 'mobile-date-overlay';
        el.className = 'mob-overlay';
        el.innerHTML = `
            <div class="mob-date-header">
                <div class="mob-steps">
                    <span class="mob-step active"></span>
                    <span class="mob-step"></span>
                    <span class="mob-step"></span>
                    <span class="mob-step"></span>
                </div>
                <button class="mob-close-btn" id="mob-date-close">✕</button>
            </div>
            <div class="mob-concert-info">
                <div class="mob-concert-title">FAM + ILY : FAMILY : FAM I LOVE YOU</div>
                <div class="mob-concert-venue">KSPO DOME</div>
            </div>
            <div class="mob-calendar">
                <div class="mob-cal-month">2026.02</div>
                <div class="mob-cal-grid">
                    <div class="mob-cal-head">일</div>
                    <div class="mob-cal-head">월</div>
                    <div class="mob-cal-head">화</div>
                    <div class="mob-cal-head">수</div>
                    <div class="mob-cal-head">목</div>
                    <div class="mob-cal-head">금</div>
                    <div class="mob-cal-head">토</div>
                    <div class="mob-cal-day disabled mob-sun">1</div>
                    <div class="mob-cal-day disabled">2</div>
                    <div class="mob-cal-day disabled">3</div>
                    <div class="mob-cal-day disabled">4</div>
                    <div class="mob-cal-day disabled">5</div>
                    <div class="mob-cal-day mob-available mob-selected">6</div>
                    <div class="mob-cal-day disabled mob-sat">7</div>
                    <div class="mob-cal-day disabled mob-sun">8</div>
                    <div class="mob-cal-day disabled">9</div>
                    <div class="mob-cal-day disabled">10</div>
                    <div class="mob-cal-day disabled">11</div>
                    <div class="mob-cal-day disabled">12</div>
                    <div class="mob-cal-day disabled">13</div>
                    <div class="mob-cal-day disabled mob-sat">14</div>
                    <div class="mob-cal-day disabled mob-sun">15</div>
                    <div class="mob-cal-day disabled">16</div>
                    <div class="mob-cal-day disabled">17</div>
                    <div class="mob-cal-day disabled">18</div>
                    <div class="mob-cal-day disabled">19</div>
                    <div class="mob-cal-day disabled">20</div>
                    <div class="mob-cal-day disabled mob-sat">21</div>
                    <div class="mob-cal-day disabled mob-sun">22</div>
                    <div class="mob-cal-day disabled">23</div>
                    <div class="mob-cal-day disabled">24</div>
                    <div class="mob-cal-day disabled">25</div>
                    <div class="mob-cal-day disabled">26</div>
                    <div class="mob-cal-day disabled">27</div>
                    <div class="mob-cal-day disabled mob-sat">28</div>
                </div>
            </div>
            <div class="mob-cal-notes">
                <div class="mob-cal-note">⊙ 예매대기가 불가한 상품입니다.</div>
                <div class="mob-cal-note">※ 본 공연은 잔여석 안내서비스를 제공하지 않습니다.</div>
            </div>
            <div class="mob-time-row">
                <span class="mob-time-label">오후 7:00</span>
                <button class="mob-select-btn" id="mob-date-select">선택 ›</button>
            </div>
            <div class="mob-grade-list">
                <div class="mob-grade-item">전석 R석</div>
            </div>
        `;
        document.body.appendChild(el);
        this.overlay = el;

        el.querySelector('#mob-date-close').addEventListener('click', () => {
            if (confirm('날짜 선택을 취소하시겠습니까?')) {
                location.href = '/practice/i-ticket/intro';
            }
        });

        el.querySelector('#mob-date-select').addEventListener('click', () => {
            this.overlay.style.display = 'none';
            MobileCaptchaScreen.show();
        });
    }
};

const MobileCaptchaScreen = {
    overlay: null,

    show() {
        document.documentElement.style.overflow = 'hidden';
        document.body.style.overflow = 'hidden';

        if (this.overlay) {
            this.overlay.style.display = 'flex';
            this._drawCaptcha();
            return;
        }

        const el = document.createElement('div');
        el.id = 'mobile-captcha-overlay';
        el.className = 'mob-overlay';
        el.innerHTML = `
            <div class="mob-cap-header">
                <button class="mob-back-btn" id="mob-cap-back">‹</button>
                <div class="mob-cap-badge">✔ 안심예매</div>
            </div>
            <div class="mob-cap-body">
                <h2 class="mob-cap-title">문자를 입력해주세요</h2>
                <div class="mob-cap-img-box" id="mob-cap-box">
                    <img id="mob-cap-img" alt="보안문자" />
                    <div class="mob-cap-side-btns">
                        <button class="mob-cap-icon-btn" id="mob-cap-refresh">
                            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12a9 9 0 1 1-9-9c2.52 0 4.93 1 6.74 2.74L21 8"/><path d="M21 3v5h-5"/></svg>
                        </button>
                        <button class="mob-cap-icon-btn" id="mob-cap-voice">
                            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M11 4.702a.705.705 0 0 0-1.203-.498L6.413 7.587A1.4 1.4 0 0 1 5.416 8H3a1 1 0 0 0-1 1v6a1 1 0 0 0 1 1h2.416a1.4 1.4 0 0 1 .997.413l3.383 3.384A.705.705 0 0 0 11 19.298z"/><path d="M16 9a5 5 0 0 1 0 6"/></svg>
                        </button>
                    </div>
                </div>
                <input type="text" id="mob-cap-input" maxlength="6" autocomplete="off"
                    placeholder="문자를 입력해주세요 (대소문자구분없음)" style="text-transform:uppercase;">
                <div id="mob-cap-error" class="mob-cap-error">입력한 문자를 다시 확인해주세요</div>
                <button class="mob-cap-submit" id="mob-cap-submit-btn">입력완료</button>
                <div class="mob-cap-notes">
                    <div>· 부정예매방지를 위해 화면의 문자를 입력해주세요.</div>
                    <div>· 인증 후 좌석을 선택할 수 있습니다.</div>
                </div>
            </div>
        `;
        document.body.appendChild(el);
        this.overlay = el;
        this._drawCaptcha();

        el.querySelector('#mob-cap-back').addEventListener('click', () => {
            this.overlay.style.display = 'none';
            if (MobileDateScreen.overlay) MobileDateScreen.overlay.style.display = 'flex';
        });

        el.querySelector('#mob-cap-refresh').addEventListener('click', () => {
            this._drawCaptcha();
            el.querySelector('#mob-cap-input').value = '';
            el.querySelector('#mob-cap-error').style.visibility = 'hidden';
        });

        el.querySelector('#mob-cap-voice').addEventListener('click', () => {
            showToast('음성안내는 연습모드입니다.');
        });

        el.querySelector('#mob-cap-input').addEventListener('keydown', (e) => {
            if (e.key === 'Enter') this._submit();
        });

        el.querySelector('#mob-cap-submit-btn').addEventListener('click', () => this._submit());
    },

    _drawCaptcha() {
        const img = document.getElementById('mob-cap-img');
        const box = document.getElementById('mob-cap-box');
        if (!img || !box) return;

        let idx;
        do {
            idx = Math.floor(Math.random() * CAPTCHA_LIST.length);
        } while (CAPTCHA_LIST.length > 1 && idx === lastCaptchaIndex);
        lastCaptchaIndex = idx;

        const picked = CAPTCHA_LIST[idx];
        STATE.captchaAnswer = picked.answer;
        img.src = `/image/i-captch/${picked.file}`;

        renderCaptchaNoise(box);

        const boxW = box.clientWidth || 300;
        img.style.left = Math.floor(Math.random() * Math.max(0, boxW - 260)) + 'px';
        img.style.top = Math.floor(Math.random() * Math.max(0, 160 - 100)) + 'px';
    },

    _submit() {
        const input = document.getElementById('mob-cap-input');
        const error = document.getElementById('mob-cap-error');
        if (!input) return;

        const val = input.value.toUpperCase().trim();
        if (val === STATE.captchaAnswer) {
            sessionStorage.setItem('captcha_solved', 'true');
            if (this.overlay) { this.overlay.remove(); this.overlay = null; }
            if (MobileDateScreen.overlay) { MobileDateScreen.overlay.remove(); MobileDateScreen.overlay = null; }
            document.documentElement.style.overflow = '';
            document.body.style.overflow = '';
            setSeatPhase('AREA');
        } else {
            if (error) error.style.visibility = 'visible';
            input.focus();
        }
    }
};

// ════════════════════════════════════════
// Seat Manager
// ════════════════════════════════════════

class SeatManager {
    constructor(totalRows, totalCols) {
        this.totalRows = totalRows;
        this.totalCols = totalCols;
        this.totalSeats = totalRows * totalCols;
        this.zoneRanks = {};
        this.decayStartAtMs = 0;
        this.snapshotAtMs = 0;
        this.snapshotSoldCount = 0;
        this.soldOutAlertShown = false;
        this.zones = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H'];

        this.totalSellOutDurationMs = 60000;
        this.rushDurationMs = 20000;
        this.rushSoldRatio = 0.80;

        this.storageKeys = {
            decayStartAt: 'iq.seat.decay.startedAt',
            zoneRanks: 'iq.seat.zoneRanks_v6'
        };

        this.init();
    }

    init() {
        const storedDecay = Number(sessionStorage.getItem(this.storageKeys.decayStartAt));
        if (Number.isFinite(storedDecay) && storedDecay > 0) {
            this.decayStartAtMs = storedDecay;
        }

        let loaded = false;
        const savedRanks = sessionStorage.getItem(this.storageKeys.zoneRanks);
        if (savedRanks) {
            try {
                const parsed = JSON.parse(savedRanks);
                if (this.isValidAllZoneRanks(parsed)) {
                    this.zoneRanks = parsed;
                    loaded = true;
                }
            } catch (e) {
                // fall through
            }
        }

        if (!loaded) {
            this.generateAllZoneRanks();
            this.persistZoneRanks();
        }
        this.refreshSnapshot();
    }

    resolveDecayStartAt(fallbackNow = Date.now()) {
        const queuePayloadRaw = sessionStorage.getItem('iq.queue.payload');
        if (queuePayloadRaw) {
            try {
                const queuePayload = JSON.parse(queuePayloadRaw);
                if (Number.isFinite(queuePayload.queueStartAtMs) && queuePayload.queueStartAtMs > 0) {
                    sessionStorage.setItem(this.storageKeys.decayStartAt, String(queuePayload.queueStartAtMs));
                    return queuePayload.queueStartAtMs;
                }
            } catch (e) {
                // ignore
            }
            sessionStorage.removeItem('iq.queue.payload');
        }

        sessionStorage.setItem(this.storageKeys.decayStartAt, String(fallbackNow));
        return fallbackNow;
    }

    generateAllZoneRanks() {
        this.zones.forEach(zone => {
            this.zoneRanks[zone] = this.generateSingleZoneRank();
        });
    }

    persistZoneRanks() {
        sessionStorage.setItem(this.storageKeys.zoneRanks, JSON.stringify(this.zoneRanks));
    }

    isValidAllZoneRanks(ranksObj) {
        if (!ranksObj || typeof ranksObj !== 'object') return false;
        return this.zones.every(zone => this.isValidZoneRank(ranksObj[zone]));
    }

    isValidZoneRank(rank) {
        if (!Array.isArray(rank) || rank.length !== this.totalSeats) return false;
        const uniq = new Set(rank);
        if (uniq.size !== this.totalSeats) return false;

        for (let r = 1; r <= this.totalRows; r++) {
            for (let c = 1; c <= this.totalCols; c++) {
                if (!uniq.has(`${r}-${c}`)) return false;
            }
        }
        return true;
    }

    generateSingleZoneRank() {
        const front = [], middle = [], back = [];

        for (let r = 1; r <= this.totalRows; r++) {
            for (let c = 1; c <= this.totalCols; c++) {
                const seatId = `${r}-${c}`;
                if (r <= 5) front.push(seatId);
                else if (r <= 10) middle.push(seatId);
                else back.push(seatId);
            }
        }

        this.shuffle(front);
        this.shuffle(middle);
        this.shuffle(back);

        const pickN = (arr, n) => {
            const out = [];
            for (let i = 0; i < n && arr.length > 0; i++) out.push(arr.shift());
            return out;
        };

        const earlyFront = pickN(front, 96);
        const earlyMiddle = pickN(middle, 80);
        const earlyBack = pickN(back, 64);

        const tailFront = pickN(front, front.length);
        const tailMiddle = pickN(middle, middle.length);
        const tailBack = pickN(back, back.length);

        const interleave = (a, b, c, weights) => {
            const out = [];
            const pools = [
                { arr: [...a], w: weights[0] },
                { arr: [...b], w: weights[1] },
                { arr: [...c], w: weights[2] }
            ];

            const popFrom = (idx) => pools[idx].arr.shift();

            while (pools.some(p => p.arr.length > 0)) {
                const alive = pools
                    .map((p, idx) => ({ idx, w: p.arr.length > 0 ? p.w : 0 }))
                    .filter(p => p.w > 0);

                const totalW = alive.reduce((sum, p) => sum + p.w, 0);
                let rv = Math.random() * totalW;
                let chosen = alive[0].idx;
                for (const p of alive) {
                    rv -= p.w;
                    if (rv <= 0) {
                        chosen = p.idx;
                        break;
                    }
                }
                out.push(popFrom(chosen));
            }
            return out;
        };

        const earlyOrder = interleave(earlyFront, earlyMiddle, earlyBack, [5.0, 2.4, 1.0]);
        const tailOrder = [...tailFront, ...interleave([], tailMiddle, tailBack, [0, 2.2, 3.2])];

        return [...earlyOrder, ...tailOrder];
    }

    shuffle(array) {
        for (let i = array.length - 1; i > 0; i--) {
            const j = Math.floor(Math.random() * (i + 1));
            [array[i], array[j]] = [array[j], array[i]];
        }
    }

    computeSoldCountAt(nowMs) {
        if (!this.decayStartAtMs) {
            const resolved = this.resolveDecayStartAt(nowMs);
            if (!resolved) return 0;
            this.decayStartAtMs = resolved;
        }

        const elapsed = Math.max(0, nowMs - this.decayStartAtMs);
        if (elapsed >= this.totalSellOutDurationMs) return this.totalSeats;

        if (elapsed <= this.rushDurationMs) {
            const t = elapsed / this.rushDurationMs;
            const eased = 1 - Math.pow(1 - t, 3);
            return Math.floor(this.totalSeats * this.rushSoldRatio * eased);
        }

        const remainingWindow = this.totalSellOutDurationMs - this.rushDurationMs;
        const postElapsed = elapsed - this.rushDurationMs;
        const t = Math.min(1, postElapsed / remainingWindow);
        const easedSlow = Math.pow(t, 1.9);
        const progress = this.rushSoldRatio + (1 - this.rushSoldRatio) * easedSlow;
        return Math.floor(this.totalSeats * progress);
    }

    refreshSnapshot(nowMs = Date.now()) {
        this.snapshotAtMs = nowMs;
        this.snapshotSoldCount = this.computeSoldCountAt(nowMs);
        return this.snapshotSoldCount;
    }

    getSoldSeatsCount() {
        return this.snapshotSoldCount;
    }

    isSoldOut() {
        return this.snapshotSoldCount >= this.totalSeats;
    }

    shouldShowSoldOutAlertNow() {
        return false;
    }

    checkAvailability(row, col, zone = 'A') {
        const seatId = `${row}-${col}`;
        const ranks = this.zoneRanks[zone] || [];
        const rankIndex = ranks.indexOf(seatId);
        if (rankIndex === -1) return true;
        return rankIndex >= this.snapshotSoldCount;
    }
}

let seatManager = null;

// ════════════════════════════════════════
// Main Booking State
// ════════════════════════════════════════

const STATE = {
    step: 'SEAT',
    seatPhase: 'CAPTCHA',
    selectedSeats: [],
    ticketPrice: 88000,
    fee: 2000,
    captchaAnswer: 'BXWUMU'
};

const DOM = {
    root: document.getElementById('ip-root'),
    captchaOverlay: document.getElementById('captcha-overlay'),
    captchaInput: document.getElementById('txtCaptcha'),
    captchaImg: document.getElementById('captchaImg'),

    seatGrid: document.getElementById('seat-grid'),
    seatRows: document.getElementById('seat-rows'),
    seatAreaLabel: document.getElementById('seat-area-label'),

    seatCount: document.getElementById('seat-count'),
    seatList: document.getElementById('seat-list'),
    seatCompleteBtn: document.getElementById('seat-complete'),

    quantitySelect: document.getElementById('ticket-quantity'),

    orderName: document.getElementById('order-name'),
    orderBirth: document.getElementById('order-birth'),
    orderPhone1: document.getElementById('order-phone-1'),
    orderPhone2: document.getElementById('order-phone-2'),
    orderPhone3: document.getElementById('order-phone-3'),
    orderEmail: document.getElementById('order-email'),

    summarySeat: document.getElementById('summary-seat'),
    summaryTicket: document.getElementById('summary-ticket'),
    summaryFee: document.getElementById('summary-fee'),
    summaryDelivery: document.getElementById('summary-delivery'),
    summaryDiscount: document.getElementById('summary-discount'),
    summaryTotal: document.getElementById('summary-total'),

    toast: document.getElementById('ip-toast')
};

// ════════════════════════════════════════
// Seat Detail (복잡 좌석 그리드)
// ════════════════════════════════════════

let currentZone = 'A';
let soldOutMonitorId = null;

function refreshSeatSnapshot() {
    if (!seatManager || typeof seatManager.refreshSnapshot !== 'function') return;
    seatManager.refreshSnapshot();
}

function checkGlobalSoldOutAndRedirect() {
    if (!seatManager || typeof seatManager.shouldShowSoldOutAlertNow !== 'function') return;
    if (seatManager.shouldShowSoldOutAlertNow()) {
        alert('모든 좌석이 소진되었습니다.');
        window.location.href = '/practice/i-ticket/intro';
    }
}

function startSoldOutMonitor() {
    if (soldOutMonitorId) return;
    soldOutMonitorId = setInterval(() => {
        checkGlobalSoldOutAndRedirect();
    }, 500);
}

function toggleSeatView(action, zoneName) {
    const map = document.getElementById('areaMap');
    const detail = document.getElementById('seatDetail');
    const title = detail.querySelector('.detail-title');
    const grid = detail.querySelector('.seat-grid-scroll');
    const noticeRow = document.querySelector('.ip-map-notice-row');

    if (action === 'show') {
        currentZone = zoneName;
        map.style.display = 'none';
        detail.style.display = 'block';
        title.innerText = '◆ ' + zoneName + '구역의 좌석배치도입니다';
        title.style.fontSize = '12px';
        title.style.color = '#666';

        noticeRow.style.display = 'none';

        renderSeats(grid, zoneName);
        validateSelectedSeats();
    } else {
        map.style.display = 'flex';
        detail.style.display = 'none';
        noticeRow.style.display = 'block';
        noticeRow.innerHTML = '<span class="diamond">◆</span> 원하시는 영역을 선택해주세요. 공연장에서 위치를 클릭하거나, 오른쪽의 좌석을 선택해주세요.';
    }
}

function renderSeats(container, zoneName) {
    let html = '';

    let selectedIds = [];
    if (STATE.selectedSeats) {
        selectedIds = STATE.selectedSeats.map(s => s.id);
    }

    refreshSeatSnapshot();

    for (let r = 1; r <= 15; r++) {
        html += `<div class="seat-row-container">`;
        html += `<div class="seat-row-label">${zoneName}구역 ${r}열</div>`;
        html += `<div class="seat-row-units">`;

        for (let s = 1; s <= 20; s++) {
            if (s === 6 || s === 16) {
                html += `<div style="width:15px; height:15px;"></div>`;
            }

            let isAvailable = true;
            if (seatManager) {
                isAvailable = seatManager.checkAvailability(r, s, zoneName);
            }

            let className = 'seat-unit';
            let onClick = '';
            const seatTitle = `${zoneName}구역 ${r}열 ${s}번`;
            const isSelected = selectedIds.includes(seatTitle);

            if (isAvailable) {
                className += ' available';
                if (isSelected) className += ' selected';
                onClick = `onclick="selectSeat(this, '${zoneName}', ${r}, ${s})"`;
            } else {
                className += ' taken';
            }

            html += `<div class="${className}" ${onClick} title="${seatTitle}"></div>`;
        }
        html += `</div></div>`;
    }
    container.innerHTML = html;
}

function selectSeat(el, zone, row, num) {
    if (seatManager && !seatManager.checkAvailability(row, num, zone)) {
        alert('이미 선택된 좌석입니다.');
        return;
    }

    el.classList.toggle('selected');
    const isSelected = el.classList.contains('selected');
    updateRightPanel(zone, row, num, isSelected);
    toggleBlinkingButton();
}

function updateRightPanel(zone, row, num, isAdded) {
    const seatId = `seat-${zone}-${row}-${num}`;
    const listContainer = document.querySelector('.choice-table-body');
    const countSpan = document.querySelector('.sect-count');

    if (listContainer.classList.contains('empty')) {
        listContainer.classList.remove('empty');
        listContainer.innerHTML = '';
        listContainer.style.background = '#fff';
    }

    const title = `${zone}구역 ${row}열 ${num}번`;
    if (isAdded) {
        const exists = STATE.selectedSeats.some(s => s.id === title);
        if (!exists) {
            if (STATE.selectedSeats.length >= 1) {
                alert('1매만 선택 가능합니다.');
                const targetEl = document.querySelector(`.seat-unit[title="${title}"]`);
                if (targetEl) targetEl.classList.remove('selected');
                return;
            }
            STATE.selectedSeats.push({ id: title, row: row, col: num, price: 88000 });
        }
    } else {
        STATE.selectedSeats = STATE.selectedSeats.filter(s => s.id !== title);
    }

    if (isAdded) {
        if (!document.getElementById(seatId)) {
            listContainer.insertAdjacentHTML('beforeend', `
                <div class="choice-seat-item" id="${seatId}">
                    <div class="c-grade">전석</div>
                    <div class="c-num">${title}</div>
                </div>
            `);
        }
    } else {
        const item = document.getElementById(seatId);
        if (item) item.remove();
    }

    const total = listContainer.children.length;
    countSpan.innerText = `총 ${total}석 선택되었습니다.`;

    if (total === 0) {
        listContainer.classList.add('empty');
        listContainer.style.background = '';
    }
}

function validateSelectedSeats() {
    if (!seatManager) return;

    const stillValid = [];
    let changed = false;

    STATE.selectedSeats.forEach(s => {
        const match = s.id.match(/([A-Z0-9]+)구역 (\d+)열 (\d+)번/);
        if (match) {
            const [, z, r, n] = match;
            if (seatManager.checkAvailability(parseInt(r), parseInt(n), z)) {
                stillValid.push(s);
            } else {
                changed = true;
                const seatId = `seat-${z}-${r}-${n}`;
                const item = document.getElementById(seatId);
                if (item) item.remove();
            }
        } else {
            stillValid.push(s);
        }
    });

    if (changed) {
        STATE.selectedSeats = stillValid;
        const countSpan = document.querySelector('.sect-count');
        const listContainer = document.querySelector('.choice-table-body');
        const total = STATE.selectedSeats.length;
        countSpan.innerText = `총 ${total}석 선택되었습니다.`;
        if (total === 0) {
            listContainer.classList.add('empty');
            listContainer.style.background = '';
        }
        toggleBlinkingButton();
    }
}

function toggleBlinkingButton() {
    const btn = document.querySelector('.btn-big-red');
    if (!btn) return;
    const hasSeats = STATE.selectedSeats.length > 0;
    if (hasSeats) btn.classList.add('blinking');
    else btn.classList.remove('blinking');
}

function resetSelection() {
    STATE.selectedSeats = [];

    const listContainer = document.querySelector('.choice-table-body');
    listContainer.innerHTML = '';
    listContainer.classList.add('empty');
    listContainer.style.background = '';

    document.querySelector('.sect-count').innerText = '총 0석 선택되었습니다.';

    toggleBlinkingButton();

    const grid = document.querySelector('.seat-grid-scroll');
    if (grid) {
        renderSeats(grid, currentZone);
    }
}

function goToStep3() {
    if (STATE.selectedSeats.length === 0) {
        alert('좌석을 선택해주세요.');
        return;
    }

    refreshSeatSnapshot();

    let hasTakenSeats = false;
    STATE.selectedSeats.forEach(s => {
        const match = s.id.match(/([A-Z0-9]+)구역 (\d+)열 (\d+)번/);
        if (match) {
            const [, z, r, n] = match;
            if (!seatManager.checkAvailability(parseInt(r), parseInt(n), z)) {
                hasTakenSeats = true;
            }
        }
    });

    if (hasTakenSeats) {
        alert('이미 선택된 좌석입니다.');
        resetSelection();
        return;
    }

    completePractice();
}

// ════════════════════════════════════════
// Main Booking Functions
// ════════════════════════════════════════

function init() {
    bindEvents();
    updateUI();

    const navEntries = performance.getEntriesByType('navigation');
    const isReload = navEntries.length > 0 && navEntries[0].type === 'reload';

    if (!isReload) {
        sessionStorage.removeItem('captcha_solved');
    }

    if (window.matchMedia('(max-width: 768px)').matches) {
        DOM.captchaOverlay.setAttribute('aria-hidden', 'true');
        if (sessionStorage.getItem('captcha_solved') === 'true') {
            setSeatPhase('AREA');
        }
        // else: MobileDateScreen이 대기열 종료 후 트리거됨
    } else {
        if (sessionStorage.getItem('captcha_solved') === 'true') {
            DOM.captchaOverlay.setAttribute('aria-hidden', 'true');
            setSeatPhase('AREA');
        } else {
            renderCaptcha();
        }
    }
}

function bindEvents() {
    document.addEventListener('click', (e) => {
        const actionBtn = e.target.closest('[data-action]');
        if (actionBtn) {
            handleAction(actionBtn.dataset.action, actionBtn);
        }
    });

    if (DOM.captchaInput) {
        DOM.captchaInput.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') handleCaptchaSubmit();
        });

        DOM.captchaInput.addEventListener('focus', () => {
            DOM.captchaInput.parentElement.classList.remove('error');
        });
    }

    document.querySelectorAll('.ip-seat-section').forEach(path => {
        path.addEventListener('click', (e) => {
            if (STATE.seatPhase === 'CAPTCHA' || STATE.seatPhase === 'FOLDED') {
                if (STATE.seatPhase === 'FOLDED') renderCaptcha();
                return;
            }
            const areaId = e.target.dataset.areaId;
            enterSeatDetail(areaId);
        });
    });

    if (DOM.quantitySelect) {
        DOM.quantitySelect.addEventListener('change', updateSummary);
    }
}

function handleAction(action, target) {
    switch (action) {
        case 'captcha-submit': handleCaptchaSubmit(); break;
        case 'captcha-refresh': refreshCaptcha(); break;
        case 'captcha-cancel':
            if (confirm('초기화면으로 돌아가시겠습니까?')) location.reload();
            break;
        case 'captcha-fold':
            setSeatPhase('FOLDED');
            DOM.captchaOverlay.setAttribute('aria-hidden', 'true');
            showToast('좌석을 둘러보시려면 구역을 클릭하세요. 예매하려면 보안문자를 확인해야 합니다.');
            break;
        case 'back-to-area': setSeatPhase('AREA'); break;
        case 'seat-reset': resetSeats(); break;
        case 'seat-complete':
            if (STATE.selectedSeats.length > 0) completePractice();
            else alert('좌석을 선택해주세요.');
            break;
        case 'prev-step': goBackStep(); break;
        case 'next-step': goNextStep(); break;
        case 'toast': showToast(target.dataset.toast); break;
    }
}

function renderCaptcha() {
    STATE.seatPhase = 'CAPTCHA';
    DOM.root.setAttribute('data-seat-phase', 'CAPTCHA');
    DOM.captchaOverlay.setAttribute('aria-hidden', 'false');
    if (DOM.captchaInput) {
        DOM.captchaInput.value = '';
        DOM.captchaInput.parentElement.classList.remove('error');
    }
    drawCaptcha();
}

const CAPTCHA_LIST = [
    { file: '001.png', answer: 'NPXLUE' },
    { file: '002.png', answer: 'TBBLQK' },
    { file: '003.png', answer: 'RLBLDU' },
    { file: '004.png', answer: 'RPZTCA' },
    { file: '005.png', answer: 'TMTKKM' },
    { file: '006.png', answer: 'RTMXXA' },
    { file: '007.png', answer: 'PUKRMU' },
    { file: '008.png', answer: 'CXPWLN' },
    { file: '009.png', answer: 'LBNLND' },
    { file: '010.png', answer: 'NEXDRX' },
    { file: '011.png', answer: 'NZCTZP' },
    { file: '012.png', answer: 'BBMSWW' },
    { file: '013.png', answer: 'LBUBXB' },
    { file: '014.png', answer: 'ZZAMUL' },
    { file: '015.png', answer: 'NDCUWA' },
    { file: '016.png', answer: 'XPAAXE' },
    { file: '017.png', answer: 'SCRNKK' },
    { file: '018.png', answer: 'ASSQLR' },
    { file: '019.png', answer: 'ZCEBZQ' },
    { file: '020.png', answer: 'DPPSXP' },
];

let lastCaptchaIndex = -1;

function renderCaptchaNoise(box) {
    const W = 600, H = 200;
    const canvas = document.createElement('canvas');
    canvas.width = W;
    canvas.height = H;
    const ctx = canvas.getContext('2d');
    ctx.fillStyle = '#fff';
    ctx.fillRect(0, 0, W, H);
    const count = Math.floor(W * H / 16);
    for (let i = 0; i < count; i++) {
        const x = Math.random() * W;
        const y = Math.random() * H;
        const r = Math.random() * 0.6 + 0.2;
        const a = (Math.random() * 0.45 + 0.25).toFixed(2);
        ctx.beginPath();
        ctx.arc(x, y, r, 0, Math.PI * 2);
        ctx.fillStyle = `rgba(80,80,80,${a})`;
        ctx.fill();
    }
    box.style.backgroundImage = `url(${canvas.toDataURL()})`;
    box.style.backgroundSize = 'cover';
}

function drawCaptcha() {
    if (!DOM.captchaImg) return;

    let idx;
    do {
        idx = Math.floor(Math.random() * CAPTCHA_LIST.length);
    } while (CAPTCHA_LIST.length > 1 && idx === lastCaptchaIndex);

    lastCaptchaIndex = idx;
    const picked = CAPTCHA_LIST[idx];
    STATE.captchaAnswer = picked.answer;

    const box = DOM.captchaImg.closest('.capchaImgBox');
    if (box) renderCaptchaNoise(box);

    const imgW = 260, imgH = 100;
    const boxW = box ? box.clientWidth : 400;
    const boxH = box ? box.clientHeight : 175;
    DOM.captchaImg.style.left = Math.floor(Math.random() * Math.max(0, boxW - imgW)) + 'px';
    DOM.captchaImg.style.top  = Math.floor(Math.random() * Math.max(0, boxH - imgH)) + 'px';

    DOM.captchaImg.src = `/image/i-captch/${picked.file}`;
}

function refreshCaptcha() {
    drawCaptcha();
    if (DOM.captchaInput) {
        DOM.captchaInput.value = '';
    }
}

function handleCaptchaSubmit() {
    if (!DOM.captchaInput) return;
    const val = DOM.captchaInput.value.toUpperCase().trim();
    if (val === STATE.captchaAnswer) {
        sessionStorage.setItem('captcha_solved', 'true');
        DOM.captchaOverlay.setAttribute('aria-hidden', 'true');
        setSeatPhase('AREA');
    } else {
        DOM.captchaInput.parentElement.classList.add('error');
        DOM.captchaInput.focus();
    }
}

function setStep(step) {
    STATE.step = step;
    DOM.root.setAttribute('data-step', step);

    document.querySelectorAll('.ip-step').forEach(el => {
        if (el.dataset.stepTarget === step) el.classList.add('ip-step-active');
        else el.classList.remove('ip-step-active');
    });

    if (step === 'PRICE') {
        DOM.summarySeat.textContent = STATE.selectedSeats.length + '석';
        updateSummary();
    }
}

function setSeatPhase(phase) {
    STATE.seatPhase = phase;
    DOM.root.setAttribute('data-seat-phase', phase);
}

function goBackStep() {
    if (STATE.step === 'PRICE') setStep('SEAT');
    else if (STATE.step === 'DELIVERY') setStep('PRICE');
    else if (STATE.step === 'PAYMENT') setStep('DELIVERY');
}

function goNextStep() {
    if (STATE.step === 'PRICE') {
        setStep('DELIVERY');
    } else if (STATE.step === 'DELIVERY') {
        if (validateDelivery()) setStep('PAYMENT');
    } else if (STATE.step === 'PAYMENT') {
        if (validatePayment()) {
            alert('예매가 완료되었습니다!');
            location.reload();
        }
    }
}

function enterSeatDetail(areaId) {
    DOM.seatAreaLabel.textContent = areaId;
    setSeatPhase('SEAT');
}

function resetSeats() {
    STATE.selectedSeats = [];
    document.querySelectorAll('.ip-seat-item.is-selected').forEach(el => el.classList.remove('is-selected'));
    updateSeatSidePanel();
}

function updateSeatSidePanel() {
    DOM.seatCount.textContent = STATE.selectedSeats.length;
    DOM.seatList.innerHTML = '';
    STATE.ticketPrice = 88000;

    STATE.selectedSeats.forEach(s => {
        const div = document.createElement('div');
        div.textContent = `[전석] ${s.row}열 ${s.col}번`;
        div.style.marginBottom = '4px';
        DOM.seatList.appendChild(div);
    });

    DOM.seatCompleteBtn.disabled = STATE.selectedSeats.length === 0;
}

function updateUI() {
    // Initial UI setup placeholder
}

function updateSummary() {
    const count = STATE.selectedSeats.length;

    if (DOM.quantitySelect) {
        DOM.quantitySelect.value = count > 0 ? count : 1;
        DOM.quantitySelect.disabled = true;
    }

    const qty = count;
    const ticketTotal = STATE.ticketPrice * qty;
    const feeTotal = STATE.fee * qty;
    const total = ticketTotal + feeTotal;

    DOM.summaryTicket.textContent = ticketTotal.toLocaleString() + '원';
    DOM.summaryFee.textContent = feeTotal.toLocaleString() + '원';
    DOM.summaryTotal.textContent = total.toLocaleString() + '원';
}

function validateDelivery() {
    if (!DOM.orderName.value.trim()) { alert('이름을 입력해주세요.'); DOM.orderName.focus(); return false; }

    const birthVal = DOM.orderBirth.value;
    if (!/^\d{6}$/.test(birthVal)) { alert('생년월일 6자리를 정확히 입력해주세요.'); DOM.orderBirth.focus(); return false; }

    const p2 = DOM.orderPhone2.value;
    const p3 = DOM.orderPhone3.value;
    if (p2.length < 3 || p3.length < 4) { alert('연락처를 정확히 입력해주세요.'); DOM.orderPhone2.focus(); return false; }

    if (!DOM.orderEmail.value.includes('@')) { alert('이메일 형식이 올바르지 않습니다.'); DOM.orderEmail.focus(); return false; }

    return true;
}

function validatePayment() {
    const method = document.querySelector('input[name="pay-method"]:checked');
    if (!method) { alert('결제방식을 선택해주세요.'); return false; }

    if (method.value === 'card') {
        const cardType = document.querySelector('input[name="card-type"]:checked');
        if (!cardType) { alert('카드 종류를 선택해주세요.'); return false; }

        if (cardType.value === 'general') {
            const select = document.getElementById('card-select');
            if (!select.value) { alert('카드사를 선택해주세요.'); return false; }
        }
    }

    return true;
}

function showToast(msg) {
    if (!msg) return;
    DOM.toast.textContent = msg;
    DOM.toast.classList.add('show');
    setTimeout(() => {
        DOM.toast.classList.remove('show');
    }, 2000);
}

async function completePractice() {
    const sessionId = sessionStorage.getItem('pkt.sessionId');
    const reactionTimeMs = parseInt(sessionStorage.getItem('pkt.reactionTimeMs') || '0');
    const queueWaitMs = parseInt(sessionStorage.getItem('pkt.queueWaitMs') || '0');
    const seatStartMs = parseInt(sessionStorage.getItem('pkt.seatSelectionStartMs') || '0');
    const seatSelectionMs = seatStartMs ? Math.max(0, Date.now() - seatStartMs) : 0;
    const queueInitialRank = parseInt(sessionStorage.getItem('pkt.queueInitialRank') || '0');
    const reactionStartMs = parseInt(sessionStorage.getItem('pkt.reactionStartMs') || '0');
    const totalDurationMs = reactionStartMs ? Math.max(0, Date.now() - reactionStartMs) : 0;

    ['pkt.sessionId', 'pkt.reactionTimeMs', 'pkt.queueWaitMs',
        'pkt.queueInitialRank', 'pkt.queueWaitStartMs',
        'pkt.seatSelectionStartMs', 'pkt.reactionStartMs'
    ].forEach(k => sessionStorage.removeItem(k));

    if (!sessionId) {
        return;
    }

    try {
        const res = await authFetch('/api/practice/complete', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ session_id: sessionId, total_duration_ms: totalDurationMs, reaction_time_ms: reactionTimeMs, queue_wait_ms: queueWaitMs, seat_selection_ms: seatSelectionMs, queue_initial_rank: queueInitialRank })
        });

        if (res.ok) {
            showCompleteModal({ reactionTimeMs, queueWaitMs, seatSelectionMs, queueInitialRank });
        } else {
            const err = await res.json().catch(() => null);
            console.warn('[Practicket] complete failed:', err);
            showCompleteModal({ reactionTimeMs, queueWaitMs, seatSelectionMs, queueInitialRank });
        }
    } catch (e) {
        console.error('[Practicket] complete error:', e);
    }
}

function showCompleteModal({ reactionTimeMs, queueWaitMs, seatSelectionMs, queueInitialRank }) {
    const totalMs = reactionTimeMs + queueWaitMs + seatSelectionMs;
    const fmt = ms => (ms / 1000).toFixed(3) + 's';

    const now = new Date();
    const dateStr = [
        now.getFullYear(),
        String(now.getMonth() + 1).padStart(2, '0'),
        String(now.getDate()).padStart(2, '0')
    ].join('.') + '  ' + String(now.getHours()).padStart(2, '0') + ':' + String(now.getMinutes()).padStart(2, '0');

    document.getElementById('pkt-meta').textContent = 'I-Ticket 구버전 · ' + dateStr;
    document.getElementById('pkt-total-num').textContent = (totalMs / 1000).toFixed(3);
    document.getElementById('pkt-reaction').textContent = fmt(reactionTimeMs);
    document.getElementById('pkt-queue').textContent = fmt(queueWaitMs);
    document.getElementById('pkt-seat').textContent = fmt(seatSelectionMs);
    document.getElementById('pkt-rank').textContent = queueInitialRank ? '#' + queueInitialRank.toLocaleString() : '-';

    document.getElementById('pkt-complete-overlay').classList.add('visible');
}

// ════════════════════════════════════════
// DOMContentLoaded
// ════════════════════════════════════════

document.addEventListener('DOMContentLoaded', () => {
    // 1. Queue
    if (document.getElementById('ip-root')) {
        QueueManager.init();
    }

    // 2. Seat Manager
    seatManager = new SeatManager(15, 20);
    startSoldOutMonitor();
    checkGlobalSoldOutAndRedirect();

    // 버튼 바인딩 (인라인 스크립트에서 이동)
    const nextBtnStep2 = document.querySelector('#step2-main .btn-big-red');
    if (nextBtnStep2) nextBtnStep2.onclick = goToStep3;

    const backBtn = document.querySelector('.ip-panel-header');
    if (backBtn) backBtn.onclick = () => toggleSeatView('hide');

    const footerBtns = document.querySelectorAll('.btn-footer-sub');
    if (footerBtns.length >= 2) {
        const prevBtn = footerBtns[0];
        if (prevBtn.innerText.includes('이전단계')) {
            prevBtn.onclick = () => alert('"관람일/회차선택" 으로 넘어가는 버튼이에요. 누르지마세요!');
        }
        const resetBtn = footerBtns[1];
        if (resetBtn.innerText.includes('다시 선택')) {
            resetBtn.onclick = resetSelection;
        }
    }

    // 3. Main init
    init();
});

// ── onclick 속성에서 호출되는 함수 전역 노출 ──
window.toggleSeatView = toggleSeatView;
window.selectSeat = selectSeat;
window.resetSelection = resetSelection;
window.goToStep3 = goToStep3;
