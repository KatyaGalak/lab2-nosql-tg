package com.university.booking.commands.user;

import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.university.booking.dto.CartDto;
import com.university.booking.dto.CartItemDto;
import com.university.booking.ui.Buttons;

public final class CartFormatter {

    private CartFormatter() {}

    public static String formatTtl(long seconds) {
        return "%d:%02d".formatted(seconds / 60, seconds % 60);
    }

    public static InlineKeyboardMarkup cartActions() {
        return new InlineKeyboardMarkup()
                .addRow(Buttons.button("Оформить заявки", "/checkout"),
                        Buttons.button("Корзина", "/cart"))
                .addRow(Buttons.button("Добавить ещё", "/book"),
                        Buttons.button("Очистить", "/clearcart"));
    }

    static String formatCart(CartDto cart) {
        StringBuilder sb = new StringBuilder("Корзина (удалится через " + formatTtl(cart.getTtlSeconds()) + "):\n\n");
        int n = 1;
        for (CartItemDto item : cart.getItems()) {
            sb.append(n++).append(". ").append(item.getEventName()).append("\n")
              .append("   Комната: ").append(item.getRoomId())
              .append(", категория: ").append(item.getCategoryId()).append("\n")
              .append("   ").append(item.getEventDate())
              .append("  ").append(item.getStartTime()).append("–").append(item.getEndTime())
              .append(", участников: ").append(item.getParticipantCount());
            sb.append("\n\n");
        }
        return sb.toString().trim();
    }
}
