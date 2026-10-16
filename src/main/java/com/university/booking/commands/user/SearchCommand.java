package com.university.booking.commands.user;

import com.university.booking.commands.Command;
import com.university.booking.commands.CommandType;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import com.university.booking.client.BackendClient;
import com.university.booking.dto.BookingDto;
import com.university.booking.dto.BookingPageDto;
import com.university.booking.enums.BookingStatus;
import com.university.booking.enums.PersonRole;
import com.university.booking.exception.AccessDeniedException;
import com.university.booking.exception.NotRegisteredException;
import com.university.booking.service.dialog.StateService;
import com.university.booking.service.routing.CommandParser;
import com.university.booking.state.Context;
import com.university.booking.ui.Buttons;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SearchCommand extends Command {

    static final int PAGE_SIZE = 5;

    private static final DateTimeFormatter CREATED = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final Map<String, BookingStatus> STATUS_ALIASES = Map.of(
            "draft", BookingStatus.DRAFT,
            "review", BookingStatus.UNDER_REVIEW,
            "approved", BookingStatus.APPROVED,
            "rejected", BookingStatus.REJECTED);

    private final BackendClient client;
    private final StateService stateService;
    private final CommandParser parser;

    public SearchCommand(BackendClient client, StateService stateService, CommandParser parser) {
        super(CommandType.SEARCH);
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
        boolean admin = ctx.getPersonRole() == PersonRole.ADMIN;

        String author = null;
        BookingStatus status = null;
        int page = 1;
        String args = parser.extractArguments(text);
        for (String token : args.isBlank() ? new String[0] : args.split("\\s+")) {
            if (token.matches("\\d{6}")) {
                author = token;
            } else if (parseStatus(token) != null) {
                status = parseStatus(token);
            } else if (token.matches("\\d{1,4}") && Integer.parseInt(token) > 0) {
                page = Integer.parseInt(token);
            } else {
                return new SendMessage(chatId, "Не понял «" + token + "». " + usage(admin));
            }
        }

        if (!admin) {
            if (author != null && !author.equals(ctx.getPersonId())) {
                throw new AccessDeniedException("Искать можно только среди своих заявок");
            }
            author = ctx.getPersonId();
        }

        if (status == null) {
            return new SendMessage(chatId, "Выбери статус заявок"
                    + (admin ? (author != null ? " автора ИСУ " + author : " (все авторы)") : "")
                    + ":\n\n" + usage(admin))
                    .replyMarkup(statusMenu(admin ? author : null));
        }

        BookingPageDto result = client.searchBookings(author, status, page - 1, PAGE_SIZE);
        return render(chatId, admin ? author : null, author, status, page, result, ctx.getPersonId());
    }

    private SendMessage render(long chatId, String authorArg, String author, BookingStatus status, int page,
                               BookingPageDto result, String currentPersonId) {
        List<BookingDto> items = result.getItems() != null ? result.getItems() : List.of();
        String authorLabel = author == null ? "все"
                : author.equals(currentPersonId) ? "вы"
                : items.isEmpty() ? "ИСУ " + author
                : BookingFormatter.person(items.get(0), author);

        StringBuilder sb = new StringBuilder("Поиск заявок\n")
                .append("Автор: ").append(authorLabel).append("\n")
                .append("Статус: ").append(status.getLabel()).append("\n")
                .append("Найдено: ").append(result.getTotalElements());

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

        if (items.isEmpty()) {
            sb.append(result.getTotalElements() > 0 ? "\n\nНа странице " + page + " заявок нет." : "\n\nНичего не найдено.");
        } else {
            sb.append(" · страница ").append(page).append(" из ").append(Math.max(result.getTotalPages(), 1)).append("\n");
            int n = (page - 1) * PAGE_SIZE + 1;
            for (BookingDto b : items) {
                sb.append("\n").append(n).append(". ").append(b.getEventName()).append("\n")
                  .append("   ").append(b.getRoomId()).append(", ").append(b.getEventDate())
                  .append(" ").append(b.getStartTime()).append("–").append(b.getEndTime()).append("\n")
                  .append("   автор: ").append(BookingFormatter.person(b, b.getPersonId(), currentPersonId));
                if (b.getOrganizationId() != null) {
                    sb.append(", от организации ").append(b.getOrganizationId());
                }
                if (b.getCreatedAt() != null) {
                    sb.append(", создана ").append(b.getCreatedAt().format(CREATED));
                }
                sb.append("\n");
                keyboard.addRow(Buttons.button(n + ". " + b.getEventName(), "/booking " + b.getId()));
                n++;
            }
        }

        List<InlineKeyboardButton> navigation = new ArrayList<>();
        if (page > 1) {
            navigation.add(Buttons.button("‹ Назад", searchCommand(authorArg, status, page - 1)));
        }
        if (page < result.getTotalPages()) {
            navigation.add(Buttons.button("Далее ›", searchCommand(authorArg, status, page + 1)));
        }
        if (!navigation.isEmpty()) {
            keyboard.addRow(navigation.toArray(InlineKeyboardButton[]::new));
        }
        keyboard.addRow(Buttons.button("Другой статус", searchCommand(authorArg, null, 1)));
        return new SendMessage(chatId, sb.toString().trim()).replyMarkup(keyboard);
    }

    private static InlineKeyboardMarkup statusMenu(String authorArg) {
        List<InlineKeyboardButton> buttons = Arrays.stream(BookingStatus.values())
                .map(s -> Buttons.button(capitalize(s.getLabel()), searchCommand(authorArg, s, 1)))
                .toList();
        return Buttons.grid(buttons, 2);
    }

    private static String searchCommand(String author, BookingStatus status, int page) {
        StringBuilder command = new StringBuilder("/search");
        if (author != null) command.append(" ").append(author);
        if (status != null) command.append(" ").append(status.name());
        if (page > 1) command.append(" ").append(page);
        return command.toString();
    }

    static BookingStatus parseStatus(String token) {
        BookingStatus alias = STATUS_ALIASES.get(token.toLowerCase());
        if (alias != null) {
            return alias;
        }
        try {
            return BookingStatus.valueOf(token.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String usage(boolean admin) {
        return admin
                ? "Формат: /search [ИСУ] [статус] — например /search 691905 UNDER_REVIEW. Без ИСУ — среди всех заявок."
                : "Формат: /search [статус] — например /search UNDER_REVIEW.";
    }

    private static String capitalize(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}