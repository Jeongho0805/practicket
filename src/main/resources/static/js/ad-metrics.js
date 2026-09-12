/**
 * 광고 노출 집계. 화면 폭에 따라 실제로 보이는 배너만 1회 보고한다.
 *
 * 서버 렌더 시점에 세면 CSS로 숨겨진 배너까지 집계되고(상단 영역은 2배), 봇도 함께 잡힌다.
 * 정액 광고라 정산 근거는 아니므로 뷰어빌리티(50%·1초) 같은 규칙까지는 두지 않는다.
 */
const REPORTED = new Set();

function isVisible(el) {
    // display:none 이면 offsetParent 가 없다. 폭/높이 0인 경우도 제외.
    if (el.offsetParent === null) return false;
    const rect = el.getBoundingClientRect();
    return rect.width > 0 && rect.height > 0;
}

function report(bannerId) {
    if (REPORTED.has(bannerId)) return;
    REPORTED.add(bannerId);

    const url = `/metrics/v/${bannerId}`;
    if (navigator.sendBeacon) {
        navigator.sendBeacon(url);
        return;
    }
    fetch(url, { method: 'POST', keepalive: true }).catch(() => {});
}

function reportVisibleBanners() {
    document.querySelectorAll('img[data-banner-id]').forEach((img) => {
        if (!isVisible(img)) return;
        // 한쪽 기기 그림만 있는 배너는 반대 기기에서 투명 픽셀로 떨어진다. 그건 노출이 아니다.
        if (img.currentSrc.startsWith('data:')) return;
        // 광고 차단기가 이미지 요청을 막으면 화면엔 안 보이므로 노출로 세지 않는다.
        if (img.naturalWidth > 0) {
            report(img.dataset.bannerId);
            return;
        }
        // loading=lazy 라 아직 안 받아왔을 수 있다. 로드되면 그때 보고(실패하면 보고 안 함).
        if (!img.complete) {
            img.addEventListener('load', () => report(img.dataset.bannerId), { once: true });
        }
    });
}

if (document.readyState === 'complete') {
    reportVisibleBanners();
} else {
    window.addEventListener('load', reportVisibleBanners);
}

// 탭 뒤에 숨어 있던 배너는 load 때 안 보여서 집계에서 빠진다. 드러낸 쪽이 직접 알려야 한다.
window.reportVisibleAdBanners = reportVisibleBanners;
