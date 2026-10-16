package com.university.booking.service.dialog;

import com.pengrad.telegrambot.request.SendMessage;
import com.university.booking.state.State;

public interface DialogStageHandler {

    boolean supports(State state);

    SendMessage handle(long chatId, String text);
}
