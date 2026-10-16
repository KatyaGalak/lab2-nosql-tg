package com.university.booking.commands.user;

import com.university.booking.commands.Command;
import com.university.booking.commands.CommandType;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.university.booking.client.BackendClient;
import com.university.booking.dto.BookingDto;
import com.university.booking.exception.NotRegisteredException;
import com.university.booking.service.dialog.StateService;
import com.university.booking.service.routing.CommandParser;
import com.university.booking.state.State;
import com.university.booking.ui.Buttons;
import org.springframework.stereotype.Component;

@Component
public class CommentCommand extends Command {

    private final BackendClient client;
    private final StateService stateService;
    private final CommandParser parser;

    public CommentCommand(BackendClient client, StateService stateService, CommandParser parser) {
        super(CommandType.COMMENT);
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
        if (stateService.get(chatId).getPersonId() == null) throw new NotRegisteredException();

        String[] args = parser.extractArguments(text).split("\\s+", 2);
        String id = args[0];
        if (id.isBlank()) {
            return new SendMessage(chatId, "Укажи ID заявки: /comment <id> [текст]");
        }
        if (args.length < 2) {
            stateService.updateContext(chatId, c -> {
                c.setPendingBookingId(id);
                c.setState(State.COMMENT_TEXT);
            });
            return new SendMessage(chatId, "Напиши текст комментария (или /cancel для отмены):");
        }
        return comment(chatId, id, args[1]);
    }

    public SendMessage comment(long chatId, String id, String text) {
        BookingDto booking = client.addComment(id, text);
        return new SendMessage(chatId, "Комментарий добавлен. Всего комментариев: " + booking.getComments().size())
                .replyMarkup(Buttons.row(Buttons.button("Открыть заявку", "/booking " + booking.getId())));
    }
}