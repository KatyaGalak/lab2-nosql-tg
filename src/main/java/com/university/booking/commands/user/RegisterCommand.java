package com.university.booking.commands.user;

import com.university.booking.commands.Command;
import com.university.booking.commands.CommandType;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.university.booking.service.dialog.StateService;
import com.university.booking.state.State;
import org.springframework.stereotype.Component;

@Component
public class RegisterCommand extends Command {

    private final StateService stateService;

    public RegisterCommand(StateService stateService) {
        super(CommandType.REGISTER);
        this.stateService = stateService;
    }

    @Override
    public SendMessage handle(Update update, long chatId, String text) {
        stateService.updateContext(chatId, ctx -> ctx.setState(State.REGISTER_LAST_NAME));
        return new SendMessage(chatId, "Введи фамилию:");
    }
}
