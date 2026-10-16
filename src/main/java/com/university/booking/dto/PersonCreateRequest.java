package com.university.booking.dto;

import com.university.booking.enums.PersonRole;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PersonCreateRequest {
    private String lastName;
    private String firstName;
    private String middleName;
    private PersonRole role;
}
