package com.craftxbox.globalbans;

import com.craftxbox.globalbans.command.CommandHandler;
import com.craftxbox.globalbans.command.user.AboutCommand;
import com.craftxbox.globalbans.command.user.LegalCommand;
import com.craftxbox.globalbans.command.user.PingCommand;
import com.craftxbox.globalbans.listener.BotFarmChecker;
import com.craftxbox.globalbans.listener.ServerEvents;
import com.craftxbox.globalbans.util.DatabaseUtil;
import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.EventDispatcher;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.guild.GuildDeleteEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.core.object.util.Snowflake;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Scanner;

public class GlobalBans {

	private static Logger mainLogger = LoggerFactory.getLogger(GlobalBans.class);

	private static Properties botProperties;
	private static DiscordClient discordClient;

	private static ConnectionFactory connectionFactory;

	public static void main(String[] args)  {
		try {
			botProperties = new Properties();
			botProperties.load(new FileInputStream(new File("config.properties")));
		} catch (IOException e) {
			mainLogger.error("Invalid or Missing Config", e);
		}

		List<String> statusList = new ArrayList<>();

		try (BufferedReader statusReader = new BufferedReader(new FileReader("status_list.tsv"))) {
			String currentLine = null;

			while ((currentLine = statusReader.readLine()) != null) {
				statusList.add(currentLine);
			}
		} catch (IOException e) {
			mainLogger.error("Missing Status List", e);
		}

		connectionFactory = ConnectionFactories.get(ConnectionFactoryOptions.builder()
				.option(ConnectionFactoryOptions.DRIVER, "postgresql")
				.option(ConnectionFactoryOptions.HOST, botProperties.getProperty("bot.core.pgsql.host"))
				.option(ConnectionFactoryOptions.PORT, Integer.valueOf(botProperties.getProperty("bot.core.pgsql.port")))
				.option(ConnectionFactoryOptions.USER, botProperties.getProperty("bot.core.pgsql.user"))
				.option(ConnectionFactoryOptions.PASSWORD, botProperties.getProperty("bot.core.pgsql.pass"))
				.option(ConnectionFactoryOptions.DATABASE, botProperties.getProperty("bot.core.pgsql.db")).build());

		DatabaseUtil.init(connectionFactory);

		discordClient = new DiscordClientBuilder(botProperties.getProperty("bot.core.token")).build();

		EventDispatcher eventDispatcher = discordClient.getEventDispatcher();

		eventDispatcher.on(GuildCreateEvent.class).flatMap(e -> new BotFarmChecker().checkServer(e.getGuild())).subscribe();
		eventDispatcher.on(GuildCreateEvent.class).flatMap(e -> new ServerEvents().onCreate(e.getGuild())).subscribe();
		eventDispatcher.on(GuildDeleteEvent.class).flatMap(e -> new ServerEvents().onDelete(e)).subscribe();

		CommandHandler commandHandler = new CommandHandler(discordClient, botProperties.getProperty("bot.core.prefix"));
		commandHandler.registerCommand("ping", new PingCommand());
		commandHandler.registerCommand("about", new AboutCommand());
		commandHandler.registerCommand("legal", new LegalCommand());

		eventDispatcher.on(MessageCreateEvent.class).flatMap(commandHandler::handle).subscribe();

		discordClient.login().subscribe();

		Random r = new Random();

		Flux.interval(Duration.ofMinutes(5L))
				.flatMap(t -> Mono.just(statusList.get(r.nextInt(statusList.size() - 1))))
				.flatMap(newStatus -> {
					String[] statusSplit = newStatus.split("\t");
					String statusType = statusSplit[0];
					String statusText = statusSplit[1];

					switch (statusType) {
						case "playing":
							return discordClient.updatePresence(Presence.online(Activity.playing(statusText)));
						case "watching":
							return discordClient.updatePresence(Presence.online(Activity.watching(statusText)));
						default:
							return Mono.empty();
					}
				}).subscribe();

		Scanner scanner = new Scanner(System.in);
		String input;

		while ((input = scanner.next()) != null) {
			if (input.equalsIgnoreCase("exit")) {
				System.exit(0);
			}
		}
	}

	public static String getConfigurationValue(String key) {
		return botProperties.getProperty(key);
	}
}

