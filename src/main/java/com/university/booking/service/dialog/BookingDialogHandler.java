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
import com.university.booking.dto.OrganizationDto;
import com.university.booking.dto.RoomDto;
import com.university.booking.dto.RoomScheduleDto;
import com.university.booking.exception.BotException;
import com.university.booking.exception.FeatureUnavailableException;
import com.university.booking.exception.ResourceNotFoundException;
import com.university.booking.state.Context;
import com.university.booking.state.State;
import com.university.booking.ui.Buttons;
import java.time.Clock;
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
    private static final String PERSONAL = "personal";
    private static final String CHANGE_START = "change_start";
    private static final String CHANGE_DATE = "change_date";

    private final BackendClient client;
    private final StateService stateService;
    private final Clock clock;

    @Override
    public boolean supports(State state) {
        return switch (state) {
            case BOOK_ROOM, BOOK_CATEGORY, BOOK_ORGANIZATION, BOOK_EVENT_NAME,
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
            case BOOK_ORGANIZATION -> handleOrganization(chatId, text.trim());
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
        RoomDto room;
        try {
            room = client.getRoom(roomId);

            if (!room.isVisibleTo(stateService.get(chatId).getPersonRole())) {
                throw new ResourceNotFoundException(roomId);
            }
        } catch (ResourceNotFoundException e) {
            return new SendMessage(chatId,
                    "Комната с ID «" + roomId + "» не найдена. Введи другой ID:")
                    .replyMarkup(Buttons.row(cancelButton()));
        }

        List<CategoryDto> cats = client.getCategories();
        if (cats.isEmpty()) {
            stateService.reset(chatId);
            return new SendMessage(chatId, "Нет доступных категорий. Обратись к администратору.");
        }
        stateService.updateContext(chatId, ctx -> {
            ctx.setBookRoomId(roomId);
            ctx.setBookRoomCapacity(room.getCapacity());
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
        List<OrganizationDto> organizations = activeOrganizations();
        if (organizations.isEmpty()) {
            stateService.updateContext(chatId, ctx -> {
                ctx.setBookCategoryId(categoryId);
                ctx.setState(State.BOOK_EVENT_NAME);
            });
            return new SendMessage(chatId, "Введи название мероприятия:");
        }
        stateService.updateContext(chatId, ctx -> {
            ctx.setBookCategoryId(categoryId);
            ctx.setState(State.BOOK_ORGANIZATION);
        });
        return organizationMenu(chatId, "От чьего имени бронирование?", organizations);
    }

    private SendMessage handleOrganization(long chatId, String text) {
        String organizationId;
        if (PERSONAL.equalsIgnoreCase(text) || "лично".equalsIgnoreCase(text)) {
            organizationId = null;
        } else {
            List<OrganizationDto> organizations = activeOrganizations();
            if (organizations.stream().noneMatch(o -> o.getId().equals(text))) {
                return organizationMenu(chatId, "Организация «" + text + "» не найдена. Выбери из списка:", organizations);
            }
            organizationId = text;
        }
        stateService.updateContext(chatId, ctx -> {
            ctx.setBookOrganizationId(organizationId);
            ctx.setState(State.BOOK_EVENT_NAME);
        });
        return new SendMessage(chatId, "Введи название мероприятия:");
    }

    private List<OrganizationDto> activeOrganizations() {
        try {
            return client.getOrganizations().stream().filter(OrganizationDto::isActive).toList();
        } catch (BotException e) {
            log.atWarn().addKeyValue("event", "organizations_unavailable").log("organization step skipped: {}", e.getMessage());
            return List.of();
        }
    }

    private SendMessage organizationMenu(long chatId, String prompt, List<OrganizationDto> organizations) {
        StringBuilder sb = new StringBuilder(prompt).append("\n\n");
        for (OrganizationDto o : organizations) {
            sb.append("• ").append(o.getName()).append(" — ").append(o.getType().getLabel()).append("\n");
        }
        sb.append("\nЕсли бронируешь для себя, выбери «Лично».");
        List<InlineKeyboardButton> buttons = new ArrayList<>();
        buttons.add(Buttons.button("Лично", PERSONAL));
        organizations.forEach(o -> buttons.add(Buttons.button(o.getName(), o.getId())));
        return new SendMessage(chatId, sb.toString()).replyMarkup(Buttons.grid(buttons, 2));
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
        LocalDate today = LocalDate.now(clock);
        if (date.isBefore(today)) {
            return datePrompt(chatId, "Дата " + date + " уже прошла. Бронировать можно начиная с сегодняшнего дня ("
                    + today + "). Выбери другую дату:");
        }
        String roomId = stateService.get(chatId).getBookRoomId();
        RoomScheduleDto schedule = client.getRoomSchedule(roomId, date);
        stateService.updateContext(chatId, ctx -> {
            ctx.setBookDate(date);
            ctx.setState(State.BOOK_START_TIME);
        });
        String hint = date.equals(today)
                ? "\n\nСегодня можно выбрать время начала позже " + now() + "."
                : "";
        return startTimePrompt(chatId, "Занятость " + roomId + " на " + date + ": " + schedule.formatBusy()
                + hint + "\n\nВыбери время начала или введи его в формате ЧЧ:ММ:", date);
    }

    private SendMessage handleStartTime(long chatId, String text) {
        LocalDate date = stateService.get(chatId).getBookDate();
        if (CHANGE_DATE.equals(text)) {
            stateService.updateContext(chatId, c -> c.setState(State.BOOK_DATE));
            return datePrompt(chatId, "Выбери новую дату или введи её в формате ГГГГ-ММ-ДД:");
        }
        LocalTime time = parseTime(text);
        if (time == null) {
            return startTimePrompt(chatId, "Неверный формат. Выбери время начала или введи его как ЧЧ:ММ:", date);
        }
        if (date.equals(LocalDate.now(clock)) && !time.isAfter(now())) {
            return startTimePrompt(chatId, "Время " + time + " сегодня уже прошло (сейчас " + now()
                    + "). Выбери более позднее время начала или другую дату:", date);
        }
        stateService.updateContext(chatId, ctx -> {
            ctx.setBookStartTime(time);
            ctx.setState(State.BOOK_END_TIME);
        });
        return endTimePrompt(chatId, "Начало в " + time + ". Выбери время окончания или введи его в формате ЧЧ:ММ:", time);
    }

    private SendMessage handleEndTime(long chatId, String text) {
        Context ctx = stateService.get(chatId);
        LocalTime start = ctx.getBookStartTime();

        if (CHANGE_START.equals(text)) {
            stateService.updateContext(chatId, c -> c.setState(State.BOOK_START_TIME));
            return startTimePrompt(chatId, "Выбери новое время начала или введи его в формате ЧЧ:ММ:", ctx.getBookDate());
        }

        LocalTime end = parseTime(text);
        if (end == null) {
            return endTimePrompt(chatId, "Неверный формат. Выбери время окончания или введи его как ЧЧ:ММ:", start);
        }
        if (!end.isAfter(start)) {
            return endTimePrompt(chatId, "Время окончания (" + end + ") должно быть позже времени начала (" + start
                    + "). Выбери другое время окончания:", start);
        }

        RoomScheduleDto.BusySlot conflict = findConflict(ctx.getBookRoomId(), ctx.getBookDate(), start, end);
        if (conflict != null) {
            return endTimePrompt(chatId, "Интервал " + start + "–" + end + " пересекается с занятым временем "
                    + conflict.getStartTime() + "–" + conflict.getEndTime()
                    + ". Выбери другое время окончания или измени время начала:", start);
        }

        stateService.updateContext(chatId, c -> {
            c.setBookEndTime(end);
            c.setState(State.BOOK_PARTICIPANTS);
        });
        return participantsPrompt(chatId, "Введи количество участников"
                + (ctx.getBookRoomCapacity() != null ? " (вместимость помещения — " + ctx.getBookRoomCapacity() + "):" : ":"));
    }

    private SendMessage handleParticipants(long chatId, String text) {
        Integer capacity = stateService.get(chatId).getBookRoomCapacity();
        int count;
        try {
            count = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return participantsPrompt(chatId, "Нужно целое число больше 0, например 12. Введи количество участников:");
        }
        if (count < 1) {
            return participantsPrompt(chatId, "Количество участников должно быть больше 0. Введи ещё раз:");
        }
        if (capacity != null && count > capacity) {
            return new SendMessage(chatId, "Помещение " + stateService.get(chatId).getBookRoomId()
                    + " вмещает не больше " + capacity + " человек, а участников " + count + ".\n\n"
                    + "Введи меньшее число, выбери другое помещение или отмени заявку:")
                    .replyMarkup(Buttons.row(
                            Buttons.button("Другое помещение", "/book"),
                            cancelButton()));
        }
        stateService.updateContext(chatId, ctx -> {
            ctx.setBookParticipants(count);
            ctx.setState(State.BOOK_PHONE);
        });
        return new SendMessage(chatId, "Введи контактный телефон, например +7 999 123-45-67:")
                .replyMarkup(Buttons.row(cancelButton()));
    }

    private SendMessage handlePhone(long chatId, String input, Context ctx) {
        String phone = PhoneNumbers.normalize(input).orElse(null);
        if (phone == null) {
            return new SendMessage(chatId, "Не похоже на номер телефона: «" + input + "».\n"
                    + "Введи номер в формате +7 999 123-45-67 или 89991234567 "
                    + "(для других стран — +<код страны><номер>):")
                    .replyMarkup(Buttons.row(cancelButton()));
        }

        BookingRequestDto req = BookingRequestDto.builder()
                .roomId(ctx.getBookRoomId())
                .categoryId(ctx.getBookCategoryId())
                .eventName(ctx.getBookEventName())
                .eventDate(ctx.getBookDate())
                .startTime(ctx.getBookStartTime())
                .endTime(ctx.getBookEndTime())
                .participantCount(ctx.getBookParticipants())
                .contactPhone(phone)
                .organizationId(ctx.getBookOrganizationId())
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
        LocalDate today = LocalDate.now(clock);
        List<InlineKeyboardButton> buttons = new ArrayList<>();
        for (int i = 0; i < DATE_BUTTONS_DAYS; i++) {
            LocalDate date = today.plusDays(i);
            String weekday = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, RU);
            buttons.add(Buttons.button(weekday + " " + date.format(DAY_MONTH), date.toString()));
        }
        return new SendMessage(chatId, prompt).replyMarkup(Buttons.grid(buttons, 4));
    }

    private SendMessage startTimePrompt(long chatId, String prompt, LocalDate date) {
        boolean today = date.equals(LocalDate.now(clock));
        LocalTime now = now();
        List<InlineKeyboardButton> buttons = PAIR_STARTS.stream()
                .filter(t -> !today || t.isAfter(now))
                .map(t -> Buttons.button(t.toString(), t.toString()))
                .toList();
        String text = today && buttons.isEmpty()
                ? prompt + "\n\nВсе пары на сегодня уже начались — можно ввести время вручную или выбрать другую дату."
                : prompt;
        InlineKeyboardMarkup keyboard = Buttons.grid(buttons, 4)
                .addRow(Buttons.button("Изменить дату", CHANGE_DATE), cancelButton());
        return new SendMessage(chatId, text).replyMarkup(keyboard);
    }

    private LocalTime now() {
        return LocalTime.now(clock).withSecond(0).withNano(0);
    }

    private SendMessage endTimePrompt(long chatId, String prompt, LocalTime start) {
        List<InlineKeyboardButton> buttons = PAIR_ENDS.stream()
                .filter(t -> t.isAfter(start))
                .map(t -> Buttons.button(t.toString(), t.toString()))
                .toList();
        InlineKeyboardMarkup keyboard = Buttons.grid(buttons, 4)
                .addRow(Buttons.button("Изменить время начала", CHANGE_START), cancelButton());
        return new SendMessage(chatId, prompt).replyMarkup(keyboard);
    }

    private SendMessage participantsPrompt(long chatId, String prompt) {
        return new SendMessage(chatId, prompt).replyMarkup(Buttons.row(cancelButton()));
    }

    private static InlineKeyboardButton cancelButton() {
        return Buttons.button("Отменить заявку", "/cancel");
    }

    private static LocalTime parseTime(String text) {
        try {
            return LocalTime.parse(text.length() == 4 && text.charAt(1) == ':' ? "0" + text : text);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private RoomScheduleDto.BusySlot findConflict(String roomId, LocalDate date, LocalTime start, LocalTime end) {
        try {
            RoomScheduleDto schedule = client.getRoomSchedule(roomId, date);
            if (schedule.getBusy() == null) {
                return null;
            }
            return schedule.getBusy().stream()
                    .filter(slot -> slot.getStartTime().isBefore(end) && slot.getEndTime().isAfter(start))
                    .findFirst()
                    .orElse(null);
        } catch (BotException e) {
            log.atWarn().addKeyValue("event", "schedule_unavailable").log("overlap pre-check skipped: {}", e.getMessage());
            return null;
        }
    }

    private static List<LocalTime> times(String... values) {
        return Arrays.stream(values).map(LocalTime::parse).toList();
    }
}
