package com.practicket.ad.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "ad_network_exposure")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdNetworkExposure {

    @Id
    private String network;

    @Column(nullable = false)
    private boolean stageEnabled;

    public void toggle() {
        stageEnabled = !stageEnabled;
    }
}
