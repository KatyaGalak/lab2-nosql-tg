package com.university.booking.commands.user;

import com.university.booking.commands.Command;
import com.university.booking.commands.CommandType;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.university.booking.client.BackendClient;
import com.university.booking.dto.BookingDto;
import com.university.booking.exception.NotRegisteredException;
import com.university.booking.service.dialog.StateService;
import com.university.booking.service.routing.CommandParser;
import org.springframework.stereotype.Component;

@Component
public class SubmitCommand extends Command {

    private final BackendClient client;
    private final StateService stateService;
    private final CommandParser parser;

    public SubmitCommand(BackendClient client, StateService stateService, CommandParser parser) {
        super(CommandType.SUBMIT);
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
        if (stateService.get(chatId).getPersonId() == null) throw new NotRegisteredException();

        String id = parser.extractArguments(text);
        if (id.isBlank()) {
            return new SendMessage(chatId, "Укажи ID брони: /submit <id>");
        }
        BookingDto booking = client.submitBooking(id);
        return new SendMessage(chatId, "Бронь отправлена на рассмотрение.\nСтатус: " + booking.getStatus());
    }
}
