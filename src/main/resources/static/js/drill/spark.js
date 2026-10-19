const PAD_TOP = 14;
const PAD_BOTTOM = 8;
const BAR_AREA = 26;

let sparkId = 0;
let dismiss = null;
document.addEventListener("pointerdown", e => dismiss?.(e));

function curve(points) {
    let d = `M${points[0].x},${points[0].y}`;
    for (let k = 0; k < points.length - 1; k++) {
        const p0 = points[k - 1] || points[k], p1 = points[k], p2 = points[k + 1], p3 = points[k + 2] || p2, t = 0.16;
        const lo = Math.min(p1.y, p2.y), hi = Math.max(p1.y, p2.y), clamp = v => Math.min(hi, Math.max(lo, v));
        d += ` C${p1.x + (p2.x - p0.x) * t},${clamp(p1.y + (p2.y - p0.y) * t)} ${p2.x - (p3.x - p1.x) * t},${clamp(p2.y - (p3.y - p1.y) * t)} ${p2.x},${p2.y}`;
    }
    return d;
}

/*
 * 시간 기록이라 빠를수록 아래에 찍힌다. bars 를 주면 아래에 오클릭 막대와 숫자를 항상 그린다.
 * 툴팁은 마우스·탭·키보드 포커스 모두로 연다.
 */
export function drawSpark(box, values, { height, label, format, bars }) {
    const svg = box.querySelector("svg"), tip = box.querySelector(".tip");
    const W = box.clientWidth, H = height + (bars ? BAR_AREA : 0), n = values.length;
    const lo = Math.min(...values), hi = Math.max(...values);
    const x = i => n === 1 ? W / 2 : 8 + i * ((W - 16) / (n - 1));
    const y = v => PAD_TOP + (hi - v) / (hi - lo || 1) * (height - PAD_TOP - PAD_BOTTOM);
    const points = values.map((v, i) => ({ i, v, x: x(i), y: n === 1 ? height / 2 : y(v) }));
    const last = points[n - 1];
    const id = "drill-spark-" + (++sparkId), step = n > 1 ? (W - 16) / (n - 1) : W;
    const barMax = bars && Math.max(3, ...bars), base = H - 4;

    svg.setAttribute("viewBox", `0 0 ${W} ${H}`);
    svg.setAttribute("height", H);
    svg.innerHTML = `<defs><linearGradient id="${id}" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#c9bcf5" stop-opacity=".3"/><stop offset="1" stop-color="#c9bcf5" stop-opacity="0"/></linearGradient></defs>`
        + (n > 1 ? `<path d="${curve(points)} L${last.x},${height} L${points[0].x},${height} Z" fill="url(#${id})"/><path d="${curve(points)}" fill="none" stroke="#c9bcf5" stroke-width="2.25" stroke-linecap="round"/>` : "")
        + `<circle cx="${last.x}" cy="${last.y}" r="9" fill="#fff" opacity=".16"/><circle cx="${last.x}" cy="${last.y}" r="4.5" fill="#fff" stroke="#2f2752" stroke-width="2"/>`
        + (bars ? `<line x1="0" x2="${W}" y1="${height + 4}" y2="${height + 4}" stroke="rgba(255,255,255,.08)"/>` + points.map(p => {
            const v = bars[p.i], h = Math.max(2, v / barMax * 12);
            return `<rect x="${p.x - 6}" y="${base - h}" width="12" height="${h}" rx="3" fill="${v ? "#e3a3c2" : "rgba(255,255,255,.18)"}"/>`
                + `<text x="${p.x}" y="${base - h - 4}" text-anchor="middle" font-size="10" font-weight="700" font-family="Pretendard, sans-serif" fill="${v ? "#f1c6da" : "#9d93bd"}">${v}</text>`;
        }).join("") : "")
        + `<line class="guide" x1="0" x2="0" y1="${PAD_TOP - 8}" y2="${H}" stroke="rgba(255,255,255,.25)" stroke-width="1.5" stroke-dasharray="3 3" opacity="0"/>`
        + points.map(p => `<rect data-i="${p.i}" x="${p.x - step / 2}" y="0" width="${step}" height="${H}" fill="transparent" tabindex="0" role="button" aria-label="${format(p.v)}, ${label(p.i)}" style="outline:none;cursor:default"/>`).join("");

    const guide = svg.querySelector(".guide");
    const hide = () => { tip.style.display = "none"; guide.setAttribute("opacity", 0); };
    const show = p => {
        tip.innerHTML = `${format(p.v)}<small>${label(p.i)}</small>`;
        tip.style.display = "block";
        const half = tip.offsetWidth / 2;
        tip.style.left = Math.min(Math.max(p.x, half), W - half) + "px";
        tip.style.top = Math.max(0, p.y) + "px";
        guide.setAttribute("x1", p.x);
        guide.setAttribute("x2", p.x);
        guide.setAttribute("opacity", 1);
    };
    svg.querySelectorAll("rect[data-i]").forEach(rect => {
        const p = points[+rect.dataset.i];
        rect.addEventListener("pointerenter", () => show(p));
        rect.addEventListener("pointerdown", e => { e.stopPropagation(); show(p); });
        rect.addEventListener("focus", () => show(p));
        rect.addEventListener("blur", hide);
        rect.addEventListener("keydown", e => { if (e.key === "Escape") hide(); });
    });
    box.addEventListener("pointerleave", hide);
    dismiss = e => { if (!box.contains(e.target)) hide(); };
}
