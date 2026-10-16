package com.university.booking.service.routing;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.university.booking.client.PersonIdContext;
import com.university.booking.commands.CommandType;
import com.university.booking.enums.PersonRole;
import com.university.booking.service.dialog.DialogService;
import com.university.booking.service.dialog.StateService;
import com.university.booking.state.Context;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateRouter {

    private final CommandService commandService;
    private final DialogService dialogService;
    private final StateService stateService;
    private final CommandParser parser;
    private final CommandMenuService commandMenuService;

    public SendMessage route(Update update, long chatId, String rawText) {
        String text = rawText.trim();

        ensureAdminMenuSet(chatId);
        PersonIdContext.set(stateService.get(chatId).getPersonId());
        try {
            if (text.startsWith("/")) {
                String commandName = parser.extractCommandName(text);

                if (stateService.isInDialog(chatId) && !commandName.equals(CommandType.CANCEL.getCommandName())) {
                    log.atInfo()
                            .addKeyValue("event", "dialog_interrupted")
                            .addKeyValue("chat_id", chatId)
                            .addKeyValue("command", commandName)
                            .log("user interrupted dialog with command");
                    stateService.reset(chatId);
                }
                return commandService.process(commandName, text, update, chatId);
            }

            if (stateService.isInDialog(chatId)) {
                return dialogService.processDialog(chatId, text);
            }

            return new SendMessage(chatId, "Введи команду или используй /help");
        } finally {
            PersonIdContext.clear();
        }
    }

    private void ensureAdminMenuSet(long chatId) {
        Context ctx = stateService.get(chatId);
        if (ctx.getPersonRole() == PersonRole.ADMIN && !ctx.isAdminMenuSet()) {
            commandMenuService.setAdminCommandsForChat(chatId);
            stateService.updateContext(chatId, c -> c.setAdminMenuSet(true));
        }
    }
}
