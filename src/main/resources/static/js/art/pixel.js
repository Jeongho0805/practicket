export const GRID_SIZE = 30;

export const COLORS = {
	background: "#f4f2fb",
	empty: "#eae5f7",
	fill: "#6b5b9a"
};

export function toStates(pixelData, size = GRID_SIZE) {
	const total = size * size;
	const states = new Array(total).fill(false);

	if (!pixelData || typeof pixelData !== "string") return states;

	const cleaned = pixelData.replace(/[^01]/g, "");
	for (let i = 0; i < Math.min(cleaned.length, total); i++) {
		states[i] = cleaned[i] === "1";
	}
	return states;
}

export function toPixelData(states) {
	return states.map(filled => filled ? "1" : "0").join("");
}

export function drawPixelArt(canvas, states, cell, size = GRID_SIZE) {
	if (!canvas) return;

	canvas.width = size * cell;
	canvas.height = size * cell;

	const ctx = canvas.getContext("2d");
	const gap = Math.max(1, cell * 0.16);
	const radius = Math.max(1, cell * 0.2);
	const inner = cell - gap;

	ctx.fillStyle = COLORS.background;
	ctx.fillRect(0, 0, canvas.width, canvas.height);

	// 빈 칸/채운 칸을 각각 하나의 path 로 몰아 그린다. 칸마다 fill 하면 900번 호출된다
	for (const filled of [false, true]) {
		ctx.fillStyle = filled ? COLORS.fill : COLORS.empty;
		ctx.beginPath();
		for (let i = 0; i < states.length; i++) {
			if (states[i] !== filled) continue;
			const col = i % size;
			const row = Math.floor(i / size);
			ctx.roundRect(col * cell + gap / 2, row * cell + gap / 2, inner, inner, radius);
		}
		ctx.fill();
	}
}
