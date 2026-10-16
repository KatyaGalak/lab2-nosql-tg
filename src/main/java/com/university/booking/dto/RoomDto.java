package com.university.booking.dto;

import com.university.booking.enums.PersonRole;
import lombok.Data;

@Data
public class RoomDto {
    private String id;
    private String name;
    private String type;
    private int capacity;
    private String location;
    private boolean teacherOnly;

    public boolean isVisibleTo(PersonRole role) {
        return !(teacherOnly && role == PersonRole.STUDENT);
    }
}
