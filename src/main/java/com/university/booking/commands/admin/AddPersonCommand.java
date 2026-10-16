package com.university.booking.commands.admin;

import com.university.booking.commands.Command;
import com.university.booking.commands.CommandType;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.university.booking.client.BackendClient;
import com.university.booking.commands.Command;
import com.university.booking.commands.CommandType;
import com.university.booking.dto.PersonCreateRequest;
import com.university.booking.dto.PersonDto;
import com.university.booking.enums.PersonRole;
import com.university.booking.exception.AccessDeniedException;
import com.university.booking.service.dialog.StateService;
import com.university.booking.service.routing.CommandParser;
import org.springframework.stereotype.Component;

@Component
public class AddPersonCommand extends Command {

    private final BackendClient client;
    private final StateService stateService;
    private final CommandParser parser;

    public AddPersonCommand(BackendClient client, StateService stateService, CommandParser parser) {
        super(CommandType.ADDPERSON);
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
        if (stateService.get(chatId).getPersonRole() != PersonRole.ADMIN) throw new AccessDeniedException();

        String args = parser.extractArguments(text);
        String[] parts = args.split("\\s+", 4);
        if (parts.length < 4 || parts[0].isBlank()) {
            return new SendMessage(chatId,
                    "Использование: /addperson <фамилия> <имя> <отчество> <роль>\nРоли: STUDENT, TEACHER, ADMIN");
        }
        PersonRole role;
        try {
            role = PersonRole.valueOf(parts[3].toUpperCase());
        } catch (IllegalArgumentException e) {
            return new SendMessage(chatId, "Неверная роль. Используй STUDENT, TEACHER или ADMIN.");
        }
        PersonDto person = client.createPerson(new PersonCreateRequest(parts[0], parts[1], parts[2], role));
        return new SendMessage(chatId, "Пользователь создан!\n"
                + person.getLastName() + " " + person.getFirstName() + " " + person.getMiddleName()
                + "\nРоль: " + person.getRole()
                + "\nИСУ: " + person.getIsuId());
    }
}
