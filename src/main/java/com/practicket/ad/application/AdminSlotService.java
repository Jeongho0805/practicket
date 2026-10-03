package com.practicket.ad.application;

import com.practicket.ad.component.AdUnitResolver;
import com.practicket.ad.component.AdNetworkSettings;
import com.practicket.ad.domain.AdSlot;
import com.practicket.ad.domain.AdSlotFillStep;
import com.practicket.ad.domain.AdSlotFillStepRepository;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 슬롯 관리. 슬롯을 새로 만들지는 못한다 — 템플릿에 조각을 넣어야 실제로 뜨므로
 * 어드민에서 행만 만들면 아무 데도 안 나가는 유령이 된다. 값 수정만 한다.
 */
@Service
@RequiredArgsConstructor
public class AdminSlotService {

    private final AdSlotRepository adSlotRepository;
    private final AdSlotFillStepRepository adSlotFillStepRepository;
    private final AdUnitRepository adUnitRepository;
    private final AdUnitResolver adUnitResolver;
    private final AdminCampaignService adminCampaignService;

    /** 어드민 목록은 슬롯을 묶어서 보여준다. 묶음이 없는 슬롯은 맨 뒤에 따로 모은다 */
    @Transactional(readOnly = true)
    public List<SlotGroup> getGroups() {
        List<AdUnit> units = adUnitRepository.findAll();
        Map<Long, Long> liveCounts = adminCampaignService.countLiveBannersBySlot();
        Map<Long, List<AdSlotFillStep>> stepsBySlot = adSlotFillStepRepository.findAllByOrderBySlotIdAscStepOrderAsc()
                .stream().collect(Collectors.groupingBy(AdSlotFillStep::getSlotId));

        Map<String, SlotGroup> groups = new LinkedHashMap<>();
        adSlotRepository.findAll().stream()
                .sorted(Comparator.comparing((AdSlot slot) -> slot.getSortOrder() == null ? 0 : slot.getSortOrder())
                        .thenComparing(AdSlot::getCode))
                .forEach(slot -> {
                    String name = slot.getGroupName() == null ? "묶음 없음" : slot.getGroupName();
                    groups.computeIfAbsent(name,
                            key -> new SlotGroup(key, slot.getGroupPath(), new ArrayList<>()))
                            .getSlots()
                            .add(toRow(slot, units, stepsBySlot.getOrDefault(slot.getId(), List.of()),
                                    liveCounts.getOrDefault(slot.getId(), 0L)));
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

    @Transactional(readOnly = true)
    public List<AdUnit> allUnits() {
        return adUnitRepository.findAllByOrderByNetworkAscNameAsc();
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

    /** 끝에 아직 순서에 없는 첫 네트워크를 붙인다. 순서가 비어 있으면 그게 1단계가 된다 */
    @Transactional
    public void addStep(Long slotId) {
        AdSlot slot = findSlot(slotId);
        List<Step> chain = chainOf(slot);
        String network = AdNetworkSettings.LABELS.keySet().stream()
                .filter(candidate -> chain.stream().noneMatch(step -> step.network().equals(candidate)))
                .findFirst()
                .orElseThrow(() -> new AdException("모든 네트워크가 이미 채움 순서에 있습니다."));
        chain.add(new Step(network, null, null));
        writeChain(slot, chain);
    }

    /** 네트워크를 바꾸면 그 단계의 직접 지정은 해제한다 — 이전 네트워크의 단위라서다 */
    @Transactional
    public void changeStep(Long slotId, int index, String network, Long pcAdUnitId, Long mobileAdUnitId) {
        AdSlot slot = findSlot(slotId);
        List<Step> chain = chainOf(slot);
        Step current = stepAt(chain, index);
        String next = validNetwork(network);
        if (next == null) {
            throw new AdException("네트워크를 골라 주세요. 단계를 없애려면 빼기를 누르세요.");
        }
        for (int i = 0; i < chain.size(); i++) {
            if (i != index && chain.get(i).network().equals(next)) {
                throw new AdException("같은 네트워크를 채움 순서에 두 번 넣을 수 없습니다.");
            }
        }
        if (!next.equals(current.network())) {
            chain.set(index, new Step(next, null, null));
        } else {
            validateUnit(next, pcAdUnitId);
            validateUnit(next, mobileAdUnitId);
            chain.set(index, new Step(next, slot.hasPcSize() ? pcAdUnitId : null,
                    slot.hasMobileSize() ? mobileAdUnitId : null));
        }
        writeChain(slot, chain);
    }

    /** 끌어 놓은 자리로 옮긴다. 0 번으로 옮기면 그 단계가 자리 칸(1단계)이 된다 */
    @Transactional
    public void moveStep(Long slotId, int from, int to) {
        AdSlot slot = findSlot(slotId);
        List<Step> chain = chainOf(slot);
        Step moving = stepAt(chain, from);
        stepAt(chain, to);
        if (from == to) {
            return;
        }
        chain.remove(from);
        chain.add(to, moving);
        writeChain(slot, chain);
    }

    /** 1단계를 빼면 2단계가 1단계로 올라온다. 마지막 하나를 빼면 자리를 비워 둔다 */
    @Transactional
    public void removeStep(Long slotId, int index) {
        AdSlot slot = findSlot(slotId);
        List<Step> chain = chainOf(slot);
        stepAt(chain, index);
        chain.remove(index);
        writeChain(slot, chain);
    }

    private AdSlot findSlot(Long id) {
        return adSlotRepository.findById(id)
                .orElseThrow(() -> new AdException("존재하지 않는 슬롯입니다."));
    }

    private Step stepAt(List<Step> chain, int index) {
        if (index < 0 || index >= chain.size()) {
            throw new AdException("채움 순서가 바뀌었습니다. 새로고침 후 다시 시도해 주세요.");
        }
        return chain.get(index);
    }

    private List<Step> chainOf(AdSlot slot) {
        return chainOf(slot, adSlotFillStepRepository.findAllBySlotIdOrderByStepOrderAsc(slot.getId()));
    }

    /** 1단계 네트워크가 없으면 2단계부터가 남아 있어도 자리는 비워 둔다 */
    private List<Step> chainOf(AdSlot slot, List<AdSlotFillStep> extraSteps) {
        List<Step> chain = new ArrayList<>();
        if (slot.getFillNetwork() == null) {
            return chain;
        }
        chain.add(new Step(slot.getFillNetwork(), slot.getPcAdUnitId(), slot.getMobileAdUnitId()));
        extraSteps.forEach(step -> chain.add(new Step(step.getNetwork(), step.getPcAdUnitId(), step.getMobileAdUnitId())));
        return chain;
    }

    /** 1단계는 자리 칸에, 2단계부터는 따로 적는다 */
    private void writeChain(AdSlot slot, List<Step> chain) {
        adSlotFillStepRepository.deleteAllBySlotId(slot.getId());
        if (chain.isEmpty()) {
            slot.changeFillNetwork(null);
            slot.changeAdUnits(null, null);
            return;
        }
        Step first = chain.get(0);
        slot.changeFillNetwork(first.network());
        slot.changeAdUnits(first.pcAdUnitId(), first.mobileAdUnitId());
        for (int i = 1; i < chain.size(); i++) {
            Step step = chain.get(i);
            adSlotFillStepRepository.save(new AdSlotFillStep(slot.getId(), i + 1,
                    step.network(), step.pcAdUnitId(), step.mobileAdUnitId()));
        }
    }

    private String validNetwork(String value) {
        String network = blankToNull(value);
        if (network != null && !AdNetworkSettings.LABELS.containsKey(network)) {
            throw new AdException("지원하지 않는 광고 네트워크입니다.");
        }
        return network;
    }

    private void validateUnit(String network, Long id) {
        if (id == null) return;
        AdUnit unit = adUnitRepository.findById(id)
                .orElseThrow(() -> new AdException("존재하지 않는 광고단위입니다."));
        if (!Objects.equals(network, unit.getNetwork())) {
            throw new AdException("선택한 네트워크에 속한 광고단위만 지정할 수 있습니다.");
        }
    }

    @Transactional
    public void save(SlotForm form) {
        AdSlot slot = adSlotRepository.findById(form.getId())
                .orElseThrow(() -> new AdException("존재하지 않는 슬롯입니다."));

        String network = validNetwork(form.getFillNetwork());
        validateUnit(network, form.getPcAdUnitId());
        validateUnit(network, form.getMobileAdUnitId());

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
                .fillNetwork(network)
                .pcAdUnitId(form.getPcAdUnitId())
                .mobileAdUnitId(form.getMobileAdUnitId())
                .createdAt(slot.getCreatedAt())
                .build());
    }

    private SlotRow toRow(AdSlot slot, List<AdUnit> units, List<AdSlotFillStep> extraSteps, long liveBannerCount) {
        List<Step> chain = chainOf(slot, extraSteps);
        List<StepRow> steps = new ArrayList<>();
        for (int i = 0; i < chain.size(); i++) {
            Step step = chain.get(i);
            AdUnit pcUnit = adUnitResolver.resolve(step.network(), step.pcAdUnitId(),
                    slot.getPcWidth(), slot.getPcHeight(), units).orElse(null);
            AdUnit mobileUnit = adUnitResolver.resolve(step.network(), step.mobileAdUnitId(),
                    slot.getMobileWidth(), slot.getMobileHeight(), units).orElse(null);
            steps.add(new StepRow(i, step.network(), step.pcAdUnitId(), step.mobileAdUnitId(), pcUnit, mobileUnit,
                    slot.hasPcSize() && pcUnit == null, slot.hasMobileSize() && mobileUnit == null));
        }
        return new SlotRow(slot, liveBannerCount,
                sizeText(slot.getPcWidth(), slot.getPcHeight()),
                sizeText(slot.getMobileWidth(), slot.getMobileHeight()),
                steps);
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

    private record Step(String network, Long pcAdUnitId, Long mobileAdUnitId) {
    }

    @Getter
    @AllArgsConstructor
    public static class SlotRow {
        private final AdSlot slot;
        private final long liveBannerCount;
        private final String pcSize;
        private final String mobileSize;
        /** 채움 순서. 비어 있으면 미판매 시 자리를 비워 둔다 */
        private final List<StepRow> steps;

        public boolean isSold() {
            return liveBannerCount > 0L;
        }

        /** 1단계에 맞는 단위가 없으면 간격과 상관없이 늘 다음 단계로 넘어간다 — 대시보드가 경고한다 */
        public boolean isPcUnitMissing() {
            return !steps.isEmpty() && steps.get(0).isPcUnitMissing();
        }

        public boolean isMobileUnitMissing() {
            return !steps.isEmpty() && steps.get(0).isMobileUnitMissing();
        }

        public boolean isUnitMissing() {
            return isPcUnitMissing() || isMobileUnitMissing();
        }
    }

    @Getter
    @AllArgsConstructor
    public static class StepRow {
        private final int index;
        private final String network;
        private final Long pcAdUnitId;
        private final Long mobileAdUnitId;
        private final AdUnit pcUnit;
        private final AdUnit mobileUnit;
        /** 네트워크는 정했는데 규격에 맞는 단위가 없다 — 그 기기에서는 이 단계를 건너뛴다 */
        private final boolean pcUnitMissing;
        private final boolean mobileUnitMissing;
    }

    @Getter
    public static class UnitOption {
        private final Long id;
        private final String network;
        private final String label;
        private final boolean pcTooBig;
        private final boolean mobileTooBig;

        UnitOption(AdUnit unit, boolean pcTooBig, boolean mobileTooBig) {
            this.id = unit.getId();
            this.network = unit.getNetwork();
            this.label = unit.getNetwork() + " · " + unit.getUnitId()
                    + (unit.getName() == null ? "" : " (" + unit.getName() + ")")
                    + (unit.isResponsive() ? "" : " · " + unit.getWidth() + "x" + unit.getHeight());
            this.pcTooBig = pcTooBig;
            this.mobileTooBig = mobileTooBig;
        }

        /** 이유를 앞에 둔다 — 닫힌 드롭다운은 폭이 좁아 뒤가 잘린다 */
        public String pcLabel() {
            return pcTooBig ? "슬롯보다 큼 — " + label : label;
        }

        public String mobileLabel() {
            return mobileTooBig ? "슬롯보다 큼 — " + label : label;
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
