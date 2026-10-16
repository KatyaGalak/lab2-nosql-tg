package com.university.booking.commands.user;

import com.university.booking.commands.Command;
import com.university.booking.commands.CommandType;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.university.booking.client.BackendClient;
import com.university.booking.service.dialog.StateService;
import com.university.booking.service.routing.CommandParser;
import org.springframework.stereotype.Component;

@Component
public class CancelCommand extends Command {

    private final BackendClient client;
    private final StateService stateService;
    private final CommandParser parser;

    public CancelCommand(BackendClient client, StateService stateService, CommandParser parser) {
        super(CommandType.CANCEL);
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
        if (stateService.isInDialog(chatId)) {
            stateService.reset(chatId);
            return new SendMessage(chatId, "Действие отменено.");
        }
        String id = parser.extractArguments(text);
        if (id.isBlank()) {
            return new SendMessage(chatId, "Укажи ID брони: /cancel <id>");
        }
        client.deleteBooking(id);
        return new SendMessage(chatId, "Бронирование удалено.");
    }
}
