// 프랙티켓 랜딩 - 불편·건의 모달 (열기/닫기 + 전송)
// 광고·제휴 문의는 별도 페이지(/advertise)로 분리됨
import { authFetch, showAlert, getOrCreateToken } from '/js/common.js';

const currentType = 'COMPLAINT';

function openInq() {
    document.getElementById('inq-overlay').classList.add('on');
}

function closeInq() {
    document.getElementById('inq-overlay').classList.remove('on');
}

async function submitInq() {
    const emailEl = document.getElementById('inq-email');
    const contentEl = document.getElementById('inq-content');
    const sendBtn = document.getElementById('inq-send');
    const email = emailEl.value.trim();
    const content = contentEl.value.trim();

    if (!email) {
        await showAlert({ title: '알림', msg: '회신 받을 이메일을 입력해주세요.' });
        return;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        await showAlert({ title: '알림', msg: '올바른 이메일 형식이 아니에요. 다시 확인해주세요.' });
        return;
    }
    if (content.length < 5) {
        await showAlert({ title: '알림', msg: '문의 내용을 5자 이상 입력해주세요.' });
        return;
    }

    sendBtn.disabled = true;
    try {
        await getOrCreateToken();
        const res = await authFetch(`${HOST}/api/inquiry`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ type: currentType, email, content })
        });
        if (res.ok) {
            emailEl.value = '';
            contentEl.value = '';
            closeInq();
            await showAlert({ title: '접수 완료', msg: '문의가 접수되었습니다. 감사합니다!' });
        } else {
            const err = await res.json().catch(() => ({}));
            await showAlert({ title: '오류', msg: err.message || '문의 전송에 실패했습니다.' });
        }
    } catch (e) {
        await showAlert({ title: '오류', msg: '네트워크 오류로 전송하지 못했습니다.' });
    } finally {
        sendBtn.disabled = false;
    }
}

// 모듈 스코프라 onclick 핸들러가 찾을 수 있도록 전역 노출
window.openInq = openInq;
window.closeInq = closeInq;

document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') closeInq();
});
document.getElementById('inq-send').addEventListener('click', submitInq);
