package com.university.booking.dto;

import java.util.List;
import lombok.Data;

@Data
public class ErrorResponseDto {
    private int status;
    private String message;
    private List<String> errors;
}
