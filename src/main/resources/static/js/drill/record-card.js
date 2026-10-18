import { initialRank } from "../practice/queue-model.js";
import { drawSpark } from "./spark.js";

export const sec = ms => (ms / 1000).toFixed(2);
export const dateLabel = iso => { const d = new Date(iso); return `${d.getMonth() + 1}/${d.getDate()}`; };
export const rankText = ms => initialRank(ms).toLocaleString();

export const EMPTY_RECORD = { count: 0, best_ms: null, best_at: null, recent: [] };

/* withRank: 반응속도만 대기순번 칸을 붙인다. */
export function renderRecord(box, record, { badge, withRank, trendLabel, sparkHeight }) {
    if (!box.getClientRects().length) return;
    const recent = record.recent, latest = recent.at(-1), hasBest = record.best_ms != null;
    const big = (value, unit) => `<div class="r-big ${latest ? "" : "ph"}">${latest ? value : "-"}<small>${unit}</small></div>`;
    const bestSub = !hasBest ? "첫 연습을 시작해 보세요"
        : (withRank ? `대기순번 ${rankText(record.best_ms)}번 · ` : "") + `${dateLabel(record.best_at)} 달성`;

    box.innerHTML = `
        <div class="r-head">
            <div class="eyebrow2"><span>${badge}</span><span>${record.count}회차</span></div>
            <div class="duo">
                <div><div class="lbl">최근 기록</div>${big(latest && sec(latest.ms), "초")}</div>
                ${withRank ? `<div><div class="lbl">대기순번</div>${big(latest && rankText(latest.ms), "번")}</div>` : ""}
            </div>
            <div class="trend">
                <div class="trend-head"><span>${trendLabel}</span></div>
                ${latest ? `<div class="spark"><svg></svg><div class="tip"></div></div>` : `<div class="tr-empty">기록이 쌓이면 그래프가 그려집니다</div>`}
            </div>
        </div>
        <div class="best">
            <div class="best-head"><b>역대 최고 기록</b></div>
            <div class="best-card"><div class="best-val ${hasBest ? "" : "ph"}">${hasBest ? sec(record.best_ms) : "-"}<small>초</small></div><div class="best-sub">${bestSub}</div></div>
        </div>`;

    if (!latest) return;
    const first = record.count - recent.length;
    drawSpark(box.querySelector(".spark"), recent.map(r => r.ms), {
        height: sparkHeight(),
        format: ms => sec(ms) + "초",
        label: i => {
            const r = recent[i];
            return `${dateLabel(r.created_at)} · ${first + i + 1}회차` + (r.latency_ms ? ` · 지연 ${sec(r.latency_ms)}초` : "");
        },
    });
}
