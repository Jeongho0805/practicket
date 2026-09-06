package com.practicket.ad.application;

import com.practicket.ad.domain.AdCampaign;
import com.practicket.ad.domain.AdCampaignRepository;
import com.practicket.ad.domain.Advertiser;
import com.practicket.ad.domain.AdvertiserRepository;
import com.practicket.ad.domain.Banner;
import com.practicket.ad.domain.BannerRepository;
import com.practicket.ad.domain.CampaignStatus;
import com.practicket.ad.exception.AdException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 거래처. 지금까지 배너에 이름 문자열만 있어 연락처도 계약 이력도 둘 데가 없었다.
 * 이름이 곧 식별자라 표기가 갈라지면 다른 거래처가 되던 문제를 UNIQUE 로 막는다.
 */
@Service
@RequiredArgsConstructor
public class AdminAdvertiserService {

    private final AdvertiserRepository advertiserRepository;
    private final AdCampaignRepository adCampaignRepository;
    private final BannerRepository bannerRepository;
    private final AdminAdStatService adminAdStatService;

    @Transactional(readOnly = true)
    public List<AdvertiserRow> getRows() {
        LocalDate today = LocalDate.now();
        Map<Long, AdminAdStatService.Totals> totals = adminAdStatService.totalsByBanner();
        Map<Long, List<AdCampaign>> campaignsByAdvertiser = adCampaignRepository.findAll().stream()
                .collect(Collectors.groupingBy(AdCampaign::getAdvertiserId));
        Map<Long, List<Banner>> bannersByCampaign = bannerRepository.findAll().stream()
                .filter(banner -> banner.getCampaignId() != null)
                .collect(Collectors.groupingBy(Banner::getCampaignId));

        return advertiserRepository.findAllByOrderByNameAsc().stream()
                .map(advertiser -> toRow(advertiser,
                        campaignsByAdvertiser.getOrDefault(advertiser.getId(), List.of()),
                        bannersByCampaign, totals, today))
                .sorted(Comparator.comparingLong(AdvertiserRow::getImpressions).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<AdvertiserDetail> findDetail(Long id) {
        return advertiserRepository.findById(id).map(advertiser -> {
            LocalDate today = LocalDate.now();
            Map<Long, AdminAdStatService.Totals> totals = adminAdStatService.totalsByBanner();
            Map<Long, List<Banner>> bannersByCampaign = bannerRepository.findAll().stream()
                    .filter(banner -> banner.getCampaignId() != null)
                    .collect(Collectors.groupingBy(Banner::getCampaignId));

            List<CampaignHistory> history = adCampaignRepository
                    .findByAdvertiserIdOrderByStartAtDesc(advertiser.getId()).stream()
                    .map(campaign -> {
                        AdminAdStatService.Totals sum = sumOf(
                                bannersByCampaign.getOrDefault(campaign.getId(), List.of()), totals);
                        return new CampaignHistory(campaign, CampaignStatus.of(campaign, today),
                                bannersByCampaign.getOrDefault(campaign.getId(), List.of()).size(),
                                sum.getImpressions(), sum.getClicks(), sum.getCtr());
                    })
                    .toList();

            return new AdvertiserDetail(advertiser, history,
                    history.stream().mapToLong(row -> row.getCampaign().getAmount()).sum(),
                    history.stream().mapToLong(CampaignHistory::getImpressions).sum(),
                    history.stream().mapToLong(CampaignHistory::getClicks).sum());
        });
    }

    @Transactional
    public Long save(AdvertiserForm form) {
        if (form.getName() == null || form.getName().isBlank()) {
            throw new AdException("광고주 이름을 입력해주세요.");
        }
        String name = form.getName().trim();

        Advertiser existing = advertiserRepository.findByName(name).orElse(null);
        if (existing != null && !existing.getId().equals(form.getId())) {
            throw new AdException("같은 이름의 광고주가 이미 있습니다.");
        }

        if (form.getId() == null) {
            return advertiserRepository.save(Advertiser.builder()
                    .name(name)
                    .company(blankToNull(form.getCompany()))
                    .manager(blankToNull(form.getManager()))
                    .phone(blankToNull(form.getPhone()))
                    .email(blankToNull(form.getEmail()))
                    .bizNo(blankToNull(form.getBizNo()))
                    .memo(blankToNull(form.getMemo()))
                    .build()).getId();
        }

        Advertiser advertiser = advertiserRepository.findById(form.getId())
                .orElseThrow(() -> new AdException("존재하지 않는 광고주입니다."));
        advertiser.update(name, blankToNull(form.getCompany()), blankToNull(form.getManager()),
                blankToNull(form.getPhone()), blankToNull(form.getEmail()),
                blankToNull(form.getBizNo()), blankToNull(form.getMemo()));
        return advertiser.getId();
    }

    /** 계약이 남아 있으면 지우지 않는다. 지우면 그 계약들이 주인 없는 행으로 남는다 */
    @Transactional
    public void delete(Long id) {
        if (adCampaignRepository.countByAdvertiserId(id) > 0) {
            throw new AdException("캠페인이 남아 있어 삭제할 수 없습니다. 캠페인을 먼저 정리해주세요.");
        }
        advertiserRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Optional<AdvertiserForm> findForm(Long id) {
        return advertiserRepository.findById(id).map(advertiser -> {
            AdvertiserForm form = new AdvertiserForm();
            form.setId(advertiser.getId());
            form.setName(advertiser.getName());
            form.setCompany(advertiser.getCompany());
            form.setManager(advertiser.getManager());
            form.setPhone(advertiser.getPhone());
            form.setEmail(advertiser.getEmail());
            form.setBizNo(advertiser.getBizNo());
            form.setMemo(advertiser.getMemo());
            return form;
        });
    }

    private AdvertiserRow toRow(Advertiser advertiser, List<AdCampaign> campaigns,
                                Map<Long, List<Banner>> bannersByCampaign,
                                Map<Long, AdminAdStatService.Totals> totals, LocalDate today) {
        AdminAdStatService.Totals sum = campaigns.stream()
                .map(campaign -> sumOf(bannersByCampaign.getOrDefault(campaign.getId(), List.of()), totals))
                .reduce(AdminAdStatService.Totals.empty(), AdminAdStatService.Totals::plus);

        return new AdvertiserRow(
                advertiser,
                campaigns.size(),
                campaigns.stream().filter(campaign -> CampaignStatus.of(campaign, today) == CampaignStatus.LIVE).count(),
                campaigns.stream().mapToLong(AdCampaign::getAmount).sum(),
                sum.getImpressions(), sum.getClicks(), sum.getCtr(),
                campaigns.stream().map(AdCampaign::getStartAt).min(LocalDate::compareTo).orElse(null),
                campaigns.stream().map(AdCampaign::getEndAt).max(LocalDate::compareTo).orElse(null));
    }

    private AdminAdStatService.Totals sumOf(List<Banner> banners, Map<Long, AdminAdStatService.Totals> totals) {
        return banners.stream()
                .map(banner -> totals.getOrDefault(banner.getId(), AdminAdStatService.Totals.empty()))
                .reduce(AdminAdStatService.Totals.empty(), AdminAdStatService.Totals::plus);
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    @Getter
    @AllArgsConstructor
    public static class AdvertiserRow {
        private final Advertiser advertiser;
        private final int campaigns;
        private final long liveCampaigns;
        private final long amount;
        private final long impressions;
        private final long clicks;
        private final double ctr;
        private final LocalDate firstStart;
        private final LocalDate lastEnd;
    }

    @Getter
    @AllArgsConstructor
    public static class CampaignHistory {
        private final AdCampaign campaign;
        private final CampaignStatus status;
        private final int bannerCount;
        private final long impressions;
        private final long clicks;
        private final double ctr;
    }

    @Getter
    @AllArgsConstructor
    public static class AdvertiserDetail {
        private final Advertiser advertiser;
        private final List<CampaignHistory> campaigns;
        private final long amount;
        private final long impressions;
        private final long clicks;

        public double getCtr() {
            return AdminAdStatService.ctrOf(impressions, clicks);
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class AdvertiserForm {
        private Long id;
        private String name;
        private String company;
        private String manager;
        private String phone;
        private String email;
        private String bizNo;
        private String memo;
    }
}
