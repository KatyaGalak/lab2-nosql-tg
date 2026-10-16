package com.university.booking.dto;

import com.university.booking.enums.BookingStatus;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class HistoryEntryDto {
    private BookingStatus status;
    private LocalDateTime changedAt;
    private String author;
    private String reason;
}
