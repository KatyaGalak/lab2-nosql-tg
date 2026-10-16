package com.university.booking.dto;

import java.util.List;
import lombok.Data;

@Data
public class BookingPageDto {
    private List<BookingDto> items;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
