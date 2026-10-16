package com.university.booking.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import lombok.Data;

@Data
public class RoomScheduleDto {
    private String roomId;
    private LocalDate date;
    private List<BusySlot> busy;

    @Data
    public static class BusySlot {
        private LocalTime startTime;
        private LocalTime endTime;
    }

    public String formatBusy() {
        if (busy == null || busy.isEmpty()) {
            return "свободно весь день (нет бронирований на этот день)";
        }
        StringBuilder sb = new StringBuilder();
        for (BusySlot slot : busy) {
            sb.append("\n  занято ").append(slot.getStartTime()).append("–").append(slot.getEndTime());
        }
        return sb.toString();
    }
}
