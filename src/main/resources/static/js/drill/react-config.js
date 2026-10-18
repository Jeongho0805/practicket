export const FORMS = {
    refresh: { code: "REFRESH", name: "일반형" },
    advanced: { code: "ADVANCED", name: "고급형" },
    "n-ticket": { code: "N_TICKET", name: "N형" },
};

export const LAT_PRESETS = [100, 300, 500, 1000];

const LATENCY_KEY = "drill:react:latency";
const DEFAULT_LATENCY = 300;

export const clampLatency = ms => Math.min(5000, Math.max(10, Math.round(ms)));

export function readForm() {
    const form = new URLSearchParams(location.search).get("form");
    return FORMS[form] ? form : "refresh";
}

export function readLatency() {
    try {
        const saved = Number(localStorage.getItem(LATENCY_KEY));
        if (saved > 0) return clampLatency(saved);
    } catch (e) { /* 저장소를 못 쓰면 기본값 */ }
    return DEFAULT_LATENCY;
}

export function saveLatency(ms) {
    try { localStorage.setItem(LATENCY_KEY, String(ms)); } catch (e) { /* 다음 방문에 기본값으로 시작할 뿐이다 */ }
}

export const readyUrl = form => `/drill/react?form=${form}`;
export const formLabel = (form, latency) => FORMS[form].name + (form === "advanced" ? ` · 지연 ${(latency / 1000).toFixed(2)}초` : "");
