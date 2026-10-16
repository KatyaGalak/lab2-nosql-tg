package com.university.booking.commands;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public abstract class Command {

    private final CommandType commandType;

    public abstract SendMessage handle(Update update, long chatId, String text);

    public boolean acceptsArguments() {
        return false;
    }
}
