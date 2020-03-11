package com.craftxbox.globalbans.command.botowner;

import com.craftxbox.globalbans.GlobalBans;
import com.craftxbox.globalbans.command.CommandInterface;
import com.craftxbox.globalbans.command.Describe;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.util.Snowflake;
import discord4j.rest.http.client.ClientException;
import reactor.core.publisher.Mono;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Describe(botOwner = true)
public class LeaveGuildCommand implements CommandInterface {

    private Pattern GUILD_REGEX = Pattern.compile("\\d{17,21}");

    @Override
    public Mono<Message> handleCommand(Member member, Message message, TextChannel channel, String[] args) {
        Snowflake mentionedGuild = null;

        for (String s : args) {
            Matcher matcher = GUILD_REGEX.matcher(s);

            if (matcher.matches()) {
                mentionedGuild = Snowflake.of(matcher.group());
                break;
            }
        }

        if (mentionedGuild != null) {
            return channel.getClient().getGuildById(mentionedGuild)
                    .flatMap(guild -> guild.leave().and(channel.createMessage(spec -> spec.setContent(String.format("%s Successfully left %s.",
                            GlobalBans.getConfigurationValue("bot.core.emote.tick"), guild.getName())))))
                                .cast(Message.class)
                    .onErrorResume(t -> t instanceof ClientException,
                            t -> channel.createMessage(spec -> spec.setContent(String.format("%s No valid guilds were specified.",
                                    GlobalBans.getConfigurationValue("bot.core.emote.cross")))));
        }

        return channel.createMessage(spec -> spec.setContent(String.format("%s No valid guilds were specified.",
                GlobalBans.getConfigurationValue("bot.core.emote.cross"))));
    }
}
