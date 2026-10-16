package com.university.booking.commands.user;

import com.university.booking.commands.Command;
import com.university.booking.commands.CommandType;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.university.booking.client.BackendClient;
import com.university.booking.dto.BookingDto;
import com.university.booking.exception.NotRegisteredException;
import com.university.booking.service.dialog.StateService;
import com.university.booking.ui.Buttons;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CheckoutCommand extends Command {

    private final BackendClient client;
    private final StateService stateService;

    public CheckoutCommand(BackendClient client, StateService stateService) {
        super(CommandType.CHECKOUT);
        this.client = client;
        this.stateService = stateService;
    }

    @Override
    public SendMessage handle(Update update, long chatId, String text) {
        if (stateService.get(chatId).getPersonId() == null) throw new NotRegisteredException();

        List<BookingDto> bookings = client.checkoutCart();
        StringBuilder sb = new StringBuilder("Заявки отправлены на рассмотрение (" + bookings.size() + "):\n\n");
        for (BookingDto b : bookings) {
            sb.append("ID: ").append(b.getId()).append("\n")
              .append("  ").append(b.getEventName()).append("\n")
              .append("  Статус: ").append(b.getStatus()).append("\n\n");
        }
        return new SendMessage(chatId, sb.toString().trim())
                .replyMarkup(Buttons.row(Buttons.button("Мои заявки", "/bookings")));
    }
}
