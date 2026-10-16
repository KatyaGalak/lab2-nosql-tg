package com.university.booking.commands.user;

import com.university.booking.commands.Command;
import com.university.booking.commands.CommandType;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.request.SendMessage;
import com.university.booking.client.BackendClient;
import com.university.booking.dto.BookingDto;
import com.university.booking.enums.BookingStatus;
import com.university.booking.enums.PersonRole;
import com.university.booking.service.dialog.StateService;
import com.university.booking.service.routing.CommandParser;
import com.university.booking.state.Context;
import com.university.booking.ui.Buttons;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class BookingCommand extends Command {

    private final BackendClient client;
    private final StateService stateService;
    private final CommandParser parser;

    public BookingCommand(BackendClient client, StateService stateService, CommandParser parser) {
        super(CommandType.BOOKING);
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
        String id = parser.extractArguments(text);
        if (id.isBlank()) {
            return new SendMessage(chatId, "Укажи ID: /booking <id>")
                    .replyMarkup(Buttons.row(Buttons.button("Мои заявки", "/bookings")));
        }
        BookingDto b = client.getBooking(id);
        SendMessage message = new SendMessage(chatId, "Заявка: " + b.getEventName()
                + "\nID: " + b.getId()
                + "\nДата: " + b.getEventDate()
                + "\nВремя: " + b.getStartTime() + "–" + b.getEndTime()
                + "\nКомната: " + b.getRoomId()
                + "\nКатегория: " + b.getCategoryId()
                + "\nУчастников: " + b.getParticipantCount()
                + "\nТелефон: " + b.getContactPhone()
                + "\nСтатус: " + b.getStatus());

        List<InlineKeyboardButton> actions = actionsFor(b, stateService.get(chatId));
        return actions.isEmpty() ? message : message.replyMarkup(Buttons.grid(actions, 2));
    }

    private List<InlineKeyboardButton> actionsFor(BookingDto b, Context ctx) {
        List<InlineKeyboardButton> actions = new ArrayList<>();
        boolean owner = b.getPersonId() != null && b.getPersonId().equals(ctx.getPersonId());
        boolean admin = ctx.getPersonRole() == PersonRole.ADMIN;

        if (admin && b.getStatus() == BookingStatus.UNDER_REVIEW) {
            actions.add(Buttons.button("Одобрить", "/approve " + b.getId()));
            actions.add(Buttons.button("Отклонить", "/reject " + b.getId()));
        }
        if (owner && b.getStatus() == BookingStatus.DRAFT) {
            actions.add(Buttons.button("Отправить на рассмотрение", "/submit " + b.getId()));
        }
        if (owner) {
            actions.add(Buttons.button("Удалить заявку", "/cancel " + b.getId()));
        }
        actions.add(Buttons.button("Занятость комнаты", "/room " + b.getRoomId() + " " + b.getEventDate()));
        return actions;
    }
}
