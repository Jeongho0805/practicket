package com.practicket.ad.application;

import com.practicket.ad.component.AdNetworkSettings;
import com.practicket.ad.domain.AdNetworkExposure;
import com.practicket.ad.domain.AdNetworkExposureRepository;
import com.practicket.ad.exception.AdException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdNetworkExposureService {

    private final AdNetworkExposureRepository repository;
    private final Environment environment;

    @Transactional(readOnly = true)
    public Map<String, Boolean> currentExposure() {
        Map<String, Boolean> result = new LinkedHashMap<>();
        AdNetworkSettings.LABELS.keySet().forEach(network -> result.put(network, false));

        if (environment.acceptsProfiles(Profiles.of("prod"))) {
            result.replaceAll((network, ignored) -> true);
            return result;
        }
        if (!isEditable()) {
            return result;
        }

        repository.findAll().forEach(setting -> {
            if (result.containsKey(setting.getNetwork())) {
                result.put(setting.getNetwork(), setting.isStageEnabled());
            }
        });
        return result;
    }

    /** 네트워크별 재요청 간격(분). 간격이 없는 네트워크는 빠진다. 노출 스위치와 달리 모든 환경에서 DB 값을 그대로 쓴다 */
    @Transactional(readOnly = true)
    public Map<String, Integer> currentRefillGaps() {
        Map<String, Integer> result = new LinkedHashMap<>();
        repository.findAll().forEach(setting -> {
            if (AdNetworkSettings.LABELS.containsKey(setting.getNetwork()) && setting.getRefillGapMinutes() != null) {
                result.put(setting.getNetwork(), setting.getRefillGapMinutes());
            }
        });
        return result;
    }

    @Transactional(readOnly = true)
    public List<ExposureRow> rows() {
        Map<String, Boolean> exposure = currentExposure();
        Map<String, Integer> gaps = currentRefillGaps();
        boolean editable = isEditable();
        return AdNetworkSettings.LABELS.entrySet().stream()
                .map(entry -> new ExposureRow(entry.getKey(), entry.getValue(),
                        exposure.getOrDefault(entry.getKey(), false), editable,
                        gaps.get(entry.getKey())))
                .toList();
    }

    @Transactional
    public void toggleStage(String network) {
        if (!isEditable()) {
            throw new AdException("로컬·스테이지 환경에서만 네트워크 노출을 변경할 수 있습니다.");
        }
        if (!AdNetworkSettings.LABELS.containsKey(network)) {
            throw new AdException("지원하지 않는 광고 네트워크입니다.");
        }
        AdNetworkExposure setting = repository.findById(network)
                .orElseGet(() -> new AdNetworkExposure(network, false));
        setting.toggle();
        repository.save(setting);
    }

    /** 값이 null 인 네트워크는 간격을 없앤다 */
    @Transactional
    public void updateRefillGaps(Map<String, Integer> gaps) {
        gaps.forEach(this::validate);
        gaps.forEach((network, minutes) -> {
            AdNetworkExposure setting = repository.findById(network)
                    .orElseGet(() -> new AdNetworkExposure(network, false));
            setting.updateRefillGap(minutes);
            repository.save(setting);
        });
    }

    private void validate(String network, Integer minutes) {
        if (!AdNetworkSettings.LABELS.containsKey(network)) {
            throw new AdException("지원하지 않는 광고 네트워크입니다.");
        }
        if (minutes != null && minutes < 1) {
            throw new AdException("재요청 간격은 1분 이상이거나 비워야 합니다.");
        }
    }

    private boolean isEditable() {
        return !environment.acceptsProfiles(Profiles.of("prod"))
                && environment.acceptsProfiles(Profiles.of("local", "stage"));
    }

    @Getter
    @AllArgsConstructor
    public static class ExposureRow {
        private final String network;
        private final String label;
        private final boolean enabled;
        private final boolean editable;
        /** null 이면 간격 없음 */
        private final Integer refillGapMinutes;

        public boolean hasGap() {
            return refillGapMinutes != null;
        }
    }
}
