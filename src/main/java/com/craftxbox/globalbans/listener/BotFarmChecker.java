package com.craftxbox.globalbans.listener;

import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

public class BotFarmChecker {

    private Logger botFarmLogger = LoggerFactory.getLogger(BotFarmChecker.class);

    public Mono<Void> checkServer(Guild guild) {
        int guildMemberCount = guild.getMemberCount().orElse(1);

        if (guildMemberCount < 25 || guild.getId().equals(Snowflake.of(474454649370312704L))
            || guild.getId().equals(Snowflake.of(110373943822540800L))) {
            return Mono.empty();
        }

        return guild.getMembers()
                .filter(User::isBot)
                .count()
                .flatMap(trueMemberCount -> {
                    double botRatio = (trueMemberCount / (double) guildMemberCount) * 100;

                    if (botRatio > 65) {
                        botFarmLogger.warn("Leaving {} ({}) - Exceeded User To Bot Ratio", guild.getName(), guild.getId().asString());

                        return guild.leave();
                    } else {
                        return Mono.empty();
                    }
                }).onErrorResume(t -> Mono.empty()).cast(Void.class);
    }

}
