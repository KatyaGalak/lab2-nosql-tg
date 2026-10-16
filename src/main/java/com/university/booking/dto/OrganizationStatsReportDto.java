package com.university.booking.dto;

import com.university.booking.enums.OrganizationType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class OrganizationStatsReportDto {
    private LocalDate from;
    private LocalDate to;
    private LocalDateTime generatedAt;
    private List<Item> items;

    @Data
    public static class Item {

        private String organizationId;
        private String name;
        private OrganizationType type;
        private long total;
        private long draft;
        private long underReview;
        private long approved;
        private long rejected;
    }
}
