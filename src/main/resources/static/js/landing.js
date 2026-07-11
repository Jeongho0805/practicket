// 프랙티켓 랜딩 - 문의 모달 (열기/닫기). 실제 전송은 다음 단계에서 연결.

function openInq(type) {
    const isAd = type === 1;
    document.getElementById('inq-title').textContent = isAd ? '광고 · 제휴 문의' : '불편 · 건의';
    document.getElementById('inq-desc').textContent = isAd
        ? '배너 광고, 제휴 제안 등 비즈니스 문의를 남겨주세요.'
        : '버그 제보나 개선 아이디어가 있다면 알려주세요.';
    document.getElementById('inq-send').textContent = isAd ? '광고 문의 보내기' : '건의 사항 보내기';
    document.getElementById('inq-overlay').classList.add('on');
}

function closeInq() {
    document.getElementById('inq-overlay').classList.remove('on');
}

// 모듈 스코프라 onclick 핸들러가 찾을 수 있도록 전역 노출
window.openInq = openInq;
window.closeInq = closeInq;

document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') closeInq();
});
