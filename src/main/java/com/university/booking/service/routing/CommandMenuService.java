package com.university.booking.service.routing;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.BotCommand;
import com.pengrad.telegrambot.model.botcommandscope.BotCommandScopeDefault;
import com.pengrad.telegrambot.model.botcommandscope.BotCommandsScopeChat;
import com.pengrad.telegrambot.request.SetMyCommands;
import com.pengrad.telegrambot.response.BaseResponse;
import com.university.booking.commands.Command;
import com.university.booking.commands.CommandType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommandMenuService {

    private final TelegramBot telegramBot;
    private final CommandService commandService;

    public void setPublicCommands() {
        BotCommand[] commands = commandService.getAllCommands().stream()
                .filter(c -> !c.getCommandType().isAdminOnly())
                .map(this::toBotCommand)
                .toArray(BotCommand[]::new);

        BaseResponse response = telegramBot.execute(new SetMyCommands(commands).scope(new BotCommandScopeDefault()));
        if (response.isOk()) {
            log.atInfo().addKeyValue("event", "public_commands_set").log("public bot commands menu updated");
        } else {
            log.atError().addKeyValue("error_code", response.errorCode()).log("failed to set public commands");
        }
    }

    public void setAdminCommandsForChat(long chatId) {
        BotCommand[] commands = commandService.getAllCommands().stream()
                .map(this::toBotCommand)
                .toArray(BotCommand[]::new);

        BaseResponse response = telegramBot.execute(
                new SetMyCommands(commands).scope(new BotCommandsScopeChat(chatId)));
        if (response.isOk()) {
            log.atInfo().addKeyValue("event", "admin_commands_set").addKeyValue("chat_id", chatId).log("admin bot commands menu updated");
        } else {
            log.atError().addKeyValue("error_code", response.errorCode()).addKeyValue("chat_id", chatId).log("failed to set admin commands");
        }
    }

    private BotCommand toBotCommand(Command command) {
        CommandType type = command.getCommandType();
        return new BotCommand(type.getCommandName(), type.getDescription());
    }
}
