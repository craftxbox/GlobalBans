package com.craftxbox.globalbans.command.user;

import com.craftxbox.globalbans.GlobalBans;
import com.craftxbox.globalbans.command.CommandInterface;
import discord4j.core.object.entity.Attachment;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.util.Snowflake;
import discord4j.rest.http.client.ClientException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ReportUserCommand implements CommandInterface {

    private Snowflake REPORT_CHANNEL_ID = Snowflake.of(GlobalBans.getConfigurationValue("bot.core.report_channel"));
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
            Matcher matcher = USER_REGEX.matcher(args[0].replaceAll("\\W", ""));

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
                                            String.format(GlobalBans.getI18nLibrary().get("en_US")
                                                        .get("report_cooldown"),
                                                    GlobalBans.getConfigurationValue("bot.core.emote.cross"),
                                                    Duration.between(Instant.now().minusSeconds(30L),
                                                            reportUserCooldown.get(member.getId())).getSeconds())));
                                } else {
                                    return channel.getClient().getUserById(Snowflake.of(matcher.group()))
                                            .flatMap(user -> channel.getGuild()
                                                .flatMap(guild -> {
                                                    StringBuilder reportInformation = new StringBuilder();
                                                    reportInformation.append("Report From: ")
                                                            .append(member.getUsername())
                                                                .append("#").append(member.getDiscriminator())
                                                                .append(" (").append(member.getId().asString()).append(")")
                                                                .append("\n");

                                                    reportInformation.append("Guild: ")
                                                            .append(guild.getName())
                                                            .append(" (").append(guild.getId().asString()).append(")")
                                                            .append("\n");

                                                    reportInformation.append("Reported User: ")
                                                            .append(user.getUsername())
                                                            .append("#").append(user.getDiscriminator())
                                                            .append(" (").append(user.getId().asString()).append(")")
                                                            .append("\n\n");

                                                    reportInformation.append("Reason: ");

                                                    return createReportMessageWithImages(
                                                            reportInformation.toString() + reportMessage.toString(),
                                                            message).then(channel.createMessage(spec -> spec.setContent(
                                                            String.format(GlobalBans.getI18nLibrary().get("en_US")
                                                                            .get("report_successful"),
                                                                    GlobalBans.getConfigurationValue("bot.core.emote.tick")))));
                                                }))
                                            .onErrorResume(t -> t instanceof ClientException, t ->
                                                    channel.createMessage(spec -> spec.setContent(
                                                            String.format(GlobalBans.getI18nLibrary().get("en_US")
                                                                        .get("no_valid_user_specified"),
                                                                    GlobalBans.getConfigurationValue("bot.core.emote.cross")))));
                                }
                            });
                } else {
                    return channel.createMessage(spec -> spec.setContent(String.format(GlobalBans.getI18nLibrary().get("en_US")
                                    .get("report_no_message"),
                            GlobalBans.getConfigurationValue("bot.core.emote.cross"))));
                }
            } else {
                return channel.createMessage(spec -> spec.setContent(String.format(GlobalBans.getI18nLibrary().get("en_US")
                                .get("no_valid_user_specified"),
                        GlobalBans.getConfigurationValue("bot.core.emote.cross"))));
            }
        } else {
            return channel.createMessage(spec -> spec.setContent(
                    String.format(GlobalBans.getI18nLibrary().get("en_US")
                                    .get("report_missing_arguments"),
                            GlobalBans.getConfigurationValue("bot.core.emote.cross"),
                            GlobalBans.getConfigurationValue("bot.core.prefix"))));
        }
    }

    private Mono<Message> createReportMessageWithImages(String messageText, Message message) {
        List<String> neededAttachments = message.getAttachments()
                .stream().map(Attachment::getUrl).collect(Collectors.toList());

        if (!neededAttachments.isEmpty()) {
            return message.getClient().getChannelById(REPORT_CHANNEL_ID).ofType(TextChannel.class)
                    .flatMap(channel -> channel.createMessage(spec -> {
                        StringBuilder attachmentUrls = new StringBuilder("\n\nAttachments:\n");
                        neededAttachments.forEach(url -> attachmentUrls.append(url).append("\n"));

                        spec.setContent(messageText + attachmentUrls);
                    }));
        } else {
            return message.getClient().getChannelById(REPORT_CHANNEL_ID).ofType(TextChannel.class)
                    .flatMap(channel -> channel.createMessage(spec -> spec.setContent(messageText)));
        }
    }

    private Mono<Boolean> userHasCooldown(Snowflake userId) {
        return Mono.just(reportUserCooldown.containsKey(userId));
    }
}
