package com.university.booking.client;

public final class PersonIdContext {

    private static final ThreadLocal<String> HOLDER = new ThreadLocal<>();

    private PersonIdContext() {}

    public static void set(String personId) {
        HOLDER.set(personId);
    }

    public static String get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
