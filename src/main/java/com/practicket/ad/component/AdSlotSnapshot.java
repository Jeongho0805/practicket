package com.practicket.ad.component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 전 자리의 렌더 재료를 한 덩어리로 담는다. 페이지마다 자리 수만큼 DB 를 치던 것을 한 번으로 줄인다.
 * 레디스에 JSON 으로 그대로 들어가므로 엔티티가 아니라 값만 담는 record 로 둔다.
 */
public record AdSlotSnapshot(List<Slot> slots, Map<String, Boolean> networkExposure) {

    public AdSlotSnapshot {
        networkExposure = networkExposure == null ? Map.of() : networkExposure;
    }

    public AdSlotSnapshot(List<Slot> slots) {
        this(slots, Map.of());
    }

    public boolean isExposed(String network) {
        return networkExposure.getOrDefault(network, false);
    }

    public Optional<Slot> find(String code) {
        return slots.stream().filter(slot -> slot.code().equals(code)).findFirst();
    }

    /**
     * fallback 단위는 첫째 네트워크가 재요청 간격 안일 때 같은 자리에 대신 넣을 것.
     * 어느 쪽을 쓸지는 브라우저가 정하므로 둘 다 담아 내려보낸다.
     * refillGapMinutes 는 첫째 네트워크의 재요청 간격. 렌더가 네트워크 설정을 따로 읽지 않도록 여기 담는다.
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
            Unit pcFallbackUnit,
            Unit mobileFallbackUnit,
            Integer refillGapMinutes,
            List<Banner> banners) {

        public Slot(String code, Integer pcWidth, Integer pcHeight, Integer mobileWidth, Integer mobileHeight,
                    String format, String fillNetwork, Unit pcUnit, Unit mobileUnit,
                    Unit pcFallbackUnit, Unit mobileFallbackUnit, List<Banner> banners) {
            this(code, pcWidth, pcHeight, mobileWidth, mobileHeight, format, fillNetwork,
                    pcUnit, mobileUnit, pcFallbackUnit, mobileFallbackUnit, null, banners);
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
