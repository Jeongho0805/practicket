package com.practicket.drill.domain;

public enum DrillReactForm {
    REFRESH, ADVANCED, N_TICKET;

    public boolean hasLatency() {
        return this == ADVANCED;
    }
}
