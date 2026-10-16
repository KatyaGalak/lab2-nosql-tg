package com.university.booking.service.dialog;

import com.pengrad.telegrambot.request.SendMessage;
import com.university.booking.state.State;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DialogService {

    private final StateService stateService;
    private final List<DialogStageHandler> handlers;

    public SendMessage processDialog(long chatId, String text) {
        State currentState = stateService.get(chatId).getState();
        log.atDebug().addKeyValue("chat_id", chatId).addKeyValue("state", currentState).log("processing dialog");

        return handlers.stream()
                .filter(h -> h.supports(currentState))
                .findFirst()
                .map(h -> h.handle(chatId, text))
                .orElseGet(() -> {
                    log.atError().addKeyValue("state", currentState).addKeyValue("chat_id", chatId).log("no handler found for state");
                    return new SendMessage(chatId, "Неизвестное состояние. Введи /cancel для отмены.");
                });
    }
}
