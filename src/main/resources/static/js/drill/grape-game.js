import { ROUNDS, fitCols, playSession, seatMap } from "./seat-play.js";

export const SEATS = [4, 5, 6, 7, 8];

function board(form, lv) {
    const t = (lv - 1) / (ROUNDS - 1);
    return form === "n-ticket"
        ? { cols: Math.round(16 + t * 10), rows: Math.round(9 + t * 5), spread: .25 + t * .75 }
        : { cols: Math.round(16 + t * 5), rows: Math.round(11 + t * 8), spread: .25 + t * .75 };
}

/* 결과: { ok: true, rounds: [{ seats, elapsed_ms, misclicks, sold_clicks }] } */
export function playGrape(box, { form, formName }) {
    const s = playSession(box, { title: "포도알 잡기", form: formName });
    const rounds = [];
    let round = 0, cur = null;

    s.every(() => { if (cur) s.live.textContent = s.sec(performance.now() - cur.t0); }, 50);

    function next() {
        if (round >= ROUNDS) return s.finish({ ok: true, rounds });
        const lv = round + 1, b = board(form, lv), total = SEATS[round], cols = fitCols(s.card, form, b.cols), n = cols * b.rows;
        s.segs[round].className = "now";
        s.card.innerHTML = seatMap(form, cols, b.rows, `라운드 ${lv} / ${ROUNDS}`, form === "n-ticket" ? "STAGE" : "무대방향 (STAGE)")
            + `<div class="seat-bar"><span class="cnt">선택 좌석 <b>0</b> / ${total}석</span></div>`;
        const seats = [...s.card.querySelectorAll(".seats i")], center = Math.floor(Math.random() * n);
        const radius = Math.max(8, Math.round(n * (.06 + b.spread * .9))), chosen = new Set();
        for (let g = 0; chosen.size < total && g < 5000; g++) chosen.add(Math.min(n - 1, Math.max(0, center + Math.round((Math.random() * 2 - 1) * radius))));
        chosen.forEach(i => seats[i].classList.add("open"));
        cur = { t0: performance.now(), total, left: total, misclicks: 0, sold: 0 };
        s.tap(s.card.querySelector(".seat-map"), { hit: pick, miss });
    }

    function pick(e) {
        if (!cur?.left) return true;
        const seat = e.target.closest(".seats i");
        if (!seat?.classList.contains("open")) return false;
        seat.classList.remove("open");
        seat.classList.add("picked");
        cur.left--;
        s.card.querySelector(".seat-bar b").textContent = cur.total - cur.left;
        if (!cur.left) complete();
        return true;
    }

    function miss(e, seat) {
        if (!cur?.left) return;
        cur.misclicks++;
        const sold = seat && !seat.classList.contains("picked");
        if (sold) { cur.sold++; seat.classList.add("bad"); s.later(() => seat.classList.remove("bad"), 260); }
        s.pop(e, sold ? "판매된 좌석" : seat ? "이미 고른 좌석" : "오클릭", "bad");
    }

    function complete() {
        const ms = Math.round(performance.now() - cur.t0);
        rounds.push({ seats: cur.total, elapsed_ms: ms, misclicks: cur.misclicks, sold_clicks: cur.sold });
        s.segs[round].className = cur.misclicks ? "bad" : "good";
        s.live.textContent = s.sec(ms);
        s.veil(`<small>${cur.total}석 선택 완료</small><b>${s.sec(ms)}초</b><small>${cur.misclicks ? `오클릭 ${cur.misclicks}회` : "오클릭 없음"}</small>`);
        cur = null;
        round++;
        s.later(next, 1000);
    }

    s.countdown(next);
    return { done: s.done, abort: s.stop };
}
