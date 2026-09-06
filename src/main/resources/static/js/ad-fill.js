/**
 * 팔리지 않은 자리에 네트워크 광고를 넣는다. 서버는 자리 정보만 내려보내고 코드 삽입은 여기서 한다.
 *
 * 서버가 미리 넣지 않는 이유는, 어느 자리가 보일지가 화면 폭에 달려 있어서다. UA 로 기기를 맞히면
 * 데스크톱 창을 좁혔을 때 어긋나고, 그냥 다 내보내고 CSS 로 가리면 안 보이는 광고를 요청하게 된다
 * (애드센스 정책 위반). 그래서 실제로 보이는 자리에만 넣는다.
 *
 * 창 크기가 바뀌어 다른 자리가 드러나면 그때 넣는다. 한 번 넣은 자리는 다시 넣지 않는다 —
 * 위젯을 두 번 초기화하면 광고가 겹쳐 뜬다.
 */
const COUPANG_SCRIPT = 'https://ads-partners.coupang.com/g.js';
const ADSENSE_SCRIPT = 'https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js';
const ADFIT_SCRIPT = 'https://t1.kakaocdn.net/kas/static/ba.min.js';

const loaded = new Map();

function isVisible(el) {
    if (el.offsetParent === null) return false;
    const rect = el.getBoundingClientRect();
    return rect.width > 0 && rect.height > 0;
}

/** 같은 외부 스크립트를 자리마다 다시 받지 않는다 */
function loadScript(src) {
    if (loaded.has(src)) return loaded.get(src);

    const promise = new Promise((resolve, reject) => {
        const script = document.createElement('script');
        script.src = src;
        script.async = true;
        script.crossOrigin = 'anonymous';
        script.onload = resolve;
        script.onerror = reject;
        document.head.appendChild(script);
    });
    loaded.set(src, promise);
    return promise;
}

function fillAdsense(el) {
    const ins = document.createElement('ins');
    ins.className = 'adsbygoogle';
    ins.style.cssText = 'display:block; width:100%; height:100%; max-height:100%';
    ins.dataset.adClient = el.dataset.account;
    ins.dataset.adSlot = el.dataset.unit;
    el.appendChild(ins);

    return loadScript(`${ADSENSE_SCRIPT}?client=${encodeURIComponent(el.dataset.account)}`)
        .then(() => {
            (window.adsbygoogle = window.adsbygoogle || []).push({});
        });
}

/**
 * 쿠팡 위젯은 자기를 실행한 script 태그의 부모에 iframe 을 넣는다. 그래서 모듈에서 직접 호출하지 않고
 * 자리 안에 script 태그를 만들어 그 안에서 부른다 — 안 그러면 iframe 이 body 끝에 붙는다.
 */
function fillCoupang(el) {
    return loadScript(COUPANG_SCRIPT).then(() => {
        const options = {
            id: Number(el.dataset.unit),
            template: el.dataset.template || 'carousel',
            trackingCode: el.dataset.account,
            width: '100%',
            height: '100%',
            tsource: '',
        };
        const inline = document.createElement('script');
        inline.text = `new PartnersCoupang.G(${JSON.stringify(options)});`;
        el.appendChild(inline);
    });
}

/**
 * 애드핏은 자기 스크립트가 실행되는 그 순간의 문서만 훑는다. 나중에 만든 자리는 못 보므로
 * 자리마다 script 태그를 새로 붙인다 — 같은 주소라도 태그를 새로 만들면 다시 실행된다.
 * 그래서 여기만 loadScript(한 번 받아 재사용)를 쓰지 않는다.
 *
 * ins 태그의 클래스·style·data 속성은 카카오가 준 그대로 둔다. 고치면 광고 요청이 실패한다.
 */
function fillAdfit(el) {
    const [width, height] = (el.dataset.size || '').split('x');

    const ins = document.createElement('ins');
    ins.className = 'kakao_ad_area';
    ins.style.display = 'none';
    ins.dataset.adUnit = el.dataset.unit;
    ins.dataset.adWidth = width;
    ins.dataset.adHeight = height;
    el.appendChild(ins);

    const script = document.createElement('script');
    script.src = ADFIT_SCRIPT;
    script.async = true;
    el.appendChild(script);

    return Promise.resolve();
}

function fill(el) {
    if (el.dataset.filled) return;
    if (!el.dataset.unit) return;
    // 애드핏은 계정 값이 따로 없다. 광고단위 ID 하나가 계정 노릇까지 한다.
    if (el.dataset.network !== 'ADFIT' && !el.dataset.account) return;
    // 애드핏은 코드에 크기를 박아야 하므로 규격을 모르면 요청 자체가 성립하지 않는다.
    if (el.dataset.network === 'ADFIT' && !el.dataset.size) return;
    el.dataset.filled = '1';

    const filler = el.dataset.network === 'ADSENSE' ? fillAdsense
        : el.dataset.network === 'COUPANG' ? fillCoupang
            : el.dataset.network === 'ADFIT' ? fillAdfit
                : null;
    if (!filler) return;

    filler(el).catch(() => {
        // 광고 차단기나 네트워크 장애. 자리는 비워 두고 페이지는 그대로 간다.
    });
}

function fillVisibleSlots() {
    document.querySelectorAll('.ad-fill').forEach((el) => {
        if (isVisible(el)) fill(el);
    });
}

if (document.readyState === 'complete') {
    fillVisibleSlots();
} else {
    window.addEventListener('load', fillVisibleSlots);
}
window.addEventListener('resize', fillVisibleSlots);
