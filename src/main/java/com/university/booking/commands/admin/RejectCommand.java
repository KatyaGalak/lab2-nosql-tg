package com.university.booking.commands.admin;

import com.university.booking.commands.Command;
import com.university.booking.commands.CommandType;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.university.booking.client.BackendClient;
import com.university.booking.dto.BookingDto;
import com.university.booking.enums.PersonRole;
import com.university.booking.exception.AccessDeniedException;
import com.university.booking.exception.NotRegisteredException;
import com.university.booking.service.dialog.BookingActionDialogHandler;
import com.university.booking.service.dialog.StateService;
import com.university.booking.service.routing.CommandParser;
import com.university.booking.state.Context;
import com.university.booking.state.State;
import com.university.booking.ui.Buttons;
import org.springframework.stereotype.Component;

@Component
public class RejectCommand extends Command {

    private final BackendClient client;
    private final StateService stateService;
    private final CommandParser parser;

    public RejectCommand(BackendClient client, StateService stateService, CommandParser parser) {
        super(CommandType.REJECT);
        this.client = client;
        this.stateService = stateService;
        this.parser = parser;
    }

    @Override
    public boolean acceptsArguments() {
        return true;
    }

    @Override
    public SendMessage handle(Update update, long chatId, String text) {
        Context ctx = stateService.get(chatId);
        if (ctx.getPersonId() == null) throw new NotRegisteredException();
        if (ctx.getPersonRole() != PersonRole.ADMIN) throw new AccessDeniedException();

        String[] args = parser.extractArguments(text).split("\\s+", 2);
        String id = args[0];
        if (id.isBlank()) {
            return new SendMessage(chatId, "Укажи ID брони: /reject <id> [причина]");
        }
        if (args.length < 2) {
            // Причина попадёт в историю заявки и в комментарий для автора, поэтому спрашиваем её отдельным сообщением.
            stateService.updateContext(chatId, c -> {
                c.setPendingBookingId(id);
                c.setState(State.REJECT_REASON);
            });
            return new SendMessage(chatId, "Напиши причину отклонения — её увидит автор заявки:")
                    .replyMarkup(Buttons.row(Buttons.button("Без причины", BookingActionDialogHandler.NO_REASON)));
        }
        return reject(chatId, id, args[1]);
    }

    public SendMessage reject(long chatId, String id, String reason) {
        BookingDto booking = client.rejectBooking(id, reason);
        return new SendMessage(chatId, "Бронирование отклонено.\nСтатус: " + booking.getStatus()
                + (reason != null ? "\nПричина: " + reason : ""))
                .replyMarkup(Buttons.row(Buttons.button("Открыть заявку", "/booking " + booking.getId())));
    }
}