package com.university.booking.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.Data;

@Data
public class CartItemDto {
    private String id;
    private String roomId;
    private String categoryId;
    private String eventName;
    private LocalDate eventDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private int participantCount;
    private String contactPhone;
    private LocalDateTime addedAt;
}
