import { authFetch } from "../common.js";
import { LETTERS } from "./captcha-game.js";

export function glyph(c, seed) {
    let x = seed * 9301 + c.charCodeAt(0) * 49297;
    const rnd = () => (x = (x * 9301 + 49297) % 233280) / 233280;
    const r = 50 + rnd() * 100 | 0, g = 50 + rnd() * 100 | 0, b = 50 + rnd() * 100 | 0;
    const ink = 0.299 * r + 0.587 * g + 0.114 * b < 118 ? "#f6f6f6" : "#141414";
    const dots = Array.from({ length: 14 }, () => `<circle cx="${(rnd() * 100).toFixed(1)}" cy="${(rnd() * 64).toFixed(1)}" r="1.4"/>`).join("");
    return `<svg viewBox="0 0 100 64"><rect width="100" height="64" fill="rgb(${r},${g},${b})"/><text x="50" y="47" text-anchor="middle" font-family="Arial, sans-serif" font-weight="700" font-size="44" fill="${ink}" transform="rotate(${(rnd() * 8 - 4).toFixed(1)} 50 32)">${c}</text>`
        + `<path d="M${rnd() * 30} ${rnd() * 64}L${70 + rnd() * 30} ${rnd() * 64}" stroke="#fff" stroke-width="1.6"/><g fill="yellow">${dots}</g></svg>`;
}

/* 오답률 = 실수한 자리 ÷ 나온 횟수. 한 자리에서 여러 번 틀린 것은 대신 친 글자 횟수로만 보여 준다. */
function report({ letters, ime_count: ime }) {
    const rate = l => l.slots / l.seen;
    const missed = letters.filter(l => l.slots > 0).sort((a, b) => rate(b) - rate(a) || b.miss - a.miss);
    const never = letters.filter(l => l.seen > 0 && l.miss === 0 && LETTERS.includes(l.letter));
    const imeLine = ime ? `<p class="wime"><span>한</span>한글로 입력한 것이 ${ime}번 있습니다</p>` : "";
    if (!missed.length) return `<p class="w3-empty">아직 틀린 글자가 없습니다</p>${imeLine}`;

    const pct = l => Math.round(rate(l) * 100);
    const card = (l, i) => `<div class="wc${i ? "" : " first"}"><span class="rk">${i + 1}위</span>${glyph(l.letter, i + 3)}
        <div class="as"><em>${l.typed[0].letter}</em> 로 ${l.typed[0].count}번</div>
        <div class="rt">${pct(l)}<small>%</small></div><div class="bar"><span style="width:${pct(l)}%"></span></div>
        <div class="nt">${l.seen}번 중 ${l.slots}번</div></div>`;
    const row = (l, i) => `<div class="wrow${i < 3 ? " top" : ""}"><span class="no">${i + 1}</span><span class="tile">${l.letter}</span>`
        + `<span class="gots">${l.typed.map((t, j) => `<em${j ? "" : ' class="main"'}>${t.letter}<small>${t.count}번</small></em>`).join("")}</span>`
        + `<div class="rate"><div class="pc"><b>${pct(l)}%</b><small>${l.slots}/${l.seen}</small></div><div class="bar"><span style="width:${pct(l)}%"></span></div></div></div>`;
    const top = missed[0];

    return `<div class="w3">${missed.slice(0, 3).map(card).join("")}</div>
        <p class="coach2"><b>${top.letter}</b> 가 나오면 ${pct(top)}% 확률로 틀립니다. 주로 <b>${top.typed[0].letter}</b> 로 읽으니 이 글자가 보이면 한 번 더 확인하세요.</p>
        <details class="wall"><summary>틀린 글자 전체 보기<b>${missed.length}자</b></summary>
            <div class="whd"><span>틀린 글자</span><span>대신 친 글자</span><span>오답률 · 틀림/나옴</span></div>
            <div class="wlist">${missed.map(row).join("")}</div>
            ${imeLine}
            ${never.length ? `<div class="wnever"><b>한 번도 안 틀린 글자 ${never.length}자</b><div>${never.map(l => `<span>${l.letter}</span>`).join("")}</div></div>` : ""}
        </details>`;
}

/* 펼칠 때 한 번만 조회한다. 새 판을 저장하면 invalidate 로 다음 펼침(또는 지금 펼친 상태)에서 다시 읽는다. */
export function bindTypoStats(details) {
    const body = details.querySelector(".v-analysis-body");
    let loaded = false, seq = 0;

    async function load() {
        loaded = true;
        const mine = ++seq;
        body.innerHTML = `<p class="w3-empty">불러오는 중입니다</p>`;
        let html = `<p class="w3-empty">누적 기록을 불러오지 못했습니다</p>`;
        try {
            const response = await authFetch(`${HOST}/api/drill/captcha/typo-stats`);
            if (response.ok) html = report(await response.json());
            else loaded = false;
        } catch (e) { loaded = false; }
        if (mine === seq) body.innerHTML = `<section class="panel">${html}</section>`;
    }

    details.addEventListener("toggle", () => { if (details.open && !loaded) load(); });
    return {
        invalidate() {
            loaded = false;
            if (details.open) load();
        },
    };
}
