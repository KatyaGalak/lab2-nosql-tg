package com.university.booking.commands.user;

import com.university.booking.commands.Command;
import com.university.booking.commands.CommandType;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.university.booking.client.BackendClient;
import com.university.booking.exception.NotRegisteredException;
import com.university.booking.service.dialog.StateService;
import org.springframework.stereotype.Component;

@Component
public class ClearCartCommand extends Command {

    private final BackendClient client;
    private final StateService stateService;

    public ClearCartCommand(BackendClient client, StateService stateService) {
        super(CommandType.CLEARCART);
        this.client = client;
        this.stateService = stateService;
    }

    @Override
    public SendMessage handle(Update update, long chatId, String text) {
        if (stateService.get(chatId).getPersonId() == null) throw new NotRegisteredException();

        client.clearCart();
        return new SendMessage(chatId, "Корзина очищена.");
    }
}
