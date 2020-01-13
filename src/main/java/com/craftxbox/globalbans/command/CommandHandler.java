package com.craftxbox.globalbans.command;

import com.craftxbox.globalbans.GlobalBans;
import discord4j.core.DiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.PermissionSet;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
                                            .flatMap(member -> channel.getEffectivePermissions(member.getId())
                                                    .flatMap(memberPermissions -> checkCommandPermissions(command, member, memberPermissions)
                                                        .flatMap(ignored -> processArgs(commandExploded)
                                                                .flatMap(args -> command.handleCommand(member, message, channel, args)))))))
                    .onErrorResume(t -> t instanceof MissingCommandPermissionException,
                            t -> channel.createMessage(spec -> spec.setContent(
                                    String.format("%s You're missing permissions to do this.",
                                            GlobalBans.getConfigurationValue("bot.core.emote.cross"))
                    ))))
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

    private Mono<CommandInterface> checkCommandPermissions(CommandInterface commandInterface, Member member,
                                                           PermissionSet memberPermissions) {
        if (commandInterface.getClass().isAnnotationPresent(Describe.class)) {
            Describe describeAnnotation = commandInterface.getClass().getAnnotation(Describe.class);
            List<Permission> requiredPermissions = new ArrayList<>(Arrays.asList(describeAnnotation.requiredPermissions()));
            boolean isOwner = isOwner(member.getId());
            boolean commandOwnerOnly = describeAnnotation.botOwner();

            memberPermissions.forEach(requiredPermissions::remove);

            if ((!requiredPermissions.isEmpty() || (commandOwnerOnly && !isOwner)) && !isOwner) {
                return Mono.error(new MissingCommandPermissionException());
            }
        }

        return Mono.just(commandInterface);
    }

    private boolean isOwner(Snowflake userId) {
        return Arrays.asList(GlobalBans.getConfigurationValue("bot.core.owner").split(";")).contains(userId.asString());
    }

    private class MissingCommandPermissionException extends Exception {

    }
}
