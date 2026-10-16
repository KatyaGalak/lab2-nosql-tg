package com.university.booking.commands.admin;

import com.university.booking.commands.Command;
import com.university.booking.commands.CommandType;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.university.booking.client.BackendClient;
import com.university.booking.dto.BookingDto;
import com.university.booking.enums.PersonRole;
import com.university.booking.exception.AccessDeniedException;
import com.university.booking.exception.NotRegisteredException;
import com.university.booking.service.dialog.StateService;
import com.university.booking.service.routing.CommandParser;
import com.university.booking.state.Context;
import org.springframework.stereotype.Component;

@Component
public class ApproveCommand extends Command {

    private final BackendClient client;
    private final StateService stateService;
    private final CommandParser parser;

    public ApproveCommand(BackendClient client, StateService stateService, CommandParser parser) {
        super(CommandType.APPROVE);
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

        String id = parser.extractArguments(text);
        if (id.isBlank()) {
            return new SendMessage(chatId, "Укажи ID брони: /approve <id>");
        }
        BookingDto booking = client.approveBooking(id);
        return new SendMessage(chatId, "Бронирование одобрено!\nСтатус: " + booking.getStatus());
    }
}
