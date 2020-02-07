package com.craftxbox.globalbans.command.botowner;

import com.craftxbox.globalbans.command.CommandInterface;
import com.craftxbox.globalbans.command.Describe;
import com.craftxbox.globalbans.util.DatabaseUtil;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.TextChannel;
import reactor.core.publisher.Mono;

@Describe(botOwner = true)
public class DebugGuildCommand implements CommandInterface {

    @Override
    public Mono<Message> handleCommand(Member member, Message message, TextChannel channel, String[] args) {
        return DatabaseUtil.getGuildConfig(channel.getGuildId(), true)
                .flatMap(guildConfig -> channel.createMessage(spec -> spec.setContent(guildConfig.toString())))
                .onErrorResume(t -> t instanceof DatabaseUtil.NoSuchGuildConfigException,
                        t -> channel.createMessage(spec -> spec.setContent("No such guild config exists.")));
    }
}
