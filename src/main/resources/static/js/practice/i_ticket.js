import { authFetch, showAlert } from '/js/common.js';

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
                    <p class="queue-concert-name">2026 HOYA SOLO CONCERT 'HOWL'</p>
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
                <div class="mob-concert-title">HO + YA : HOYA : HO I LOVE YOU</div>
                <div class="mob-concert-venue">프랙티켓홀</div>
            </div>
            <div class="mob-calendar">
                <div class="mob-cal-month">2026.02</div>
                <div class="mob-cal-grid">
                    <div class="mob-cal-head mob-head-sun">일</div>
                    <div class="mob-cal-head">월</div>
                    <div class="mob-cal-head">화</div>
                    <div class="mob-cal-head">수</div>
                    <div class="mob-cal-head">목</div>
                    <div class="mob-cal-head">금</div>
                    <div class="mob-cal-head">토</div>
                    <div class="mob-cal-day disabled"></div>
                    <div class="mob-cal-day disabled"></div>
                    <div class="mob-cal-day disabled"></div>
                    <div class="mob-cal-day disabled"></div>
                    <div class="mob-cal-day disabled"></div>
                    <div class="mob-cal-day disabled"></div>
                    <div class="mob-cal-day disabled">1</div>
                    <div class="mob-cal-day disabled mob-sun">2</div>
                    <div class="mob-cal-day disabled">3</div>
                    <div class="mob-cal-day disabled">4</div>
                    <div class="mob-cal-day disabled">5</div>
                    <div class="mob-cal-day disabled">6</div>
                    <div class="mob-cal-day disabled">7</div>
                    <div class="mob-cal-day mob-available mob-sat" id="mob-day-8">8</div>
                    <div class="mob-cal-day disabled mob-sun">9</div>
                    <div class="mob-cal-day disabled">10</div>
                    <div class="mob-cal-day disabled">11</div>
                    <div class="mob-cal-day disabled">12</div>
                    <div class="mob-cal-day disabled">13</div>
                    <div class="mob-cal-day disabled">14</div>
                    <div class="mob-cal-day disabled">15</div>
                    <div class="mob-cal-day disabled mob-sun">16</div>
                    <div class="mob-cal-day disabled">17</div>
                    <div class="mob-cal-day disabled">18</div>
                    <div class="mob-cal-day disabled">19</div>
                    <div class="mob-cal-day disabled">20</div>
                    <div class="mob-cal-day disabled">21</div>
                    <div class="mob-cal-day disabled">22</div>
                    <div class="mob-cal-day disabled mob-sun">23</div>
                    <div class="mob-cal-day disabled">24</div>
                    <div class="mob-cal-day disabled">25</div>
                    <div class="mob-cal-day disabled">26</div>
                    <div class="mob-cal-day disabled">27</div>
                    <div class="mob-cal-day disabled">28</div>
                </div>
            </div>
            <div class="mob-date-placeholder" id="mob-date-placeholder">날짜를 선택해 주세요.</div>
            <div class="mob-date-info" id="mob-date-info">
                <div class="mob-cal-notes">
                    <div class="mob-cal-note">⊙ 예매대기가 불가한 상품입니다.</div>
                    <div class="mob-cal-note">※ 본 공연은 잔여석 안내서비스를 제공하지 않습니다.</div>
                </div>
                <div class="mob-time-row">
                    <span class="mob-time-label">오후 7:00</span>
                    <button class="mob-select-btn" id="mob-date-select">선택 ›</button>
                </div>
                <div class="mob-grade-list">
                    <div class="mob-grade-item">전석</div>
                </div>
            </div>
        `;
        document.body.appendChild(el);
        this.overlay = el;

        el.querySelector('#mob-date-close').addEventListener('click', () => {
            if (confirm('날짜 선택을 취소하시겠습니까?')) {
                location.href = '/practice/i-ticket/intro';
            }
        });

        el.querySelector('#mob-day-8').addEventListener('click', () => {
            el.querySelectorAll('.mob-cal-day.mob-selected').forEach(d => d.classList.remove('mob-selected'));
            el.querySelector('#mob-day-8').classList.add('mob-selected');
            el.querySelector('#mob-date-placeholder').style.display = 'none';
            el.querySelector('#mob-date-info').style.display = 'flex';
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
        document.querySelector('meta[name="viewport"]').content = 'width=device-width, initial-scale=1.0, user-scalable=no';

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
                <button class="mob-back-btn" id="mob-cap-back"><svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M15 18l-6-6 6-6"/></svg></button>
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
            document.querySelector('meta[name="viewport"]').content = 'width=device-width, initial-scale=1.0';
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

        const imgW = 150;
        const imgH = 60;
        img.style.width = imgW + 'px';
        const boxW = box.clientWidth || 280;
        const boxH = box.clientHeight || 110;
        const btnAreaW = 48; // 오른쪽 버튼 영역 제외
        img.style.left = Math.floor(Math.random() * Math.max(0, boxW - imgW - btnAreaW)) + 'px';
        img.style.top = Math.floor(Math.random() * Math.max(0, boxH - imgH)) + 'px';
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
            document.querySelector('meta[name="viewport"]').content = 'width=device-width, initial-scale=1.0';
            setSeatPhase('AREA');
            MobileSeatScreen.show();
        } else {
            if (error) error.style.visibility = 'visible';
            input.focus();
        }
    }
};

// ════════════════════════════════════════
// PinchZoomScroll — 콘텐츠 영역 전용 핀치 줌
// 브라우저 기본 줌 대신 JS가 직접 처리.
// scrollEl: overflow:auto 스크롤 컨테이너
// contentEl: zoom을 적용할 내부 콘텐츠 요소
// ════════════════════════════════════════

class PinchZoomScroll {
    constructor(scrollEl, contentEl) {
        this.scrollEl = scrollEl;
        this.contentEl = contentEl;
        this.scale = 1;
        this.MIN = 1;
        this.MAX = 3;
        this.startDist = 0;
        this.startScale = 1;
        this.lastX = 0;
        this.lastY = 0;
        this.pinching = false;

        // touch-action: none → JS가 1손가락 스크롤 + 2손가락 줌 모두 직접 처리
        scrollEl.style.touchAction = 'none';

        scrollEl.addEventListener('touchstart',  this._onStart.bind(this), { passive: false });
        scrollEl.addEventListener('touchmove',   this._onMove.bind(this),  { passive: false });
        scrollEl.addEventListener('touchend',    this._onEnd.bind(this),   { passive: true });
    }

    _dist(touches) {
        return Math.hypot(
            touches[0].clientX - touches[1].clientX,
            touches[0].clientY - touches[1].clientY
        );
    }

    _onStart(e) {
        if (e.touches.length === 2) {
            this.pinching = true;
            this.startDist = this._dist(e.touches);
            this.startScale = this.scale;
        } else if (e.touches.length === 1) {
            this.pinching = false;
            this.lastX = e.touches[0].clientX;
            this.lastY = e.touches[0].clientY;
        }
    }

    _onMove(e) {
        e.preventDefault();
        if (e.touches.length === 2 && this.pinching) {
            const dist = this._dist(e.touches);
            this.scale = Math.min(Math.max(this.startScale * (dist / this.startDist), this.MIN), this.MAX);
            this.contentEl.style.zoom = this.scale;
        } else if (e.touches.length === 1 && !this.pinching) {
            const dx = this.lastX - e.touches[0].clientX;
            const dy = this.lastY - e.touches[0].clientY;
            this.scrollEl.scrollLeft += dx;
            this.scrollEl.scrollTop  += dy;
            this.lastX = e.touches[0].clientX;
            this.lastY = e.touches[0].clientY;
        }
    }

    _onEnd(e) {
        if (e.touches.length < 2) this.pinching = false;
        if (e.touches.length === 1) {
            this.lastX = e.touches[0].clientX;
            this.lastY = e.touches[0].clientY;
        }
    }

    reset() {
        this.scale = 1;
        this.contentEl.style.zoom = 1;
    }
}

// ════════════════════════════════════════
// Mobile Seat Area Screen (모바일 전용)
// ════════════════════════════════════════

const MobileSeatScreen = {
    overlay: null,

    show() {
        document.documentElement.style.overflow = 'hidden';
        document.body.style.overflow = 'hidden';

        if (this.overlay) {
            this.overlay.style.display = 'flex';
            return;
        }

        const el = document.createElement('div');
        el.id = 'mob-seat-overlay';
        el.className = 'mob-overlay';
        el.innerHTML = `
            <div class="mob-seat-nav">
                <button class="mob-seat-nav-btn" id="mob-seat-back">
                    <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M15 18l-6-6 6-6"/></svg>
                </button>
                <div class="mob-seat-tabs">
                    <button class="mob-seat-tab active" id="mob-tab-remain">잔여좌석보기</button>
                    <button class="mob-seat-tab" id="mob-tab-price">좌석가격보기</button>
                </div>
                <button class="mob-seat-nav-btn" id="mob-seat-refresh">
                    <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12a9 9 0 1 1-9-9c2.52 0 4.93 1 6.74 2.74L21 8"/><path d="M21 3v5h-5"/></svg>
                </button>
            </div>
            <div class="mob-seat-guide-area">
                <div class="mob-seat-guide-top">구역내 상단이 무대와 가까운 쪽입니다. <small>The upper end of the section is the closest area to the stage.</small></div>
                <div class="mob-seat-guide-warn">※ 가로로 (한줄로 나란히) 예매해 주세요 &nbsp; Please reserve seats horizontally (in a row).</div>
            </div>
            <div class="mob-seat-map-wrap">
                <div class="mob-seat-svg-scroll">
                    <svg class="mob-seat-svg" viewBox="0 0 620 470" role="img" aria-label="좌석 구역 지도">
                        <text class="level-label" x="150" y="115">2F</text>
                        <text class="level-label" x="462" y="115">2F</text>
                        <text class="level-label floor-label" x="306" y="332">FLOOR</text>
                        <path class="stage-top-shape" d="M206 110 H414 L392 142 H228 Z"></path>
                        <text class="stage-top-text" x="310" y="136">STAGE</text>
                        <path class="outer-frame" d="M112 128 L170 128 L170 320 L222 382 H398 L450 320 L450 128 L508 128 L508 340 L420 426 H200 L112 340 Z"></path>
                        <g class="svg-zone zone-floor-purple" onclick="toggleSeatView('show', 'E')">
                            <polygon points="146,136 186,136 186,296 146,296"></polygon>
                            <text x="166" y="216">E</text>
                        </g>
                        <g class="svg-zone zone-floor-purple" onclick="toggleSeatView('show', 'F')">
                            <polygon points="434,136 474,136 474,296 434,296"></polygon>
                            <text x="454" y="216">F</text>
                        </g>
                        <g class="svg-zone zone-floor-purple" onclick="toggleSeatView('show', 'G')">
                            <polygon points="146,290 186,290 186,334 236,334 236,382 146,382"></polygon>
                            <text x="192" y="352">G</text>
                        </g>
                        <g class="svg-zone zone-floor-purple" onclick="toggleSeatView('show', 'H')">
                            <polygon points="434,290 474,290 474,382 384,382 384,334 434,334"></polygon>
                            <text x="426" y="352">H</text>
                        </g>
                        <g class="svg-zone zone-floor-purple" onclick="toggleSeatView('show', 'A')">
                            <polygon points="226,158 300,158 300,235 206,235 206,184"></polygon>
                            <text x="246" y="200">A</text>
                        </g>
                        <g class="svg-zone zone-floor-purple" onclick="toggleSeatView('show', 'B')">
                            <polygon points="320,158 394,158 414,184 414,235 320,235"></polygon>
                            <text x="364" y="200">B</text>
                        </g>
                        <g class="svg-zone zone-floor-purple" onclick="toggleSeatView('show', 'C')">
                            <polygon points="206,246 300,246 300,320 226,320 206,300"></polygon>
                            <text x="246" y="292">C</text>
                        </g>
                        <g class="svg-zone zone-floor-purple" onclick="toggleSeatView('show', 'D')">
                            <polygon points="320,246 414,246 414,300 394,320 320,320"></polygon>
                            <text x="364" y="292">D</text>
                        </g>
                        <rect class="stage-center-box" x="270" y="206" width="80" height="74"></rect>
                        <text class="stage-center-text" x="310" y="249">STAGE</text>
                        <rect class="console-box" x="254" y="350" width="112" height="28"></rect>
                        <text class="console-text" x="310" y="370">CONSOLE</text>
                    </svg>
                </div>
            </div>
        `;
        document.body.appendChild(el);
        this.overlay = el;

        // 구역 지도 영역에 핀치 줌 적용 (nav/footer 영향 없이 지도만 확대)
        this._pinchZoom = new PinchZoomScroll(
            el.querySelector('.mob-seat-map-wrap'),
            el.querySelector('.mob-seat-svg-scroll')
        );

        el.querySelector('#mob-seat-back').addEventListener('click', () => {
            this._pinchZoom.reset();
            this.overlay.style.display = 'none';
            if (MobileDateScreen.overlay) {
                MobileDateScreen.overlay.style.display = 'flex';
            } else {
                MobileDateScreen.show();
            }
        });

        el.querySelector('#mob-seat-refresh').addEventListener('click', () => {
            refreshSeatSnapshot();
        });

        el.querySelector('#mob-tab-price').addEventListener('click', () => {
            showToast('좌석 가격 보기는 연습 모드에서 지원하지 않습니다.');
        });
    }
};

// ════════════════════════════════════════
// Mobile Seat Detail Screen (모바일 전용)
// ════════════════════════════════════════

const MobileSeatDetailScreen = {
    overlay: null,

    show(zoneName) {
        currentZone = zoneName;
        refreshSeatSnapshot();
        validateSelectedSeats();

        if (this.overlay) {
            document.getElementById('mob-detail-title').textContent = zoneName + '구역 좌석선택';
            this._renderGrid(zoneName);
            this.overlay.style.display = 'flex';
            return;
        }

        const el = document.createElement('div');
        el.id = 'mob-detail-overlay';
        el.className = 'mob-overlay';
        el.innerHTML = `
            <div class="mob-seat-nav">
                <button class="mob-seat-nav-btn" id="mob-detail-back">
                    <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M15 18l-6-6 6-6"/></svg>
                </button>
                <div class="mob-seat-tabs">
                    <button class="mob-seat-tab active">잔여좌석보기</button>
                    <button class="mob-seat-tab" id="mob-detail-tab-price">좌석가격보기</button>
                </div>
                <button class="mob-seat-nav-btn" id="mob-detail-refresh">
                    <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12a9 9 0 1 1-9-9c2.52 0 4.93 1 6.74 2.74L21 8"/><path d="M21 3v5h-5"/></svg>
                </button>
            </div>
            <div class="mob-detail-zone-title" id="mob-detail-title">${zoneName}구역 좌석선택</div>
            <div class="mob-detail-grid-wrap" id="mob-detail-grid"></div>
            <div class="mob-detail-bottom-bar" id="mob-seat-detail-bar" style="display:none;">
                <div class="mob-bar-info">
                    <div class="mob-bar-row1">
                        <span class="mob-bar-grade" id="mob-bar-grade"></span>
                    </div>
                    <div class="mob-bar-row2">
                        <span class="mob-bar-seat-detail" id="mob-bar-seat-info"></span>
                    </div>
                </div>
                <button class="mob-bar-complete-btn" id="mob-detail-complete">
                    <span class="mob-bar-count" id="mob-bar-count">총 0 매</span>
                    <span class="mob-bar-price-label">티켓가격선택</span>
                </button>
            </div>
        `;
        document.body.appendChild(el);
        this.overlay = el;

        // 줌 대상 inner wrapper 생성 (scroll 컨테이너와 분리)
        const gridWrap = el.querySelector('.mob-detail-grid-wrap');
        const gridInner = document.createElement('div');
        gridInner.id = 'mob-detail-grid-inner';
        gridWrap.appendChild(gridInner);

        this._gridInner = gridInner;
        this._renderGrid(zoneName);

        // 좌석 그리드 영역에 핀치 줌 적용 (nav/하단바 영향 없이 그리드만 확대)
        this._pinchZoom = new PinchZoomScroll(gridWrap, gridInner);

        el.querySelector('#mob-detail-back').addEventListener('click', () => {
            this._pinchZoom.reset();
            this.overlay.style.display = 'none';
        });

        el.querySelector('#mob-detail-refresh').addEventListener('click', () => {
            refreshSeatSnapshot();
            resetSelection();
        });

        el.querySelector('#mob-detail-tab-price').addEventListener('click', () => {
            showToast('좌석 가격 보기는 연습 모드에서 지원하지 않습니다.');
        });

        el.querySelector('#mob-detail-complete').addEventListener('click', () => {
            goToStep3();
        });
    },

    _renderGrid(zoneName) {
        const target = this._gridInner || document.getElementById('mob-detail-grid');
        if (!target) return;
        target.innerHTML = '';
        renderSeats(target, zoneName);
        this._syncBar();
    },

    _syncBar() {
        const bar = document.getElementById('mob-seat-detail-bar');
        if (!bar) return;
        const total = STATE.selectedSeats.length;

        if (total > 0) {
            const seat = STATE.selectedSeats[0];
            bar.querySelector('#mob-bar-grade').textContent = `⊙ R석 ${total}매`;
            bar.querySelector('#mob-bar-seat-info').textContent = `R석 ${seat.id}`;
            bar.querySelector('#mob-bar-count').textContent = `총 ${total} 매`;
            bar.style.display = 'flex';
        } else {
            bar.style.display = 'none';
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
        this.rushSoldRatio = 0.95;

        this.storageKeys = {
            decayStartAt: 'iq.seat.decay.startedAt',
            zoneRanks: 'iq.seat.zoneRanks_v7'
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

        const earlyFront = pickN(front, Math.floor(front.length * 0.96));
        const earlyMiddle = pickN(middle, Math.floor(middle.length * 0.80));
        const earlyBack = pickN(back, Math.floor(back.length * 0.64));

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
        if (this.soldOutAlertShown) return false;
        if (this.isSoldOut()) {
            this.soldOutAlertShown = true;
            return true;
        }
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
    ticketPrice: 0,
    fee: 0,
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
    refreshSeatSnapshot();
    if (seatManager.shouldShowSoldOutAlertNow()) {
        showSoldOutModal();
    }
}

function showSoldOutModal() {
    const overlay = document.getElementById('pkt-soldout-overlay');
    if (!overlay) return;

    document.documentElement.style.overflow = '';
    document.body.style.overflow = '';

    const fmt = ms => ms > 0 ? (ms / 1000).toFixed(2) + '초' : '-';

    const reactionMs = parseInt(sessionStorage.getItem('pkt.reactionTimeMs') || '0');
    const queueWaitMs = parseInt(sessionStorage.getItem('pkt.queueWaitMs') || '0');
    const initialRank = sessionStorage.getItem('pkt.queueInitialRank');
    const seatStartMs = parseInt(sessionStorage.getItem('pkt.seatSelectionStartMs') || '0');
    const seatMs = seatStartMs ? Math.max(0, Date.now() - seatStartMs) : 0;
    const totalMs = reactionMs + queueWaitMs + seatMs;

    const meta = ['I-Ticket'];
    if (initialRank) meta.push('대기 순번 ' + Number(initialRank).toLocaleString() + '번에서 출발');
    document.getElementById('pkt-fail-meta').textContent = meta.join(' · ');

    document.getElementById('pkt-fail-reaction').textContent = fmt(reactionMs);
    document.getElementById('pkt-fail-queue').textContent = fmt(queueWaitMs);
    document.getElementById('pkt-fail-seat').textContent = fmt(seatMs);
    /* 같은 값이 문장에도 나오므로 소수 자릿수를 맞춘다 */
    document.getElementById('pkt-fail-total').textContent = (totalMs / 1000).toFixed(1) + '초';

    /* 좌석 구간까지 그린다. 매진으로 끝났다면 시간을 가장 많이 쓴 곳이 좌석 화면인데,
       그 구간을 빼면 막대가 대기열로 꽉 차 "반응 속도를 줄여라"는 힌트와 서로 어긋난다. */
    renderSplitBar(document.getElementById('pkt-fail-stack'),
                   [reactionMs, queueWaitMs, seatMs], totalMs);

    const sellOutMs = seatManager ? seatManager.totalSellOutDurationMs : 0;
    document.getElementById('pkt-fail-msg').textContent = sellOutMs
        ? `좌석이 다 팔리기까지 ${(sellOutMs / 1000).toFixed(0)}초, 여기까지 ${(totalMs / 1000).toFixed(1)}초 걸렸어요`
        : '다음엔 더 빠르게 도전해 보세요';

    const failHint = document.getElementById('pkt-fail-hint');
    const worst = [[reactionMs, '반응 속도'], [queueWaitMs, '대기열'], [seatMs, '좌석 화면']]
        .sort((a, b) => b[0] - a[0])[0];
    if (worst[0] > 0) {
        failHint.innerHTML = '가장 오래 걸린 구간은 '
            + `<b>${worst[1]} ${(worst[0] / 1000).toFixed(1)}초</b>예요.`;
        failHint.style.display = 'block';
    } else {
        failHint.style.display = 'none';
    }

    overlay.classList.add('visible');
}

function startSoldOutMonitor() {
    if (soldOutMonitorId) return;
    soldOutMonitorId = setInterval(() => {
        checkGlobalSoldOutAndRedirect();
    }, 500);
}

function toggleSeatView(action, zoneName) {
    if (action === 'show' && window.matchMedia('(max-width: 768px)').matches) {
        MobileSeatDetailScreen.show(zoneName);
        return;
    }
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

        for (let s = 1; s <= 30; s++) {
            if (s === 11 || s === 21) {
                html += `<div style="width:8px; height:12px;"></div>`;
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
                showToast('1매만 선택 가능합니다.');
                const targetEl = document.querySelector(`.seat-unit[title="${title}"]`);
                if (targetEl) targetEl.classList.remove('selected');
                return;
            }
            STATE.selectedSeats.push({ id: title, row: row, col: num, price: 0 });
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

    if (window.matchMedia('(max-width: 768px)').matches) {
        MobileSeatDetailScreen._syncBar();
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
        showAlert({ title: '안내', msg: '이미 선택된 좌석입니다.' });
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

    if (MobileSeatDetailScreen.overlay) {
        MobileSeatDetailScreen._renderGrid(currentZone);
    }
}

async function goToStep3() {
    if (STATE.selectedSeats.length === 0) {
        showToast('좌석을 선택해주세요.');
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
        await showAlert({ title: '안내', msg: '이미 선택된 좌석입니다.' });
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
            MobileSeatScreen.show();
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
            else showToast('좌석을 선택해주세요.');
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

async function goNextStep() {
    if (STATE.step === 'PRICE') {
        setStep('DELIVERY');
    } else if (STATE.step === 'DELIVERY') {
        if (validateDelivery()) setStep('PAYMENT');
    } else if (STATE.step === 'PAYMENT') {
        if (validatePayment()) {
            await showAlert({ title: '예매 완료', msg: '예매가 완료되었습니다!' });
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
    STATE.ticketPrice = 0;

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

    DOM.summaryTicket.textContent = '무료';
    DOM.summaryFee.textContent = '무료';
    DOM.summaryTotal.textContent = '무료';
}

function validateDelivery() {
    if (!DOM.orderName.value.trim()) { showToast('이름을 입력해주세요.'); DOM.orderName.focus(); return false; }

    const birthVal = DOM.orderBirth.value;
    if (!/^\d{6}$/.test(birthVal)) { showToast('생년월일 6자리를 정확히 입력해주세요.'); DOM.orderBirth.focus(); return false; }

    const p2 = DOM.orderPhone2.value;
    const p3 = DOM.orderPhone3.value;
    if (p2.length < 3 || p3.length < 4) { showToast('연락처를 정확히 입력해주세요.'); DOM.orderPhone2.focus(); return false; }

    if (!DOM.orderEmail.value.includes('@')) { showToast('이메일 형식이 올바르지 않습니다.'); DOM.orderEmail.focus(); return false; }

    return true;
}

function validatePayment() {
    const method = document.querySelector('input[name="pay-method"]:checked');
    if (!method) { showToast('결제방식을 선택해주세요.'); return false; }

    if (method.value === 'card') {
        const cardType = document.querySelector('input[name="card-type"]:checked');
        if (!cardType) { showToast('카드 종류를 선택해주세요.'); return false; }

        if (cardType.value === 'general') {
            const select = document.getElementById('card-select');
            if (!select.value) { showToast('카드사를 선택해주세요.'); return false; }
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
        await showAlert({ title: '오류', msg: '에러가 발생하였습니다. 다시 시도해주세요.' });
        location.href = '/practice';
        return;
    }

    try {
        const res = await authFetch('/api/practice/complete', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ session_id: sessionId, total_duration_ms: totalDurationMs, reaction_time_ms: reactionTimeMs, queue_wait_ms: queueWaitMs, seat_selection_ms: seatSelectionMs, queue_initial_rank: queueInitialRank })
        });

        clearInterval(soldOutMonitorId);

        if (res.ok) {
            showCompleteModal(await res.json());
        } else {
            console.warn('[Practicket] complete failed');
            showCompleteModal({ total_duration_ms: totalDurationMs, reaction_time_ms: reactionTimeMs, queue_wait_ms: queueWaitMs, seat_selection_ms: seatSelectionMs, queue_initial_rank: queueInitialRank });
        }
    } catch (e) {
        console.error('[Practicket] complete error:', e);
    }
}

const BEST_RECORD_KEY = 'pkt.best.i-ticket';
const SEGMENT_LABELS = ['반응', '대기열', '좌석 선택'];

/* 좁은 구간에 숫자를 넣으면 글자가 잘려 오히려 지저분해진다. */
function renderSplitBar(el, segments, scale, withLabel = true) {
    if (!el || !scale) return;
    el.innerHTML = segments.map((ms, i) => {
        const pct = Math.max(0, ms / scale * 100);
        const label = withLabel && pct >= 12 ? (ms / 1000).toFixed(1) : '';
        return `<i class="pkt-seg${i + 1}" style="width:${pct.toFixed(1)}%">${label}</i>`;
    }).join('');
}

function readBestRecord() {
    try {
        const raw = localStorage.getItem(BEST_RECORD_KEY);
        if (!raw) return null;
        const parsed = JSON.parse(raw);
        return Number.isFinite(parsed.total) && Array.isArray(parsed.segments) ? parsed : null;
    } catch (e) {
        return null;
    }
}

function saveBestRecord(totalMs, segments, best) {
    if (best && best.total <= totalMs) return;
    try {
        localStorage.setItem(BEST_RECORD_KEY, JSON.stringify({ total: totalMs, segments }));
    } catch (e) {
        /* 사파리 사생활 모드에서 쓰기가 막힌다. 비교 막대만 안 나올 뿐이라 삼킨다. */
    }
}

function renderCompleteHint(segments, segmentSum, best) {
    const hint = document.getElementById('pkt-hint');
    if (!hint || !segmentSum) return;

    let slowest = 0;
    segments.forEach((ms, i) => { if (ms > segments[slowest]) slowest = i; });

    const share = Math.round(segments[slowest] / segmentSum * 100);
    let text = `세 구간 중 <b>${share}%</b>를 ${SEGMENT_LABELS[slowest]}에 썼어요`;

    if (best) {
        const diffSec = (segments[slowest] - best.segments[slowest]) / 1000;
        if (diffSec > 0.05) {
            text += ` · 최고 기록보다 <b>${diffSec.toFixed(2)}초</b> 깁니다`;
        }
    }

    hint.innerHTML = text;
    hint.style.display = 'block';
}

function renderBestChip(totalMs, best) {
    const chip = document.getElementById('pkt-pb-chip');
    if (!chip) return;

    if (best && totalMs < best.total) {
        chip.textContent = `▼ ${((best.total - totalMs) / 1000).toFixed(2)}초 단축 · 개인 신기록`;
        chip.style.display = 'inline-flex';
    } else {
        chip.style.display = 'none';
    }
}

/* 이미지를 보내는 게 아니라 링크를 보낸다. 카톡·X 가 그 링크의 og:image 를
   긁어가 카드로 그려주고, 그 카드는 클릭이 된다. 이미지 안의 주소는 클릭이 안 된다. */
function bindShareButton(result) {
    const btn = document.getElementById('pkt-share');
    if (!btn) return;

    const params = new URLSearchParams({
        type: 'I_TICKET_OLD',
        total: result.total_duration_ms,
        reaction: result.reaction_time_ms,
        queue: result.queue_wait_ms,
        seat: result.seat_selection_ms,
        rank: result.queue_initial_rank || 0
    });
    if (result.percentile != null) params.set('pct', result.percentile);

    const url = `${window.location.origin}/practice/result?${params.toString()}`;
    const text = `티켓팅 연습 ${(result.total_duration_ms / 1000).toFixed(3)}초`;

    btn.onclick = async () => {
        if (navigator.share) {
            try {
                await navigator.share({ title: '프랙티켓', text, url });
                return;
            } catch (e) {
                if (e && e.name === 'AbortError') return;
            }
        }
        try {
            await navigator.clipboard.writeText(url);
            btn.textContent = '링크 복사됨';
            setTimeout(() => { btn.textContent = '공유'; }, 1500);
        } catch (e) {
            window.open(url, '_blank');
        }
    };
}

function showCompleteModal({ total_duration_ms, reaction_time_ms, queue_wait_ms, seat_selection_ms, queue_initial_rank, percentile, my_rank, total_users }) {
    const fmt = ms => (ms / 1000).toFixed(3) + '초';

    const now = new Date();
    const dateStr = [
        now.getFullYear(),
        String(now.getMonth() + 1).padStart(2, '0'),
        String(now.getDate()).padStart(2, '0')
    ].join('.') + '  ' + String(now.getHours()).padStart(2, '0') + ':' + String(now.getMinutes()).padStart(2, '0');

    const meta = ['I-Ticket', dateStr];
    if (queue_initial_rank) meta.push('대기 순번 ' + queue_initial_rank.toLocaleString() + '번에서 출발');
    document.getElementById('pkt-meta').textContent = meta.join(' · ');

    document.getElementById('pkt-total-num').textContent = (total_duration_ms / 1000).toFixed(3);
    document.getElementById('pkt-reaction').textContent = fmt(reaction_time_ms);
    document.getElementById('pkt-queue').textContent = fmt(queue_wait_ms);
    document.getElementById('pkt-seat').textContent = fmt(seat_selection_ms);

    const segments = [reaction_time_ms, queue_wait_ms, seat_selection_ms];
    const segmentSum = segments.reduce((a, b) => a + b, 0);
    const best = readBestRecord();
    const bestSum = best ? best.segments.reduce((a, b) => a + b, 0) : 0;

    /* 총 시간에는 인트로에서 좌석 페이지로 넘어오는 시간처럼 어느 구간에도 잡히지 않는
       몫이 섞여 있다. 막대는 구간끼리의 비중을 보는 것이라 구간 합을 기준으로 그린다.
       (총 시간 기준으로 그리면 끝에 빈 꼬리가 남는다.) */
    const scale = Math.max(segmentSum, bestSum);
    renderSplitBar(document.getElementById('pkt-stack'), segments, scale);

    const refBar = document.getElementById('pkt-refbar');
    const refLabel = document.getElementById('pkt-ref-label');
    if (best) {
        renderSplitBar(refBar, best.segments, scale, false);
        refBar.style.display = 'flex';
        refLabel.textContent = '흐린 막대 = 내 최고 기록';
    } else {
        refBar.style.display = 'none';
        refLabel.textContent = '';
    }

    renderCompleteHint(segments, segmentSum, best);
    renderBestChip(total_duration_ms, best);
    saveBestRecord(total_duration_ms, segments, best);
    bindShareButton(arguments[0]);

    const bar = document.getElementById('pkt-percentile-bar');
    if (percentile != null && total_users >= 2) {
        document.getElementById('pkt-percentile-value').innerHTML =
            `상위 ${percentile}%<span class="pb-sub">/ ${total_users.toLocaleString()}명 중 ${my_rank}위</span>`;
        bar.style.display = 'flex';
    } else {
        bar.style.display = 'none';
    }

    document.getElementById('pkt-complete-overlay').classList.add('visible');
}

// ════════════════════════════════════════
// DOMContentLoaded
// ════════════════════════════════════════

document.addEventListener('DOMContentLoaded', () => {
    if (!sessionStorage.getItem('pkt.sessionId')) {
        window.location.href = '/practice/i-ticket/intro';
        return;
    }

    // 1. Queue
    if (document.getElementById('ip-root')) {
        QueueManager.init();
    }

    // 2. Seat Manager
    seatManager = new SeatManager(15, 30);
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
            prevBtn.onclick = () => showAlert({ title: '안내', msg: '"관람일/회차선택" 으로 넘어가는 버튼이에요. 누르지마세요!' });
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
