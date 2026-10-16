package com.university.booking.commands.user;

import com.university.booking.commands.Command;
import com.university.booking.commands.CommandType;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.request.SendMessage;
import com.university.booking.client.BackendClient;
import com.university.booking.dto.BookingDto;
import com.university.booking.ui.Buttons;
import java.util.List;
import java.util.stream.IntStream;
import org.springframework.stereotype.Component;

@Component
public class BookingsCommand extends Command {

    private static final int MAX_BUTTONS = 50;

    private final BackendClient client;

    public BookingsCommand(BackendClient client) {
        super(CommandType.BOOKINGS);
        this.client = client;
    }

    @Override
    public SendMessage handle(Update update, long chatId, String text) {
        List<BookingDto> bookings = client.getBookings();
        if (bookings.isEmpty()) {
            return new SendMessage(chatId, "Заявок пока нет.")
                    .replyMarkup(Buttons.row(Buttons.button("Забронировать", "/book")));
        }
        StringBuilder sb = new StringBuilder("Заявки:\n\n");
        int n = 1;
        for (BookingDto b : bookings) {
            sb.append(n++).append(". ").append(b.getEventName()).append("\n")
              .append("   ").append(b.getRoomId()).append(", ").append(b.getEventDate())
              .append("  ").append(b.getStartTime()).append("–").append(b.getEndTime()).append("\n")
              .append("   Статус: ").append(b.getStatus()).append("\n\n");
        }
        sb.append("Нажми на заявку, чтобы открыть её:");

        List<InlineKeyboardButton> buttons = IntStream.range(0, Math.min(bookings.size(), MAX_BUTTONS))
                .mapToObj(i -> Buttons.button((i + 1) + ". " + bookings.get(i).getEventName() + " · " + bookings.get(i).getStatus(),
                        "/booking " + bookings.get(i).getId()))
                .toList();
        return new SendMessage(chatId, sb.toString()).replyMarkup(Buttons.grid(buttons, 1));
    }
}
