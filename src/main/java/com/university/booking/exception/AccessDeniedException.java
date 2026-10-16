package com.university.booking.exception;

public class AccessDeniedException extends BotException {
    public AccessDeniedException() {
        super("Недостаточно прав для выполнения этой команды");
    }

    public AccessDeniedException(String message) {
        super(message);
    }
}
