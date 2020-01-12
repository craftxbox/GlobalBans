package com.craftxbox.globalbans.command.user;

import com.craftxbox.globalbans.GlobalBans;
import com.craftxbox.globalbans.command.CommandInterface;
import com.craftxbox.globalbans.data.PunishmentInfo;
import com.craftxbox.globalbans.util.DatabaseUtil;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.MessageCreateSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserInfoCommand implements CommandInterface {

    private Pattern USER_REGEX = Pattern.compile("\\d{17,21}");

    @Override
    public Mono<Message> handleCommand(Member member, Message message, TextChannel channel, String[] args) {
        Snowflake mentionedUser = null;

        for (String s : args) {
            Matcher matcher = USER_REGEX.matcher(s);

            if (matcher.matches()) {
                mentionedUser = Snowflake.of(matcher.group());
                break;
            }
        }

        if (mentionedUser != null) {
            AtomicBoolean userWarned = new AtomicBoolean(false);
            AtomicBoolean userBanned = new AtomicBoolean(false);

            return channel.getClient().getUserById(mentionedUser)
                    .flatMapMany(user -> DatabaseUtil.getPunishmentsForUser(user)
                        .flatMap(punishmentInfo -> {
                            if (punishmentInfo.getCaseType() == PunishmentInfo.CaseType.GLOBAL) {
                                if (punishmentInfo.getPunishmentType() == PunishmentInfo.PunishmentType.WARN) {
                                    userWarned.set(true);
                                } else if (punishmentInfo.getPunishmentType() == PunishmentInfo.PunishmentType.BAN) {
                                    userBanned.set(true);
                                }
                            }

                            return Mono.empty();
                        })).then(Mono.defer(() -> channel.createMessage(spec -> {
                            spec.setContent(String.format("Warn: %s\r\nBan: %s", userWarned, userBanned));
                        })))
                        // Why is this exception private?
                        .onErrorResume(t -> t.getClass().getName().equals(
                                "io.r2dbc.postgresql.PostgresqlConnectionFactory$PostgresConnectionException"),
                                t -> channel.createMessage(spec -> {
                                    spec.setContent(String.format(
                                         "%s Could not retrieve data.",
                                        GlobalBans.getConfigurationValue("bot.core.emote.cross")
                                ));
                        }));
        }

        return channel.createMessage(spec -> spec.setContent(String.format(
                "%s No valid users were specified.",
                GlobalBans.getConfigurationValue("bot.core.emote.cross")
        )));
    }
}
