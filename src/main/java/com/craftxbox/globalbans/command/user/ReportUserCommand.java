package com.craftxbox.globalbans.command.user;

import com.craftxbox.globalbans.GlobalBans;
import com.craftxbox.globalbans.command.CommandInterface;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.util.Snowflake;
import discord4j.rest.http.client.ClientException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReportUserCommand implements CommandInterface {

    private Pattern USER_REGEX = Pattern.compile("\\d{17,21}");

    private Map<Snowflake, Instant> reportUserCooldown = new ConcurrentHashMap<Snowflake, Instant>() {
        @Override
        public Instant put(Snowflake key, Instant value) {
            expireKeys();
            return super.put(key, value);
        }

        @Override
        public boolean containsKey(Object key) {
            expireKeys();
            return super.containsKey(key);
        }

        private void expireKeys() {
            this.keySet().forEach((userId) -> {
                Instant expiry = this.get(userId);

                if (expiry.isBefore(Instant.now().minusSeconds(29L))) {
                    this.remove(userId);
                }
            });
        }
    };

    @Override
    public Mono<Message> handleCommand(Member member, Message message, TextChannel channel, String[] args) {
        if (args.length > 0) {
            Matcher matcher = USER_REGEX.matcher(args[0]);

            if (matcher.matches()) {
                if (args.length > 1 || !message.getAttachments().isEmpty()) {
                    StringBuilder reportMessage = new StringBuilder();

                    if (args.length > 1) {
                        int index = 0;

                        for (String s : args) {
                            if (index++ != 0) {
                                reportMessage.append(s).append(" ");
                            }
                        }
                    }

                    return userHasCooldown(member.getId())
                            .flatMap(hasCooldown -> {
                                if (hasCooldown) {
                                    return channel.createMessage(spec -> spec.setContent(
                                            String.format("%s You cannot report a user for another %ss.",
                                                    GlobalBans.getConfigurationValue("bot.core.emote.cross"),
                                                    Duration.between(Instant.now().minusSeconds(30L),
                                                            reportUserCooldown.get(member.getId())).getSeconds())));
                                } else {
                                    return channel.getClient().getUserById(Snowflake.of(matcher.group()))
                                            .flatMap(user -> channel.createMessage(spec -> spec.setContent(reportMessage.toString())))
                                            .onErrorResume(t -> t instanceof ClientException, t ->
                                                    channel.createMessage(spec -> spec.setContent(
                                                            String.format("%s No valid users were specified.",
                                                                    GlobalBans.getConfigurationValue("bot.core.emote.cross")))));
                                }
                            });
                } else {
                    return channel.createMessage(spec -> spec.setContent(String.format("%s No report message was provided.",
                            GlobalBans.getConfigurationValue("bot.core.emote.cross"))));
                }
            } else {
                return channel.createMessage(spec -> spec.setContent(String.format("%s No valid users were specified.",
                        GlobalBans.getConfigurationValue("bot.core.emote.cross"))));
            }
        } else {
            return channel.createMessage(spec -> spec.setContent(
                    String.format("%s Missing command arguments. Usage %sreport <user> <reason w/ evidence>.",
                            GlobalBans.getConfigurationValue("bot.core.emote.cross"),
                            GlobalBans.getConfigurationValue("bot.core.prefix"))));
        }
    }

    private Mono<Boolean> userHasCooldown(Snowflake userId) {
        return Mono.just(reportUserCooldown.containsKey(userId));
    }
}
