package com.craftxbox.globalbans.command.user;

import com.craftxbox.globalbans.GlobalBans;
import com.craftxbox.globalbans.command.CommandInterface;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.TextChannel;
import reactor.core.publisher.Mono;

public class InviteCommand implements CommandInterface {

    @Override
    public Mono<Message> handleCommand(Member member, Message message, TextChannel channel, String[] args) {
        return channel.createMessage(spec -> spec.setContent(
                String.format("%s GlobalBans, Invite me with <https://discordapp.com/oauth2/authorize?scope=bot&client_id=%s&permissions=268774582>",
                        GlobalBans.getConfigurationValue("bot.core.emote.tick"),
                        member.getClient().getSelfId().get().asString())));
    }
}
