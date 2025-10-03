const PIXEL_THEME = {
	gridSize: 30,
	cellSize: 24,
	gap: 6,
	corner: 6,
	previewScale: 12,
	colors: {
		background: "#12082a",
		base: "#6633cc",
		hover: "#7c4dff",
		active: "#cbb5ff",
		activeHover: "#e4daff",
		border: "rgba(255,255,255,0.55)",
		shadow: "rgba(85,34,170,0.25)",
		shadowActive: "rgba(161,120,255,0.35)"
	}
};

class Gallery {
	constructor() {
		this.currentPage = 0;
		this.size = 20;
		this.hasMore = true;
		this.galleryContainer = document.getElementById("gallery-grid");
		this.init();
	}

	init() {
		this.loadArts();
		this.setupInfiniteScroll();
	}

	async loadArts() {
		if (!this.hasMore) {
			return;
		}
		try {
			const response = await fetch(`${HOST}/api/arts?page=${this.currentPage}&size=${this.size}`);
			const data = await response.json();

			if (data.content && data.content.length > 0) {
				this.renderArts(data.content);
				this.currentPage++;
				this.hasMore = !data.last;
			} else {
				this.hasMore = false;
			}
		} catch (error) {
			console.error("failed to load arts", error);
			alert("작품을 불러오는데 실패했습니다.");
		}
	}

	renderArts(arts) {
		arts.forEach((art) => {
			const artCard = this.createArtCard(art);
			this.galleryContainer.appendChild(artCard);
		});
	}

	createArtCard(art) {
		const card = document.createElement("div");
		card.className = "art-card";
		card.onclick = () => {
			window.location.href = `/art/${art.id}`;
		};

		const artPreview = document.createElement("div");
		artPreview.className = "art-preview";

		const canvas = document.createElement("canvas");
		canvas.className = "art-canvas";

		const pixelSize = PIXEL_THEME.previewScale;
		const width = art.width ?? PIXEL_THEME.gridSize;
		const height = art.height ?? PIXEL_THEME.gridSize;
		canvas.width = width * pixelSize;
		canvas.height = height * pixelSize;

		this.renderPixelArt(canvas, art.pixel_data, width, height, pixelSize);
		artPreview.appendChild(canvas);

		const artInfo = document.createElement("div");
		artInfo.className = "art-info";
		artInfo.innerHTML = `
			<h3 class="art-title">${art.title}</h3>
			<p class="art-description">${this.escapeHtml(art.description)}</p>
			<div class="art-meta">
				<span class="art-author">작가명 : ${art.author_name}</span>
				<span>${this.formatDate(art.created_at)}</span>
			</div>
		`;

		card.appendChild(artPreview);
		card.appendChild(artInfo);

		return card;
	}

	renderPixelArt(canvas, rawPixelData, width, height, pixelSize = 1) {
		const ctx = canvas.getContext("2d");
		ctx.imageSmoothingEnabled = false;

		ctx.fillStyle = PIXEL_THEME.colors.background;
		ctx.fillRect(0, 0, canvas.width, canvas.height);

		const pixels = this.normalizePixelData(rawPixelData, width, height);
		if (!pixels.length) return;

		const gapRatio = PIXEL_THEME.gap / PIXEL_THEME.cellSize;
		const cornerRatio = PIXEL_THEME.corner / PIXEL_THEME.cellSize;
		const gap = pixelSize * gapRatio;
		const innerSize = pixelSize - gap;
		const radius = Math.min(pixelSize * cornerRatio, innerSize / 2);

		pixels.forEach((isActive, index) => {
			const col = index % width;
			const row = Math.floor(index / width);
			const x = col * pixelSize + gap / 2;
			const y = row * pixelSize + gap / 2;

			ctx.save();
			ctx.beginPath();
			this.roundedRectPath(ctx, x, y, innerSize, innerSize, radius);
			ctx.fillStyle = isActive ? PIXEL_THEME.colors.active : PIXEL_THEME.colors.base;
			ctx.shadowColor = isActive ? PIXEL_THEME.colors.shadowActive : PIXEL_THEME.colors.shadow;
			ctx.shadowBlur = isActive ? 14 : 8;
			ctx.fill();
			ctx.restore();
		});
	}

	normalizePixelData(pixelData, width, height) {
		if (pixelData == null) return [];

		const total = width * height;
		const coerceBool = (value) => {
			if (value === true || value === 1) return true;
			if (value === false || value === 0) return false;
			if (typeof value === "string") {
				const lowered = value.trim().toLowerCase();
				if (lowered === "true" || lowered === "1") return true;
				if (lowered === "false" || lowered === "0") return false;
			}
			return false;
		};

		if (typeof pixelData === "string") {
			const trimmed = pixelData.trim();
			const compactDigits = trimmed.replace(/[^01]/g, "");

			if (compactDigits.length > 0 && /^[01]+$/.test(compactDigits)) {
				return this.fillToSize([...compactDigits].map((digit) => digit === "1"), total);
			}

			if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
				try {
					const parsed = JSON.parse(trimmed);
					if (Array.isArray(parsed)) {
						return this.normalizePixelData(parsed, width, height);
					}
				} catch (error) {
					// fallback to token parsing
				}
			}

			const tokens = trimmed.includes(",")
				? trimmed.split(/[\s,]+/).filter(Boolean)
				: trimmed.split("");

			const mapped = tokens.map((token) => {
				const lowered = token.trim().toLowerCase();
				if (lowered === "1" || lowered === "true") return true;
				if (lowered === "0" || lowered === "false") return false;
				return false;
			});
			return this.fillToSize(mapped, total);
		}

		if (Array.isArray(pixelData)) {
			const flattened = pixelData.flat(Infinity);
			const mapped = flattened.map((value) => coerceBool(value));
			return this.fillToSize(mapped, total);
		}

		return [];
	}

	fillToSize(pixels, total) {
		if (pixels.length >= total) {
			return pixels.slice(0, total);
		}
		const padding = Array(total - pixels.length).fill(false);
		return pixels.concat(padding);
	}

	roundedRectPath(ctx, x, y, width, height, radius) {
		ctx.moveTo(x + radius, y);
		ctx.arcTo(x + width, y, x + width, y + height, radius);
		ctx.arcTo(x + width, y + height, x, y + height, radius);
		ctx.arcTo(x, y + height, x, y, radius);
		ctx.arcTo(x, y, x + width, y, radius);
		ctx.closePath();
	}

	setupInfiniteScroll() {
		window.addEventListener("scroll", () => {
			if (window.innerHeight + window.scrollY >= document.body.offsetHeight - 1000) {
				this.loadArts();
			}
		});
	}

	escapeHtml(text) {
		const div = document.createElement("div");
		div.textContent = text ?? "";
		return div.innerHTML;
	}

	formatDate(dateString) {
		const date = new Date(dateString);
		return date.toLocaleDateString("ko-KR", {
			year: "numeric",
			month: "short",
			day: "numeric"
		});
	}
}

document.addEventListener("DOMContentLoaded", () => {
	new Gallery();
});
