package com.craftxbox.globalbans.util;

import com.craftxbox.globalbans.data.GuildConfig;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.util.Snowflake;
import io.r2dbc.spi.ConnectionFactory;
import reactor.core.publisher.Mono;

import java.time.Instant;

public class DatabaseUtil {

    private static ConnectionFactory connectionFactory;

    public static void init(ConnectionFactory connFactory) {
        if (connectionFactory != null) {
            throw new IllegalStateException("Already initialized");
        }

        connectionFactory = connFactory;
    }

    public static Mono<GuildConfig> getGuildConfig(Guild guild) {
        return getGuildConfig(guild.getId());
    }

    public static Mono<GuildConfig> getGuildConfig(Snowflake guildId) {
        if (connectionFactory != null) {
            return Mono.from(connectionFactory.create())
                    .flatMapMany(connection -> connection.createStatement(
                            "SELECT created_on, notification_channel, warn_only FROM guild_config WHERE server_id = $1")
                                .bind("$1", guildId.asString()).execute())
                    .flatMap(result -> result.map((row, rowMetadata) -> new GuildConfig(
                            Instant.ofEpochMilli(Long.valueOf(row.get("created_on", String.class))),
                            row.get("notification_channel", String.class),
                            row.get("warn_only", Boolean.class)
                    )))
                    .next()
                    .switchIfEmpty(Mono.error(new NoSuchGuildConfigException()));
        }

        return Mono.empty();
    }

    public static Mono<Void> submitConfig(Guild guild, GuildConfig guildConfig) {
        return submitConfig(guild.getId(), guildConfig);
    }

    public static Mono<Void> submitConfig(Snowflake guildId, GuildConfig guildConfig) {
        if (connectionFactory != null) {
            return Mono.from(connectionFactory.create())
                    .flatMapMany(connection -> connection.createStatement(
                            "INSERT INTO guild_config (server_id, created_on, notification_channel, warn_only) VALUES ($1, $2, $3, $4) " +
                                    "ON CONFLICT (server_id) DO UPDATE SET created_on = $2, notification_channel = $3, warn_only = $4")
                    .bind("$1", guildId.asString())
                    .bind("$2", guildConfig.getCreatedOn().toEpochMilli())
                    .bind("$3", guildConfig.getNotificationChannel())
                    .bind("$4", guildConfig.isWarnOnly())
                    .execute()).then();
        }

        return Mono.empty();
    }

    public static class NoSuchGuildConfigException extends Exception {

    }
}
