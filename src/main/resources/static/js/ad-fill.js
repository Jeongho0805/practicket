/**
 * 팔리지 않은 자리에 네트워크 광고를 넣는다. 서버는 자리 정보만 내려보내고 코드 삽입은 여기서 한다.
 *
 * 서버가 미리 넣지 않는 이유는, 어느 자리가 보일지가 화면 폭에 달려 있어서다. UA 로 기기를 맞히면
 * 데스크톱 창을 좁혔을 때 어긋나고, 그냥 다 내보내고 CSS 로 가리면 안 보이는 광고를 요청하게 된다
 * (애드센스 정책 위반). 그래서 실제로 보이는 자리에만 넣는다.
 *
 * 창 크기가 바뀌어 다른 자리가 드러나면 그때 넣는다. 한 번 넣은 자리는 다시 넣지 않는다 —
 * 위젯을 두 번 초기화하면 광고가 겹쳐 뜬다.
 *
 * 재요청 간격(data-gap-minutes)이 있는 네트워크는 같은 탭에서 그 시간 안에 다시 부르지 않는다.
 * 연습 루프가 인트로를 몇 초마다 다시 열어 애드센스 요청이 수십 번 나가던 것이 무효 트래픽으로
 * 잡혔기 때문이다. 간격 안이면 같은 자리를 data-fb-* 네트워크로 대신 채운다.
 */
const COUPANG_SCRIPT = 'https://ads-partners.coupang.com/g.js';
const ADSENSE_SCRIPT = 'https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js';
const ADFIT_SCRIPT = 'https://t1.kakaocdn.net/kas/static/ba.min.js';
const MOBSENSE_SCRIPT = 'https://img.mobon.net/js/common/HawkEyesMaker.js';

/** 계정 값 없이 단위 ID 하나로 요청하는 네트워크. 규격도 코드에 숫자로 박아야 한다 */
const FIXED_SIZE_NETWORKS = ['ADFIT', 'MOBSENSE'];

const loaded = new Map();

function isVisible(el) {
    if (el.offsetParent === null) return false;
    const rect = el.getBoundingClientRect();
    return rect.width > 0 && rect.height > 0;
}

/** 같은 외부 스크립트를 자리마다 다시 받지 않는다 */
function loadScript(src, crossOrigin = false) {
    if (loaded.has(src)) return loaded.get(src);

    const promise = new Promise((resolve, reject) => {
        const script = document.createElement('script');
        script.src = src;
        script.async = true;
        if (crossOrigin) script.crossOrigin = 'anonymous';
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

    return loadScript(`${ADSENSE_SCRIPT}?client=${encodeURIComponent(el.dataset.account)}`, true)
        .then(() => {
            (window.adsbygoogle = window.adsbygoogle || []).push({});
        });
}

/**
 * 쿠팡 위젯은 container 를 안 주면 문서의 마지막 script 태그 앞에 iframe 을 넣는다.
 * 자리 안에 인라인 script 를 만들어도 마지막 태그가 아니라서 페이지 끝에 붙는다.
 */
function fillCoupang(el) {
    return loadScript(COUPANG_SCRIPT).then(() => {
        new PartnersCoupang.G({
            id: Number(el.dataset.unit),
            template: el.dataset.template || 'carousel',
            trackingCode: el.dataset.account,
            width: '100%',
            height: '100%',
            tsource: '',
            container: el,
        });
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

/**
 * 모비센스는 문서의 script 태그를 뒤에서부터 훑어 자기 지면 번호가 적힌 태그 바로 뒤에 iframe 을 붙인다.
 * 그래서 자리 안에 `new HawkEyes(...)` 인라인 script 를 만들어 넣는다. 공용 스크립트는 한 번만 받는다.
 * frameCode·settings 는 지면마다 달라 어드민에 JSON 으로 적어 둔 것(data-extra)을 그대로 합친다.
 */
function fillMobsense(el) {
    const [width, height] = (el.dataset.size || '').split('x');
    let extra = {};
    try {
        extra = JSON.parse(el.dataset.extra || '{}');
    } catch (e) {
        // 어드민이 저장 때 JSON 을 검사하므로 여기 올 일은 드물다. 기본값만으로 간다.
    }
    const options = Object.assign({
        type: 'banner',
        responsive: 'N',
        platform: el.classList.contains('ad-face-mobile') ? 'M' : 'W',
        scriptCode: el.dataset.unit,
        width,
        height,
    }, extra);

    return loadScript(MOBSENSE_SCRIPT).then(() => {
        const script = document.createElement('script');
        script.text = `new HawkEyes(${JSON.stringify(options)});`;
        el.appendChild(script);
    });
}

const GAP_KEY_PREFIX = 'adfill:gap:';
const gapDecisions = new Map();

/** 같은 페이지 로드 안에서는 네트워크마다 한 번만 판정한다. 자리가 둘이어도 같은 답을 쓴다 */
function gapAllows(network, minutes) {
    if (gapDecisions.has(network)) return gapDecisions.get(network);
    let allowed = true;
    try {
        const last = Number(sessionStorage.getItem(GAP_KEY_PREFIX + network) || 0);
        allowed = !last || Date.now() - last >= minutes * 60000;
    } catch (e) {
        // 저장소를 못 쓰는 환경(비공개 탭 등)은 제한 없이 지나간다.
    }
    gapDecisions.set(network, allowed);
    return allowed;
}

function markRequested(network) {
    try {
        sessionStorage.setItem(GAP_KEY_PREFIX + network, String(Date.now()));
    } catch (e) {
        // 위와 같다.
    }
}

const STEP_KEYS = ['network', 'unit', 'account', 'template', 'size', 'extra', 'gapMinutes'];

function fallbackSteps(el) {
    try {
        return JSON.parse(el.dataset.fallbacks || '[]');
    } catch (e) {
        return [];
    }
}

/** 자리 크기는 네트워크별 클래스(pc-net-*)가 정하므로 다른 단계로 바꾸면 그 클래스도 같이 바꾼다 */
function useStep(el, step) {
    const d = el.dataset;
    const slot = el.closest('.ad-slot');
    const prefix = el.classList.contains('ad-face-mobile') ? 'mo' : 'pc';
    if (slot) {
        slot.classList.remove(`${prefix}-net-${d.network}`);
        slot.classList.add(`${prefix}-net-${step.network}`);
    }
    STEP_KEYS.forEach((key) => {
        if (step[key] != null) d[key] = String(step[key]); else delete d[key];
    });
}

/** 채움 순서를 위에서부터 보며 재요청 간격에 안 걸린 첫 단계를 고른다. 다 걸리면 false */
function chooseStep(el) {
    const first = { network: el.dataset.network, gapMinutes: el.dataset.gapMinutes };
    const steps = [first, ...fallbackSteps(el)];
    const chosen = steps.find((step) => {
        const minutes = Number(step.gapMinutes || 0);
        return minutes <= 0 || gapAllows(step.network, minutes);
    });
    if (!chosen) return false;
    if (chosen !== first) useStep(el, chosen);
    return true;
}

function fill(el) {
    if (el.dataset.filled) return;
    if (!el.dataset.unit) return;

    if (!chooseStep(el)) {
        el.dataset.filled = '1';
        return;
    }
    const countAfterFill = Number(el.dataset.gapMinutes || 0) > 0 ? el.dataset.network : null;

    const fixedSize = FIXED_SIZE_NETWORKS.includes(el.dataset.network);
    if (!fixedSize && !el.dataset.account) return;
    if (fixedSize && !el.dataset.size) return;
    el.dataset.filled = '1';

    const filler = el.dataset.network === 'ADSENSE' ? fillAdsense
        : el.dataset.network === 'COUPANG' ? fillCoupang
            : el.dataset.network === 'ADFIT' ? fillAdfit
                : el.dataset.network === 'MOBSENSE' ? fillMobsense
                    : null;
    if (!filler) return;

    filler(el).then(() => {
        if (countAfterFill) markRequested(countAfterFill);
    }).catch(() => {
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

// 탭 뒤에 숨어 있던 자리는 load 때 안 보여서 건너뛴다. 드러낸 쪽이 직접 알려야 한다.
window.fillVisibleAdSlots = fillVisibleSlots;
