package com.craftxbox.globalbans.listener;

import discord4j.core.object.entity.Guild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

public class BotFarmChecker {

    private Logger botFarmLogger = LoggerFactory.getLogger(BotFarmChecker.class);

    public Mono<Void> checkServer(Guild guild) {
        int guildMemberCount = guild.getMemberCount().orElseGet(() -> 1);

        return guild.getMembers()
                .filter(m -> !m.isBot())
                .count()
                .flatMap(trueMemberCount -> {
                    double botRatio = (trueMemberCount / (double) guildMemberCount) * 100;

                    if (botRatio > 65) {
                        botFarmLogger.warn("Leaving {} ({}) - Exceeded User To Bot Ratio", guild.getName(), guild.getId().asString());

                        return guild.leave();
                    } else {
                        return Mono.empty();
                    }
                });
    }

}
