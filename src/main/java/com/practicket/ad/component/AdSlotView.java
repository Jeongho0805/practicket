package com.practicket.ad.component;

import com.practicket.ad.application.AdNetworkExposureService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 템플릿(fragment)에서 호출하는 렌더 진입점.
 * 사용: th:with="s=${@adSlotView.render('PC_LEFT')}"
 *
 * 요청 스코프인 이유는 두 가지다. 한 페이지에 자리가 여럿이라 스냅샷 조회를 한 번으로 묶어야 하고,
 * 배너 선택이 무작위라 같은 자리를 두 번 물으면 다른 배너가 나오기 때문이다.
 *
 * 노출 집계는 여기서 하지 않는다. 화면 폭에 따라 실제로 보이는 배너만 클라이언트(/js/ad-metrics.js)가
 * 보고하므로, 서버 렌더 시점에 세면 안 보인 배너까지 집계된다.
 */
@Component
@RequestScope
public class AdSlotView {

    private final AdSlotSnapshotStore adSlotSnapshotStore;
    private final AdNetworkSettings adNetworkSettings;
    private final AdNetworkExposureService adNetworkExposureService;

    private final Map<String, AdSlotRender> rendered = new HashMap<>();
    private AdSlotSnapshot snapshot;
    private Map<String, Boolean> networkExposure;

    public AdSlotView(AdSlotSnapshotStore adSlotSnapshotStore, AdNetworkSettings adNetworkSettings,
                      AdNetworkExposureService adNetworkExposureService) {
        this.adSlotSnapshotStore = adSlotSnapshotStore;
        this.adNetworkSettings = adNetworkSettings;
        this.adNetworkExposureService = adNetworkExposureService;
    }

    public AdSlotRender render(String slotCode) {
        return rendered.computeIfAbsent(slotCode, this::build);
    }

    private AdSlotRender build(String slotCode) {
        if (snapshot == null) {
            snapshot = adSlotSnapshotStore.get();
        }
        return snapshot.find(slotCode)
                .map(this::toRender)
                .orElseGet(() -> AdSlotRender.empty(slotCode));
    }

    private AdSlotRender toRender(AdSlotSnapshot.Slot slot) {
        AdSlotSnapshot.Banner banner = pick(slot.banners());
        return new AdSlotRender(
                slot.code(),
                face(slot.hasPcSize(), banner != null && banner.hasPcImage(),
                        banner, banner == null ? null : banner.pcImagePath(), slot.pcUnit()),
                face(slot.hasMobileSize(), banner != null && banner.hasMobileImage(),
                        banner, banner == null ? null : banner.mobileImagePath(), slot.mobileUnit()));
    }

    /**
     * 판정 순서는 하나다. 기기 규격이 없으면 안 그리고, 그 기기 그림이 있으면 배너,
     * 없으면 네트워크가 채우고, 채울 네트워크마저 없으면 안 그린다.
     */
    private AdFace face(boolean served, boolean hasImage,
                        AdSlotSnapshot.Banner banner, String imagePath, AdSlotSnapshot.Unit unit) {
        if (!served) {
            return AdFace.none();
        }
        if (hasImage) {
            return AdFace.banner(banner.id(), banner.advertiserName(), imagePath);
        }
        if (unit == null) {
            return AdFace.none();
        }
        if (networkExposure == null) {
            networkExposure = adNetworkExposureService.currentExposure();
        }
        return AdFace.fill(networkExposure.getOrDefault(unit.network(), false), unit,
                adNetworkSettings.accountOf(unit.network()),
                adNetworkSettings.templateOf(unit.network()));
    }

    private AdSlotSnapshot.Banner pick(List<AdSlotSnapshot.Banner> candidates) {
        if (candidates.isEmpty()) {
            return null;
        }
        if (candidates.size() == 1) {
            return candidates.get(0);
        }
        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }
}
