package com.university.booking.service.dialog;

import com.pengrad.telegrambot.model.request.KeyboardButton;
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ReplyKeyboardRemove;
import com.pengrad.telegrambot.request.SendMessage;
import com.university.booking.client.BackendClient;
import com.university.booking.dto.PersonCreateRequest;
import com.university.booking.dto.PersonDto;
import com.university.booking.enums.PersonRole;
import com.university.booking.state.Context;
import com.university.booking.state.State;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RegisterDialogHandler implements DialogStageHandler {

    private final BackendClient client;
    private final StateService stateService;

    @Override
    public boolean supports(State state) {
        return state == State.REGISTER_LAST_NAME
                || state == State.REGISTER_FIRST_NAME
                || state == State.REGISTER_MIDDLE_NAME
                || state == State.REGISTER_ROLE;
    }

    @Override
    public SendMessage handle(long chatId, String text) {
        Context ctx = stateService.get(chatId);
        return switch (ctx.getState()) {
            case REGISTER_LAST_NAME   -> handleLastName(chatId, text.trim());
            case REGISTER_FIRST_NAME  -> handleFirstName(chatId, text.trim());
            case REGISTER_MIDDLE_NAME -> handleMiddleName(chatId, text.trim());
            case REGISTER_ROLE        -> handleRole(chatId, text.trim(), ctx);
            default -> new SendMessage(chatId, "Неизвестное состояние регистрации.");
        };
    }

    private SendMessage handleLastName(long chatId, String lastName) {
        stateService.updateContext(chatId, ctx -> {
            ctx.setRegLastName(lastName);
            ctx.setState(State.REGISTER_FIRST_NAME);
        });
        return new SendMessage(chatId, "Введи имя:");
    }

    private SendMessage handleFirstName(long chatId, String firstName) {
        stateService.updateContext(chatId, ctx -> {
            ctx.setRegFirstName(firstName);
            ctx.setState(State.REGISTER_MIDDLE_NAME);
        });
        return new SendMessage(chatId, "Введи отчество:");
    }

    private SendMessage handleMiddleName(long chatId, String middleName) {
        stateService.updateContext(chatId, ctx -> {
            ctx.setRegMiddleName(middleName);
            ctx.setState(State.REGISTER_ROLE);
        });
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup(
                new KeyboardButton[]{new KeyboardButton("Студент"), new KeyboardButton("Преподаватель")}
        ).resizeKeyboard(true).oneTimeKeyboard(true);
        return new SendMessage(chatId, "Выбери роль:").replyMarkup(keyboard);
    }

    private SendMessage handleRole(long chatId, String text, Context ctx) {
        PersonRole role = switch (text) {
            case "Студент"       -> PersonRole.STUDENT;
            case "Преподаватель" -> PersonRole.TEACHER;
            default -> {
                try {
                    PersonRole r = PersonRole.valueOf(text.toUpperCase());
                    yield r == PersonRole.ADMIN ? null : r;
                } catch (IllegalArgumentException e) {
                    yield null;
                }
            }
        };
        if (role == null) {
            ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup(
                    new KeyboardButton[]{new KeyboardButton("Студент"), new KeyboardButton("Преподаватель")}
            ).resizeKeyboard(true).oneTimeKeyboard(true);
            return new SendMessage(chatId, "Выбери роль из предложенных:").replyMarkup(keyboard);
        }
        PersonDto person = client.createPerson(
                new PersonCreateRequest(ctx.getRegLastName(), ctx.getRegFirstName(), ctx.getRegMiddleName(), role));
        stateService.updateContext(chatId, c -> {
            c.setPersonId(person.getIsuId());
            c.setPersonRole(person.getRole());
            c.setState(State.DEFAULT);
            c.resetRegister();
        });
        log.atInfo()
                .addKeyValue("event", "user_registered")
                .addKeyValue("chat_id", chatId)
                .addKeyValue("isu_id", person.getIsuId())
                .addKeyValue("role", person.getRole())
                .log("user registered successfully");
        return new SendMessage(chatId, "Готово! Ты зарегистрирован.\n"
                + person.getLastName() + " " + person.getFirstName() + " " + person.getMiddleName()
                + "\nРоль: " + person.getRole()
                + "\nИСУ: " + person.getIsuId())
                .replyMarkup(new ReplyKeyboardRemove());
    }
}
