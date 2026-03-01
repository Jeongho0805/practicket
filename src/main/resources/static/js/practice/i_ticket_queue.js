/**
 * I-Ticket Queue System
 * Handles the waiting queue simulation before entering the booking flow.
 * Works in tandem with i_ticket.js but maintains its own state in sessionStorage.
 */

const QueueConfig = {
    // 5 seconds max duration, regardless of entry time
    MAX_DURATION_MS: 5000,

    // Target Queue Size range: 180,000 ~ 200,000
    MIN_INITIAL_QUEUE: 180000,
    MAX_INITIAL_QUEUE: 200000,

    STORAGE_KEY: 'iq.queue.payload'
};

const QueueManager = {
    payload: null,
    intervalId: null,
    dom: {},

    init() {
        // Create Queue DOM elements immediately
        this.createQueueDOM();

        // Read Payload
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

        // Check TTL (15 mins)
        const now = Date.now();
        if (now - this.payload.createdAtMs > 15 * 60 * 1000) {
            console.warn('Queue Payload Expired. Resetting.');
            sessionStorage.removeItem(QueueConfig.STORAGE_KEY);
            this.createFallbackPayload();
        }

        // Determine Start Action based on Status
        if (this.payload.status === 'PASSED') {
            this.removeQueueDOM();
            // Let the main script proceed (captcha already visible or handled there)
            return;
        }

        // Start Queue Flow
        this.renderPhase('LOADING');

        // Simulate network loading time if just arrived from Intro
        // If reloading (queueStartAtMs exists), skip loading simulation
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
        const initialQueue = Math.floor(Math.random() * (QueueConfig.MAX_INITIAL_QUEUE - QueueConfig.MIN_INITIAL_QUEUE) + QueueConfig.MIN_INITIAL_QUEUE);

        // 첫 큐 순번 기록
        sessionStorage.setItem('pkt.queueInitialRank', initialQueue.toString());

        this.payload = {
            version: 2,
            createdAtMs: now,
            introClickedAtMs: now,
            introLoadingMs: 800,
            queueStartAtMs: null,
            initialQueue: initialQueue,
            dequeuePerSec: initialQueue / (QueueConfig.MAX_DURATION_MS / 1000),
            status: 'INTRO_LOADING'
        };
        this.savePayload();
    },

    savePayload() {
        sessionStorage.setItem(QueueConfig.STORAGE_KEY, JSON.stringify(this.payload));
    },

    createQueueDOM() {
        // Check if exists
        if (document.getElementById('queue-overlay')) return;

        const overlay = document.createElement('div');
        overlay.id = 'queue-overlay';
        overlay.className = 'queue-overlay';
        overlay.innerHTML = `
            <!-- Loading Phase (Spinner) -->
            <div id="queue-loading" class="queue-loading-container" style="display:none;">
                <div style="width: 50px; height: 50px; border: 5px solid #e0e0e0; border-top: 5px solid #448aff; border-radius: 50%; animation: spin 1s linear infinite; margin: 0 auto 20px;"></div>
                <div style="font-size: 18px; font-weight: bold; color: #333;">예매 정보를 불러오는 중입니다.</div>
                <style>@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }</style>
            </div>

            <!-- Queue Phase (Main Card UI) -->
            <div id="queue-card-container" class="queue-container" style="display:none;">
                
                <div class="queue-header-group">
                    <h1 class="queue-header-title">접속 인원이 많아 대기 중입니다.</h1>
                    <h2 class="queue-header-subtitle">조금만 기다려주세요.</h2>
                    <p class="queue-concert-name">2026 G-DRAGON 'FAM' MEETING</p>
                </div>

                <div class="queue-card">
                    <div class="queue-my-order-label">나의 대기순서</div>
                    
                    <!-- Big Countdown Number -->
                    <div id="queue-count" class="queue-number-display">---</div>

                    <!-- Progress Bar -->
                    <div class="queue-progress-track">
                        <div id="queue-progress" class="queue-progress-fill"></div>
                    </div>

                    <!-- Info Grid -->
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

        // 큐 대기 시작 시점 기록
        sessionStorage.setItem('pkt.queueWaitStartMs', this.payload.queueStartAtMs.toString());

        this.renderPhase('QUEUE');
        this.updateLoop();
        this.intervalId = setInterval(() => this.updateLoop(), 200);
    },

    updateLoop() {
        const now = Date.now();
        const elapsedSec = (now - this.payload.queueStartAtMs) / 1000; // Float seconds

        // Calculate Current Queue
        // Formula: Linear decrease
        // Max(0, Initial - (Elapsed * Speed))
        let currentQueue = Math.max(0, Math.floor(this.payload.initialQueue - (elapsedSec * this.payload.dequeuePerSec)));

        // Render
        this.renderQueue(currentQueue);

        // Check Finish
        if (currentQueue <= 0) {
            this.finishQueue();
        }
    },

    renderQueue(num) {
        if (!this.dom.count) return;

        // 1. Queue Number
        this.dom.count.innerText = num.toLocaleString();

        // 2. Progress Bar (Increases as queue decreases)
        const total = this.payload.initialQueue;
        // If queue is 0 => progress 100%
        // If queue is total => progress 0%
        // Avoid division by zero
        const percent = total > 0 ? Math.min(100, Math.max(0, ((total - num) / total) * 100)) : 100;
        this.dom.progress.style.width = percent + '%';

        // 3. Time Left
        // Remaining people / speed
        const secondsLeft = this.payload.dequeuePerSec > 0 ? Math.ceil(num / this.payload.dequeuePerSec) : 0;
        this.dom.timeLeft.innerText = secondsLeft + '초';

        // 4. Urgent State (Red for Progress only)
        if (num <= 5000 && num > 0) {
            // this.dom.count.classList.add('urgent'); // User requested no red number
            this.dom.progress.classList.add('urgent');
            // document.querySelector('.q-status-badge').style.color = '#e60000'; // Optional
        } else {
            // this.dom.count.classList.remove('urgent');
            this.dom.progress.classList.remove('urgent');
        }
    },

    finishQueue() {
        if (this.intervalId) {
            clearInterval(this.intervalId);
        }

        this.payload.status = 'PASSED';
        this.savePayload();

        // 큐 대기 종료 → 소요 시간 및 좌석 선택 시작 시점 기록
        const queueWaitStart = parseInt(sessionStorage.getItem('pkt.queueWaitStartMs') || '0');
        if (queueWaitStart) {
            sessionStorage.setItem('pkt.queueWaitMs', (Date.now() - queueWaitStart).toString());
        }
        sessionStorage.setItem('pkt.seatSelectionStartMs', Date.now().toString());

        if (this.dom.overlay) {
            this.dom.overlay.classList.add('finished');
            setTimeout(() => {
                this.removeQueueDOM();
                console.log('Queue Passed');
            }, 700);
        } else {
            this.removeQueueDOM();
        }
    }
};

// Auto-start when script loads, but wait for DOM
document.addEventListener('DOMContentLoaded', () => {
    // Only init if we are on the ticket page, checked by presence of ip-root
    if (document.getElementById('ip-root')) {
        QueueManager.init();
    }
});
