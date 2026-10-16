package com.university.booking.exception;

public class ResourceNotFoundException extends BotException {
    public ResourceNotFoundException(String id) {
        super("Ресурс не найден: " + id);
    }
}
