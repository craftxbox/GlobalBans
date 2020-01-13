package com.craftxbox.globalbans.command.user;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.craftxbox.globalbans.GlobalBans;
import com.craftxbox.globalbans.command.CommandInterface;
import com.craftxbox.globalbans.data.PunishmentInfo;
import com.craftxbox.globalbans.util.DatabaseUtil;

import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import discord4j.rest.http.client.ClientException;
import io.r2dbc.spi.R2dbcNonTransientException;
import reactor.core.publisher.Mono;

public class UserInfoCommand implements CommandInterface {

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
			AtomicBoolean userWarned = new AtomicBoolean(false);
			AtomicBoolean userBanned = new AtomicBoolean(false);
			AtomicInteger userPunishmentCount = new AtomicInteger(0);

			return channel.getClient().getUserById(mentionedUser.get())
					.flatMapMany(user -> DatabaseUtil.getPunishmentsForUser(user).flatMap(punishmentInfo -> {
						if (punishmentInfo.getCaseType() == PunishmentInfo.CaseType.GLOBAL) {
							if (punishmentInfo.getPunishmentType() == PunishmentInfo.PunishmentType.WARN) {
								userWarned.set(true);
							} else if (punishmentInfo.getPunishmentType() == PunishmentInfo.PunishmentType.BAN) {
								userBanned.set(true);
							}
							userPunishmentCount.incrementAndGet();
						}

						return Mono.just(user);
					}).switchIfEmpty(Mono.just(user))).next()
					.flatMap(user -> channel.getGuild()
							.flatMap(guild -> guild.getMemberById(mentionedUser.get())
									.flatMap(guildMember -> guildMember.getPresence()
											.flatMap(presence -> createUserEmbed(channel, user, guildMember,
													presence.getStatus().getValue(), userWarned.get(), userBanned.get(),
													userPunishmentCount.get())))
									.onErrorResume(t -> createUserEmbed(channel, user, null, null, userWarned.get(),
											userBanned.get(), userPunishmentCount.get()))))
					.onErrorResume(t -> t instanceof ClientException,
							t -> channel
									.createMessage(spec -> spec.setContent(String.format("%s Unable to retrieve user.",
											GlobalBans.getConfigurationValue("bot.core.emote.cross")))))
					.onErrorResume(t -> t instanceof R2dbcNonTransientException, t -> channel.createMessage(spec -> {
						spec.setContent(String.format("%s Could not retrieve data.",
								GlobalBans.getConfigurationValue("bot.core.emote.cross")));
					}));

		}

		return channel.createMessage(spec -> spec.setContent(String.format("%s No valid users were specified.",
				GlobalBans.getConfigurationValue("bot.core.emote.cross"))));

	}

	private Mono<Message> createUserEmbed(TextChannel channel, User user, Member member, String presence,
			boolean warned, boolean banned, Integer count) {
		return channel.createEmbed(embed -> {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("E L dd yyyy hh:mm a z")
					.withZone(ZoneId.of("UTC"));
			embed.setAuthor(user.getUsername(), "", user.getAvatarUrl());
			embed.addField("Nickname", member != null ? member.getNickname().orElse("N/A") : "Not in guild.", true);
			embed.addField("Discriminator", user.getDiscriminator(), true);
			embed.addField("Date Registered", formatter.format(user.getId().getTimestamp()), true);
			embed.addField("Is Bot", Boolean.toString(user.isBot()), true);
			embed.addField("Status", presence != null ? presence : "Not in guild.", true);
			embed.addField("ID", user.getId().asString(), true);
			embed.addField("GlobalBans Listed", warned && banned ? // if warned and banned
			"Banned" : // list only banned
			banned ? // else if banned
			"Banned" : // list banned
			warned ? // else if warned
			"Warned" : // list warned
			"No", true); // else list no
			if (warned || banned) {
				embed.addField("GlobalBans Entries", Integer.toString(count), true);
			}
		});
	}
}
