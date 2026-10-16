package com.university.booking.commands.admin;

import com.university.booking.commands.Command;
import com.university.booking.commands.CommandType;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.university.booking.client.BackendClient;
import com.university.booking.dto.OrganizationStatsReportDto;
import com.university.booking.enums.PersonRole;
import com.university.booking.exception.AccessDeniedException;
import com.university.booking.exception.NotRegisteredException;
import com.university.booking.service.dialog.StateService;
import com.university.booking.service.routing.CommandParser;
import com.university.booking.state.Context;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.springframework.stereotype.Component;

@Component
public class StatsCommand extends Command {

    private static final DateTimeFormatter GENERATED_AT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private final BackendClient client;
    private final StateService stateService;
    private final CommandParser parser;

    public StatsCommand(BackendClient client, StateService stateService, CommandParser parser) {
        super(CommandType.STATS);
        this.client = client;
        this.stateService = stateService;
        this.parser = parser;
    }

    @Override
    public boolean acceptsArguments() {
        return true;
    }

    @Override
    public SendMessage handle(Update update, long chatId, String text) {
        Context ctx = stateService.get(chatId);
        if (ctx.getPersonId() == null) throw new NotRegisteredException();
        if (ctx.getPersonRole() != PersonRole.ADMIN) throw new AccessDeniedException();

        String args = parser.extractArguments(text);
        LocalDate from = null;
        LocalDate to = null;
        if (!args.isBlank()) {
            String[] parts = args.split("\\s+");
            try {
                from = LocalDate.parse(parts[0]);
                to = parts.length > 1 ? LocalDate.parse(parts[1]) : null;
            } catch (DateTimeParseException e) {
                return new SendMessage(chatId, "Даты указываются в формате ГГГГ-ММ-ДД: /stats 2026-09-01 2026-12-31");
            }
        }

        OrganizationStatsReportDto report = client.getOrganizationStats(from, to);
        StringBuilder sb = new StringBuilder("Заявки по организациям");
        if (from != null || to != null) {
            sb.append(" (дата мероприятия ")
              .append(from != null ? "с " + from + " " : "")
              .append(to != null ? "по " + to : "")
              .append(")");
        }
        sb.append(":\n");

        long total = 0;
        for (OrganizationStatsReportDto.Item item : report.getItems()) {
            total += item.getTotal();
            sb.append("\n").append(item.getName()).append(" — ").append(item.getTotal()).append("\n")
              .append("  черновики: ").append(item.getDraft())
              .append(", на рассмотрении: ").append(item.getUnderReview())
              .append(", одобрено: ").append(item.getApproved())
              .append(", отклонено: ").append(item.getRejected()).append("\n");
        }
        sb.append("\nВсего заявок: ").append(total);
        if (report.getGeneratedAt() != null) {
            sb.append("\nОтчёт построен: ").append(report.getGeneratedAt().format(GENERATED_AT))
              .append(" (повторный запрос в течение минуты берётся из кэша)");
        }
        return new SendMessage(chatId, sb.toString());
    }
}