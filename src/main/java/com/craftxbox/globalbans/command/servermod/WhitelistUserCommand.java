package com.craftxbox.globalbans.command.servermod;

import com.craftxbox.globalbans.GlobalBans;
import com.craftxbox.globalbans.command.CommandInterface;
import com.craftxbox.globalbans.command.Describe;
import com.craftxbox.globalbans.util.DatabaseUtil;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import discord4j.rest.http.client.ClientException;
import io.r2dbc.spi.R2dbcNonTransientException;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Describe(requiredPermissions = Permission.BAN_MEMBERS)
public class WhitelistUserCommand implements CommandInterface {

    private Pattern USER_REGEX = Pattern.compile("\\d{17,21}");

    @Override
    public Mono<Message> handleCommand(Member member, Message message, TextChannel channel, String[] args) {
        AtomicReference<Snowflake> mentionedUser = new AtomicReference<>();

        // Check for any real mentions first
        if (!message.getUserMentionIds().isEmpty()) {
            mentionedUser.set(message.getUserMentionIds().toArray(new Snowflake[]{})[0]);
        } else {
            for (String s : args) {
                Matcher matcher = USER_REGEX.matcher(s);

                if (matcher.matches()) {
                    mentionedUser.set(Snowflake.of(matcher.group()));
                    break;
                }
            }
        }

        if (mentionedUser.get() != null) {
            return channel.getClient().getUserById(mentionedUser.get())
                    .flatMap(user -> channel.getGuild()
                        .flatMap(guild -> DatabaseUtil.isUserWhitelistedForGuild(guild.getId(), user.getId())
                            .flatMap(whitelisted -> {
                                if (whitelisted) {
                                    return channel.createMessage(spec -> spec.setContent(
                                            String.format("%s %s is already whitelisted. " +
                                                    "To remove a user from the whitelist do %sunwhitelist <user>",
                                                        GlobalBans.getConfigurationValue("bot.core.emote.cross"),
                                                        user.getUsername() + "#" + user.getDiscriminator(),
                                                        GlobalBans.getConfigurationValue("bot.core.prefix"))));
                                } else {
                                    return DatabaseUtil.createWhitelist(guild.getId(), user.getId()).then(channel.createMessage(
                                            String.format("%s Successfully whitelisted %s.",
                                                    GlobalBans.getConfigurationValue("bot.core.emote.tick"),
                                                    user.getUsername() + "#" + user.getDiscriminator())));
                                }
                            })))
                    .onErrorResume(t -> t instanceof ClientException,
                            t -> channel
                                    .createMessage(spec -> spec.setContent(String.format("%s Unable to retrieve user.",
                                            GlobalBans.getConfigurationValue("bot.core.emote.cross")))))
                    .onErrorResume(t -> t instanceof R2dbcNonTransientException, t -> channel.createMessage(spec -> {
                        spec.setContent(String.format("%s There was a database error, please try again.",
                                GlobalBans.getConfigurationValue("bot.core.emote.cross")));
                    }));
        }

        return channel.createMessage(spec -> spec.setContent(String.format("%s No valid users were specified.",
                GlobalBans.getConfigurationValue("bot.core.emote.cross"))));
    }
}
