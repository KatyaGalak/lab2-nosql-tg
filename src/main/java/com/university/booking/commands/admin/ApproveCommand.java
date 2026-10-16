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
public class ApproveCommand extends Command {

    private final BackendClient client;
    private final StateService stateService;
    private final CommandParser parser;

    public ApproveCommand(BackendClient client, StateService stateService, CommandParser parser) {
        super(CommandType.APPROVE);
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
            return new SendMessage(chatId, "Укажи ID брони: /approve <id> [комментарий]");
        }
        if (args.length < 2) {
            // Как и при отклонении, сначала спрашиваем комментарий: он попадёт в историю и в комментарии заявки.
            stateService.updateContext(chatId, c -> {
                c.setPendingBookingId(id);
                c.setState(State.APPROVE_COMMENT);
            });
            return new SendMessage(chatId, "Добавь комментарий для автора заявки (например, где взять ключ) "
                    + "или нажми «Без комментария»:")
                    .replyMarkup(Buttons.row(
                            Buttons.button("Без комментария", BookingActionDialogHandler.NO_COMMENT),
                            Buttons.button("Отмена", "/cancel")));
        }
        return approve(chatId, id, args[1]);
    }

    public SendMessage approve(long chatId, String id, String comment) {
        BookingDto booking = client.approveBooking(id, comment);
        return new SendMessage(chatId, "Бронирование одобрено!\nСтатус: " + booking.getStatus()
                + (comment != null ? "\nКомментарий: " + comment : ""))
                .replyMarkup(Buttons.row(Buttons.button("Открыть заявку", "/booking " + booking.getId())));
    }
}