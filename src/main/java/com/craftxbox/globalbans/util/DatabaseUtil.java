package com.craftxbox.globalbans.util;

import com.craftxbox.globalbans.data.GuildConfig;
import com.craftxbox.globalbans.data.PunishmentInfo;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import io.r2dbc.spi.ConnectionFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

public class DatabaseUtil {

    private static ConnectionFactory connectionFactory;
    private static String schemaName;

    public static void init(ConnectionFactory connFactory, String schmName) {
        if (connectionFactory != null) {
            throw new IllegalStateException("Already initialized");
        }

        connectionFactory = connFactory;
        schemaName = schmName;
    }

    public static Mono<GuildConfig> getGuildConfig(Guild guild) {
        return getGuildConfig(guild.getId(), true);
    }

    public static Mono<GuildConfig> getGuildConfig(Guild guild, boolean forceCreate) {
        return getGuildConfig(guild.getId(), forceCreate);
    }

    public static Mono<GuildConfig> getGuildConfig(Snowflake guildId, boolean forceCreate) {
        if (connectionFactory != null) {
            return Mono.from(connectionFactory.create())
                    .flatMapMany(connection -> connection.createStatement(
                            "SELECT created_on, notification_channel, warn_only FROM " + schemaName + ".guild_config WHERE server_id = $1")
                            .bind("$1", guildId.asString()).execute())
                    .flatMap(result -> result.map((row, rowMetadata) -> new GuildConfig(
                            Instant.ofEpochMilli(Long.valueOf(row.get("created_on", String.class))),
                            row.get("notification_channel", String.class),
                            row.get("warn_only", Boolean.class)
                    )))
                    .next()
                    .switchIfEmpty(forceCreate ? submitConfig(guildId,
                            new GuildConfig(Instant.now(), "not_set", false))
                                : Mono.error(new NoSuchGuildConfigException()));
        }

        return Mono.empty();
    }

    public static Mono<GuildConfig> submitConfig(Guild guild, GuildConfig guildConfig) {
        return submitConfig(guild.getId(), guildConfig);
    }

    public static Mono<GuildConfig> submitConfig(Snowflake guildId, GuildConfig guildConfig) {
        if (connectionFactory != null) {
            return Mono.from(connectionFactory.create())
                    .flatMapMany(connection -> connection.createStatement(
                            "INSERT INTO  " + schemaName + ".guild_config (server_id, created_on, notification_channel, warn_only) VALUES ($1, $2, $3, $4) " +
                                    "ON CONFLICT (server_id) DO UPDATE SET created_on = $2, notification_channel = $3, warn_only = $4")
                            .bind("$1", guildId.asString())
                            .bind("$2", guildConfig.getCreatedOn().toEpochMilli())
                            .bind("$3", guildConfig.getNotificationChannel())
                            .bind("$4", guildConfig.isWarnOnly())
                            .execute()).then(Mono.just(guildConfig));
        }

        return Mono.empty();
    }

    public static Mono<Void> deleteConfig(Snowflake guildId) {
        if (connectionFactory != null) {
            return Mono.from(connectionFactory.create())
                    .flatMapMany(connection -> connection.createStatement("DELETE FROM  " + schemaName + ".guild_config WHERE server_id = $1")
                            .bind("$1", guildId.asString())
                            .execute()).then();
        }

        return Mono.empty();
    }

    public static Flux<PunishmentInfo> getPunishmentsForUser(User user) {
        return getPunishmentsForUser(user.getId());
    }

    public static Flux<PunishmentInfo> getPunishmentsForUser(Snowflake userId) {
        if (connectionFactory != null) {
            return Flux.from(connectionFactory.create())
                    .flatMap(connection -> connection.createStatement(
                            "SELECT issued_by, type, case_id, punishment_type, punishment_time, " +
                                    "punishment_expiry, reason FROM  " + schemaName + ".punishments WHERE user_id = $1")
                            .bind("$1", userId.asString())
                            .execute())
                    .flatMap(result -> result.map((row, rowMetadata) -> new PunishmentInfo(
                            userId,
                            Snowflake.of(row.get("issued_by", String.class)),
                            PunishmentInfo.CaseType.valueOf(row.get("type", String.class).toUpperCase()),
                            Integer.valueOf(row.get("case_id", String.class)),
                            PunishmentInfo.PunishmentType.valueOf(row.get("punishment_type", String.class).toUpperCase()),
                            Instant.ofEpochMilli(Long.valueOf(row.get("punishment_time", String.class))),
                            Instant.ofEpochMilli(Long.valueOf(row.get("punishment_expiry", String.class))),
                            row.get("reason", String.class)
                    )));
        }

        return Flux.empty();
    }

    public static Mono<Long> getPunishmentCountForUser(User user) {
        return getPunishmentCountForUser(user.getId());
    }

    public static Mono<Long> getPunishmentCountForUser(Snowflake userId) {
        if (connectionFactory != null) {
            return Flux.from(connectionFactory.create())
                    .flatMap(connection -> connection.createStatement(
                            "SELECT COUNT(id) FROM  " + schemaName + ".punishments WHERE user_id = $1")
                            .bind("$1", userId.asString())
                            .execute())
                    .flatMap(result -> result.map((row, rowMetadata) -> row.get("count", Long.class)))
                    .single();
        }

        return Mono.empty();
    }

    public static class NoSuchGuildConfigException extends Exception {

    }
}
