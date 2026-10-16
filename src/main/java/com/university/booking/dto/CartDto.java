package com.university.booking.dto;

import java.util.List;
import lombok.Data;

@Data
public class CartDto {
    private String personId;
    private List<CartItemDto> items;
    private long ttlSeconds;
}
