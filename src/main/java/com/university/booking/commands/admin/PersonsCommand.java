package com.university.booking.commands.admin;

import com.university.booking.commands.Command;
import com.university.booking.commands.CommandType;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.university.booking.client.BackendClient;
import com.university.booking.dto.PersonDto;
import com.university.booking.enums.PersonRole;
import com.university.booking.exception.AccessDeniedException;
import com.university.booking.service.dialog.StateService;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PersonsCommand extends Command {

    private final BackendClient client;
    private final StateService stateService;

    public PersonsCommand(BackendClient client, StateService stateService) {
        super(CommandType.PERSONS);
        this.client = client;
        this.stateService = stateService;
    }

    @Override
    public SendMessage handle(Update update, long chatId, String text) {
        if (stateService.get(chatId).getPersonRole() != PersonRole.ADMIN) throw new AccessDeniedException();
        List<PersonDto> persons = client.getPersons();
        if (persons.isEmpty()) {
            return new SendMessage(chatId, "Пользователей пока нет.");
        }
        StringBuilder sb = new StringBuilder("Пользователи:\n\n");
        for (PersonDto p : persons) {
            sb.append("ИСУ: ").append(p.getIsuId()).append("\n")
              .append("  ").append(p.getLastName()).append(" ").append(p.getFirstName()).append(" ").append(p.getMiddleName())
              .append(" [").append(p.getRole()).append("]\n\n");
        }
        return new SendMessage(chatId, sb.toString().trim());
    }
}
