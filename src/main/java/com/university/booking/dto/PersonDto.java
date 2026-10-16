package com.university.booking.dto;

import com.university.booking.enums.PersonRole;
import lombok.Data;

@Data
public class PersonDto {
    private String isuId;
    private String lastName;
    private String firstName;
    private String middleName;
    private PersonRole role;
}
