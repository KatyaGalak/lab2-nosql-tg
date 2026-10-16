package com.university.booking.commands.user;

import com.university.booking.commands.Command;
import com.university.booking.commands.CommandType;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.university.booking.client.BackendClient;
import com.university.booking.client.PersonIdContext;
import com.university.booking.dto.PersonDto;
import com.university.booking.exception.NotRegisteredException;
import com.university.booking.exception.ResourceNotFoundException;
import com.university.booking.service.dialog.StateService;
import com.university.booking.service.routing.CommandParser;
import org.springframework.stereotype.Component;

@Component
public class LoginCommand extends Command {

    private final BackendClient client;
    private final StateService stateService;
    private final CommandParser parser;

    public LoginCommand(BackendClient client, StateService stateService, CommandParser parser) {
        super(CommandType.LOGIN);
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
        String isu = parser.extractArguments(text);
        if (isu.isBlank()) {
            return new SendMessage(chatId, "Укажи номер ИСУ: /login <ИСУ>");
        }

        PersonDto person;
        try {
            PersonIdContext.set(isu);
            person = client.getPerson(isu);
        } catch (NotRegisteredException | ResourceNotFoundException e) {
            return new SendMessage(chatId, "Пользователь с ИСУ " + isu + " не найден. Зарегистрируйся: /register");
        }

        stateService.updateContext(chatId, ctx -> {
            ctx.setPersonId(person.getIsuId());
            ctx.setPersonRole(person.getRole());
            ctx.setAdminMenuSet(false);
        });
        return new SendMessage(chatId, "Вход выполнен.\n"
                + person.getLastName() + " " + person.getFirstName() + " " + person.getMiddleName()
                + "\nРоль: " + person.getRole()
                + "\nИСУ: " + person.getIsuId()
                + "\n\nСписок команд: /help");
    }
}
