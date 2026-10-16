package com.university.booking.commands.user;

import com.university.booking.commands.Command;
import com.university.booking.commands.CommandType;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.university.booking.client.BackendClient;
import com.university.booking.dto.RoomDto;
import com.university.booking.dto.RoomScheduleDto;
import com.university.booking.exception.ResourceNotFoundException;
import com.university.booking.service.dialog.StateService;
import com.university.booking.service.routing.CommandParser;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import org.springframework.stereotype.Component;

@Component
public class RoomCommand extends Command {

    private final BackendClient client;
    private final StateService stateService;
    private final CommandParser parser;

    public RoomCommand(BackendClient client, StateService stateService, CommandParser parser) {
        super(CommandType.ROOM);
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
        String[] args = parser.extractArguments(text).split("\\s+");
        String id = args[0];
        if (id.isBlank()) {
            return new SendMessage(chatId, "Укажи ID комнаты: /room <id> [ГГГГ-ММ-ДД]");
        }

        LocalDate date;
        try {
            date = args.length > 1 ? LocalDate.parse(args[1]) : LocalDate.now();
        } catch (DateTimeParseException e) {
            return new SendMessage(chatId, "Неверный формат даты. Используй ГГГГ-ММ-ДД: /room " + id + " 2026-10-01");
        }

        RoomDto r = client.getRoom(id);

        if (!r.isVisibleTo(stateService.get(chatId).getPersonRole())) {
            throw new ResourceNotFoundException(id);
        }

        RoomScheduleDto schedule = client.getRoomSchedule(id, date);
        return new SendMessage(chatId, "Комната: " + r.getName()
                + "\nID: " + r.getId()
                + "\nТип: " + r.getType()
                + "\nВместимость: " + r.getCapacity()
                + "\nРасположение: " + r.getLocation()
                + (r.isTeacherOnly() ? "\nДоступ: только для преподавателей" : "")
                + "\n\nЗанятость на " + date + ": " + schedule.formatBusy());
    }
}
