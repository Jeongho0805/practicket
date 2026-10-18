import { startCountdown } from "./countdown.js";
import { sec } from "./record-card.js";

export const ROUNDS = 5;

/* 판 하나가 쓰는 타이머·팝업·카운트다운을 모아 두고 abort 때 한 번에 정리한다. */
export function playSession(box, { title, form }) {
    let resolve;
    const done = new Promise(r => { resolve = r; });
    const timers = new Set(), pops = new Set();
    let cancelCountdown = () => {};

    box.insertAdjacentHTML("beforeend", `<div class="ptop"><div class="psteps">${"<i></i>".repeat(ROUNDS)}</div><div class="ptimer"><b>—</b><small>초</small></div></div><div class="seat-card"></div>`);
    const s = {
        done, card: box.querySelector(".seat-card"), segs: box.querySelector(".psteps").children, live: box.querySelector(".ptimer b"),
        later(fn, ms) { const id = setTimeout(() => { timers.delete(id); fn(); }, Math.max(0, ms)); timers.add(id); return id; },
        every(fn, ms) { const id = setInterval(fn, ms); timers.add(id); return id; },
        cancel(id) { clearTimeout(id); timers.delete(id); },
        pop(e, text, cls) {
            const el = document.createElement("div");
            el.className = `seat-pop ${cls}`;
            el.textContent = text;
            el.style.left = e.clientX + "px";
            el.style.top = e.clientY + "px";
            document.body.appendChild(el);
            pops.add(el);
            s.later(() => { el.remove(); pops.delete(el); }, 700);
        },
        countdown(go) { cancelCountdown = startCountdown({ title: `${title} · ${form}`, sub: `${ROUNDS}라운드` }, go); },
        stop() { timers.forEach(id => { clearTimeout(id); clearInterval(id); }); timers.clear(); pops.forEach(el => el.remove()); cancelCountdown(); },
        finish(outcome) { s.stop(); resolve(outcome); },
        veil(html) { s.card.insertAdjacentHTML("beforeend", `<div class="seat-veil"><div>${html}</div></div>`); },
        sec,
    };
    return s;
}

export function seatMap(form, cols, rows, caption, stage) {
    return `<div class="seat-cap"><b>좌석 선택</b><small>${caption}</small></div>`
        + `<div class="seat-map ${form === "n-ticket" ? "is-n" : "is-g"}"><div class="stage">${stage}</div>`
        + `<div class="seat-scroll"><div class="seats" style="grid-template-columns:repeat(${cols},var(--s))">${"<i></i>".repeat(cols * rows)}</div></div></div>`;
}
