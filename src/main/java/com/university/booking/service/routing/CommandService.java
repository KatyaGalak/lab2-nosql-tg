package com.university.booking.service.routing;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.university.booking.commands.Command;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CommandService {

    private final Map<String, Command> commands;
    private final CommandParser parser;

    @Getter
    private final List<Command> allCommands;

    public CommandService(List<Command> commandList, CommandParser parser) {
        this.commands = commandList.stream()
                .collect(Collectors.toMap(cmd -> cmd.getCommandType().getCommandName(), Function.identity()));
        this.allCommands = commandList;
        this.parser = parser;
    }

    public SendMessage process(String commandName, String fullText, Update update, long chatId) {
        Command command = commands.get(commandName);
        if (command == null) {
            log.atWarn().addKeyValue("command", commandName).addKeyValue("chat_id", chatId).log("unknown command received");
            return new SendMessage(chatId, "Неизвестная команда. Введи /help");
        }
        if (parser.hasArguments(fullText) && !command.acceptsArguments()) {
            return new SendMessage(chatId, "Неизвестная команда. Введи /help");
        }
        log.atDebug().addKeyValue("command", commandName).addKeyValue("chat_id", chatId).log("executing command");
        return command.handle(update, chatId, fullText);
    }
}
