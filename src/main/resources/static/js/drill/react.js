import { authFetch } from "../common.js";
import { loadActivity, showActivity } from "./activity.js";
import { createDrillPage } from "./drill-page.js";
import { playReact } from "./react-game.js";
import { EMPTY_RECORD, rankText, renderRecord, sec } from "./record-card.js";
import { deltaLine, failBody } from "./result-modal.js";
import { FORMS, LAT_PRESETS, clampLatency, formLabel, readForm, readLatency, readyUrl, saveLatency } from "./react-config.js";

let form = readForm();
let latency = readLatency();
let record = EMPTY_RECORD;
let loadSeq = 0;

const recBox = document.getElementById("rec");
const latBox = document.getElementById("lats");
const latInput = document.getElementById("lat-in");

function paintRecord() {
    renderRecord(recBox, record, {
        cells: [{ label: "대기순번", unit: "번", value: r => rankText(r.ms) }],
        bestPrefix: ms => `대기순번 ${rankText(ms)}번 · `,
        trendLabel: "최근 10회 추이",
    });
}

async function loadRecord() {
    const seq = ++loadSeq;
    let loaded = EMPTY_RECORD;
    try {
        const response = await authFetch(`${HOST}/api/drill/react/record?form=${FORMS[form].code}`);
        if (response.ok) loaded = await response.json();
    } catch (e) { /* 빈 카드로 보여 준다 */ }
    if (seq !== loadSeq) return;
    record = loaded;
    paintRecord();
}

function paintLatency() {
    latBox.querySelectorAll(".lat[data-ms]").forEach(b => b.classList.toggle("on", +b.dataset.ms === latency));
    const custom = !LAT_PRESETS.includes(latency);
    latBox.querySelector(".lat.own").classList.toggle("on", custom);
    if (document.activeElement !== latInput) latInput.value = custom ? (latency / 1000).toFixed(2) : "";
    latInput.placeholder = innerWidth <= 768 ? "직접" : "직접 입력";
    saveLatency(latency);
}

function paintForm() {
    document.querySelectorAll(".form[data-form]").forEach(a => a.classList.toggle("on", a.dataset.form === form));
    latBox.hidden = form !== "advanced";
    if (form === "advanced") paintLatency();
}

function bindForms() {
    document.querySelectorAll(".form[data-form]").forEach(a => a.addEventListener("click", e => {
        e.preventDefault();
        if (a.dataset.form === form) return;
        form = a.dataset.form;
        history.replaceState(null, "", readyUrl(form));
        paintForm();
        record = EMPTY_RECORD;
        paintRecord();
        loadRecord();
    }));
}

function bindLatency() {
    latBox.querySelectorAll(".lat[data-ms]").forEach(b => b.addEventListener("click", () => { latency = +b.dataset.ms; paintLatency(); }));
    latInput.addEventListener("input", () => {
        const s = parseFloat(latInput.value);
        if (s > 0) { latency = clampLatency(s * 1000); paintLatency(); }
    });
    latInput.addEventListener("blur", paintLatency);
}

async function save(playedForm, playedLatency, ms) {
    try {
        const response = await authFetch(`${HOST}/api/drill/react/results`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ form: FORMS[playedForm].code, reaction_ms: ms, latency_ms: playedForm === "advanced" ? playedLatency : null }),
        });
        return response.ok ? await response.json() : null;
    } catch (e) {
        return null;
    }
}

createDrillPage({
    onReady: paintRecord,
    start(box, setStep) {
        setStep(formLabel(form, latency));
        return playReact(box, { form, latency });
    },
    finish(outcome) {
        const meta = "반응속도 · " + formLabel(form, latency);
        if (!outcome.ok) return { fail: true, meta, body: failBody(outcome.title, outcome.why) };

        const played = form, previous = record.recent.at(-1)?.ms;
        const saving = save(played, latency, outcome.ms).then(saved => {
            if (saved && played === form) { record = saved; ++loadSeq; paintRecord(); }
            return !!saved;
        });
        return {
            meta,
            saving,
            body: `<div class="rb-main"><div class="rb-num">${sec(outcome.ms)}<small>초</small></div>`
                + `<p class="rb-rank">대기순번 <b>${rankText(outcome.ms)}번</b></p>${deltaLine(outcome.ms, previous)}</div>`,
        };
    },
});

bindForms();
bindLatency();
paintForm();
paintRecord();
loadRecord();
loadActivity().then(activity => showActivity(document.getElementById("drill-live"), activity?.react));
