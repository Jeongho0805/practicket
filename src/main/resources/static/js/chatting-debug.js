// 임시 계측 도구. 주소에 ?vvdebug=1 이 있을 때만 chatting.js 가 로드한다.
// 키보드 전환 중 Safari 가 페이지를 움직인 양(sy)과 앱이 패널에 적용한 보정(set)을
// 나란히 찍어, 둘 중 어느 쪽이 튐을 만드는지 구분한다.

const DURATION = 2200;
const MAX_ROWS = 28;
const HEADER = "  ms   vh   vt   ih   sy  set  top  bot   ph st";

const box = document.createElement("div");
box.style.cssText = [
    "position:fixed", "top:0", "left:0", "right:0", "z-index:2147483647",
    "background:rgba(0,0,0,.85)", "color:#3f6",
    "font:10px/1.3 ui-monospace,Menlo,monospace",
    "padding:4px 6px", "max-height:46vh", "overflow:hidden",
    "white-space:pre", "pointer-events:none"
].join(";");
box.textContent = HEADER + "\n(채팅 입력창을 탭하세요)";
document.body.appendChild(box);

const clearBtn = document.createElement("button");
clearBtn.textContent = "CLR";
clearBtn.style.cssText = [
    "position:fixed", "top:2px", "right:4px", "z-index:2147483647",
    "background:#333", "color:#fff", "border:1px solid #666", "border-radius:4px",
    "font:10px ui-monospace,Menlo,monospace", "padding:3px 6px"
].join(";");
document.body.appendChild(clearBtn);

// Simulator 접근성 자동화가 화면 아래 입력창 탭을 전달하지 못하는 경우를 위한
// 실기기/시뮬레이터 전용 테스트 버튼. vvdebug 쿼리에서만 이 모듈과 함께 나타난다.
const focusBtn = document.createElement("button");
focusBtn.textContent = "TST";
focusBtn.style.cssText = [
    "position:fixed", "top:2px", "right:52px", "z-index:2147483647",
    "background:#315", "color:#fff", "border:1px solid #86f", "border-radius:4px",
    "font:10px ui-monospace,Menlo,monospace", "padding:3px 6px"
].join(";");
document.body.appendChild(focusBtn);

let t0 = 0;
let raf = 0;
let rows = [];
let lastKey = "";

function pad(value, width) {
    return String(value).padStart(width);
}

function sample() {
    const vv = window.visualViewport;
    const panel = document.getElementById("chat-panel");
    const rect = panel ? panel.getBoundingClientRect() : null;
    const bottom = panel && panel.style.bottom ? parseFloat(panel.style.bottom) : NaN;
    return [
        Math.round(vv ? vv.height : window.innerHeight),
        Math.round(vv ? vv.offsetTop : 0),
        window.innerHeight,
        Math.round(window.scrollY),
        Number.isNaN(bottom) ? "-" : Math.round(bottom),
        rect ? Math.round(rect.top) : "-",
        rect ? Math.round(rect.bottom) : "-",
        rect ? Math.round(rect.height) : "-",
        panel?.dataset.viewportState?.slice(0, 2) || "-"
    ];
}

function render() {
    box.textContent = HEADER + "\n" + rows.join("\n");
}

function tick() {
    const t = Math.round(performance.now() - t0);
    const values = sample();
    const key = values.join(",");
    if (key !== lastKey) {
        lastKey = key;
        rows.push(pad(t, 4) + values.map((v, index) => pad(v, index === values.length - 1 ? 3 : 5)).join(""));
        render();
    }
    if (t < DURATION && rows.length < MAX_ROWS) {
        raf = requestAnimationFrame(tick);
    }
}

function start(label) {
    cancelAnimationFrame(raf);
    t0 = performance.now();
    rows = [label];
    lastKey = "";
    render();
    raf = requestAnimationFrame(tick);
}

function mark(label) {
    if (!rows.length) return;
    rows.push(label + " @" + Math.round(performance.now() - t0));
    render();
}

document.addEventListener("focusin", (e) => {
    if (!e.target.closest || !e.target.closest("#chat-panel")) return;
    start("-- focusin");
});

document.addEventListener("focusout", (e) => {
    if (!e.target.closest || !e.target.closest("#chat-panel")) return;
    mark("-- focusout");
});

clearBtn.addEventListener("click", () => {
    cancelAnimationFrame(raf);
    rows = [];
    lastKey = "";
    box.textContent = HEADER + "\n(채팅 입력창을 탭하세요)";
});

focusBtn.addEventListener("click", () => {
    const widget = document.getElementById("chat-widget");
    if (!widget?.classList.contains("open")) {
        document.getElementById("chat-launcher")?.click();
    }
    document.getElementById("chatting-input")?.focus({ preventScroll: true });
});
