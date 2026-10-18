import { loadActivity, showActivity } from "./activity.js";

const PUR = "#6633cc", INK = "#2e1065";

const ARROW = (x, y) => `<path transform="translate(${x} ${y})" d="M0 0V16.5L4 12.8L6.8 19.2L9.6 18L6.9 11.7H12.2Z" fill="#fff" stroke="${INK}" stroke-width="1.2" stroke-linejoin="round"/>`;
const HAND = (x, y) => `<g transform="translate(${x - 5.2} ${y - 0.6})" stroke="${INK}" stroke-width="1.2" stroke-linejoin="round" stroke-linecap="round">
    <path d="M3.4 2.4A1.8 1.8 0 0 1 7 2.4V7.6A1.6 1.6 0 0 1 10.2 7.6V8.4A1.6 1.6 0 0 1 13.4 8.4V9.3A1.6 1.6 0 0 1 16.6 9.3V14C16.6 17 14.4 19.4 11.6 19.4H8.6C7 19.4 5.8 18.8 4.8 17.6L.8 12.8A1.7 1.7 0 0 1 3.2 10.4L3.4 10.6Z" fill="#fff"/>
    <path d="M7 7.6V10.8M10.2 8.4V11.2M13.4 9.3V11.6" fill="none"/></g>`;

const COLS = 9, ROWS = 4, SW = 13, SG = 5;
const MAP_W = COLS * SW + (COLS - 1) * SG, MAP_H = ROWS * SW + (ROWS - 1) * SG;
const OX = (240 - MAP_W) / 2, OY = 38;
const seatPos = k => [OX + (k % COLS) * (SW + SG), OY + Math.floor(k / COLS) * (SW + SG)];
const handOn = k => { const [x, y] = seatPos(k); return HAND(x + SW / 2, y + SW / 2); };

function seatMap(seatFill, extra = "") {
    const top = OY - 26;
    let out = `<rect x="${OX - 12}" y="${top}" width="${MAP_W + 24}" height="${MAP_H + 36}" rx="4" fill="#f3f3f3"/>`
        + `<rect x="${OX + 30}" y="${top + 7}" width="${MAP_W - 60}" height="9" fill="#c8c8c8"/><text x="120" y="${top + 13.7}" text-anchor="middle" font-family="Pretendard" font-weight="500" font-size="6" fill="#fff">STAGE</text>`;
    for (let k = 0; k < COLS * ROWS; k++) {
        const [x, y] = seatPos(k), st = seatFill(k);
        out += `<rect x="${x}" y="${y}" width="${SW}" height="${SW}" fill="${st === "empty" ? "#e2e2e2" : "#8b7cf3"}"${st === "pick" ? ' stroke="#111" stroke-width="2"' : ""}/>`;
    }
    return out + extra;
}

const CAPTCHA_LETTERS = [["K", 76.2, 48.8, 1.5], ["T", 93.8, 48, -.1], ["Q", 111.2, 47.6, 1], ["Z", 128.8, 49.8, .4], ["M", 146.2, 50.1, -1.1], ["W", 163.8, 49.6, .6]];
const CAPTCHA_DOTS = [[99.6, 28], [137.5, 24.9], [124.2, 36.6], [68.7, 42.3], [66.3, 39.3], [70.1, 25.6], [111.2, 55.1], [76.4, 30.9], [134.8, 59.9], [128.9, 37.9], [175.2, 23.9], [161.6, 33.6], [78.7, 26.7], [97.8, 54.6], [83, 45.3], [136.1, 36.9], [125.5, 24.5], [68.9, 30.2], [140.9, 39.1], [98.4, 45.4], [114.6, 34], [154.1, 50], [90.3, 45], [122.9, 57], [146.6, 33.5], [175.7, 26.7]];

const ART = {
    react: () => `<rect x="44" y="42" width="152" height="36" rx="8" fill="#c9c4d6"/>
        <text x="120" y="65" text-anchor="middle" fill="#fff"><tspan font-family="Pretendard" font-weight="600" font-size="11">남은 시간 </tspan><tspan font-family="Roboto Mono" font-weight="700" font-size="14">00:00</tspan></text>${ARROW(174, 62)}`,
    captcha: () => `<rect x="62" y="22" width="116" height="40" rx="4" fill="rgb(84,106,128)"/>
        <g font-family="Arial, sans-serif" font-weight="700" font-size="17" fill="#fff" text-anchor="middle">${CAPTCHA_LETTERS.map(([c, x, y, r]) => `<text x="${x}" y="${y}" transform="rotate(${r} ${x} 43)">${c}</text>`).join("")}</g>
        <path d="M64 30L176 52M70 58L170 26" stroke="#fff" stroke-width=".8"/><g fill="yellow">${CAPTCHA_DOTS.map(([x, y]) => `<circle cx="${x}" cy="${y}" r=".9"/>`).join("")}</g>
        <rect x="62" y="72" width="116" height="28" rx="8" fill="#fff" stroke="${PUR}" stroke-width="1.5"/><text x="72" y="91" font-family="Roboto Mono" font-weight="700" font-size="14" fill="${INK}">KTQ</text><rect x="99" y="80" width="1.6" height="13" fill="${PUR}"/>`,
    grape: () => seatMap(k => [2, 10, 15].includes(k) ? "pick" : [6, 21, 25, 30, 34].includes(k) ? "open" : "empty") + handOn(21),
    cancel: () => seatMap(k => k === 22 ? "open" : "empty",
        `<text x="${OX + MAP_W + 8}" y="${OY - 12.3}" text-anchor="end" font-family="Pretendard" font-weight="500" font-size="6.5" fill="#666" text-decoration="underline">새로고침</text>`) + handOn(22),
};

const DRILLS = [
    { key: "react", name: "반응속도", desc: "열리자마자 예매 버튼 누르기", href: "/drill/react" },
    { key: "captcha", name: "보안문자", desc: "영문 6자 빠르게 입력하기", href: "/drill/captcha" },
    { key: "grape", name: "포도알 잡기", desc: "흩어진 빈 좌석을 전부 누르기", href: "/drill/grape" },
    { key: "cancel", name: "취소표 잡기", desc: "풀리는 한 자리를 먼저 낚아채기", href: "/drill/cancel" },
];

document.getElementById("hub-grid").innerHTML = DRILLS.map(d => `<a class="hub-card" href="${d.href}">
    <div class="hub-thumb"><svg viewBox="0 0 240 120" preserveAspectRatio="xMidYMid meet">${ART[d.key]()}</svg></div>
    <div class="hub-body"><div class="hub-nm">${d.name}</div><div class="hub-ds">${d.desc}</div></div>
</a>`).join("");

loadActivity().then(activity => showActivity(document.getElementById("drill-live"), activity));
