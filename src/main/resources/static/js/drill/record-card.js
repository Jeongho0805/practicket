import { initialRank } from "../practice/queue-model.js";
import { drawSpark } from "./spark.js";

export const sec = ms => (ms / 1000).toFixed(2);
export const dateLabel = iso => { const d = new Date(iso); return `${d.getMonth() + 1}/${d.getDate()}`; };
export const rankText = ms => initialRank(ms).toLocaleString();

export const EMPTY_RECORD = { count: 0, best_ms: null, best_at: null, recent: [] };

const SPARK_MIN = 60;
const SPARK_MAX_PC = 140;

/*
 * cells: 최근 기록 옆 칸 [{ label, unit, value(latest) }].
 * bests: 역대 최고를 여러 칸으로 [{ label, ms, sub, empty, clean }]. 없으면 record.best_ms 한 칸에 bestPrefix 를 앞에 붙인다.
 * bars: 그래프 아래 막대 값을 뽑는 함수(recent → 숫자 배열). tip: 툴팁 둘째 줄 뒤에 붙일 문구.
 * 그래프는 연습 화면 아래로 남는 높이만큼 커진다. 누적 오타 분석은 펼쳐져 있어도 접힌 높이로 친다.
 */
export function renderRecord(box, record, { cells = [], bests, bestPrefix, trendLabel, bars, tip = () => "" }) {
    if (!box.getClientRects().length) return;
    const recent = record.recent, latest = recent.at(-1);
    const big = (value, unit) => `<div class="r-big ${latest ? "" : "ph"}">${latest ? value : "-"}<small>${unit}</small></div>`;
    const cards = bests ?? [{ ms: record.best_ms, sub: record.best_ms != null && (bestPrefix?.(record.best_ms) ?? "") + `${dateLabel(record.best_at)} 달성`, empty: "첫 연습을 시작해 보세요" }];
    const card = c => `<div class="best-card${c.clean ? " clean" : ""}">${c.label ? `<div class="best-lbl">${c.label}</div>` : ""}`
        + `<div class="best-val ${c.ms != null ? "" : "ph"}">${c.ms != null ? sec(c.ms) : "-"}<small>초</small></div><div class="best-sub">${c.ms != null ? c.sub : c.empty}</div></div>`;

    box.innerHTML = `
        <div class="r-head">
            <div class="duo${cells.length > 1 ? " trio" : ""}">
                <div><div class="lbl">최근 기록</div>${big(latest && sec(latest.ms), "초")}</div>
                ${cells.map(c => `<div><div class="lbl">${c.label}</div>${big(latest && c.value(latest), c.unit)}</div>`).join("")}
            </div>
            <div class="trend">
                <div class="trend-head"><span>${trendLabel}</span><span>${record.count}회차</span></div>
                ${latest ? `<div class="spark"><svg></svg><div class="tip"></div></div>` : `<div class="tr-empty">기록이 쌓이면 그래프가 그려집니다</div>`}
            </div>
        </div>
        <div class="best">
            <div class="best-head"><b>역대 최고 기록</b></div>
            ${cards.length > 1 ? `<div class="best-grid">${cards.map(card).join("")}</div>` : card(cards[0])}
        </div>`;

    if (!latest) return;
    const first = record.count - recent.length, spark = box.querySelector(".spark");
    const draw = height => drawSpark(spark, recent.map(r => r.ms), {
        height,
        format: ms => sec(ms) + "초",
        bars: bars?.(recent),
        label: i => {
            const r = recent[i];
            return `${dateLabel(r.created_at)} · ${first + i + 1}회차` + (r.latency_ms ? ` · 지연 ${sec(r.latency_ms)}초` : "") + tip(r);
        },
    });
    box.fit = () => {
        if (!spark.isConnected || !box.getClientRects().length) return;
        draw(SPARK_MIN);
        const room = roomBelow(box.closest(".drill-page")) - 4;
        if (room > 0) draw(matchMedia("(max-width: 768px)").matches ? SPARK_MIN + room : Math.min(SPARK_MAX_PC, SPARK_MIN + room));
    };
    box.fit();
    if (!box.dataset.fit) {
        box.dataset.fit = "1";
        const page = box.closest(".drill-page"), observer = new ResizeObserver(() => box.fit());
        observer.observe(page);
        observer.observe(page.querySelector(".practice-sticky"));
        document.fonts.ready.then(() => box.fit());
    }
}

function roomBelow(page) {
    const last = [...page.children].filter(el => el.offsetParent).pop();
    const folded = page.querySelector(".v-analysis[open] .v-analysis-body")?.offsetHeight ?? 0;
    const used = last.getBoundingClientRect().bottom - page.getBoundingClientRect().top + page.scrollTop - folded;
    return Math.floor(page.clientHeight - parseFloat(getComputedStyle(page).paddingBottom) - used);
}
