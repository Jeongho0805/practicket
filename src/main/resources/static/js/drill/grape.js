import { deltaLine } from "./result-modal.js";
import { SEATS, playGrape } from "./grape-game.js";
import { roundCard, seatPage } from "./seat-page.js";
import { sec } from "./record-card.js";

const total = rounds => rounds.reduce((a, r) => a + r.elapsed_ms, 0);
const sum = (rounds, k) => rounds.reduce((a, r) => a + r[k], 0);

seatPage({
    drill: "grape",
    title: "포도알 잡기",
    avgLabel: "좌석 1개당 평균",
    lostFor: () => false,
    play: playGrape,
    toRow: outcome => ({ ms: Math.round(total(outcome.rounds) / 30), misclicks: sum(outcome.rounds, "misclicks"), missed: 0 }),
    modal(outcome, row, previous, form, note) {
        const rounds = outcome.rounds, sold = sum(rounds, "sold_clicks");
        const cards = rounds.map((r, i) => roundCard(`${i + 1}라운드 · ${r.seats}석`, `${sec(r.elapsed_ms)}초`,
            [r.misclicks ? { no: true, text: `오클릭 ${r.misclicks}회` } : { text: "오클릭 없음" }],
            r.misclicks ? `<div class="n-sub">판매된 좌석 ${r.sold_clicks} · 빈 공간 ${r.misclicks - r.sold_clicks} · 좌석당 ${sec(r.elapsed_ms / r.seats)}초</div>` : "")).join("");
        return `<div class="rb-main"><div class="rb-label">좌석 1개당 평균</div><div class="rb-num">${sec(row.ms)}<small>초</small></div>${deltaLine(row.ms, previous)}`
            + `<div class="rb-stats"><span>전체 시간<b>${sec(total(rounds))}초</b></span><span>오클릭<b>${row.misclicks}회</b></span></div></div>` + note
            + `<section class="round-notes"><section class="round-details"><h3>라운드별 기록${row.misclicks ? `<small>판매된 좌석 ${sold} · 빈 공간 ${row.misclicks - sold}</small>` : ""}</h3><div class="n-cards">${cards}</div></section></section>`;
    },
});
