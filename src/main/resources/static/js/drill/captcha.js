import { authFetch } from "../common.js";
import { IME, playCaptcha } from "./captcha-game.js";
import { bindTypoStats } from "./captcha-stats.js";
import { loadActivity, showActivity } from "./activity.js";
import { createDrillPage } from "./drill-page.js";
import { EMPTY_RECORD, renderRecord, sec } from "./record-card.js";
import { deltaLine, failBody } from "./result-modal.js";

const META = "보안문자 · 3문제";

let record = EMPTY_RECORD;
const recBox = document.getElementById("rec");
const typoStats = bindTypoStats(document.getElementById("typo-stats"));

function paintRecord() {
    renderRecord(recBox, record, { badge: "보안문자", trendLabel: "문제당 평균 · 최근 10회 추이", sparkHeight: () => 90 });
}

async function loadRecord() {
    try {
        const response = await authFetch(`${HOST}/api/drill/captcha/record`);
        if (response.ok) record = await response.json();
    } catch (e) { /* 빈 카드로 보여 준다 */ }
    paintRecord();
}

async function save(problems) {
    try {
        const response = await authFetch(`${HOST}/api/drill/captcha/results`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ problems }),
        });
        return response.ok ? await response.json() : null;
    } catch (e) {
        return null;
    }
}

/* 한 자리에 여러 글자를 쳤으면 대표 글자 +N, 아래에 그 자리 상세 줄을 단다. */
function noteCard(p, i) {
    const bad = {};
    p.typos.forEach(t => { const m = bad[t.position] = bad[t.position] || {}; m[t.typed] = (m[t.typed] || 0) + 1; });
    const n = p.typos.length;
    const top = `<div class="n-top"><b>${i + 1}번 문제</b><span class="n-r">${n ? `<span class="n-pill no">오타 ${n}회</span>` : `<span class="n-pill ok">오타 없음</span>`}<span class="n-time">${sec(p.elapsed_ms)}초</span></span></div>`;
    if (!n) return `<div class="n-card clean">${top}</div>`;

    const ranked = m => Object.entries(m).sort((a, b) => b[1] - a[1]);
    const ch = g => g === IME ? `<span class="n-ime">한</span>` : g;
    const crowded = m => { const e = Object.entries(m || {}); return e.length > 2 || (e.length === 2 && e.some(([, k]) => k > 1)); };
    const cell = m => {
        if (!m) return `<span class="n-got ok">·</span>`;
        const e = ranked(m);
        if (e.length === 1) return `<span class="n-got">${ch(e[0][0])}${e[0][1] > 1 ? `<small>×${e[0][1]}</small>` : ""}</span>`;
        if (e.length === 2 && e.every(([, k]) => k === 1)) return `<span class="n-got">${ch(e[0][0])} ${ch(e[1][0])}</span>`;
        return `<span class="n-got">${ch(e[0][0])}<span class="n-more">+${e.length - 1}</span></span>`;
    };
    const chars = [...p.answer];
    const more = chars.map((c, j) => crowded(bad[j])
        ? `<div><b>${c} 자리</b>${ranked(bad[j]).map(([g, k]) => `<em>${g === IME ? "한글" : g}${k > 1 ? `<small>×${k}</small>` : ""}</em>`).join("")}</div>` : "").join("");
    return `<div class="n-card">${top}<div class="n-grid"><span class="n-rl">정답</span>${chars.map((c, j) => `<span class="n-ans${bad[j] ? " bad" : ""}">${c}</span>`).join("")}`
        + `<span class="n-rl">내 입력</span>${chars.map((c, j) => cell(bad[j])).join("")}</div>${more ? `<div class="n-moreline">${more}</div>` : ""}</div>`;
}

createDrillPage({
    onReady: paintRecord,
    start: (box, setStep) => playCaptcha(box, setStep),
    finish(outcome) {
        if (!outcome.ok) return { fail: true, meta: META, body: failBody(outcome.title, outcome.why) };

        const { problems, keys } = outcome;
        const ms = Math.round(problems.reduce((a, p) => a + p.elapsed_ms, 0) / problems.length);
        const typos = problems.reduce((a, p) => a + p.typos.length, 0);
        const accuracy = keys ? Math.max(0, Math.round((keys - typos) / keys * 100)) : 100;
        const previous = record.recent.at(-1)?.ms;
        const saving = save(problems).then(saved => {
            if (saved) { record = saved; typoStats.invalidate(); paintRecord(); }
            return !!saved;
        });
        return {
            meta: META,
            wide: true,
            saving,
            body: `<div class="rb-main"><div class="rb-label">문제당 평균</div><div class="rb-num">${sec(ms)}<small>초</small></div>${deltaLine(ms, previous)}`
                + `<div class="rb-stats"><span>정확도<b>${accuracy}%</b></span><span>오타<b>${typos}회</b></span></div></div>`
                + `<section class="round-notes"><section class="round-details"><h3>이번 판 오답노트</h3><div class="n-cards">${problems.map(noteCard).join("")}</div></section></section>`,
        };
    },
});

paintRecord();
loadRecord();
loadActivity().then(activity => showActivity(document.getElementById("drill-live"), activity?.captcha));
