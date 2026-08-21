/* 연습 결과 모달의 구간 막대와 그 주변. 예매처마다 화면은 달라도 결과 모달은 같은 것을 쓴다.
   마크업과 색은 css/practice/result-modal.css 가 짝이다. */

const SEGMENT_LABELS = ['반응', '대기열', '좌석 선택'];

/** 예매처별 최고 기록 키. 한 브라우저에서 예매처를 옮겨 다녀도 기록이 섞이면 안 된다. */
export function bestRecordKey(agency) {
    return `pkt.best.${agency}`;
}

/* 좁은 구간에 숫자를 넣으면 글자가 잘려 오히려 지저분해진다. */
export function renderSplitBar(el, segments, scale, withLabel = true) {
    if (!el || !scale) return;
    el.innerHTML = segments.map((ms, i) => {
        const pct = Math.max(0, ms / scale * 100);
        const label = withLabel && pct >= 12 ? (ms / 1000).toFixed(1) : '';
        return `<i class="pkt-seg${i + 1}" style="width:${pct.toFixed(1)}%">${label}</i>`;
    }).join('');
}

export function readBestRecord(key) {
    try {
        const raw = localStorage.getItem(key);
        if (!raw) return null;
        const parsed = JSON.parse(raw);
        return Number.isFinite(parsed.total) && Array.isArray(parsed.segments) ? parsed : null;
    } catch (e) {
        return null;
    }
}

export function saveBestRecord(key, totalMs, segments, best) {
    if (best && best.total <= totalMs) return;
    try {
        localStorage.setItem(key, JSON.stringify({ total: totalMs, segments }));
    } catch (e) {
        /* 사파리 사생활 모드에서 쓰기가 막힌다. 비교 막대만 안 나올 뿐이라 삼킨다. */
    }
}

export function renderCompleteHint(segments, segmentSum) {
    const hint = document.getElementById('pkt-hint');
    if (!hint || !segmentSum) return;

    let slowest = 0;
    segments.forEach((ms, i) => { if (ms > segments[slowest]) slowest = i; });

    const share = Math.round(segments[slowest] / segmentSum * 100);
    hint.innerHTML = `세 구간 중 <b>${share}%</b>를 ${SEGMENT_LABELS[slowest]}에 썼어요`;
    hint.style.display = 'block';
}

/* 최고 기록은 초를 따로 보여주지 않고, 갱신했다는 사실만 헤더 딱지로 알린다. */
export function renderBestTag(totalMs, best) {
    const tag = document.getElementById('pkt-best-tag');
    if (!tag) return;
    tag.style.display = best && totalMs < best.total ? 'inline-flex' : 'none';
}

/* 매진 모달의 힌트. 성공과 달리 비교할 최고 기록이 없어 가장 오래 걸린 구간만 짚는다. */
export function renderFailHint(el, segments) {
    if (!el) return;
    const worst = segments
        .map((ms, i) => [ms, ['반응 속도', '대기열', '좌석 화면'][i]])
        .sort((a, b) => b[0] - a[0])[0];

    if (worst[0] > 0) {
        el.innerHTML = `가장 오래 걸린 구간은 <b>${worst[1]} ${(worst[0] / 1000).toFixed(1)}초</b>예요.`;
        el.style.display = 'block';
    } else {
        el.style.display = 'none';
    }
}

/* ── 결과 공유 ──
   링크가 아니라 결과 카드 이미지를 파일로 건넨다. 링크 미리보기를 그려주지 않는 앱(X)에서도
   이미지는 반드시 보이기 때문이다. 그림은 서버가 아니라 화면의 카드를 복제해 브라우저가 그린다. */

const CAPTURE_LIB = '/js/vendor/html2canvas.min.js';

let capturePromise = null;

function loadCapture() {
    if (window.html2canvas) return Promise.resolve(window.html2canvas);
    if (!capturePromise) {
        capturePromise = new Promise((resolve, reject) => {
            const tag = document.createElement('script');
            tag.src = CAPTURE_LIB;
            tag.onload = () => resolve(window.html2canvas);
            tag.onerror = () => { capturePromise = null; reject(new Error('capture lib')); };
            document.head.appendChild(tag);
        });
    }
    return capturePromise;
}

function today() {
    const d = new Date();
    return d.getFullYear()
        + '.' + String(d.getMonth() + 1).padStart(2, '0')
        + '.' + String(d.getDate()).padStart(2, '0');
}

/* 화면의 카드를 그대로 보내면 누를 수도 없는 버튼이 사진에 박힌다.
   그래서 복제본에서 버튼을 걷어내고 그 자리에 로고와 날짜를 넣는다. */
function buildCaptureNode(card) {
    const node = card.cloneNode(true);
    node.classList.add('pkt-capture');

    const line = document.createElement('div');
    line.className = 'pkt-share-line';
    node.appendChild(line);

    const foot = document.createElement('div');
    foot.className = 'pkt-share-foot';

    const brand = document.createElement('span');
    brand.className = 'pkt-share-brand';
    const mark = document.createElement('img');
    mark.src = '/image/logo.png';
    mark.alt = '';
    const name = document.createElement('b');
    name.textContent = '프랙티켓';
    brand.append(mark, name);

    const date = document.createElement('span');
    date.className = 'pkt-share-date';
    date.textContent = today();

    foot.append(brand, date);
    node.appendChild(foot);
    return node;
}

function waitImages(node) {
    const list = [...node.querySelectorAll('img')].filter(img => !img.complete);
    return Promise.all(list.map(img => new Promise(done => {
        img.onload = img.onerror = done;
    })));
}

async function renderCardImage(card) {
    const html2canvas = await loadCapture();

    const stage = document.createElement('div');
    stage.className = 'pkt-capture-stage';
    const node = buildCaptureNode(card);
    stage.appendChild(node);
    document.body.appendChild(stage);

    try {
        await document.fonts.ready;
        await waitImages(node);
        /* 배경을 비워 두면 둥근 모서리가 투명해져 앱마다 검거나 희게 채운다. */
        const canvas = await html2canvas(node, {
            scale: 2,
            backgroundColor: '#ffffff',
            logging: false
        });
        const blob = await new Promise(done => canvas.toBlob(done, 'image/png'));
        return blob && new File([blob], 'practicket.png', { type: 'image/png' });
    } finally {
        stage.remove();
    }
}

function downloadFile(file) {
    const url = URL.createObjectURL(file);
    const link = document.createElement('a');
    link.href = url;
    link.download = file.name;
    document.body.appendChild(link);
    link.click();
    link.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
}

export function bindShareButton() {
    const btn = document.getElementById('pkt-share');
    const card = btn && btn.closest('.pkt-modal-card');
    if (!card) return;

    /* 누른 뒤에 받으면 그 시간만큼 공유창이 늦게 열린다. 캐시되므로 두 번째 연습부터는 공짜다. */
    loadCapture().catch(() => {});

    let file = null;
    let busy = false;

    const label = text => { btn.textContent = text; };

    btn.onclick = async () => {
        if (busy) return;
        busy = true;
        try {
            if (!file) {
                label('준비 중');
                file = await renderCardImage(card);
                if (!file) throw new Error('capture failed');
            }

            if (navigator.canShare && navigator.canShare({ files: [file] })) {
                try {
                    await navigator.share({ files: [file] });
                    label('공유');
                } catch (e) {
                    /* 사파리는 누른 지 1초가 지나면 공유창을 막는다. 이미지는 이미 만들어 뒀으니
                       다시 누르면 기다릴 것이 없어 그 규칙에 걸리지 않는다. */
                    label(e && e.name === 'AbortError' ? '공유' : '다시 누르기');
                }
                return;
            }

            downloadFile(file);
            label('이미지 저장됨');
            setTimeout(() => label('공유'), 1500);
        } catch (e) {
            label('다시 누르기');
        } finally {
            busy = false;
        }
    };
}
