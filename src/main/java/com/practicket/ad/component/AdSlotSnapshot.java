package com.practicket.ad.component;

import java.util.List;
import java.util.Optional;

/**
 * 전 자리의 렌더 재료를 한 덩어리로 담는다. 페이지마다 자리 수만큼 DB 를 치던 것을 한 번으로 줄인다.
 * 레디스에 JSON 으로 그대로 들어가므로 엔티티가 아니라 값만 담는 record 로 둔다.
 */
public record AdSlotSnapshot(List<Slot> slots) {

    public Optional<Slot> find(String code) {
        return slots.stream().filter(slot -> slot.code().equals(code)).findFirst();
    }

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
            List<Banner> banners) {

        public boolean hasPcSize() {
            return pcWidth != null && pcHeight != null;
        }

        public boolean hasMobileSize() {
            return mobileWidth != null && mobileHeight != null;
        }
    }

    public record Unit(String network, String unitId, Integer width, Integer height) {
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
