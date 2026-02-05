
// I-Ticket Booking Logic
// Implements the specific flow described in plan.md
// CAPTCHA -> AREA_SELECT -> SEAT_SELECT -> PRICE_DISCOUNT -> DELIVERY_ORDER -> PAYMENT

const STATE = {
    step: 'SEAT', // SEAT, PRICE, DELIVERY, PAYMENT
    seatPhase: 'CAPTCHA', // CAPTCHA, AREA, SEAT, FOLDED
    selectedSeats: [], // Array of {row, col, grade, price, id}
    ticketPrice: 88000,
    fee: 2000,
    captchaAnswer: 'BXWUMU'
};

const DOM = {
    root: document.getElementById('ip-root'),
    captchaOverlay: document.getElementById('captcha-overlay'),
    captchaInput: document.getElementById('txtCaptcha'),
    captchaText: document.getElementById('captcha-text'),
    captchaPlaceholder: document.getElementById('captcha-placeholder'),

    // Seat Phase
    seatGrid: document.getElementById('seat-grid'),
    seatRows: document.getElementById('seat-rows'),
    seatAreaLabel: document.getElementById('seat-area-label'),

    // Right Panel (Seat)
    seatCount: document.getElementById('seat-count'),
    seatList: document.getElementById('seat-list'),
    seatCompleteBtn: document.getElementById('seat-complete'),

    // Price Phase
    quantitySelect: document.getElementById('ticket-quantity'),

    // Forms
    orderName: document.getElementById('order-name'),
    orderBirth: document.getElementById('order-birth'),
    orderPhone1: document.getElementById('order-phone-1'),
    orderPhone2: document.getElementById('order-phone-2'),
    orderPhone3: document.getElementById('order-phone-3'),
    orderEmail: document.getElementById('order-email'),

    // Summary
    summarySeat: document.getElementById('summary-seat'),
    summaryTicket: document.getElementById('summary-ticket'),
    summaryFee: document.getElementById('summary-fee'),
    summaryDelivery: document.getElementById('summary-delivery'),
    summaryDiscount: document.getElementById('summary-discount'),
    summaryTotal: document.getElementById('summary-total'),

    toast: document.getElementById('ip-toast')
};

document.addEventListener('DOMContentLoaded', () => {
    init();
});

function init() {
    renderCaptcha();
    bindEvents();
    renderSeats(20, 15); // Mock 20 rows, 15 cols
    updateUI();
}

function bindEvents() {
    // Global Action Handling (Delegation)
    document.addEventListener('click', (e) => {
        const actionBtn = e.target.closest('[data-action]');
        if (actionBtn) {
            handleAction(actionBtn.dataset.action, actionBtn);
        }
    });

    // Captcha Input Enter Key
    if (DOM.captchaInput) {
        DOM.captchaInput.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') handleCaptchaSubmit();
        });

        // Placeholder Logic
        DOM.captchaInput.addEventListener('focus', () => {
            if (DOM.captchaPlaceholder) DOM.captchaPlaceholder.style.display = 'none';
            DOM.captchaInput.parentElement.classList.remove('error');
        });
        DOM.captchaInput.addEventListener('blur', () => {
            if (!DOM.captchaInput.value && DOM.captchaPlaceholder) {
                DOM.captchaPlaceholder.style.display = 'block';
            }
        });
        DOM.captchaInput.addEventListener('input', () => {
            if (DOM.captchaPlaceholder) DOM.captchaPlaceholder.style.display = 'none';
        });
    }

    // Seat Section Click
    document.querySelectorAll('.ip-seat-section').forEach(path => {
        path.addEventListener('click', (e) => {
            if (STATE.seatPhase === 'CAPTCHA' || STATE.seatPhase === 'FOLDED') {
                if (STATE.seatPhase === 'FOLDED') {
                    // Bring back captcha
                    renderCaptcha();
                }
                return; // Block interaction
            }
            const areaId = e.target.dataset.areaId;
            enterSeatDetail(areaId);
        });
    });

    // Quantity Change
    if (DOM.quantitySelect) {
        DOM.quantitySelect.addEventListener('change', updateSummary);
    }
}

function handleAction(action, target) {
    switch (action) {
        case 'captcha-submit': handleCaptchaSubmit(); break;
        case 'captcha-refresh': refreshCaptcha(); break;
        case 'captcha-cancel':
            if (confirm('초기화면으로 돌아가시겠습니까?')) location.reload();
            break;
        case 'captcha-fold':
            setSeatPhase('FOLDED');
            DOM.captchaOverlay.setAttribute('aria-hidden', 'true');
            showToast('좌석을 둘러보시려면 구역을 클릭하세요. 예매하려면 보안문자를 확인해야 합니다.');
            break;
        case 'back-to-area': setSeatPhase('AREA'); break;
        case 'seat-reset': resetSeats(); break;
        case 'seat-complete':
            if (STATE.selectedSeats.length > 0) setStep('PRICE');
            else alert('좌석을 선택해주세요.');
            break;
        case 'prev-step': goBackStep(); break;
        case 'next-step': goNextStep(); break;
        case 'toast': showToast(target.dataset.toast); break;
    }
}

// --- Captcha ---

function renderCaptcha() {
    STATE.seatPhase = 'CAPTCHA';
    DOM.root.setAttribute('data-seat-phase', 'CAPTCHA');
    DOM.captchaOverlay.setAttribute('aria-hidden', 'false'); // Show
    if (DOM.captchaInput) {
        DOM.captchaInput.value = '';
        DOM.captchaInput.focus();
        if (DOM.captchaPlaceholder) DOM.captchaPlaceholder.style.display = 'none'; // Input focused
    }
}

function refreshCaptcha() {
    // In a real app, fetch new image. Here just visual feedback.
    showToast('보안문자가 새로고침 되었습니다. (연습)');
    // We could randomize the text here if we wanted
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
    let text = '';
    for (let i = 0; i < 6; i++) text += chars.charAt(Math.floor(Math.random() * chars.length));
    STATE.captchaAnswer = text;
    if (DOM.captchaText) DOM.captchaText.textContent = text;
    if (DOM.captchaInput) {
        DOM.captchaInput.value = '';
        DOM.captchaInput.focus();
    }
}

function handleCaptchaSubmit() {
    if (!DOM.captchaInput) return;
    const val = DOM.captchaInput.value.toUpperCase();
    if (val === STATE.captchaAnswer) {
        DOM.captchaOverlay.setAttribute('aria-hidden', 'true');
        setSeatPhase('AREA');
    } else {
        // Show error UI in new structure
        DOM.captchaInput.parentElement.classList.add('error');
        alert('보안문자가 일치하지 않습니다. 다시 입력해주세요.');
        DOM.captchaInput.value = '';
        DOM.captchaInput.focus();
    }
}

// --- Navigation & State ---

function setStep(step) {
    STATE.step = step;
    DOM.root.setAttribute('data-step', step);

    // Update Active Nav
    document.querySelectorAll('.ip-step').forEach(el => {
        if (el.dataset.stepTarget === step) el.classList.add('ip-step-active');
        else el.classList.remove('ip-step-active');
    });

    // Special handling for phases
    if (step === 'PRICE') {
        DOM.summarySeat.textContent = STATE.selectedSeats.length + '석';
        updateSummary();
    }
}

function setSeatPhase(phase) {
    STATE.seatPhase = phase;
    DOM.root.setAttribute('data-seat-phase', phase);
}

function goBackStep() {
    if (STATE.step === 'PRICE') setStep('SEAT');
    else if (STATE.step === 'DELIVERY') setStep('PRICE');
    else if (STATE.step === 'PAYMENT') setStep('DELIVERY');
}

function goNextStep() {
    if (STATE.step === 'PRICE') {
        setStep('DELIVERY');
    } else if (STATE.step === 'DELIVERY') {
        if (validateDelivery()) setStep('PAYMENT');
    } else if (STATE.step === 'PAYMENT') {
        if (validatePayment()) {
            alert('예매가 완료되었습니다!');
            location.reload();
        }
    }
}

// --- Seat Selection ---

function enterSeatDetail(areaId) {
    DOM.seatAreaLabel.textContent = areaId;
    setSeatPhase('SEAT');
}

function renderSeats(rows, cols) {
    if (!DOM.seatGrid) return;
    DOM.seatGrid.innerHTML = '';
    DOM.seatGrid.style.gridTemplateColumns = `repeat(2, 1fr)`; // 2 blocks
    DOM.seatRows.innerHTML = '';

    // Create Row Labels
    for (let r = 1; r <= rows; r++) {
        const span = document.createElement('span');
        span.textContent = r;
        DOM.seatRows.appendChild(span);
    }

    // Create 2 Blocks of seats
    for (let b = 0; b < 2; b++) {
        const block = document.createElement('div');
        block.className = 'ip-seat-block';
        block.style.gridTemplateColumns = `repeat(${cols}, 12px)`;

        for (let r = 1; r <= rows; r++) {
            for (let c = 1; c <= cols; c++) {
                const seat = document.createElement('div');
                seat.className = 'ip-seat-item';
                seat.dataset.row = r;
                seat.dataset.col = (b * cols) + c; // Continuous column numbering
                seat.title = `${r}열 ${(b * cols) + c}번`;

                seat.addEventListener('click', () => toggleSeat(seat));
                block.appendChild(seat);
            }
        }
        DOM.seatGrid.appendChild(block);
    }
}

function toggleSeat(el) {
    const isSelected = el.classList.contains('is-selected');

    if (isSelected) {
        el.classList.remove('is-selected');
        STATE.selectedSeats = STATE.selectedSeats.filter(s => s.id !== el.title);
    } else {
        if (STATE.selectedSeats.length >= 4) {
            alert('최대 4매까지만 선택 가능합니다.');
            return;
        }
        el.classList.add('is-selected');
        STATE.selectedSeats.push({
            id: el.title,
            row: el.dataset.row,
            col: el.dataset.col,
            price: STATE.ticketPrice
        });
    }
    updateSeatSidePanel();
}

function resetSeats() {
    STATE.selectedSeats = [];
    document.querySelectorAll('.ip-seat-item.is-selected').forEach(el => el.classList.remove('is-selected'));
    updateSeatSidePanel();
}

function updateSeatSidePanel() {
    DOM.seatCount.textContent = STATE.selectedSeats.length;
    DOM.seatList.innerHTML = '';
    STATE.ticketPrice = 88000; // Reset price base

    STATE.selectedSeats.forEach(s => {
        const div = document.createElement('div');
        div.textContent = `[전석] ${s.row}열 ${s.col}번`;
        div.style.marginBottom = "4px";
        DOM.seatList.appendChild(div);
    });

    DOM.seatCompleteBtn.disabled = STATE.selectedSeats.length === 0;
}

// --- Validation & Calculation ---

function updateUI() {
    // Initial UI Setup
    if (DOM.captchaPlaceholder) DOM.captchaPlaceholder.style.display = 'block';
}

function updateSummary() {
    const count = STATE.selectedSeats.length;
    // Force quantity to match seat count (since we are reserving specific seats)

    if (DOM.quantitySelect) {
        DOM.quantitySelect.value = count > 0 ? count : 1;
        DOM.quantitySelect.disabled = true; // Fixed to seat count
    }

    const qty = count;
    const ticketTotal = STATE.ticketPrice * qty;
    const feeTotal = STATE.fee * qty;
    const total = ticketTotal + feeTotal;

    DOM.summaryTicket.textContent = ticketTotal.toLocaleString() + '원';
    DOM.summaryFee.textContent = feeTotal.toLocaleString() + '원';
    DOM.summaryTotal.textContent = total.toLocaleString() + '원';
}

function validateDelivery() {
    if (!DOM.orderName.value.trim()) { alert('이름을 입력해주세요.'); DOM.orderName.focus(); return false; }

    const birthVal = DOM.orderBirth.value;
    if (!/^\d{6}$/.test(birthVal)) { alert('생년월일 6자리를 정확히 입력해주세요.'); DOM.orderBirth.focus(); return false; }

    const p2 = DOM.orderPhone2.value;
    const p3 = DOM.orderPhone3.value;
    if (p2.length < 3 || p3.length < 4) { alert('연락처를 정확히 입력해주세요.'); DOM.orderPhone2.focus(); return false; }

    // Email basic check
    if (!DOM.orderEmail.value.includes('@')) { alert('이메일 형식이 올바르지 않습니다.'); DOM.orderEmail.focus(); return false; }

    return true;
}

function validatePayment() {
    const method = document.querySelector('input[name="pay-method"]:checked');
    if (!method) { alert('결제방식을 선택해주세요.'); return false; }

    if (method.value === 'card') {
        const cardType = document.querySelector('input[name="card-type"]:checked');
        if (!cardType) { alert('카드 종류를 선택해주세요.'); return false; }

        if (cardType.value === 'general') {
            const select = document.getElementById('card-select');
            if (!select.value) { alert('카드사를 선택해주세요.'); return false; }
        }
    }

    return true;
}

// --- Utils ---

function showToast(msg) {
    if (!msg) return;
    DOM.toast.textContent = msg;
    DOM.toast.classList.add('show');
    setTimeout(() => {
        DOM.toast.classList.remove('show');
    }, 2000);
}
