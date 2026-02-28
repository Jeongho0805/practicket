/**
 * Seat Manager for Realistic Decay
 * Handles seat ranking, time-based availability, and improved rendering.
 */
class SeatManager {
    constructor(totalRows, totalCols) {
        this.totalRows = totalRows;
        this.totalCols = totalCols;
        this.totalSeats = totalRows * totalCols;
        this.zoneRanks = {}; // Object: { 'A': [...], 'B': [...] }
        this.decayStartAtMs = 0;
        this.snapshotAtMs = 0;
        this.snapshotSoldCount = 0;
        this.soldOutAlertShown = false;
        this.zones = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H'];

        // Config
        this.totalSellOutDurationMs = 60000; // Fully sold out in 1 minute
        this.rushDurationMs = 20000; // First 20s
        this.rushSoldRatio = 0.80; // 80% sold by 20s

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

        // Load or Create Ranks (strict validation, always 15x20 = 300 per zone)
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
                // Fall through to regeneration.
            }
        }

        if (!loaded) {
            this.generateAllZoneRanks();
            this.persistZoneRanks();
        }
        this.refreshSnapshot();
    }

    resolveDecayStartAt(fallbackNow = Date.now()) {
        // Prefer queue start time if it exists (legacy payload compatibility).
        const queuePayloadRaw = sessionStorage.getItem('iq.queue.payload');
        if (queuePayloadRaw) {
            try {
                const queuePayload = JSON.parse(queuePayloadRaw);
                if (Number.isFinite(queuePayload.queueStartAtMs) && queuePayload.queueStartAtMs > 0) {
                    sessionStorage.setItem(this.storageKeys.decayStartAt, String(queuePayload.queueStartAtMs));
                    return queuePayload.queueStartAtMs;
                }
            } catch (e) {
                // Ignore parse failure and fall back immediately.
            }

            // Queue feature is disabled. Ignore stale queue payload.
            sessionStorage.removeItem('iq.queue.payload');
        }

        // Start immediately.
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
        // Deterministic weighted staging:
        // 1) By 20s, about 80% seats are sold.
        // 2) Front rows are consumed faster than middle/back rows.
        // 3) Remaining 20% is mostly middle/back seats.
        const front = []; // rows 1~5
        const middle = []; // rows 6~10
        const back = []; // rows 11~15

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

        // Early sold bucket (80% = 240/300)
        // Front-heavy so front disappears faster.
        const earlyFront = pickN(front, 96);   // 96% of front gone by 20s
        const earlyMiddle = pickN(middle, 80); // 80% of middle gone by 20s
        const earlyBack = pickN(back, 64);     // 64% of back gone by 20s

        // Tail bucket (20% = 60/300): mostly middle/back.
        const tailFront = pickN(front, front.length);   // usually ~4
        const tailMiddle = pickN(middle, middle.length); // usually ~20
        const tailBack = pickN(back, back.length);       // usually ~36

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
                let r = Math.random() * totalW;
                let chosen = alive[0].idx;
                for (const p of alive) {
                    r -= p.w;
                    if (r <= 0) {
                        chosen = p.idx;
                        break;
                    }
                }
                out.push(popFrom(chosen));
            }
            return out;
        };

        // Early: front > middle > back
        const earlyOrder = interleave(earlyFront, earlyMiddle, earlyBack, [5.0, 2.4, 1.0]);
        // Tail: mostly middle/back; remaining front seats are consumed first in tail.
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
        if (elapsed >= this.totalSellOutDurationMs) {
            return this.totalSeats;
        }

        if (elapsed <= this.rushDurationMs) {
            const t = elapsed / this.rushDurationMs; // 0..1
            const eased = 1 - Math.pow(1 - t, 3); // Fast early sell
            return Math.floor(this.totalSeats * this.rushSoldRatio * eased);
        }

        const remainingWindow = this.totalSellOutDurationMs - this.rushDurationMs;
        const postElapsed = elapsed - this.rushDurationMs;
        const t = Math.min(1, postElapsed / remainingWindow); // 0..1
        const easedSlow = Math.pow(t, 1.9); // Slow-ish fade from 80% to 100%
        const progress = this.rushSoldRatio + (1 - this.rushSoldRatio) * easedSlow;
        return Math.floor(this.totalSeats * progress);
    }

    // Capture current real-time state as snapshot (called on user action)
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

    shouldShowSoldOutAlertNow(nowMs = Date.now()) {
        const soldOutNow = this.computeSoldCountAt(nowMs) >= this.totalSeats;
        // if (soldOutNow && !this.soldOutAlertShown) {
        //     this.soldOutAlertShown = true;
        //     return true;
        // }
        return false;
    }

    // Returns true if seat is available, false if sold
    checkAvailability(row, col, zone = 'A') {
        const globalCol = col;
        const seatId = `${row}-${globalCol}`;

        const ranks = this.zoneRanks[zone] || [];
        const rankIndex = ranks.indexOf(seatId);

        if (rankIndex === -1) return true;

        // Use snapshot for UI (updated only on refresh/entry actions)
        const soldCount = this.snapshotSoldCount;
        const isSold = rankIndex < soldCount;

        return !isSold;
    }
}

// Global Manager
let seatManager = null;
