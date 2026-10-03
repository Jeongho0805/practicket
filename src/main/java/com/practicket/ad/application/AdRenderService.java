package com.practicket.ad.application;

import com.practicket.ad.component.AdSlotSnapshot;
import com.practicket.ad.component.AdUnitResolver;
import com.practicket.ad.domain.AdCampaign;
import com.practicket.ad.domain.AdCampaignRepository;
import com.practicket.ad.domain.AdSlot;
import com.practicket.ad.domain.AdSlotFillStep;
import com.practicket.ad.domain.AdSlotFillStepRepository;
import com.practicket.ad.domain.AdSlotRepository;
import com.practicket.ad.domain.AdUnit;
import com.practicket.ad.domain.AdUnitRepository;
import com.practicket.ad.domain.Advertiser;
import com.practicket.ad.domain.AdvertiserRepository;
import com.practicket.ad.domain.Banner;
import com.practicket.ad.domain.BannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 자리·광고단위·판매 배너를 모아 렌더용 스냅샷 한 덩어리로 만든다.
 * 지금 어느 배너를 내보낼지 고르는 일은 여기서 하지 않는다 — 스냅샷이 후보를 다 담고
 * 요청마다 {@link com.practicket.ad.component.AdSlotView} 가 무작위로 하나를 뽑는다.
 */
@Service
@RequiredArgsConstructor
public class AdRenderService {

    private final AdSlotRepository adSlotRepository;
    private final AdSlotFillStepRepository adSlotFillStepRepository;
    private final AdUnitRepository adUnitRepository;
    private final BannerRepository bannerRepository;
    private final AdCampaignRepository adCampaignRepository;
    private final AdvertiserRepository advertiserRepository;
    private final AdUnitResolver adUnitResolver;
    private final AdNetworkExposureService adNetworkExposureService;

    @Transactional(readOnly = true)
    public AdSlotSnapshot build() {
        LocalDate today = LocalDate.now();

        List<AdUnit> units = adUnitRepository.findAll();
        Map<Long, List<AdSlotFillStep>> stepsBySlot = adSlotFillStepRepository.findAllByOrderBySlotIdAscStepOrderAsc()
                .stream().collect(Collectors.groupingBy(AdSlotFillStep::getSlotId));

        List<Banner> banners = bannerRepository.findAllEnabledWithSlot();
        Map<Long, AdCampaign> campaigns = adCampaignRepository.findAllById(
                        banners.stream().map(Banner::getCampaignId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(AdCampaign::getId, Function.identity()));
        Map<Long, Advertiser> advertisers = advertiserRepository.findAllById(
                        campaigns.values().stream().map(AdCampaign::getAdvertiserId).distinct().toList())
                .stream().collect(Collectors.toMap(Advertiser::getId, Function.identity()));

        Map<Long, List<Banner>> bannersBySlot = banners.stream()
                .filter(banner -> isActive(banner, campaigns.get(banner.getCampaignId()), today))
                .collect(Collectors.groupingBy(banner -> banner.getSlot().getId()));

        List<AdSlotSnapshot.Slot> slots = adSlotRepository.findAll().stream()
                .map(slot -> toSlot(slot, bannersBySlot.getOrDefault(slot.getId(), List.of()),
                        units, stepsBySlot.getOrDefault(slot.getId(), List.of()), campaigns, advertisers))
                .toList();

        return new AdSlotSnapshot(slots, adNetworkExposureService.currentExposure(),
                adNetworkExposureService.currentRefillGaps());
    }

    private AdSlotSnapshot.Slot toSlot(AdSlot slot, List<Banner> slotBanners, List<AdUnit> units,
                                       List<AdSlotFillStep> steps,
                                       Map<Long, AdCampaign> campaigns, Map<Long, Advertiser> advertisers) {
        boolean filled = slot.getFillNetwork() != null;
        return new AdSlotSnapshot.Slot(
                slot.getCode(),
                slot.getPcWidth(), slot.getPcHeight(),
                slot.getMobileWidth(), slot.getMobileHeight(),
                slot.getFormat(),
                slot.getFillNetwork(),
                adUnitResolver.resolve(slot, true, units).map(this::toUnit).orElse(null),
                adUnitResolver.resolve(slot, false, units).map(this::toUnit).orElse(null),
                filled ? fallbacks(steps, units, slot.getPcWidth(), slot.getPcHeight(), true) : List.of(),
                filled ? fallbacks(steps, units, slot.getMobileWidth(), slot.getMobileHeight(), false) : List.of(),
                slotBanners.stream()
                        .map(banner -> toBanner(banner, campaigns, advertisers))
                        .toList());
    }

    /** 규격에 맞는 단위가 없는 단계는 그 기기에서 건너뛴다 */
    private List<AdSlotSnapshot.Unit> fallbacks(List<AdSlotFillStep> steps, List<AdUnit> units,
                                                Integer width, Integer height, boolean pc) {
        return steps.stream()
                .map(step -> adUnitResolver.resolve(step.getNetwork(),
                        pc ? step.getPcAdUnitId() : step.getMobileAdUnitId(), width, height, units))
                .flatMap(Optional::stream)
                .map(this::toUnit)
                .toList();
    }

    private AdSlotSnapshot.Banner toBanner(Banner banner,
                                           Map<Long, AdCampaign> campaigns, Map<Long, Advertiser> advertisers) {
        return new AdSlotSnapshot.Banner(
                banner.getId(),
                advertiserNameOf(banner, campaigns, advertisers),
                banner.getPcImagePath(),
                banner.getMobileImagePath());
    }

    private String advertiserNameOf(Banner banner,
                                    Map<Long, AdCampaign> campaigns, Map<Long, Advertiser> advertisers) {
        AdCampaign campaign = campaigns.get(banner.getCampaignId());
        if (campaign != null) {
            Advertiser advertiser = advertisers.get(campaign.getAdvertiserId());
            if (advertiser != null) {
                return advertiser.getName();
            }
        }
        return banner.getAdvertiserName();
    }

    /**
     * 기간을 배너가 비우면 계약을 따른다. 양쪽 다 모르면 언제까지 나가야 할지 알 수 없으므로 안 내보낸다.
     * 그림이 하나도 없는 배너도 마찬가지다 — 자리를 차지만 하고 아무것도 안 보인다.
     */
    private boolean isActive(Banner banner, AdCampaign campaign, LocalDate today) {
        if (banner.getPcImagePath() == null && banner.getMobileImagePath() == null) {
            return false;
        }
        LocalDate start = banner.getStartAt() != null ? banner.getStartAt()
                : (campaign != null ? campaign.getStartAt() : null);
        LocalDate end = banner.getEndAt() != null ? banner.getEndAt()
                : (campaign != null ? campaign.getEndAt() : null);
        return start != null && end != null && !today.isBefore(start) && !today.isAfter(end);
    }

    private AdSlotSnapshot.Unit toUnit(AdUnit unit) {
        return new AdSlotSnapshot.Unit(unit.getNetwork(), unit.getUnitId(),
                unit.getWidth(), unit.getHeight(), unit.getExtra());
    }
}
