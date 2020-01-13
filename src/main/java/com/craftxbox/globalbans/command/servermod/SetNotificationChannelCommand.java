package com.craftxbox.globalbans.command.servermod;

import com.craftxbox.globalbans.command.CommandInterface;
import com.craftxbox.globalbans.command.Describe;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.util.Permission;
import reactor.core.publisher.Mono;

@Describe(commandDescription = "Set the notification channel.", requiredPermissions = Permission.MANAGE_CHANNELS)
public class SetNotificationChannelCommand implements CommandInterface {

    @Override
    public Mono<Message> handleCommand(Member member, Message message, TextChannel channel, String[] args) {
        return Mono.empty();
    }
}
