import * as util from "../common.js";
import { GRID_SIZE, drawPixelArt, toPixelData, toStates } from "./pixel.js";

const CELL_SIZE = 16;
const DRAFT_KEY = "pk_art_draft";
const HISTORY_LIMIT = 40;

class GrapePalette {
	constructor() {
		this.canvas = document.getElementById("pixel-canvas");
		if (!this.canvas) return;

		this.form = document.getElementById("art-form");
		this.titleInput = document.getElementById("title");
		this.filledNumber = document.getElementById("filled-number");
		this.draftNote = document.getElementById("draft-note");

		this.isEdit = IS_EDIT;
		this.artId = ART_ID;

		this.states = new Array(GRID_SIZE * GRID_SIZE).fill(false);
		this.history = [];
		this.tool = "draw";
		this.isPainting = false;

		this.bindEvents();
		this.init();
	}

	async init() {
		if (this.isEdit && this.artId) {
			document.getElementById("autosave-hint").remove();
			await this.loadExistingArt();
		} else if (this.restoreDraft()) {
			this.draftNote.hidden = false;
		}
		this.draw();
	}

	async loadExistingArt() {
		try {
			const response = await util.authFetch(`${HOST}/api/arts/${this.artId}`, {
				credentials: 'include'
			});

			if (!response.ok) throw new Error('작품을 불러올 수 없습니다.');

			const artData = await response.json();

			this.titleInput.value = artData.title;
			this.states = toStates(artData.pixel_data);

			document.getElementById("submit-btn").textContent = '작품 수정';
			document.getElementById("header-title").textContent = '포도아트 수정하기';
		} catch (error) {
			console.error('작품 로딩 실패:', error);
			await util.showAlert({ title: '오류', msg: '작품을 불러오는데 실패했습니다.' });
			window.location.href = '/art';
		}
	}

	bindEvents() {
		this.canvas.addEventListener("pointerdown", (e) => this.startStroke(e));
		this.canvas.addEventListener("pointermove", (e) => this.continueStroke(e));
		// 캔버스 밖에서 손을 떼도 획이 끝나야 한다
		window.addEventListener("pointerup", () => this.endStroke());
		window.addEventListener("pointercancel", () => this.endStroke());

		document.querySelectorAll("[data-tool]").forEach((button) => {
			button.addEventListener("click", () => this.handleTool(button));
		});

		document.getElementById("draft-new").addEventListener("click", () => this.discardDraft());

		this.form.addEventListener("submit", (e) => this.handleSubmit(e));

		this.titleInput.addEventListener("keypress", (e) => {
			if (e.key === "Enter") {
				e.preventDefault();
				this.titleInput.blur();
			}
		});
	}

	handleTool(button) {
		const tool = button.dataset.tool;

		if (tool === "undo") {
			if (!this.history.length) return;
			this.states = this.history.pop();
			this.draw();
			this.saveDraft();
			return;
		}

		if (tool === "clear") {
			this.pushHistory();
			this.states = new Array(GRID_SIZE * GRID_SIZE).fill(false);
			this.draw();
			this.saveDraft();
			return;
		}

		this.tool = tool;
		document.querySelectorAll('[data-tool="draw"],[data-tool="erase"]')
			.forEach((b) => b.classList.toggle("current", b === button));
	}

	startStroke(event) {
		const index = this.eventToIndex(event);
		if (index === null) return;

		this.pushHistory();
		this.isPainting = true;
		this.paintAt(index);
	}

	continueStroke(event) {
		if (!this.isPainting) return;
		const index = this.eventToIndex(event);
		if (index === null) return;
		this.paintAt(index);
	}

	endStroke() {
		if (!this.isPainting) return;
		this.isPainting = false;
		this.saveDraft();
	}

	paintAt(index) {
		const value = this.tool !== "erase";
		if (this.states[index] === value) return;

		this.states[index] = value;
		this.draw();
	}

	pushHistory() {
		this.history.push(this.states.slice());
		if (this.history.length > HISTORY_LIMIT) this.history.shift();
	}

	eventToIndex(event) {
		const rect = this.canvas.getBoundingClientRect();
		const scaleX = this.canvas.width / rect.width;
		const scaleY = this.canvas.height / rect.height;

		const col = Math.floor((event.clientX - rect.left) * scaleX / CELL_SIZE);
		const row = Math.floor((event.clientY - rect.top) * scaleY / CELL_SIZE);

		if (Number.isNaN(col) || Number.isNaN(row)) return null;
		if (col < 0 || col >= GRID_SIZE || row < 0 || row >= GRID_SIZE) return null;

		return row * GRID_SIZE + col;
	}

	draw() {
		drawPixelArt(this.canvas, this.states, CELL_SIZE);
		this.filledNumber.textContent = this.states.filter(Boolean).length;
	}

	// 임시저장은 새로 그릴 때만. 수정 화면에서 덮어쓰면 원본이 다른 작품의 초안으로 바뀐다
	saveDraft() {
		if (this.isEdit) return;
		try {
			localStorage.setItem(DRAFT_KEY, toPixelData(this.states));
		} catch (error) {
			console.error("임시저장 실패:", error);
		}
	}

	restoreDraft() {
		const draft = localStorage.getItem(DRAFT_KEY);
		if (!draft || !draft.includes("1")) return false;

		this.states = toStates(draft);
		return true;
	}

	discardDraft() {
		localStorage.removeItem(DRAFT_KEY);
		this.history = [];
		this.states = new Array(GRID_SIZE * GRID_SIZE).fill(false);
		this.draftNote.hidden = true;
		this.draw();
	}

	async handleSubmit(event) {
		event.preventDefault();

		const title = this.titleInput.value.trim();
		if (!title) {
			await util.showAlert({ title: '입력 오류', msg: '제목을 입력해주세요.' });
			return;
		}

		const pixelData = toPixelData(this.states);
		if (!pixelData.includes('1')) {
			await util.showAlert({ title: '입력 오류', msg: '작품을 그려주세요.' });
			return;
		}

		const artData = {
			title,
			pixel_data: pixelData,
			width: GRID_SIZE,
			height: GRID_SIZE
		};

		try {
			const response = await util.authFetch(
				this.isEdit ? `${HOST}/api/arts/${this.artId}` : `${HOST}/api/arts`,
				{
					method: this.isEdit ? "PUT" : "POST",
					headers: { "Content-Type": "application/json" },
					body: JSON.stringify(artData),
					credentials: "same-origin"
				}
			);

			if (!response.ok) {
				const err = await response.json().catch(() => ({}));
				throw new Error(err.message || '작품 등록/수정에 실패했습니다. 다시 시도해주세요.');
			}

			const result = await response.json();
			localStorage.removeItem(DRAFT_KEY);
			window.location.href = `/art/${result.id}`;
		} catch (error) {
			console.error("작품 등록/수정 실패:", error);
			await util.showAlert({ title: '오류', msg: error.message || '작품 등록/수정에 실패했습니다. 다시 시도해주세요.' });
		}
	}
}

document.addEventListener("DOMContentLoaded", () => {
	new GrapePalette();
});
