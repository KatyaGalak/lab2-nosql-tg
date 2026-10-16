package com.university.booking.service.routing;

import org.springframework.stereotype.Component;

@Component
public class CommandParser {

    public String extractCommandName(String text) {
        String word = text.split("\\s+")[0];
        int at = word.indexOf('@');
        return (at == -1 ? word : word.substring(0, at)).toLowerCase();
    }

    public boolean hasArguments(String text) {
        return text.trim().contains(" ");
    }

    public String extractArguments(String text) {
        int space = text.indexOf(' ');
        return space == -1 ? "" : text.substring(space + 1).trim();
    }
}
