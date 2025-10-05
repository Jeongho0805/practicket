import * as util from "../common.js";

const CONFIG = {
	gridSize: 30,
	cellSize: 16,
	gap: 4,
	corner: 4,
	colors: {
		base: "#6633cc",
		hover: "#7c4dff",
		active: "#cbb5ff",
		activeHover: "#e4daff",
		border: "rgba(255,255,255,0.55)",
		shadow: "rgba(85,34,170,0.25)",
		shadowActive: "rgba(161,120,255,0.35)",
		background: "#12082a"
	}
};

class GrapePalette {
	constructor({ canvasId, resetId, formId }) {
		this.canvas = document.getElementById(canvasId);
		if (!this.canvas) return;

		this.ctx = this.canvas.getContext("2d");
		this.resetBtn = document.getElementById(resetId);
		this.form = document.getElementById(formId);

		this.gridSize = CONFIG.gridSize;
		this.cellSize = CONFIG.cellSize;
		this.gap = CONFIG.gap;
		this.corner = CONFIG.corner;
		this.innerSize = this.cellSize - this.gap;

		this.canvas.width = this.gridSize * this.cellSize;
		this.canvas.height = this.gridSize * this.cellSize;
		this.canvas.style.touchAction = "manipulation";

		this.state = new Array(this.gridSize * this.gridSize).fill(false);
		this.hoverIndex = null;

		this.bindEvents();
		this.drawBoard();
	}

	bindEvents() {
		this.canvas.addEventListener("pointermove", (e) => this.handleHover(e));
		this.canvas.addEventListener("pointerleave", () => this.clearHover());
		this.canvas.addEventListener("click", (e) => this.handleToggle(e));

		this.resetBtn?.addEventListener("click", () => this.reset());
		this.form?.addEventListener("submit", (e) => this.handleSubmit(e));
	}

	handleHover(event) {
		const index = this.eventToIndex(event);
		if (index === this.hoverIndex) return;
		this.hoverIndex = index;
		this.drawBoard();
	}

	clearHover() {
		if (this.hoverIndex === null) return;
		this.hoverIndex = null;
		this.drawBoard();
	}

	handleToggle(event) {
		const index = this.eventToIndex(event);
		if (index === null) return;
		this.state[index] = !this.state[index];
		this.drawCell(index);
	}

	eventToIndex(event) {
		const rect = this.canvas.getBoundingClientRect();
		const clientX = event.clientX ?? event.touches?.[0]?.clientX;
		const clientY = event.clientY ?? event.touches?.[0]?.clientY;

		const x = Math.floor((clientX - rect.left) / this.cellSize);
		const y = Math.floor((clientY - rect.top) / this.cellSize);

		if (Number.isNaN(x) || Number.isNaN(y)) return null;
		if (x < 0 || x >= this.gridSize || y < 0 || y >= this.gridSize) return null;

		return y * this.gridSize + x;
	}

	drawBoard() {
		this.ctx.fillStyle = CONFIG.colors.background;
		this.ctx.fillRect(0, 0, this.canvas.width, this.canvas.height);
		this.state.forEach((_, index) => this.drawCell(index));
	}

	drawCell(index) {
		const { x, y } = this.indexToPoint(index);
		const isActive = this.state[index];
		const isHover = index === this.hoverIndex;

		const color = isActive
			? (isHover ? CONFIG.colors.activeHover : CONFIG.colors.active)
			: (isHover ? CONFIG.colors.hover : CONFIG.colors.base);

		const drawX = x + this.gap / 2;
		const drawY = y + this.gap / 2;
		const size = this.innerSize;
		const radius = Math.min(this.corner, size / 2);

		this.ctx.save();
		this.ctx.beginPath();
		this.roundedRectPath(drawX, drawY, size, size, radius);
		this.ctx.fillStyle = color;
		this.ctx.shadowColor = isActive ? CONFIG.colors.shadowActive : CONFIG.colors.shadow;
		this.ctx.shadowBlur = isActive ? 14 : 8;
		this.ctx.fill();

		if (isHover) {
			this.ctx.lineWidth = 1.5;
			this.ctx.strokeStyle = CONFIG.colors.border;
			this.ctx.stroke();
		}

		this.ctx.restore();
	}

	roundedRectPath(x, y, width, height, radius) {
		const ctx = this.ctx;
		ctx.moveTo(x + radius, y);
		ctx.arcTo(x + width, y, x + width, y + height, radius);
		ctx.arcTo(x + width, y + height, x, y + height, radius);
		ctx.arcTo(x, y + height, x, y, radius);
		ctx.arcTo(x, y, x + width, y, radius);
		ctx.closePath();
	}

	indexToPoint(index) {
		const col = index % this.gridSize;
		const row = Math.floor(index / this.gridSize);
		return { x: col * this.cellSize, y: row * this.cellSize };
	}

	reset() {
		this.state.fill(false);
		this.drawBoard();
	}

	toPixelData() {
		return this.state.map(active => active ? '1' : '0').join('');
	}

	async handleSubmit(event) {
		if (!this.form) return;
		event.preventDefault();

		const title = this.form.querySelector("#title")?.value.trim();

		if (!title) {
			alert("제목을 입력해주세요.");
			return;
		}

		const artData = {
			title,
			pixel_data: this.toPixelData(),
			width: this.gridSize,
			height: this.gridSize
		};

		try {
			const response = await util.authFetch(`${HOST}/api/arts`, {
				method: "POST",
				headers: { "Content-Type": "application/json" },
				body: JSON.stringify(artData),
				credentials: "same-origin"
			});

			if (!response.ok) throw new Error(await response.text());
			const result = await response.json();
			window.location.href = `/art/${result.id}`;
		} catch (error) {
			console.error("작품 등록 실패:", error);
			alert("작품 등록에 실패했습니다. 다시 시도해주세요.");
		}
	}
}

document.addEventListener("DOMContentLoaded", () => {
	new GrapePalette({
		canvasId: "pixel-canvas",
		resetId: "clear-btn",
		formId: "art-form"
	});
});
