package com.university.booking.enums;

import lombok.Getter;

@Getter
public enum BookingStatus {
    DRAFT("черновики"),
    UNDER_REVIEW("на рассмотрении"),
    APPROVED("одобренные"),
    REJECTED("отклонённые");

    private final String label;

    BookingStatus(String label) {
        this.label = label;
    }
}
