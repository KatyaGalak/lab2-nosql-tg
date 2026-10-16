package com.university.booking.ui;

import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import java.util.List;

public final class Buttons {

    private Buttons() {}

    public static InlineKeyboardButton button(String text, String data) {
        return new InlineKeyboardButton(text).callbackData(data);
    }

    public static InlineKeyboardMarkup row(InlineKeyboardButton... buttons) {
        return new InlineKeyboardMarkup(buttons);
    }

    public static InlineKeyboardMarkup grid(List<InlineKeyboardButton> buttons, int perRow) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        for (int i = 0; i < buttons.size(); i += perRow) {
            markup.addRow(buttons.subList(i, Math.min(i + perRow, buttons.size())).toArray(InlineKeyboardButton[]::new));
        }
        return markup;
    }
}
