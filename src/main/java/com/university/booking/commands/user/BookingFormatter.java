package com.university.booking.commands.user;

import com.university.booking.dto.BookingDto;
import com.university.booking.dto.CommentDto;
import com.university.booking.dto.HistoryEntryDto;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class BookingFormatter {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private BookingFormatter() {}

    public static String details(BookingDto b, String organizationName, String currentPersonId) {
        StringBuilder sb = new StringBuilder("Заявка: ").append(b.getEventName())
                .append("\nID: ").append(b.getId())
                .append("\nАвтор: ").append(person(b, b.getPersonId()))
                .append("\nОт имени: ").append(organizationName != null ? organizationName : "лично")
                .append("\nДата: ").append(b.getEventDate())
                .append("\nВремя: ").append(b.getStartTime()).append("–").append(b.getEndTime())
                .append("\nКомната: ").append(b.getRoomId())
                .append("\nКатегория: ").append(b.getCategoryId())
                .append("\nУчастников: ").append(b.getParticipantCount())
                .append("\nТелефон: ").append(b.getContactPhone())
                .append("\nСтатус: ").append(b.getStatus());

        if (b.getHistory() != null && !b.getHistory().isEmpty()) {
            sb.append("\n\nИстория:");
            for (HistoryEntryDto h : b.getHistory()) {
                sb.append("\n• ").append(format(h.getChangedAt())).append(" — ").append(h.getStatus())
                  .append(", ").append(person(b, h.getAuthor(), currentPersonId));
                if (h.getReason() != null) {
                    sb.append(": ").append(h.getReason());
                }
            }
        }

        sb.append("\n\nКомментарии:");
        if (b.getComments() == null || b.getComments().isEmpty()) {
            sb.append(" пока нет");
        } else {
            for (CommentDto c : b.getComments()) {
                sb.append("\n• ").append(format(c.getCreatedAt())).append(", ")
                  .append(person(b, c.getAuthorId(), currentPersonId)).append(":\n  ").append(c.getText());
            }
        }
        return sb.toString();
    }

    public static String person(BookingDto b, String personId) {
        if (personId == null) return "неизвестно";
        String name = b.getPersonNames() != null ? b.getPersonNames().get(personId) : null;
        return name != null ? name + " (ИСУ " + personId + ")" : "ИСУ " + personId;
    }

    public static String person(BookingDto b, String personId, String currentPersonId) {
        return personId != null && personId.equals(currentPersonId) ? "вы" : person(b, personId);
    }

    private static String format(LocalDateTime time) {
        return time != null ? time.format(DATE_TIME) : "—";
    }
}