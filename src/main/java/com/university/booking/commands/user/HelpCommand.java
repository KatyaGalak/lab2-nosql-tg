package com.university.booking.commands.user;

import com.university.booking.commands.Command;
import com.university.booking.commands.CommandType;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.university.booking.enums.PersonRole;
import com.university.booking.service.dialog.StateService;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class HelpCommand extends Command {

    private final StateService stateService;

    public HelpCommand(StateService stateService) {
        super(CommandType.HELP);
        this.stateService = stateService;
    }

    @Override
    public SendMessage handle(Update update, long chatId, String text) {
        boolean isAdmin = stateService.get(chatId).getPersonRole() == PersonRole.ADMIN;
        String body = Arrays.stream(CommandType.values())
                .filter(cmd -> !cmd.isAdminOnly() || isAdmin)
                .map(cmd -> cmd.getCommandName() + " — " + cmd.getDescription())
                .collect(Collectors.joining("\n"));
        return new SendMessage(chatId, "Доступные команды:\n\n" + body);
    }
}
