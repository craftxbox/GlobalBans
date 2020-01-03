package com.craftxbox.globalbans.command;

import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.TextChannel;
import reactor.core.publisher.Mono;

public interface CommandInterface {

    Mono<Message> handleCommand(Member member, Message message, TextChannel channel, String[] args);

}
