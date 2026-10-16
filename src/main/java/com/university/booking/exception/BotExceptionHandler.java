package com.university.booking.exception;

import com.pengrad.telegrambot.request.SendMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BotExceptionHandler {

    public SendMessage handle(long chatId, Exception ex) {
        if (ex instanceof NotRegisteredException e) return handleNotRegistered(chatId, e);
        if (ex instanceof AccessDeniedException e) return handleAccessDenied(chatId, e);
        if (ex instanceof ResourceNotFoundException e) return handleNotFound(chatId, e);
        if (ex instanceof BackendException e) return handleBackend(chatId, e);
        if (ex instanceof BotException e) return handleRejected(chatId, e);
        return handleUnexpected(chatId, ex);
    }

    private SendMessage handleRejected(long chatId, BotException ex) {
        log.atInfo().addKeyValue("chat_id", chatId).log("request rejected: {}", ex.getMessage());
        return new SendMessage(chatId, ex.getMessage());
    }

    private SendMessage handleNotRegistered(long chatId, NotRegisteredException ex) {
        log.atInfo().addKeyValue("chat_id", chatId).log("unregistered user attempted action");
        return new SendMessage(chatId, ex.getMessage());
    }

    private SendMessage handleAccessDenied(long chatId, AccessDeniedException ex) {
        log.atWarn().addKeyValue("chat_id", chatId).log("access denied");
        return new SendMessage(chatId, ex.getMessage());
    }

    private SendMessage handleNotFound(long chatId, ResourceNotFoundException ex) {
        log.atWarn().addKeyValue("chat_id", chatId).log("resource not found: {}", ex.getMessage());
        return new SendMessage(chatId, ex.getMessage());
    }

    private SendMessage handleBackend(long chatId, BackendException ex) {
        log.atError().addKeyValue("chat_id", chatId).log("backend unavailable");
        return new SendMessage(chatId, ex.getMessage());
    }

    private SendMessage handleUnexpected(long chatId, Exception ex) {
        log.atError()
                .addKeyValue("chat_id", chatId)
                .addKeyValue("exception", ex.getClass().getName())
                .setCause(ex)
                .log("unexpected error processing update");
        return new SendMessage(chatId, "Произошла непредвиденная ошибка. Попробуй позже");
    }
}
