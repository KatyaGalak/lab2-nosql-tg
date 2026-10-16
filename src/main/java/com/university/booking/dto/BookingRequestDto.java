package com.university.booking.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BookingRequestDto {
    private String roomId;
    private String categoryId;
    private String eventName;
    private LocalDate eventDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private int participantCount;
    private String contactPhone;
}
