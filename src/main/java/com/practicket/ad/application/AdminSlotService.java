package com.practicket.ad.application;

import com.practicket.ad.component.AdUnitResolver;
import com.practicket.ad.domain.AdSlot;
import com.practicket.ad.domain.AdSlotRepository;
import com.practicket.ad.domain.AdUnit;
import com.practicket.ad.domain.AdUnitRepository;
import com.practicket.ad.exception.AdException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * 슬롯 관리. 슬롯을 새로 만들지는 못한다 — 템플릿에 조각을 넣어야 실제로 뜨므로
 * 어드민에서 행만 만들면 아무 데도 안 나가는 유령이 된다. 값 수정만 한다.
 */
@Service
@RequiredArgsConstructor
public class AdminSlotService {

    private final AdSlotRepository adSlotRepository;
    private final AdUnitRepository adUnitRepository;
    private final AdUnitResolver adUnitResolver;
    private final AdminCampaignService adminCampaignService;

    /** 어드민 목록은 슬롯을 묶어서 보여준다. 묶음이 없는 슬롯은 맨 뒤에 따로 모은다 */
    @Transactional(readOnly = true)
    public List<SlotGroup> getGroups() {
        List<AdUnit> units = adUnitRepository.findAll();
        Map<Long, Long> liveCounts = adminCampaignService.countLiveBannersBySlot();

        Map<String, SlotGroup> groups = new LinkedHashMap<>();
        adSlotRepository.findAll().stream()
                .sorted(Comparator.comparing((AdSlot slot) -> slot.getSortOrder() == null ? 0 : slot.getSortOrder())
                        .thenComparing(AdSlot::getCode))
                .forEach(slot -> {
                    String name = slot.getGroupName() == null ? "묶음 없음" : slot.getGroupName();
                    groups.computeIfAbsent(name,
                            key -> new SlotGroup(key, slot.getGroupPath(), new ArrayList<>()))
                            .getSlots()
                            .add(toRow(slot, units, liveCounts.getOrDefault(slot.getId(), 0L)));
                });
        return List.copyOf(groups.values());
    }

    @Transactional(readOnly = true)
    public Optional<SlotForm> findForm(Long id) {
        return adSlotRepository.findById(id).map(slot -> {
            SlotForm form = new SlotForm();
            form.setId(slot.getId());
            form.setName(slot.getName());
            form.setPcWidth(slot.getPcWidth());
            form.setPcHeight(slot.getPcHeight());
            form.setMobileWidth(slot.getMobileWidth());
            form.setMobileHeight(slot.getMobileHeight());
            form.setGroupName(slot.getGroupName());
            form.setGroupPath(slot.getGroupPath());
            form.setFormat(slot.getFormat());
            form.setSortOrder(slot.getSortOrder());
            form.setFillNetwork(slot.getFillNetwork());
            form.setPcAdUnitId(slot.getPcAdUnitId());
            form.setMobileAdUnitId(slot.getMobileAdUnitId());
            return form;
        });
    }

    /**
     * 슬롯 폼의 광고단위 후보. 안 들어가는 단위도 고를 수 있게 두고 표시만 한다 —
     * 잠그면 그 값이 폼 전송에서 빠져 지정이 조용히 사라진다.
     */
    @Transactional(readOnly = true)
    public List<UnitOption> unitOptions(SlotForm form) {
        return adUnitRepository.findAllByOrderByNetworkAscNameAsc().stream()
                .map(unit -> new UnitOption(unit,
                        tooBig(unit, form.getPcWidth(), form.getPcHeight()),
                        tooBig(unit, form.getMobileWidth(), form.getMobileHeight())))
                .toList();
    }

    /** 규격이 비어 있는 기기는 슬롯 자체가 안 나가므로 단위를 따지지 않는다 */
    private boolean tooBig(AdUnit unit, Integer slotWidth, Integer slotHeight) {
        return slotWidth != null && slotHeight != null && !unit.fitsIn(slotWidth, slotHeight);
    }

    /** 이미 고른 단위가 슬롯보다 크면 폼 위에 띄울 경고. 저장은 막지 않는다 */
    @Transactional(readOnly = true)
    public String oversizeWarning(SlotForm form) {
        List<AdUnit> units = adUnitRepository.findAllById(
                Stream.of(form.getPcAdUnitId(), form.getMobileAdUnitId()).filter(Objects::nonNull).toList());

        boolean pcOver = units.stream().anyMatch(unit -> unit.getId().equals(form.getPcAdUnitId())
                && tooBig(unit, form.getPcWidth(), form.getPcHeight()));
        boolean mobileOver = units.stream().anyMatch(unit -> unit.getId().equals(form.getMobileAdUnitId())
                && tooBig(unit, form.getMobileWidth(), form.getMobileHeight()));

        if (!pcOver && !mobileOver) {
            return null;
        }
        String device = pcOver && mobileOver ? "데스크톱과 모바일" : (pcOver ? "데스크톱" : "모바일");
        return device + " 광고단위가 슬롯보다 큽니다. 저장은 되지만 광고가 잘려 나갑니다.";
    }

    @Transactional
    public void save(SlotForm form) {
        AdSlot slot = adSlotRepository.findById(form.getId())
                .orElseThrow(() -> new AdException("존재하지 않는 슬롯입니다."));

        adSlotRepository.save(AdSlot.builder()
                .id(slot.getId())
                .code(slot.getCode())
                .name(blankToNull(form.getName()) == null ? slot.getName() : form.getName().trim())
                .recommendedSize(slot.getRecommendedSize())
                .enabled(slot.getEnabled())
                .pcWidth(form.getPcWidth())
                .pcHeight(form.getPcHeight())
                .mobileWidth(form.getMobileWidth())
                .mobileHeight(form.getMobileHeight())
                .groupName(blankToNull(form.getGroupName()))
                .groupPath(blankToNull(form.getGroupPath()))
                .format(blankToNull(form.getFormat()))
                .sortOrder(form.getSortOrder() == null ? 0 : form.getSortOrder())
                .fillNetwork(blankToNull(form.getFillNetwork()))
                .pcAdUnitId(form.getPcAdUnitId())
                .mobileAdUnitId(form.getMobileAdUnitId())
                .createdAt(slot.getCreatedAt())
                .build());
    }

    private SlotRow toRow(AdSlot slot, List<AdUnit> units, long liveBannerCount) {
        AdUnit pcUnit = adUnitResolver.resolve(slot, true, units).orElse(null);
        AdUnit mobileUnit = adUnitResolver.resolve(slot, false, units).orElse(null);

        return new SlotRow(slot, liveBannerCount,
                sizeText(slot.getPcWidth(), slot.getPcHeight()),
                sizeText(slot.getMobileWidth(), slot.getMobileHeight()),
                pcUnit, mobileUnit,
                slot.hasPcSize() && slot.getFillNetwork() != null && pcUnit == null,
                slot.hasMobileSize() && slot.getFillNetwork() != null && mobileUnit == null);
    }

    private String sizeText(Integer width, Integer height) {
        return (width == null || height == null) ? null : width + "x" + height;
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    @Getter
    @AllArgsConstructor
    public static class SlotGroup {
        private final String name;
        private final String path;
        private final List<SlotRow> slots;
    }

    @Getter
    @AllArgsConstructor
    public static class SlotRow {
        private final AdSlot slot;
        private final long liveBannerCount;
        private final String pcSize;
        private final String mobileSize;
        private final AdUnit pcUnit;
        private final AdUnit mobileUnit;
        /** 채울 네트워크는 정했는데 규격에 맞는 단위가 없다 — 그 기기 슬롯이 빈 채로 나간다 */
        private final boolean pcUnitMissing;
        private final boolean mobileUnitMissing;

        public boolean isSold() {
            return liveBannerCount > 0L;
        }

        public boolean isUnitMissing() {
            return pcUnitMissing || mobileUnitMissing;
        }
    }

    @Getter
    public static class UnitOption {
        private final Long id;
        private final String label;
        private final boolean pcTooBig;
        private final boolean mobileTooBig;

        UnitOption(AdUnit unit, boolean pcTooBig, boolean mobileTooBig) {
            this.id = unit.getId();
            this.label = unit.getNetwork() + " · " + unit.getUnitId()
                    + (unit.getName() == null ? "" : " (" + unit.getName() + ")")
                    + (unit.isResponsive() ? "" : " · " + unit.getWidth() + "x" + unit.getHeight());
            this.pcTooBig = pcTooBig;
            this.mobileTooBig = mobileTooBig;
        }

        public String pcLabel() {
            return pcTooBig ? label + " — 슬롯보다 큼" : label;
        }

        public String mobileLabel() {
            return mobileTooBig ? label + " — 슬롯보다 큼" : label;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class SlotForm {
        private Long id;
        private String name;
        private Integer pcWidth;
        private Integer pcHeight;
        private Integer mobileWidth;
        private Integer mobileHeight;
        private String groupName;
        private String groupPath;
        private String format;
        private Integer sortOrder;
        private String fillNetwork;
        private Long pcAdUnitId;
        private Long mobileAdUnitId;
    }
}
