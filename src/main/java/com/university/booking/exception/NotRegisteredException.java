package com.university.booking.exception;

public class NotRegisteredException extends BotException {
    public NotRegisteredException() {
        super("Сначала зарегистрируйся: /register");
    }
}
