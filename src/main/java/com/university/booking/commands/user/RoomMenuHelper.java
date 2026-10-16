package com.university.booking.commands.user;

import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.request.SendMessage;
import com.university.booking.dto.RoomDto;
import com.university.booking.ui.Buttons;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

final class RoomMenuHelper {

    static final Map<String, String> BUILDING_NAMES = new LinkedHashMap<>();

    static {
        BUILDING_NAMES.put("K49", "Кронверкский пр., 49");
        BUILDING_NAMES.put("L9",  "ул. Ломоносова, 9");
        BUILDING_NAMES.put("GR",  "пер. Гривцова, 14-16");
        BUILDING_NAMES.put("G12", "ул. Гастелло, 12");
        BUILDING_NAMES.put("B14", "Биржевая линия, 14");
    }

    private RoomMenuHelper() {}

    static SendMessage typeMenu(long chatId, String command, String prompt) {
        return new SendMessage(chatId, prompt).replyMarkup(Buttons.row(
                Buttons.button("Аудитории", "/" + command + " classroom"),
                Buttons.button("Коворкинги", "/" + command + " coworking")));
    }

    static SendMessage buildingMenu(long chatId, String label, String command, String type, List<RoomDto> rooms) {
        Map<String, Long> counts = rooms.stream()
                .collect(Collectors.groupingBy(r -> r.getId().split("-")[0], Collectors.counting()));

        List<InlineKeyboardButton> buttons = new ArrayList<>();
        for (Map.Entry<String, String> entry : BUILDING_NAMES.entrySet()) {
            String prefix = entry.getKey();
            if (!counts.containsKey(prefix)) continue;
            buttons.add(Buttons.button(entry.getValue() + " (" + counts.get(prefix) + ")",
                    "/" + command + " " + type + " " + prefix.toLowerCase()));
        }
        return new SendMessage(chatId, label + " — выбери корпус:").replyMarkup(Buttons.grid(buttons, 1));
    }

    static SendMessage roomMenu(long chatId, String header, List<RoomDto> rooms, String footer,
                                Function<RoomDto, String> callbackData) {
        StringBuilder sb = new StringBuilder(header).append("\n\n");
        for (RoomDto r : rooms) {
            sb.append(formatRoomLine(r)).append("\n");
        }
        sb.append("\n").append(footer);

        List<InlineKeyboardButton> buttons = rooms.stream()
                .map(r -> Buttons.button(r.getId() + " · " + r.getCapacity() + " мест", callbackData.apply(r)))
                .toList();
        return new SendMessage(chatId, sb.toString()).replyMarkup(Buttons.grid(buttons, 2));
    }

    static String formatRoomLine(RoomDto r) {
        String floor = r.getLocation().replaceAll(".*,\\s*", "");
        return r.getId() + " — " + r.getName()
                + " | вмест. " + r.getCapacity()
                + " | " + floor
                + (r.isTeacherOnly() ? " | только для преподавателей" : "");
    }
}
