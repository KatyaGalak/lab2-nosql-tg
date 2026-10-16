package com.university.booking.service.routing;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.Keyboard;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.BaseResponse;
import com.university.booking.exception.BotExceptionHandler;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ListenerService {

    private final TelegramBot telegramBot;
    private final UpdateRouter updateRouter;
    private final CommandMenuService commandMenuService;
    private final BotExceptionHandler exceptionHandler;

    @EventListener(ApplicationReadyEvent.class)
    public void run() {
        commandMenuService.setPublicCommands();
        telegramBot.setUpdatesListener(
                updates -> {
                    updates.forEach(this::processUpdate);
                    return UpdatesListener.CONFIRMED_UPDATES_ALL;
                },
                e -> log.atError().addKeyValue("event", "telegram_api_error").setCause(e).log("error in telegram updates listener"));
        log.atInfo().addKeyValue("bot_status", "started").log("telegram bot listener initialized");
    }

    private void processUpdate(Update update) {
        if (update.callbackQuery() != null) {
            processButton(update);
            return;
        }
        if (update.message() == null || update.message().text() == null) return;

        long chatId = update.message().chat().id();
        log.atDebug().addKeyValue("chat_id", chatId).log("new message received: '{}'", update.message().text());
        route(update, chatId, update.message().text());
    }

    private void processButton(Update update) {
        CallbackQuery callback = update.callbackQuery();
        execute(new AnswerCallbackQuery(callback.id()));
        if (callback.data() == null || callback.maybeInaccessibleMessage() == null) return;

        long chatId = callback.maybeInaccessibleMessage().chat().id();
        log.atDebug().addKeyValue("chat_id", chatId).log("button pressed: '{}'", callback.data());
        route(update, chatId, callback.data());
    }

    private void route(Update update, long chatId, String text) {
        try {
            SendMessage response = updateRouter.route(update, chatId, text);
            sendResponse(response);
        } catch (Exception e) {
            sendResponse(exceptionHandler.handle(chatId, e));
        }
    }

    private void sendResponse(SendMessage response) {
        if (response == null) return;
        Object rawText = response.getParameters().get("text");
        Object rawChatId = response.getParameters().get("chat_id");
        if (rawText instanceof String text && text.length() > 4096 && rawChatId instanceof Long chatId) {
            List<String> chunks = splitText(text);
            for (int i = 0; i < chunks.size(); i++) {
                SendMessage chunk = new SendMessage(chatId, chunks.get(i));

                if (i == chunks.size() - 1 && response.getParameters().get("reply_markup") instanceof Keyboard keyboard) {
                    chunk.replyMarkup(keyboard);
                }
                execute(chunk);
            }
        } else {
            execute(response);
        }
    }

    private <T extends BaseRequest<T, R>, R extends BaseResponse> void execute(BaseRequest<T, R> request) {
        BaseResponse result = telegramBot.execute(request);
        if (!result.isOk()) {
            log.atError()
                    .addKeyValue("error_code", result.errorCode())
                    .addKeyValue("description", result.description())
                    .log("failed to send message");
        }
    }

    private List<String> splitText(String text) {
        List<String> chunks = new ArrayList<>();
        while (text.length() > 4096) {
            int cut = text.lastIndexOf('\n', 4096);
            if (cut <= 0) cut = 4096;
            chunks.add(text.substring(0, cut));
            text = text.substring(cut).stripLeading();
        }
        if (!text.isBlank()) chunks.add(text);
        return chunks;
    }
}
