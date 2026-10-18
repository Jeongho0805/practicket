import { authFetch } from "../common.js";
import { loadActivity, showActivity } from "./activity.js";
import { createDrillPage } from "./drill-page.js";
import { dateLabel, renderRecord } from "./record-card.js";
import { unsavedLine } from "./result-modal.js";

export const FORM_NAME = { general: "일반형", "n-ticket": "N형" };
const FORM_CODE = { general: "GENERAL", "n-ticket": "N_TICKET" };
const EMPTY = { count: 0, best: null, clean_best: null, recent: [] };

/* 포도알·취소표 준비 화면과 저장. lostFor(form) 이면 놓친 취소표 칸과 놓침 없는 최고를 쓴다. */
export function seatPage({ drill, title, avgLabel, lostFor, play, toRow, modal }) {
    let form = new URLSearchParams(location.search).get("form") === "n-ticket" ? "n-ticket" : "general";
    let record = EMPTY, loadSeq = 0;
    const rec = document.getElementById("rec");

    const paint = () => {
        const lost = lostFor(form), { best, clean_best: clean } = record;
        renderRecord(rec, record, {
            badge: FORM_NAME[form],
            cells: [{ label: "오클릭", unit: "회", value: r => r.misclicks }].concat(lost ? [{ label: "놓친 취소표", unit: "회", value: r => r.missed }] : []),
            bests: [
                { label: "가장 빠른 기록", ms: best?.ms, empty: "첫 연습을 시작해 보세요",
                  sub: best && [best.misclicks ? `오클릭 ${best.misclicks}회` : "", lost && best.missed ? `놓침 ${best.missed}회` : "", `${dateLabel(best.created_at)} 달성`].filter(Boolean).join(" · ") },
                { label: lost ? "오클릭·놓침 없는 최고" : "오클릭 없는 최고", ms: clean?.ms, clean: true, empty: lost ? "오클릭·놓침 0회에 도전해 보세요" : "오클릭 0회에 도전해 보세요",
                  sub: clean && `${dateLabel(clean.created_at)} 달성` },
            ],
            trendLabel: `${avgLabel} · 최근 10회 추이`,
            sparkHeight: () => 70,
            bars: recent => recent.map(r => r.misclicks),
            tip: r => ` · 오클릭 ${r.misclicks}회` + (lost ? ` · 놓침 ${r.missed}회` : ""),
        });
    };

    async function load() {
        const seq = ++loadSeq;
        let loaded = EMPTY;
        try {
            const response = await authFetch(`${HOST}/api/drill/${drill}/record?form=${FORM_CODE[form]}`);
            if (response.ok) loaded = await response.json();
        } catch (e) { /* 빈 카드로 보여 준다 */ }
        if (seq !== loadSeq) return;
        record = loaded;
        paint();
    }

    async function save(playedForm, rounds) {
        try {
            const response = await authFetch(`${HOST}/api/drill/${drill}/results`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ form: FORM_CODE[playedForm], rounds }),
            });
            return response.ok ? await response.json() : null;
        } catch (e) {
            return null;
        }
    }

    document.querySelectorAll(".form[data-form]").forEach(a => {
        a.classList.toggle("on", a.dataset.form === form);
        a.addEventListener("click", e => {
            e.preventDefault();
            if (a.dataset.form === form) return;
            form = a.dataset.form;
            const url = new URL(location.href);
            url.searchParams.set("form", form);
            history.replaceState(null, "", url);
            document.querySelectorAll(".form[data-form]").forEach(b => b.classList.toggle("on", b === a));
            record = EMPTY;
            paint();
            load();
        });
    });

    createDrillPage({
        onReady: paint,
        start(box, setStep) {
            setStep(`${FORM_NAME[form]} · 5라운드`);
            return play(box, { form, formName: FORM_NAME[form] });
        },
        async finish(outcome) {
            const played = form, previous = record.recent.at(-1)?.ms;
            const saved = await save(played, outcome.rounds);
            if (saved) record = saved;
            return { meta: `${title} · ${FORM_NAME[played]} · 5라운드`, wide: true, body: modal(outcome, toRow(outcome, played), previous, played, saved ? "" : unsavedLine) };
        },
    });

    paint();
    load();
    loadActivity().then(activity => showActivity(document.getElementById("drill-live"), activity?.[drill]));
}

export const roundCard = (title, time, pills, extra = "") => {
    const clean = !extra && !pills.some(p => p.no);
    return `<div class="n-card${clean ? " clean" : ""}"><div class="n-top"><b>${title}</b><span class="n-r">`
        + pills.map(p => `<span class="n-pill ${p.no ? "no" : "ok"}">${p.text}</span>`).join("")
        + `<span class="n-time">${time}</span></span></div>${extra}</div>`;
};
