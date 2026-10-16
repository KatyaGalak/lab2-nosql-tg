package com.university.booking.service.dialog;

import java.util.Optional;
import java.util.regex.Pattern;

public final class PhoneNumbers {

    private static final Pattern SEPARATORS = Pattern.compile("[\\s()\\-]");
    private static final Pattern RUSSIAN = Pattern.compile("^(?:\\+7|7|8)(\\d{10})$");
    private static final Pattern INTERNATIONAL = Pattern.compile("^\\+\\d{10,15}$");

    private PhoneNumbers() {}

    public static Optional<String> normalize(String input) {
        if (input == null) {
            return Optional.empty();
        }
        String compact = SEPARATORS.matcher(input.trim()).replaceAll("");
        var russian = RUSSIAN.matcher(compact);
        if (russian.matches()) {
            return Optional.of("+7" + russian.group(1));
        }

        if (!compact.startsWith("+7") && INTERNATIONAL.matcher(compact).matches()) {
            return Optional.of(compact);
        }
        return Optional.empty();
    }
}
