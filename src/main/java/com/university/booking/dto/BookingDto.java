package com.university.booking.dto;

import com.university.booking.enums.BookingStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.Data;

@Data
public class BookingDto {
    private String id;
    private String personId;
    private String roomId;
    private String categoryId;
    private String eventName;
    private LocalDate eventDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private int participantCount;
    private String contactPhone;
    private BookingStatus status;
    private String processedBy;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}
