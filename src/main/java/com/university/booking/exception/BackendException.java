package com.university.booking.exception;

public class BackendException extends BotException {
    public BackendException() {
        super("Ошибка связи с сервером. Попробуй позже");
    }
}
