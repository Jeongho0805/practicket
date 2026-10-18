import { deltaLine } from "./result-modal.js";
import { playCancel } from "./cancel-game.js";
import { roundCard, seatPage } from "./seat-page.js";
import { sec } from "./record-card.js";

const sum = (rounds, k) => rounds.reduce((a, r) => a + (r[k] || 0), 0);
const PARTS = [["discover", "발견", "새로고침으로 찾기"], ["select", "선택", "좌석 누르기"], ["confirm", "완료", "완료 버튼 누르기"]];
const split = r => ({ discover: r.discover_ms, select: r.select_ms, confirm: r.elapsed_ms - r.discover_ms - r.select_ms });

function parts(rounds, general) {
    const n = rounds.length;
    return PARTS.filter(([k]) => general || k === "select").map(([k, name, how]) => ({ k, name, how, ms: rounds.reduce((a, r) => a + split(r)[k], 0) / n }));
}

const bar = (r, general) => `<div class="seat-split">${parts([r], general).map(p => `<i class="${p.k}" style="flex:${Math.max(p.ms, 1)}"></i>`).join("")}</div>`;

seatPage({
    drill: "cancel",
    title: "취소표 잡기",
    avgLabel: "좌석 잡기까지 평균",
    lostFor: form => form === "general",
    play: playCancel,
    toRow: (outcome, form) => ({ ms: Math.round(sum(outcome.rounds, "elapsed_ms") / 5), misclicks: sum(outcome.rounds, "misclicks"), missed: form === "general" ? sum(outcome.rounds, "missed") : 0 }),
    modal(outcome, row, previous, form, note) {
        const general = form === "general", rounds = outcome.rounds;
        let acc = 0;
        const step = (k, name, how, plus, at) => `<div class="flow-step ${k}"><i></i><div class="nm"><b>${name}</b>${how ? `<small>${how}</small>` : ""}</div><span class="plus">${plus}</span><b class="at">${at}</b></div>`;
        const flow = [step("start", general ? "취소표 풀림" : "좌석 노출", "", "", "0.00초")]
            .concat(parts(rounds, general).map(p => { acc += p.ms; return step(p.k, p.name, p.how, `+${sec(p.ms)}`, `${sec(acc)}초`); })).join("");
        const cards = rounds.map((r, i) => roundCard(`${i + 1}라운드`, `${sec(r.elapsed_ms)}초`,
            [r.misclicks ? { no: true, text: `오클릭 ${r.misclicks}회` } : { text: "오클릭 없음" }].concat(general && r.missed ? [{ no: true, text: `놓침 ${r.missed}회` }] : []),
            general ? bar(r, general) : "")).join("");
        return `<div class="rb-main"><div class="rb-label">${general ? "좌석이 풀린 순간부터 선택 완료까지 평균" : "좌석이 뜬 순간부터 좌석을 누를 때까지 평균"}</div><div class="rb-num">${sec(row.ms)}<small>초</small></div>${deltaLine(row.ms, previous)}`
            + `<div class="rb-stats"><span>오클릭<b>${row.misclicks}회</b></span>${general ? `<span>놓친 취소표<b>${row.missed}회</b></span>` : ""}</div></div>` + note
            + `<section class="round-notes">${general ? `<section class="round-details"><h3>한 라운드 평균 흐름</h3><div class="flow-card">${flow}</div></section>` : ""}`
            + `<section class="round-details"><h3>라운드별 기록${general ? `<span class="split-key">${parts(rounds, general).map(p => `<span><i class="${p.k}"></i>${p.name}</span>`).join("")}</span>` : ""}</h3><div class="n-cards">${cards}</div></section></section>`;
    },
});
