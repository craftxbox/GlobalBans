package com.craftxbox.globalbans.command;

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

public class CommandHandler {

    private Snowflake botId;
    private String botPrefix;

    private Map<String, CommandInterface> registeredCommands = new HashMap<>();

    public CommandHandler(DiscordClient client, String botPrefix) {
        this.botId = client.getSelfId().orElse(Snowflake.of(0));

        client.getEventDispatcher().on(ReadyEvent.class)
                .flatMap(readyEvent -> {
                    this.botId = client.getSelfId().orElse(Snowflake.of(0));

                    return Mono.empty();
                })
                .next()
                .subscribe();

        this.botPrefix = botPrefix;
    }

    public void registerCommand(String name, CommandInterface commandInterface) {
        registeredCommands.put(name, commandInterface);
    }

    private Mono<CommandInterface> findCommand(String command) {
        if (registeredCommands.containsKey(command)) {
            return Mono.just(registeredCommands.get(command));
        } else {
            return Mono.empty();
        }
    }

    public boolean isCommand(String command) {
        return registeredCommands.containsKey(command);
    }

    public Mono<?> handle(MessageCreateEvent event) {
        Message message = event.getMessage();
        String messageContent = message.getContent().orElse("");

        if (messageContent.startsWith(botPrefix)) {
            String[] commandExploded = messageContent.split(" ");
            String commandName = commandExploded[0].substring(botPrefix.length());

            return message.getChannel()
                    .ofType(TextChannel.class)
                    .flatMap(channel -> channel.getEffectivePermissions(botId)
                            .filter(permissions -> permissions.contains(Permission.SEND_MESSAGES))
                            .flatMap(permissions -> findCommand(commandName)
                                    .flatMap(command -> message.getAuthorAsMember()
                                            .flatMap(member -> processArgs(commandExploded)
                                                    .flatMap(args -> command.handleCommand(member, message, channel, args))))))
                    .onErrorResume((t) -> {
                        t.printStackTrace();
                        return Mono.empty();
                    });
        }

        return Mono.empty();
    }

    private Mono<String[]> processArgs(String[] args) {
        String[] newArgs = new String[args.length - 1];
        System.arraycopy(args, 1, newArgs, 0, args.length - 1);

        return Mono.just(newArgs);
    }
}
