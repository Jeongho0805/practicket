package com.practicket.ad.component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 전 자리의 렌더 재료를 한 덩어리로 담는다. 페이지마다 자리 수만큼 DB 를 치던 것을 한 번으로 줄인다.
 * 레디스에 JSON 으로 그대로 들어가므로 엔티티가 아니라 값만 담는 record 로 둔다.
 */
public record AdSlotSnapshot(List<Slot> slots, Map<String, Boolean> networkExposure,
                             Map<String, Integer> refillGaps) {

    public AdSlotSnapshot {
        networkExposure = networkExposure == null ? Map.of() : networkExposure;
        refillGaps = refillGaps == null ? Map.of() : refillGaps;
    }

    public AdSlotSnapshot(List<Slot> slots) {
        this(slots, Map.of(), Map.of());
    }

    public AdSlotSnapshot(List<Slot> slots, Map<String, Boolean> networkExposure) {
        this(slots, networkExposure, Map.of());
    }

    public boolean isExposed(String network) {
        return networkExposure.getOrDefault(network, false);
    }

    /** 같은 탭에서 이 네트워크를 다시 부르기까지 비울 분. null 이면 제한 없음 */
    public Integer refillGapOf(String network) {
        return refillGaps.get(network);
    }

    public Optional<Slot> find(String code) {
        return slots.stream().filter(slot -> slot.code().equals(code)).findFirst();
    }

    /**
     * pcUnit·mobileUnit 은 채움 순서 1단계, fallbacks 는 2단계부터 차례대로.
     * 앞 단계가 재요청 간격에 걸렸는지는 브라우저만 알므로 전부 담아 내려보낸다.
     */
    public record Slot(
            String code,
            Integer pcWidth,
            Integer pcHeight,
            Integer mobileWidth,
            Integer mobileHeight,
            String format,
            String fillNetwork,
            Unit pcUnit,
            Unit mobileUnit,
            List<Unit> pcFallbacks,
            List<Unit> mobileFallbacks,
            List<Banner> banners) {

        public Slot {
            pcFallbacks = pcFallbacks == null ? List.of() : pcFallbacks;
            mobileFallbacks = mobileFallbacks == null ? List.of() : mobileFallbacks;
        }

        public Slot(String code, Integer pcWidth, Integer pcHeight, Integer mobileWidth, Integer mobileHeight,
                    String format, String fillNetwork, Unit pcUnit, Unit mobileUnit, List<Banner> banners) {
            this(code, pcWidth, pcHeight, mobileWidth, mobileHeight, format, fillNetwork,
                    pcUnit, mobileUnit, List.of(), List.of(), banners);
        }

        public boolean hasPcSize() {
            return pcWidth != null && pcHeight != null;
        }

        public boolean hasMobileSize() {
            return mobileWidth != null && mobileHeight != null;
        }
    }

    public record Unit(String network, String unitId, Integer width, Integer height, String extra) {

        public Unit(String network, String unitId, Integer width, Integer height) {
            this(network, unitId, width, height, null);
        }
    }

    public record Banner(Long id, String advertiserName, String pcImagePath, String mobileImagePath) {

        public boolean hasPcImage() {
            return pcImagePath != null && !pcImagePath.isBlank();
        }

        public boolean hasMobileImage() {
            return mobileImagePath != null && !mobileImagePath.isBlank();
        }
    }
}
