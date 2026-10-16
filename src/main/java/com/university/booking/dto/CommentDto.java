package com.university.booking.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class CommentDto {
    private String id;
    private String authorId;
    private String text;
    private LocalDateTime createdAt;
}
