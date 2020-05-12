package com.craftxbox.globalbans.command.user;

import com.craftxbox.globalbans.command.CommandInterface;
import com.craftxbox.globalbans.command.Describe;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.TextChannel;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Describe(commandDescription = "Shows the bot's ping to Discord.")
public class PingCommand implements CommandInterface {

    @Override
    public Mono<Message> handleCommand(Member member, Message message, TextChannel channel, String[] args) {
        return channel.createMessage(spec -> spec.setContent(String.format(
                ":door:  **Gateway Ping -** %sms\r\n" +
                        ":speech_balloon:  **Rest Ping -** %sms",
                channel.getClient().getResponseTime(),
                "Calculating...")
        )).doOnSuccess(pongMessage -> pongMessage.edit(spec -> spec.setContent(
            String.format(
                    ":door:  **Gateway Ping -** %sms\r\n" +
                    ":speech_balloon:  **Rest Ping -** %sms",
                    channel.getClient().getResponseTime(),
                    Instant.from(pongMessage.getId().getTimestamp()).getNano() / 1000000)
        )).subscribe());
    }
}
