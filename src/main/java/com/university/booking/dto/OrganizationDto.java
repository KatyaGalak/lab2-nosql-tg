package com.university.booking.dto;

import com.university.booking.enums.OrganizationType;
import lombok.Data;

@Data
public class OrganizationDto {
    private String id;
    private String name;
    private OrganizationType type;
    private String contact;
    private boolean active;
    private Stats stats;

    @Data
    public static class Stats {
        private long total;
        private long draft;
        private long underReview;
        private long approved;
        private long rejected;
    }
}
