import * as util from "../common.js";

const PIXEL_THEME = {
	gridSize: 30,
	cellSize: 24,
	gap: 6,
	corner: 6,
	previewScale: 12,
	colors: {
		background: "#281d43",
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
		this.isLoading = false;
		this.galleryContainer = document.getElementById("gallery-grid");

		// 검색/필터 상태
		this.searchKeyword = "";
		this.sortBy = "LATEST";
		this.sortDirection = "DESC";
		this.onlyMine = false;

		this.init();
	}

	init() {
		this.setupControls();
		this.loadArts();
		this.setupInfiniteScroll();
	}

	setupControls() {
		const searchInput = document.getElementById("search-input");
		const searchBtn = document.getElementById("search-btn");
		const sortSelectType = document.getElementById("sort-select-type");
		const sortTrigger = document.getElementById("sort-trigger");
		const sortValue = document.getElementById("sort-value");
		const sortOptions = document.getElementById("sort-options");
		const directionToggle = document.getElementById("direction-toggle");
		const onlyMineCheckbox = document.getElementById("only-mine-checkbox");

		// 검색 버튼 클릭
		const performSearch = () => {
			this.searchKeyword = searchInput.value.trim();
			this.resetAndReload();
		};

		searchBtn.addEventListener("click", performSearch);

		// 검색 입력 (디바운스 적용)
		let searchTimeout;
		searchInput.addEventListener("input", (e) => {
			clearTimeout(searchTimeout);
			searchTimeout = setTimeout(() => {
				this.searchKeyword = e.target.value.trim();
				this.resetAndReload();
			}, 500);
		});

		// 엔터키로도 검색
		searchInput.addEventListener("keypress", (e) => {
			if (e.key === "Enter") {
				clearTimeout(searchTimeout);
				performSearch();
			}
		});

		// 정렬 타입 셀렉트 토글
		sortTrigger.addEventListener("click", (e) => {
			e.stopPropagation();
			sortTrigger.classList.toggle("active");
			sortOptions.classList.toggle("active");
		});

		// 정렬 타입 옵션 선택
		sortOptions.querySelectorAll("li").forEach((option) => {
			option.addEventListener("click", (e) => {
				e.stopPropagation();
				const value = option.dataset.value;
				const text = option.textContent;

				// 선택된 옵션 업데이트
				sortOptions.querySelectorAll("li").forEach((li) => li.classList.remove("selected"));
				option.classList.add("selected");
				sortValue.textContent = text;

				// 상태 업데이트 및 재로드
				this.sortBy = value;
				this.resetAndReload();

				// 드롭다운 닫기
				sortTrigger.classList.remove("active");
				sortOptions.classList.remove("active");
			});
		});

		// 외부 클릭시 드롭다운 닫기
		document.addEventListener("click", (e) => {
			if (!sortSelectType.contains(e.target)) {
				sortTrigger.classList.remove("active");
				sortOptions.classList.remove("active");
			}
		});

		// 정렬 방향 토글
		directionToggle.addEventListener("click", () => {
			if (this.sortDirection === "DESC") {
				this.sortDirection = "ASC";
				directionToggle.setAttribute("data-direction", "asc");
				directionToggle.setAttribute("title", "오름차순");
				directionToggle.innerHTML = `
					<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
						<path d="M12 19V5M5 12l7-7 7 7"/>
					</svg>
				`;
			} else {
				this.sortDirection = "DESC";
				directionToggle.setAttribute("data-direction", "desc");
				directionToggle.setAttribute("title", "내림차순");
				directionToggle.innerHTML = `
					<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
						<path d="M12 5v14M19 12l-7 7-7-7"/>
					</svg>
				`;
			}
			this.resetAndReload();
		});

		// 내 작품만 보기
		onlyMineCheckbox.addEventListener("change", (e) => {
			this.onlyMine = e.target.checked;
			this.resetAndReload();
		});
	}

	resetAndReload() {
		this.currentPage = 0;
		this.hasMore = true;
		this.galleryContainer.innerHTML = "";
		this.loadArts();
	}

	setupInfiniteScroll() {
		const galleryContent = document.getElementById("gallery-content");
		galleryContent.addEventListener("scroll", () => {
			if (galleryContent.scrollHeight - galleryContent.scrollTop <= galleryContent.clientHeight + 500) {
				this.loadArts();
			}
		});
	}

	buildQueryString() {
		const params = new URLSearchParams();
		params.append("page", this.currentPage);
		params.append("size", this.size);

		if (this.searchKeyword) {
			params.append("keyword", this.searchKeyword);
		}

		params.append("sortBy", this.sortBy);
		params.append("sortDirection", this.sortDirection);

		if (this.onlyMine) {
			params.append("onlyMine", "true");
		}

		return params.toString();
	}

	async loadArts() {
		if (!this.hasMore || this.isLoading) {
			return;
		}

		this.isLoading = true;

		try {
			const queryString = this.buildQueryString();
			const response = await util.authFetch(`${HOST}/api/arts?${queryString}`);
			const data = await response.json();

			if (data.content && data.content.length > 0) {
				this.renderArts(data.content);
				this.currentPage++;
				this.hasMore = !data.last;
			} else {
				this.hasMore = false;
				// 첫 페이지에서 결과가 없으면 빈 결과 메시지 표시
				if (this.currentPage === 0) {
					this.showEmptyMessage();
				}
			}
		} catch (error) {
			console.error("failed to load arts", error);
			alert("작품을 불러오는데 실패했습니다.");
		} finally {
			this.isLoading = false;
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

		const artPreview = document.createElement("div");
		artPreview.className = "art-preview";
		artPreview.onclick = () => {
			window.location.href = `/art/${art.id}`;
		};

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

		const isLiked = art.is_liked_by_current_user || false;
		const likeIconPath = 'M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41 0.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z';

		const likeFill = isLiked ? '#ed4956' : 'none';
		const likeStroke = isLiked ? '#ed4956' : 'currentColor';
		const strokeColor = '#1C1B20FF';

		artInfo.innerHTML = `
			<div class="art-stats">
				<div class="art-likes ${isLiked ? 'liked' : ''}" data-art-id="${art.id}">

					<svg aria-label="좋아요" height="20" width="20" viewBox="0 0 24 24">
						<path d="${likeIconPath}" fill="${likeFill}" stroke="${likeStroke}" stroke-width="2"></path>
					</svg>
					<span class="art-stats-number like-count">${this.formatCount(art.like_count || 0)}</span>
				</div>
				<div class="art-comments" onclick="window.location.href='/art/${art.id}'">
					<svg aria-label="댓글" height="20" width="20" viewBox="0 0 24 24">
						<path d="M20.656 17.008a9.993 9.993 0 1 0-3.59 3.615L22 22Z" fill="none" stroke="currentColor" stroke-linejoin="round" stroke-width="2"></path>
					</svg>
					<span class="art-stats-number">${this.formatCount(art.comment_count || 0)}</span>
				</div>
				<div class="art-views">
					<svg aria-label="조회수" height="20" width="20" viewBox="0 0 109.21 122.88">
						<path fill="#000000"  stroke="#000000" stroke-width="2" stroke-linejoin="round" stroke-width="2" d="M86,122.31a5.57,5.57,0,0,1-.9.35,5.09,5.09,0,0,1-1,.18,5.46,5.46,0,0,1-1,0,6.77,6.77,0,0,1-1-.15,6,6,0,0,1-1-.36l0,0a5.51,5.51,0,0,1-.92-.53l0,0a6.41,6.41,0,0,1-.78-.69,5.19,5.19,0,0,1-.65-.87l-9.08-14.88-7.69,9a15.49,15.49,0,0,1-1.1,1.18c-.39.37-.78.71-1.18,1l-.08.06a12.19,12.19,0,0,1-1.2.82,9.66,9.66,0,0,1-1.24.63,6.91,6.91,0,0,1-1,.37,6.21,6.21,0,0,1-1,.22,7.55,7.55,0,0,1-1.06.07,7.19,7.19,0,0,1-1-.11,6.14,6.14,0,0,1-1.18-.35,5.42,5.42,0,0,1-1.06-.57,6.22,6.22,0,0,1-.92-.78l0,0a7.31,7.31,0,0,1-.75-1l-.11-.2-.09-.21L47.72,112l0-.17L40.91,43.26a4.52,4.52,0,0,1,0-1.33,4.3,4.3,0,0,1,.43-1.25,4.31,4.31,0,0,1,1.39-1.55l0,0a3.82,3.82,0,0,1,.9-.46,4.25,4.25,0,0,1,1-.24h0a4.31,4.31,0,0,1,1.29.05,4.67,4.67,0,0,1,1.25.44l.3.16c13.51,8.84,26.1,17.06,38.64,25.25l19,12.39a11.72,11.72,0,0,1,1,.72l0,0a8.78,8.78,0,0,1,.82.73l.06.07a7.41,7.41,0,0,1,.71.82,5.91,5.91,0,0,1,.57.87,6.42,6.42,0,0,1,.51,1.14,5.6,5.6,0,0,1,.26,1.17,5.44,5.44,0,0,1,0,1.21h0a6.59,6.59,0,0,1-.23,1.19,6.54,6.54,0,0,1-.94,1.88,6.41,6.41,0,0,1-.67.83,7.45,7.45,0,0,1-.82.76,10.42,10.42,0,0,1-1.16.83,12.92,12.92,0,0,1-1.34.7c-.47.21-1,.41-1.46.58a14.27,14.27,0,0,1-1.55.43h0c-2.77.54-5.53,1.21-8.27,1.87l-3.25.77,9,14.94a5.84,5.84,0,0,1,.46,1,5.59,5.59,0,0,1,.15,3.21l0,.1a5.53,5.53,0,0,1-.33.94,6.43,6.43,0,0,1-.51.89,5.62,5.62,0,0,1-.68.81,6,6,0,0,1-.82.67l-2,1.29A83,83,0,0,1,86,122.31ZM37.63,19.46a4,4,0,0,1-6.92,4l-8-14a4,4,0,0,1,6.91-4l8.06,14Zm-15,46.77a4,4,0,0,1,4,6.91l-14,8.06a4,4,0,0,1-4-6.91l14-8.06ZM20.56,39.84a4,4,0,0,1-2.07,7.72L3,43.36A4,4,0,0,1,5,35.64l15.53,4.2ZM82,41.17a4,4,0,0,1-4-6.91L92,26.2a4,4,0,0,1,4,6.91L82,41.17ZM63.46,20.57a4,4,0,1,1-7.71-2.06L59.87,3A4,4,0,0,1,67.59,5L63.46,20.57Zm20.17,96.36,9.67-5.86c-3.38-5.62-8.85-13.55-11.51-19.17a2.17,2.17,0,0,1-.12-.36,2.4,2.4,0,0,1,1.81-2.87c5.38-1.23,10.88-2.39,16.22-3.73a10.28,10.28,0,0,0,1.8-.58,6.11,6.11,0,0,0,1.3-.77,3.38,3.38,0,0,0,.38-.38.9.9,0,0,0,.14-.24l-.06-.18a2.15,2.15,0,0,0-.44-.53,5.75,5.75,0,0,0-.83-.63L47.06,45.75c2.11,21.36,5.2,44.1,6.45,65.31a6.28,6.28,0,0,0,.18,1,2.89,2.89,0,0,0,.26.62l.13.14a1,1,0,0,0,.29,0,2.76,2.76,0,0,0,.51-.17,5.71,5.71,0,0,0,1.28-.79,11.22,11.22,0,0,0,1.35-1.33c1.93-2.27,9.6-12.14,11.4-13.18a2.4,2.4,0,0,1,3.28.82l11.44,18.75Z"/>
					</svg>
					<span class="art-stats-number">${this.formatCount(art.view_count || 0)}</span>
				</div>
			</div>
			<div class="art-caption">
				<span class="art-author">${art.author_name}</span>
				<span class="art-title">${art.title}</span>
			</div>
			<div>
				<span class="art-date">${this.formatDate(art.created_at)}</span>
			</div>
		`;

		// 좋아요 버튼 이벤트 리스너
		const likeBtn = artInfo.querySelector(".art-likes");
		likeBtn.addEventListener("click", (e) => {
			e.stopPropagation();
			this.toggleLike(art.id, likeBtn);
		});

		card.appendChild(artPreview);
		card.appendChild(artInfo);

		return card;
	}

	async toggleLike(artId, likeBtn) {
		try {
			const response = await util.authFetch(`${HOST}/api/arts/${artId}/like`, {
				method: "POST",
				credentials: "same-origin"
			});

			if (!response.ok) throw new Error("좋아요 실패");

			const result = await response.json();
			const isLiked = result.is_liked;
			const likeCount = result.like_count;

			// UI 업데이트
			const svgPath = likeBtn.querySelector("svg path");
			const countSpan = likeBtn.querySelector(".like-count");

			if (isLiked) {
				likeBtn.classList.add("liked");
				svgPath.setAttribute("fill", "#ed4956");
				svgPath.setAttribute("stroke", "#ed4956");
			} else {
				likeBtn.classList.remove("liked");
				svgPath.setAttribute("fill", "none");
				svgPath.setAttribute("stroke", "currentColor");
			}

			countSpan.textContent = this.formatCount(likeCount);
		} catch (error) {
			console.error("좋아요 토글 실패:", error);
			alert("좋아요 처리에 실패했습니다.");
		}
	}

	renderPixelArt(canvas, rawPixelData, width, height, pixelSize = 1) {
		const ctx = canvas.getContext("2d");
		ctx.imageSmoothingEnabled = false;

		// 배경 작업
		ctx.fillStyle = PIXEL_THEME.colors.background;
		ctx.fillRect(0, 0, canvas.width, canvas.height);

		// 문자열 데이터 가공 -> boolean 배열 반환
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
		if (!pixelData || typeof pixelData !== "string") return [];

		const total = width * height;
		const cleaned = pixelData.replace(/[^01]/g, "");
		const pixels = [...cleaned].map((digit) => digit === "1");

		return this.fillToSize(pixels, total);
	}

	// 픽셀 데이터가 canvas 사이즈에 맞지 않은 예외 케이스 처리
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

	escapeHtml(text) {
		const div = document.createElement("div");
		div.textContent = text ?? "";
		return div.innerHTML;
	}

	formatDate(dateString) {
		const date = new Date(dateString);
		const now = new Date();

		const diffMs = now - date; // 시간 차 (ms)
		const diffSec = Math.floor(diffMs / 1000);
		const diffMin = Math.floor(diffSec / 60);
		const diffHour = Math.floor(diffMin / 60);

		// 오늘 하루 안에 속하는 경우
		if (
			now.toDateString() === date.toDateString() // 같은 날인지 비교
		) {
			if (diffSec < 60) {
				return `${diffSec}초 전`;
			} else if (diffMin < 60) {
				return `${diffMin}분 전`;
			} else {
				return `${diffHour}시간 전`;
			}
		}

		// 하루 이상 차이나면 날짜로 표시
		return date.toLocaleDateString("ko-KR", {
			year: "numeric",
			month: "short",
			day: "numeric",
		});;
	}

	formatCount(count) {
		if (count >= 1000000) {
			return (count / 1000000).toFixed(1).replace(/\.0$/, '') + 'm';
		}
		if (count >= 1000) {
			return (count / 1000).toFixed(1).replace(/\.0$/, '') + 'k';
		}
		return count.toString();
	}

	showEmptyMessage() {
		const emptyMessage = document.createElement("div");
		emptyMessage.className = "gallery-empty";
		emptyMessage.textContent = "검색 결과가 없습니다.";
		this.galleryContainer.appendChild(emptyMessage);
	}
}

document.addEventListener("DOMContentLoaded", () => {
	new Gallery();
});
