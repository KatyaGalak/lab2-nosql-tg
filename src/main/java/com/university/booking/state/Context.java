package com.university.booking.state;

import com.university.booking.enums.PersonRole;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Context {
    private State state = State.DEFAULT;

    private String personId;
    private PersonRole personRole;
    private boolean adminMenuSet;

    private String regLastName;
    private String regFirstName;
    private String regMiddleName;

    private String bookRoomId;
    private String bookCategoryId;
    private String bookEventName;
    private LocalDate bookDate;
    private LocalTime bookStartTime;
    private LocalTime bookEndTime;
    private Integer bookParticipants;
    private String bookPhone;

    public void resetBooking() {
        bookRoomId = null;
        bookCategoryId = null;
        bookEventName = null;
        bookDate = null;
        bookStartTime = null;
        bookEndTime = null;
        bookParticipants = null;
        bookPhone = null;
    }

    public void resetRegister() {
        regLastName = null;
        regFirstName = null;
        regMiddleName = null;
    }
}
