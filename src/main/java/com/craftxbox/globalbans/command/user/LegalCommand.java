package com.craftxbox.globalbans.command.user;

import com.craftxbox.globalbans.command.CommandInterface;
import com.craftxbox.globalbans.command.Describe;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.TextChannel;
import reactor.core.publisher.Mono;

@Describe(commandDescription = "Shows legal information about the bot.")
public class LegalCommand implements CommandInterface {

    @Override
    public Mono<Message> handleCommand(Member member, Message message, TextChannel channel, String[] args) {
        return channel.createMessage(spec -> spec.setContent(
                "By having this bot in your discord server you agree that "
                        + "\n(i) usage of this bot may be terminated at any time without warning"
                        + "\n(ii) bot usage, responses and errors may be logged"
                        + "\n(iii) any data sent to the bot may be indefinately stored"
                        + "\n(iv) staff of this bot may look through your discord guild to gather evidence on reports"
                        + "\nNo warranty is provided for using this bot and I(craftxbox) disclaim responsibility for any damages that are caused as a result of use of this bot"
        ));
    }
}
