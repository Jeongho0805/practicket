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

export function renderCompleteHint(segments, segmentSum, best) {
    const hint = document.getElementById('pkt-hint');
    if (!hint || !segmentSum) return;

    let slowest = 0;
    segments.forEach((ms, i) => { if (ms > segments[slowest]) slowest = i; });

    const share = Math.round(segments[slowest] / segmentSum * 100);
    let text = `세 구간 중 <b>${share}%</b>를 ${SEGMENT_LABELS[slowest]}에 썼어요`;

    if (best) {
        const diffSec = (segments[slowest] - best.segments[slowest]) / 1000;
        if (diffSec > 0.05) {
            text += ` · 최고 기록보다 <b>${diffSec.toFixed(2)}초</b> 깁니다`;
        }
    }

    hint.innerHTML = text;
    hint.style.display = 'block';
}

export function renderBestChip(totalMs, best) {
    const chip = document.getElementById('pkt-pb-chip');
    if (!chip) return;

    if (best && totalMs < best.total) {
        chip.textContent = `▼ ${((best.total - totalMs) / 1000).toFixed(2)}초 단축 · 개인 신기록`;
        chip.style.display = 'inline-flex';
    } else {
        chip.style.display = 'none';
    }
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

/* 이미지를 보내는 게 아니라 링크를 보낸다. 카톡·X 가 그 링크의 og:image 를
   긁어가 카드로 그려주고, 그 카드는 클릭이 된다. 이미지 안의 주소는 클릭이 안 된다. */
export function bindShareButton(result, type) {
    const btn = document.getElementById('pkt-share');
    if (!btn) return;

    const params = new URLSearchParams({
        type,
        total: result.total_duration_ms,
        reaction: result.reaction_time_ms,
        queue: result.queue_wait_ms,
        seat: result.seat_selection_ms,
        rank: result.queue_initial_rank || 0
    });
    if (result.percentile != null) params.set('pct', result.percentile);

    const url = `${window.location.origin}/practice/result?${params.toString()}`;
    const text = `티켓팅 연습 ${(result.total_duration_ms / 1000).toFixed(3)}초`;

    btn.onclick = async () => {
        if (navigator.share) {
            try {
                await navigator.share({ title: '프랙티켓', text, url });
                return;
            } catch (e) {
                if (e && e.name === 'AbortError') return;
            }
        }
        try {
            await navigator.clipboard.writeText(url);
            btn.textContent = '링크 복사됨';
            setTimeout(() => { btn.textContent = '공유'; }, 1500);
        } catch (e) {
            window.open(url, '_blank');
        }
    };
}
