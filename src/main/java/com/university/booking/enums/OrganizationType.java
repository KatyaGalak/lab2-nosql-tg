package com.university.booking.enums;

import lombok.Getter;

@Getter
public enum OrganizationType {
    DEPARTMENT("подразделение"),
    STUDENT_CLUB("студенческое объединение"),
    PARTNER("партнёр");

    private final String label;

    OrganizationType(String label) {
        this.label = label;
    }
}
