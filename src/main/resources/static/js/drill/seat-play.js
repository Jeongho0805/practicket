import { startCountdown } from "./countdown.js";
import { sec } from "./record-card.js";

export const ROUNDS = 5;
const TAP_SLOP_PX = 10;

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
        /* 맞는 좌석은 닿는 순간 잡고, 오클릭은 손을 뗄 때 판단한다. 밀었으면 스크롤이라 세지 않는다. */
        tap(el, { hit, miss }) {
            let down = null;
            el.addEventListener("pointerdown", e => {
                down = hit(e) ? null : { x: e.clientX, y: e.clientY, seat: e.target.closest(".seats i") };
            });
            el.addEventListener("pointercancel", () => { down = null; });
            el.addEventListener("pointerup", e => {
                if (down && Math.hypot(e.clientX - down.x, e.clientY - down.y) < TAP_SLOP_PX) miss({ clientX: down.x, clientY: down.y }, down.seat);
                down = null;
            });
        },
        veil(html) { s.card.insertAdjacentHTML("beforeend", `<div class="seat-veil"><div>${html}</div></div>`); },
        sec,
    };
    return s;
}

/* 좌석 크기는 그대로 두고, 화면 폭에 안 들어가는 열만 덜어 낸다. */
export function fitCols(card, form, cols) {
    card.innerHTML = seatMap(form, 1, 1, "", "");
    const map = card.querySelector(".seat-map"), m = getComputedStyle(map), st = getComputedStyle(card.querySelector(".seats"));
    const room = map.clientWidth - parseFloat(m.paddingLeft) - parseFloat(m.paddingRight) - parseFloat(st.paddingLeft) - parseFloat(st.paddingRight);
    const s = parseFloat(st.getPropertyValue("--s")), g = parseFloat(st.getPropertyValue("--g"));
    return Math.max(1, Math.min(cols, Math.floor((room + g) / (s + g))));
}

export function seatMap(form, cols, rows, caption, stage) {
    return `<div class="seat-cap"><b>좌석 선택</b><small>${caption}</small></div>`
        + `<div class="seat-map ${form === "n-ticket" ? "is-n" : "is-g"}"><div class="stage">${stage}</div>`
        + `<div class="seat-scroll"><div class="seats" style="grid-template-columns:repeat(${cols},var(--s))">${"<i></i>".repeat(cols * rows)}</div></div></div>`;
}
