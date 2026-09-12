package com.practicket.ad.application;

import com.practicket.ad.domain.AdNetworkExposure;
import com.practicket.ad.domain.AdNetworkExposureRepository;
import com.practicket.ad.exception.AdException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AdNetworkExposureServiceTest {

    @ParameterizedTest
    @ValueSource(strings = {"stage", "local"})
    void editableEnvironmentsUseNetworkFlags(String profile) {
        AdNetworkExposureRepository repository = mock(AdNetworkExposureRepository.class);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profile);
        given(repository.findAll()).willReturn(List.of(
                new AdNetworkExposure("COUPANG", true),
                new AdNetworkExposure("ADSENSE", false),
                new AdNetworkExposure("ADFIT", true)));

        AdNetworkExposureService service = new AdNetworkExposureService(repository, environment);

        assertThat(service.currentExposure())
                .containsEntry("COUPANG", true)
                .containsEntry("ADSENSE", false)
                .containsEntry("ADFIT", true);
        assertThat(service.rows()).allMatch(AdNetworkExposureService.ExposureRow::isEditable);
    }

    @Test
    void productionIsAlwaysEnabledAndUnknownEnvironmentIsDisabled() {
        AdNetworkExposureRepository repository = mock(AdNetworkExposureRepository.class);
        MockEnvironment production = new MockEnvironment();
        production.setActiveProfiles("prod");
        MockEnvironment local = new MockEnvironment();
        local.setActiveProfiles("test");

        assertThat(new AdNetworkExposureService(repository, production).currentExposure().values())
                .containsOnly(true);
        assertThat(new AdNetworkExposureService(repository, local).currentExposure().values())
                .containsOnly(false);
    }

    @ParameterizedTest
    @ValueSource(strings = {"stage", "local"})
    void editableEnvironmentsCanToggleOneNetwork(String profile) {
        AdNetworkExposureRepository repository = mock(AdNetworkExposureRepository.class);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profile);
        AdNetworkExposure setting = new AdNetworkExposure("ADSENSE", false);
        given(repository.findById("ADSENSE")).willReturn(Optional.of(setting));

        new AdNetworkExposureService(repository, environment).toggleStage("ADSENSE");

        assertThat(setting.isStageEnabled()).isTrue();
        verify(repository).save(setting);
    }

    @Test
    void productionWinsWhenLocalAndStageProfilesAreAlsoActive() {
        AdNetworkExposureRepository repository = mock(AdNetworkExposureRepository.class);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod", "local", "stage");

        assertThat(new AdNetworkExposureService(repository, environment).rows())
                .noneMatch(AdNetworkExposureService.ExposureRow::isEditable);

        assertThatThrownBy(() -> new AdNetworkExposureService(repository, environment).toggleStage("COUPANG"))
                .isInstanceOf(AdException.class);
    }
}
