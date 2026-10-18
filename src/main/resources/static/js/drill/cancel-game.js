import { ROUNDS, fitCols, playSession, seatMap } from "./seat-play.js";

const LATENCY_MS = 150;
/* 라운드가 올라갈수록 취소표가 늦게 풀린다 [최소, 최대] ms */
const WAITS = [[1000, 3000], [2000, 4500], [3000, 6500], [4000, 8500], [5000, 11000]];

/*
 * 결과: { ok: true, rounds: [{ elapsed_ms, discover_ms, select_ms, misclicks, sold_clicks, missed }] }
 * 일반형은 좌석이 풀린 순간부터 완료 버튼까지, N형은 좌석이 뜬 순간부터 좌석을 누른 순간까지 잰다.
 * N형은 좌석을 누르면 바로 선점되므로 완료 버튼이 없다.
 */
export function playCancel(box, { form, formName }) {
    const s = playSession(box, { title: "취소표 잡기", form: formName });
    const general = form === "general", rounds = [];
    let round = 0, cur = null, lostBefore = 0;
    const $ = sel => s.card.querySelector(sel);

    s.every(() => { if (cur) s.live.textContent = cur.shownAt ? s.sec(performance.now() - cur.openAt) : "—"; }, 50);

    function next() {
        if (round >= ROUNDS) return s.finish({ ok: true, rounds });
        const cols = fitCols(s.card, form, 16 + Math.floor(round / 2) * 2), rows = 10 + round;
        s.segs[round].className = "now";
        s.live.textContent = "—";
        s.card.innerHTML = seatMap(form, cols, rows, general ? "새로고침해서 잔여 좌석 확인" : "잔여 좌석이 자동으로 표시됩니다", general ? "무대방향 (STAGE)" : "STAGE")
            + `<div class="seat-bar"><span class="cnt" data-status>${general ? "잔여 좌석을 확인해 주세요" : "취소 좌석을 기다리는 중"}</span>`
            + (general ? `<span class="lost">놓친 취소표 <b>${lostBefore}</b>회</span><button type="button" class="refresh">새로고침</button>` : `<span class="auto"><i></i>자동 갱신</span>`) + `</div>`
            + (general ? `<div class="seat-sel"><span data-sel>선택한 좌석 없음</span><button type="button" class="confirm" disabled>좌석 선택 완료</button></div>` : "");
        $(".seat-map").insertAdjacentHTML("beforeend", `<div class="seat-load"><i></i>좌석 정보를 불러오는 중</div>`);
        cur = { ticket: 0, shownAt: null, pickedAt: null, loading: false, misclicks: 0, sold: 0, missed: 0 };
        schedule();
        s.tap($(".seat-map"), { hit: pick, miss });
        if (general) $(".confirm").addEventListener("click", confirm);
        if (general) $(".refresh").addEventListener("click", refresh);
    }

    function schedule() {
        const [lo, hi] = WAITS[round], ticket = ++cur.ticket, c = cur;
        c.target = Math.floor(Math.random() * $(".seats").children.length);
        c.openAt = performance.now() + lo + Math.random() * (hi - lo);
        if (!general) s.later(() => { if (cur === c && c.ticket === ticket) reveal(); }, c.openAt - performance.now());
    }

    const status = text => { $("[data-status]").textContent = text; };
    const selection = picked => {
        $(".confirm").disabled = !picked;
        $("[data-sel]").textContent = picked ? "선택 좌석 1석" : "선택한 좌석 없음";
    };

    /* 요청이 서버에 닿는 순간(LATENCY_MS 뒤)의 좌석 상태를 응답 때(다시 LATENCY_MS 뒤) 보여 준다. */
    function refresh() {
        const c = cur;
        if (!c || c.loading) return;
        if (c.shownAt) return lose();
        const ticket = c.ticket, snapshot = performance.now() + LATENCY_MS, btn = $(".refresh");
        c.loading = true;
        btn.disabled = true;
        $(".seat-load").classList.add("on");
        s.later(() => {
            if (cur !== c) return;
            c.loading = false;
            btn.disabled = false;
            $(".seat-load").classList.remove("on");
            if (c.ticket !== ticket || c.shownAt) return;
            if (c.openAt <= snapshot) reveal(); else status("잔여 좌석이 없습니다");
        }, LATENCY_MS * 2);
    }

    function reveal() {
        cur.shownAt = performance.now();
        if (!general) cur.openAt = cur.shownAt;
        $(".seats").children[cur.target].classList.add("open");
        status("잔여 좌석 1석");
    }

    function lose() {
        const seat = $(".seats").children[cur.target];
        seat.classList.remove("open", "picked");
        cur.shownAt = cur.pickedAt = null;
        cur.missed++;
        selection(false);
        s.live.textContent = "—";
        status("새로고침해서 취소표를 놓쳤어요 · 다음 좌석 대기");
        $(".lost b").textContent = lostBefore + cur.missed;
        schedule();
    }

    function pick(e) {
        const c = cur;
        if (!c || c.loading) return true;
        const seat = e.target.closest(".seats i");
        if (c.shownAt && seat?.classList.contains("open")) {
            seat.classList.replace("open", "picked");
            c.pickedAt = performance.now();
            if (!general) confirm();
            else { selection(true); status("선택 완료 버튼을 눌러 주세요"); }
            return true;
        }
        if (c.shownAt && seat?.classList.contains("picked")) {
            seat.classList.replace("picked", "open");
            c.pickedAt = null;
            selection(false);
            status("잔여 좌석 1석");
            return true;
        }
        return false;
    }

    function miss(e, seat) {
        const c = cur;
        if (!c || c.loading) return;
        c.misclicks++;
        if (seat) { c.sold++; seat.classList.add("bad"); s.later(() => seat.classList.remove("bad"), 220); }
        s.pop(e, seat ? "판매된 좌석" : "오클릭", "bad");
    }

    function confirm() {
        const c = cur;
        if (!c?.pickedAt) return;
        const now = general ? performance.now() : c.pickedAt, ms = Math.round(now - c.openAt);
        rounds.push({ elapsed_ms: ms, discover_ms: Math.round(c.shownAt - c.openAt), select_ms: Math.round(c.pickedAt - c.shownAt), misclicks: c.misclicks, sold_clicks: c.sold, missed: c.missed });
        lostBefore += c.missed;
        cur = null;
        if (general) $(".confirm").disabled = true;
        s.segs[round].className = c.misclicks || c.missed ? "bad" : "good";
        s.live.textContent = s.sec(ms);
        const parts = [general ? `발견 ${s.sec(c.shownAt - c.openAt)}초` : "", c.misclicks ? `오클릭 ${c.misclicks}회` : "오클릭 없음", general && c.missed ? `놓침 ${c.missed}회` : ""].filter(Boolean);
        s.veil(`<small>${general ? "좌석 선택 완료" : "좌석 선점"}</small><b>${s.sec(ms)}초</b><small>${parts.join(" · ")}</small>`);
        round++;
        s.later(next, 1000);
    }

    s.countdown(next);
    return { done: s.done, abort: s.stop };
}
