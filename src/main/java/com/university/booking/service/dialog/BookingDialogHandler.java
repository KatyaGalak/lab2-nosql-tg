package com.university.booking.service.dialog;

import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import com.university.booking.client.BackendClient;
import com.university.booking.commands.user.CartFormatter;
import com.university.booking.dto.CartDto;
import com.university.booking.dto.BookingDto;
import com.university.booking.dto.BookingRequestDto;
import com.university.booking.dto.CategoryDto;
import com.university.booking.dto.RoomDto;
import com.university.booking.dto.RoomScheduleDto;
import com.university.booking.exception.FeatureUnavailableException;
import com.university.booking.exception.ResourceNotFoundException;
import com.university.booking.state.Context;
import com.university.booking.state.State;
import com.university.booking.ui.Buttons;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingDialogHandler implements DialogStageHandler {

    private static final List<LocalTime> PAIR_STARTS = times("08:20", "10:00", "11:40", "13:30", "15:20", "17:00", "18:40", "20:20");
    private static final List<LocalTime> PAIR_ENDS = times("09:50", "11:30", "13:10", "15:00", "16:50", "18:30", "20:10", "21:50");
    private static final int DATE_BUTTONS_DAYS = 7;
    private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("dd.MM");
    private static final Locale RU = Locale.forLanguageTag("ru");

    private final BackendClient client;
    private final StateService stateService;

    @Override
    public boolean supports(State state) {
        return switch (state) {
            case BOOK_ROOM, BOOK_CATEGORY, BOOK_EVENT_NAME,
                 BOOK_DATE, BOOK_START_TIME, BOOK_END_TIME,
                 BOOK_PARTICIPANTS, BOOK_PHONE -> true;
            default -> false;
        };
    }

    @Override
    public SendMessage handle(long chatId, String text) {
        Context ctx = stateService.get(chatId);
        return switch (ctx.getState()) {
            case BOOK_ROOM        -> handleRoom(chatId, text.trim());
            case BOOK_CATEGORY    -> handleCategory(chatId, text.trim());
            case BOOK_EVENT_NAME  -> handleEventName(chatId, text.trim());
            case BOOK_DATE        -> handleDate(chatId, text.trim());
            case BOOK_START_TIME  -> handleStartTime(chatId, text.trim());
            case BOOK_END_TIME    -> handleEndTime(chatId, text.trim());
            case BOOK_PARTICIPANTS -> handleParticipants(chatId, text.trim());
            case BOOK_PHONE       -> handlePhone(chatId, text.trim(), ctx);
            default -> new SendMessage(chatId, "Неизвестное состояние бронирования.");
        };
    }

    private SendMessage handleRoom(long chatId, String roomId) {
        try {
            RoomDto room = client.getRoom(roomId);

            if (!room.isVisibleTo(stateService.get(chatId).getPersonRole())) {
                throw new ResourceNotFoundException(roomId);
            }
        } catch (ResourceNotFoundException e) {
            return new SendMessage(chatId,
                    "Комната с ID «" + roomId + "» не найдена. Введи другой ID:");
        }

        List<CategoryDto> cats = client.getCategories();
        if (cats.isEmpty()) {
            stateService.reset(chatId);
            return new SendMessage(chatId, "Нет доступных категорий. Обратись к администратору.");
        }
        stateService.updateContext(chatId, ctx -> {
            ctx.setBookRoomId(roomId);
            ctx.setState(State.BOOK_CATEGORY);
        });
        return categoryMenu(chatId, "Выбери категорию:", cats);
    }

    private SendMessage categoryMenu(long chatId, String prompt, List<CategoryDto> cats) {
        StringBuilder sb = new StringBuilder(prompt).append("\n\n");
        for (CategoryDto c : cats) {
            sb.append("• ").append(c.getName());
            if (c.getDescription() != null && !c.getDescription().isBlank()) {
                sb.append(" — ").append(c.getDescription());
            }
            sb.append("\n");
        }
        List<InlineKeyboardButton> buttons = cats.stream()
                .map(c -> Buttons.button(c.getName(), c.getId()))
                .toList();
        return new SendMessage(chatId, sb.toString().trim()).replyMarkup(Buttons.grid(buttons, 2));
    }

    private SendMessage handleCategory(long chatId, String categoryId) {
        List<CategoryDto> cats = client.getCategories();
        boolean exists = cats.stream().anyMatch(c -> c.getId().equals(categoryId));
        if (!exists) {
            return categoryMenu(chatId, "Категория «" + categoryId + "» не найдена. Выбери из списка:", cats);
        }
        stateService.updateContext(chatId, ctx -> {
            ctx.setBookCategoryId(categoryId);
            ctx.setState(State.BOOK_EVENT_NAME);
        });
        return new SendMessage(chatId, "Введи название мероприятия:");
    }

    private SendMessage handleEventName(long chatId, String name) {
        stateService.updateContext(chatId, ctx -> {
            ctx.setBookEventName(name);
            ctx.setState(State.BOOK_DATE);
        });
        return datePrompt(chatId, "Выбери дату или введи её в формате ГГГГ-ММ-ДД:");
    }

    private SendMessage handleDate(long chatId, String text) {
        LocalDate date;
        try {
            date = LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            return datePrompt(chatId, "Неверный формат. Выбери дату или введи её как ГГГГ-ММ-ДД:");
        }
        String roomId = stateService.get(chatId).getBookRoomId();
        RoomScheduleDto schedule = client.getRoomSchedule(roomId, date);
        stateService.updateContext(chatId, ctx -> {
            ctx.setBookDate(date);
            ctx.setState(State.BOOK_START_TIME);
        });
        return timePrompt(chatId, "Занятость " + roomId + " на " + date + ": " + schedule.formatBusy()
                + "\n\nВыбери время начала или введи его в формате ЧЧ:ММ:", PAIR_STARTS);
    }

    private SendMessage handleStartTime(long chatId, String text) {
        try {
            LocalTime time = LocalTime.parse(text);
            stateService.updateContext(chatId, ctx -> {
                ctx.setBookStartTime(time);
                ctx.setState(State.BOOK_END_TIME);
            });
            List<LocalTime> ends = PAIR_ENDS.stream().filter(t -> t.isAfter(time)).toList();
            return timePrompt(chatId, "Выбери время окончания или введи его в формате ЧЧ:ММ:", ends);
        } catch (DateTimeParseException e) {
            return timePrompt(chatId, "Неверный формат. Выбери время начала или введи его как ЧЧ:ММ:", PAIR_STARTS);
        }
    }

    private SendMessage handleEndTime(long chatId, String text) {
        try {
            LocalTime time = LocalTime.parse(text);
            stateService.updateContext(chatId, ctx -> {
                ctx.setBookEndTime(time);
                ctx.setState(State.BOOK_PARTICIPANTS);
            });
            return new SendMessage(chatId, "Введи количество участников:");
        } catch (DateTimeParseException e) {
            LocalTime start = stateService.get(chatId).getBookStartTime();
            List<LocalTime> ends = PAIR_ENDS.stream().filter(t -> t.isAfter(start)).toList();
            return timePrompt(chatId, "Неверный формат. Выбери время окончания или введи его как ЧЧ:ММ:", ends);
        }
    }

    private SendMessage handleParticipants(long chatId, String text) {
        int count;
        try {
            count = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return new SendMessage(chatId, "Введи целое число больше 0:");
        }
        if (count < 1) {
            return new SendMessage(chatId, "Введи целое число больше 0:");
        }
        stateService.updateContext(chatId, ctx -> {
            ctx.setBookParticipants(count);
            ctx.setState(State.BOOK_PHONE);
        });
        return new SendMessage(chatId, "Введи контактный телефон:");
    }

    private SendMessage handlePhone(long chatId, String phone, Context ctx) {
        BookingRequestDto req = BookingRequestDto.builder()
                .roomId(ctx.getBookRoomId())
                .categoryId(ctx.getBookCategoryId())
                .eventName(ctx.getBookEventName())
                .eventDate(ctx.getBookDate())
                .startTime(ctx.getBookStartTime())
                .endTime(ctx.getBookEndTime())
                .participantCount(ctx.getBookParticipants())
                .contactPhone(phone)
                .build();
        try {
            return addToCart(chatId, req);
        } catch (FeatureUnavailableException e) {

            log.atWarn()
                    .addKeyValue("event", "cart_unavailable_fallback")
                    .addKeyValue("chat_id", chatId)
                    .log("cart unavailable, creating booking directly");
            return createDirectly(chatId, req);
        } finally {
            stateService.reset(chatId);
        }
    }

    private SendMessage addToCart(long chatId, BookingRequestDto req) {
        CartDto cart = client.addToCart(req);
        log.atInfo()
                .addKeyValue("event", "cart_item_added")
                .addKeyValue("cart_size", cart.getItems().size())
                .addKeyValue("chat_id", chatId)
                .log("booking added to cart");
        return new SendMessage(chatId, "Заявка добавлена во временную корзину.\n"
                + "Позиций в корзине: " + cart.getItems().size()
                + "\nКорзина удалится автоматически через " + CartFormatter.formatTtl(cart.getTtlSeconds())
                + ", если её не оформить.")
                .replyMarkup(CartFormatter.cartActions());
    }

    private SendMessage createDirectly(long chatId, BookingRequestDto req) {
        BookingDto booking = client.createBooking(req);
        log.atInfo()
                .addKeyValue("event", "booking_created_directly")
                .addKeyValue("booking_id", booking.getId())
                .addKeyValue("chat_id", chatId)
                .log("booking created without cart");
        return new SendMessage(chatId, "Временная корзина сейчас недоступна, поэтому заявка создана напрямую "
                + "и сохранена как черновик.\n"
                + "ID: " + booking.getId()
                + "\nСтатус: " + booking.getStatus()
                + "\n\nОтправьте её на рассмотрение администратору:")
                .replyMarkup(Buttons.row(
                        Buttons.button("Отправить на рассмотрение", "/submit " + booking.getId()),
                        Buttons.button("Мои заявки", "/bookings")));
    }

    private SendMessage datePrompt(long chatId, String prompt) {
        LocalDate today = LocalDate.now();
        List<InlineKeyboardButton> buttons = new ArrayList<>();
        for (int i = 0; i < DATE_BUTTONS_DAYS; i++) {
            LocalDate date = today.plusDays(i);
            String weekday = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, RU);
            buttons.add(Buttons.button(weekday + " " + date.format(DAY_MONTH), date.toString()));
        }
        return new SendMessage(chatId, prompt).replyMarkup(Buttons.grid(buttons, 4));
    }

    private SendMessage timePrompt(long chatId, String prompt, List<LocalTime> options) {
        if (options.isEmpty()) {
            return new SendMessage(chatId, prompt);
        }
        List<InlineKeyboardButton> buttons = options.stream()
                .map(t -> Buttons.button(t.toString(), t.toString()))
                .toList();
        InlineKeyboardMarkup keyboard = Buttons.grid(buttons, 4);
        return new SendMessage(chatId, prompt).replyMarkup(keyboard);
    }

    private static List<LocalTime> times(String... values) {
        return Arrays.stream(values).map(LocalTime::parse).toList();
    }
}
