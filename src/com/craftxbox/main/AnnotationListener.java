package com.craftxbox.main;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bson.Document;
import org.ini4j.Wini;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.ReplaceOptions;

import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Attachment;
import discord4j.core.object.entity.Channel;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.entity.User;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.core.object.util.Image.Format;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;


public class AnnotationListener {
	  
	String instance = Double.toString(Math.random());
	final static Logger logger = LoggerFactory.getLogger(AnnotationListener.class);
	public static MongoDatabase db = MongoClients.create("mongodb://localhost:27017").getDatabase("gbans");
	Map<Long,Long> lastActionTime = new HashMap<>();
	Map<Long,Integer> userMisuses = new HashMap<>();
	static List<String> blacklisted = new ArrayList<>();
	static List<String> justBanned = new ArrayList<>();
	public static List<String> commands = new ArrayList<>(Arrays.asList("b;about", "b;help", "b;invite", "b;userinfo", "b;report", "b;ping", "b;recount", "b;leave", "b;getguild", "b;eval", "b;kick", "b;accept", "b;deny", "b;update",
				"b;setnotifychannel", "b;report", "b;whitelist", "b;eval", "b;ban", "b;warn","b;togglewarnonly", "b;cullfarms"));
	String visible[]={"b;about", "b;help", "b;invite", "b;userinfo", "b;report", "b;whitelist", "b;setnotifychannel", "b;togglewarnonly", "b;kick", "b;ban"};
	
	public static Boolean[] checkPerms(MessageCreateEvent event, Permission perm){
    	List<Boolean> out = new ArrayList<>();
    	if(event.getMember().get().getBasePermissions().block().contains(perm)){
    		out.add(true);
    	} else{
    		out.add(false);
    	}
    	if(event.getGuild().block().getMemberById(event.getClient().getSelfId().get()).block().getBasePermissions().block().contains(perm)){
    		out.add(true);
    	} else{
    		out.add(false);
    	}
    	if(out.get(0) == true && out.get(1) == true){
    		out.add(true);
    	} else{
    		out.add(false);
    	}
    	return out.toArray(new Boolean[out.size()]);
    }

    public void onMessageReceive(MessageCreateEvent event) {
    	Runnable r = new Runnable(){
    		@Override
			public void run(){
    			processCommand(event);
    		}
    	};
    	new Thread(r).start();
    }
    public void processCommand(MessageCreateEvent event) {
    	String msg = event.getMessage().toString();
    	String cmd = msg.split(" ")[0];
    	String[] args = msg.split(" ");
    	Wini help;
    	User author = event.getMessage().getAuthor().orElse(null);
    	MessageChannel channel = event.getMessage().getChannel().block();
    	if(lastActionTime.get(event.getMember().get().getId().asLong()) == null) {
    		lastActionTime.put(event.getMember().get().getId().asLong(), 0l);
    	}
    	if(userMisuses.get(event.getMember().get().getId().asLong()) == null) {
    		userMisuses.put(event.getMember().get().getId().asLong(), 0);
    	}
    	
    	if(event.getMember().get().isBot()){
			return;
		}
    	if(msg.toLowerCase().startsWith("b;")){
    		if(!commands.contains(cmd)){
        		//sendMessage(channel, "DEBUG: invalid command");
    			return;
    		}

    		if(blacklisted.contains(event.getMember().get().getId().asString())){
    			return;
    		}
			if(cmd.equalsIgnoreCase("b;about")){
				sendMessage(event.getMessage().getChannel().block(), "<:vpGreenTick:319191454331437058> GlobalBans, A database of rulebreakers. \nSupport: https://discord.gg/jcYq5ch");
			}
			if(cmd.equalsIgnoreCase("b;help")){
				if(msg.split(" ").length > 1){
					try {
						help = new Wini(new File("help.ini"));
						sendMessage(event.getMessage().getChannel().block(), help.get("commands", msg.split(" ")[1].replace("b;", "")));
					} catch (FileNotFoundException e) {
						File inicheck = new File("help.ini");
						try {
							inicheck.createNewFile();
							help = new Wini(new File("help.ini"));
							sendMessage(event.getMessage().getChannel().block(), help.get("commands", msg.split(" ")[1].replace("b;", "")));
						} catch (Exception e1) {
							sendMessageError(event.getMessage().getChannel().ofType(TextChannel.class).block(), event, e1,true);
						}
						sendMessageError(event.getMessage().getChannel().ofType(TextChannel.class).block(), event, e,true);
					} catch(Exception e){
						sendMessageError(event.getMessage().getChannel().ofType(TextChannel.class).block(), event, e,true);
					} finally{
						
					}
				}
				else{
					sendMessage(event.getMessage().getChannel().block(), "<:vpGreenTick:319191454331437058> Commands:\n```"+ Arrays.toString(visible).replace("[", "").replace("]", "")+"```\nUsage: b;help <command>");
				}
				
			}
			if(cmd.equalsIgnoreCase("b;invite")){
				sendMessage(event.getMessage().getChannel().block(), "<:vpGreenTick:319191454331437058> Invite me with https://discordapp.com/oauth2/authorize?client_id=" + event.getClient().getSelfId().get().asString()+"&scope=bot&permissions=-1 !");
			}
			if(cmd.equalsIgnoreCase("b;legal")){
				sendMessage(event.getMessage().getChannel().block(), "By having this bot in your discord server you agree that "
						+ "\n(i) usage of this bot may be terminated at any time without warning"
						+ "\n(ii) bot usage, responses and errors may be logged"
						+ "\n(iii) any data sent to the bot may be indefinately stored"
						+ "\n(iv) staff of this bot may look through your discord guild to gather evidence on reports"
						+ "\nNo warranty is provided for using this bot and I(craftxbox) disclaim responsibility for any damages that are caused as a result of use of this bot");
			}
			if(cmd.equalsIgnoreCase("b;userinfo")){
				User mentioned;
				if(event.getMessage().getUserMentions().count().block() < 1) {
					try {
						mentioned = event.getClient().getUserById(Snowflake.of(Long.parseLong(msg.substring(("b;userinfo").length()).split(" ")[1]))).block();
					}
					catch(Exception e) {
						e.printStackTrace();
						sendMessage(event.getMessage().getChannel().block(),"<:vpRedTick:319191455589728256> User doesnt exist! Usage: b;userinfo <mention or id>");
						return;
					}
				} else {
					mentioned = event.getMessage().getUserMentions().blockFirst();
				}
				Member mentionedMember = event.getGuild().block().getMemberById(mentioned.getId()).block();
				MongoCollection<Document> bans = db.getCollection("bans");
				String nick = mentionedMember.getNickname().orElse("N/A");	
				LocalDateTime userJoined = LocalDateTime.ofInstant(Instant.EPOCH,ZoneId.of("UTC"));
				LocalDateTime userCreated = LocalDateTime.ofInstant(mentioned.getId().getTimestamp(), ZoneId.of("UTC"));
				String formattedJoin = "N/A";
				String formattedCreate = userCreated.getHour() + ":" + userCreated.getMinute() 
						+ " " +userCreated.getDayOfMonth()+"/"+userCreated.getMonthValue() + "/" +
						userCreated.getYear();
				if(mentionedMember != null) {
					Instant joinTime = mentionedMember.getJoinTime();
					userJoined = LocalDateTime.ofInstant(joinTime,ZoneId.of("UTC"));
					formattedJoin = userJoined.getHour() + ":" + userJoined.getMinute() 
					+ " " +userJoined.getDayOfMonth()+"/"+userJoined.getMonthValue() + "/" +
					userJoined.getYear();
				}
				event.getMessage().getChannel().block().createEmbed(spec->{
					spec.setColor(mentionedMember.getColor().block());
					spec.setAuthor(mentioned.getUsername(), null, mentioned.getAvatarUrl());
					spec.addField("Nickname:", nick, true);
					spec.addField("Discriminator:", mentioned.getDiscriminator(), true);
					spec.addField("Date Registered:", formattedCreate.toString(), true); 
					if(userJoined.getYear() > 2014) spec.addField("Date Joined:", formattedJoin, true);
					spec.addField("Is Bot:", Boolean.toString(mentioned.isBot()), true);
					spec.addField("Status:", mentionedMember.getPresence().block().getStatus().name(), true);
					spec.addField("ID:",mentioned.getId().asString(),true);
					if(bans.find(Document.parse("{\"id\":"+mentioned.getId().asString()+"}")).first() != null) {
						if(bans.find(Document.parse("{\"id\":"+mentioned.getId().asString()+"}")).first().getString("type").equals("ban")) {
							spec.addField("GlobalBans Listed","Banned", true);
						}
						else if(bans.find(Document.parse("{\"id\":"+mentioned.getId().asString()+"}")).first().getString("type").equals("ban")) {
							spec.addField("GlobalBans Listed","Warned", true);
						}
					}
					spec.addField("Roles:", ifnull(mentionedMember.getRoles().collectList().block().toString(),"Not in server."), true);
				}).subscribe();
			}

    		if(cmd.equalsIgnoreCase("b;ping")){
    			long gatewayheartbeat = event.getClient().getResponseTime();
    			Instant before = Instant.now();
    			Message pingmsg = event.getMessage().getChannel().block().createMessage("Gateway: "+gatewayheartbeat+"ms").block();
    			long total = Instant.now().toEpochMilli()-before.toEpochMilli();
    			pingmsg.edit(spec->{spec.setContent("Heartbeat: "+gatewayheartbeat+"ms\nRound-Trip: "+total+"ms");}).subscribe();
    		}
    		if(cmd.equalsIgnoreCase("b;recount") && author.getId().asLong() == 153353572711530496L){
    			event.getClient().updatePresence(Presence.online(Activity.watching(event.getClient().getGuilds().count().block()+" Servers | b;help")));
    	    	sendMessage(event.getMessage().getChannel().block(), "Recounted: " + event.getClient().getGuilds().count().block());
    		}
    		if(cmd.equalsIgnoreCase("b;leave") && event.getClient().getGuilds().count().block() == 153353572711530496L){
    			event.getClient().getGuildById(Snowflake.of(Long.parseLong(msg.split(" ")[1]))).block().leave().subscribe();
    		}
    		if(cmd.equalsIgnoreCase("b;getGuild") && event.getClient().getGuilds().count().block() == 153353572711530496L){
    			Guild guild = event.getClient().getGuildById(Snowflake.of(Long.parseLong(msg.split(" ")[1]))).block();
    			channel.createEmbed(em->{
    				em.setAuthor(guild.getName(), "", guild.getIconUrl(Format.PNG).orElse("https://crxb.tk/vy30qk"));
    				em.addField("Bots", Integer.toString(guild.getMembers().toStream().filter(u -> u.isBot()).collect(Collectors.toList()).size()), true);
    				em.addField("Humans", Integer.toString(guild.getMembers().toStream().filter(u -> !u.isBot()).collect(Collectors.toList()).size()), true);
    				em.addField("Total Users", Long.toString(guild.getMembers().count().block()), true);
    				em.addField("Bot Percentage", "%" + Float.toString((guild.getMembers().toStream().filter(u -> u.isBot()).collect(Collectors.toList()).size() * 100.0f) / guild.getMembers().count().block()), true );
    				em.addField("Owner", guild.getOwner().block().getId().asString(), true);
    			}).subscribe();
    		}
    		if(cmd.equalsIgnoreCase("b;report")){
    			if(lastActionTime.get(author.getId().asLong()) + 60000 < System.currentTimeMillis()) {
	    			if(msg.split(" ").length > 2){
	    				if(checkPerms(event,Permission.KICK_MEMBERS)[0] == true){
	    					if(event.getMessage().getUserMentions().count().block() > 0) {
	    						sendMessage((MessageChannel)event.getClient().getChannelById(Snowflake.of(356298368919797760L)).block(), 
	    								    "Report from: " + author.getId().asString()
	    								+ "\nGuild: " + event.getGuild().block().getId().asString()
	    								+ "\nChannel: " + channel.getId().asString()
		    							+ "\nReported user: " + event.getMessage().getUserMentionIds().toArray(new Snowflake[] {})[0].asString()
		        						+ "\nReason: " + msg.substring(msg.split(" ")[1].length()+9));
		    					for(Attachment i : event.getMessage().getAttachments()) {
		    						try {
		    							HttpURLConnection urlconn = (HttpURLConnection)new URL(i.getUrl()).openConnection();
		    							urlconn.setRequestProperty ("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/40.0.2214.85 Safari/537.36");
		    							urlconn.setRequestMethod("GET");
		    							InputStream istream = urlconn.getInputStream();
										event.getClient().getChannelById(Snowflake.of(356298368919797760L)).ofType(MessageChannel.class).block().createMessage(spec->{
											spec.addFile(i.getFilename(),istream);
										});
										Thread.sleep(200);
									} catch (IOException e) {
										sendMessageError((TextChannel) channel, event, e,true);
									} catch (InterruptedException e) {
										sendMessageError((TextChannel) channel, event, e,true);
									}
		    					}"".chars().mapToObj(c -> (char) c).collect(Collectors.toList()).stream().filter(chare->{return Character.isUpperCase(chare);}).count();
		    					sendMessage(channel, "Successfully reported user " + event.getMessage().getUserMentions().collectList().block().get(0).getId().asString() + ". Thank you.");
		    					lastActionTime.put(author.getId().asLong(), System.currentTimeMillis());
	    					} else {
	    						try {
	    							Long.parseLong(msg.split(" ")[1]);
	    						} catch(@SuppressWarnings("unused") NumberFormatException e) {
	    							sendMessage(channel,"Please enter a valid id or mention!");
	    						}
	    						if(event.getClient().getUserById(Snowflake.of(Long.parseLong(msg.split(" ")[1])))==null) {
	    							sendMessage(channel,"User doesnt exist!");
	    							return;
	    						}
	    						sendMessage((MessageChannel) event.getClient().getChannelById(Snowflake.of(356298368919797760L)), 
	    								"Report from: " + author.getId().asString()
	    								+ "\nGuild: " + event.getGuild().block().getId().asString()
	    								+ "\nChannel: " + channel.getId().asString()
		    							+ "\nReported user: " + event.getMessage().getUserMentionIds().toArray(new Snowflake[] {})[0].asString()
		        						+ "\nReason: " + msg.substring(msg.split(" ")[1].length()+9));
		    					for(Attachment i : event.getMessage().getAttachments()) {
		    						try {
										event.getClient().getChannelById(Snowflake.of(356298368919797760L)).ofType(MessageChannel.class).block().createMessage(spec->spec.addFile(i.getFilename(), new URL(i.getUrl()).openStream()));
										Thread.sleep(200);
									} catch (IOException e) {
										sendMessageError((TextChannel) channel, event, e,true);
									} catch (InterruptedException e) {
										sendMessageError((TextChannel) channel, event, e,true);
									}
		    					}
		    					sendMessage(channel, "Successfully reported user " + msg.split(" ")[1] + ". Thank you.");
		    					lastActionTime.put(author.getId().asLong(), System.currentTimeMillis());
	    					}
	    				} else {
	    					sendMessage(channel,"You do not have permission!");
	    				}
	    			} else {
	    				sendMessage(channel,"Not enough arguments! Usage: b;report <@ mention or id> <reason and proof>");
	    			}
    			} else {
    				if(userMisuses.get(author.getId().asLong()) > 0) {
    					if(userMisuses.get(author.getId().asLong()) > 5) {
    						sendMessage(channel,"You have been Auto-Blacklisted for exceeding maximum misuses. This will expire in 2 hours");
    						Runnable r = new Runnable() {
    							@Override
								public void run() {
    								blacklisted.add(author.getId().asString());
    								try {
										Thread.sleep(7200000);
									} catch (Exception e) {
										sendMessageError((TextChannel)channel,event,e,true);
									}
    							}
    						};
    						new Thread(r).start();
    					} else {
    						sendMessage(channel,"You are being ratelimited! You are at " + userMisuses.get(author.getId().asLong()) + " out of 5 misuses!");
    						userMisuses.put(author.getId().asLong(), userMisuses.get(author.getId().asLong()) + 1);
    					}
    				} else {
    					sendMessage(channel,"You are being ratelimited!");
    					userMisuses.put(author.getId().asLong(), 1);
    				}
    				lastActionTime.put(author.getId().asLong(), System.currentTimeMillis());
    			}
    		}
			if(cmd.equalsIgnoreCase("b;whitelist")){
				if(msg.split(" ").length > 1){
					User toWhitelist = null;
					try {
						toWhitelist = event.getClient().getUserById(Snowflake.of(Long.parseLong(msg.replaceAll("<>@!", "").split(" ")[1]))).block();
					} catch (@SuppressWarnings("unused") NumberFormatException e1) {
						sendMessage(channel, "This user doesnt exist!");
						return;
					}
					if(toWhitelist == null){
						sendMessage(channel, "This user doesnt exist!");
						return;
					}
					try{
						if(db.getCollection("bans").find(Document.parse("{\"id\":\""+args[1]+"\"}")).first() != null){
							if(checkPerms(event,Permission.KICK_MEMBERS)[0] == true){
								if(db.getCollection("bans").find(Document.parse("{\"id\":\""+args[1]+"\"}")).first().getString("type").equals("ban")){
									Document guildOpts = db.getCollection("guilds").find(Document.parse("{\"id\":"+event.getGuild().block().getId().asString()+"}")).first();
									if(guildOpts == null) guildOpts = initGuild(event.getGuild().block());
									if(guildOpts.containsKey("whitelist")) {
										@SuppressWarnings("unchecked")
										ArrayList<String> whitelist = (ArrayList<String>) guildOpts.get("whitelist");
										whitelist.add(args[1]);
										guildOpts.put("whitelist", whitelist);
									} else {
										guildOpts.put("whitelist", Arrays.asList(new String[]{args[1]}));
									}
									sendMessage(channel, "Successfully whitelisted " + msg.split(" ")[1]);
									return;
								}
								sendMessage(channel, "This user isnt banned in the GlobalBan System!");
								return;
							}
							sendMessage(channel, "You dont have the permission to do this! (missing kick permissions)");
							return;
						}
						sendMessage(channel, "This user isnt banned in the GlobalBan System!");
						return;
					}catch(NullPointerException e){
						sendMessageError((TextChannel)channel,event,e,true);
						return;
					}
				}
				sendMessage(channel, "Usage: b;whitelist <user id>");
				return;
			}
			if(cmd.equalsIgnoreCase("b;setnotifychannel")){
				if(author.getId().asString().equals("153353572711530496") || checkPerms(event,Permission.MANAGE_CHANNELS)[0]){
					Document guildOpts = db.getCollection("guilds").find(Document.parse("{\"id\":"+event.getGuild().block().getId().asString()+"}")).first();
					if(guildOpts == null) guildOpts = initGuild(event.getGuild().block());
					try {
						if(event.getGuild().block().getChannelById(Snowflake.of(args[1].replaceAll("[^\\d]",""))) == null) {
							guildOpts.put("notifychannel", channel.getId().asString());
						} else {
							guildOpts.put("notifychannel",args[1].replaceAll("[^\\d]",""));
						}
					} catch (@SuppressWarnings("unused") NumberFormatException e) {
						sendMessage(channel,"That isnt a channel! Got:"+ args[1]);
						return;
					} catch (@SuppressWarnings("unused") IndexOutOfBoundsException e) {
						guildOpts.put("notifychannel", channel.getId().asString());
					}
				    db.getCollection("guilds").replaceOne(Document.parse("{\"id\":"+event.getGuild().block().getId().asString()+"}"), guildOpts);
				    sendMessage(channel, "Successfully set the Notification Channel to " + event.getGuild().block().getChannelById(Snowflake.of(guildOpts.getString("notifychannel"))));
				    return;
				}
				sendMessage(channel, "You do not have permission! (missing manage channels)");
				return;
			}
			if(cmd.equalsIgnoreCase("b;togglewarnonly")){
				if(checkPerms(event,Permission.MANAGE_CHANNELS)[0] || author.getId().asString().equals("153353572711530496")){
					Document guildOpts = db.getCollection("guilds").find(Document.parse("{\"id\":"+event.getGuild().block().getId().asString()+"}")).first();
					if(guildOpts == null) guildOpts = initGuild(event.getGuild().block());
					if(!guildOpts.getBoolean("warnonly", false)) {
						guildOpts.put("warnonly", true);
					    sendMessage(channel, "Successfully set the bot to Warn Only Mode");
					} else {
						guildOpts.put("warnonly", false);
					    sendMessage(channel, "Successfully set the bot to Ban Mode");
					}
					db.getCollection("guilds").replaceOne(Document.parse("{\"id\":"+event.getGuild().block().getId().asString()+"}"), guildOpts);
				}
				else {
					sendMessage(channel, "You do not have permission! (missing manage channels)");
					return;
				}
			}
			
			if(cmd.equalsIgnoreCase("b;ban")){
				if(author.getId().asString().equals("153353572711530496")) {
					try {
						Document bannedUser = Document.parse("{\"id\":\""+args[1]+"\",\"type\":\"ban\",\"reason\":\""+msg.split(args[1]+" ")[1]+"\"}");
						db.getCollection("bans").replaceOne(Document.parse("{\"id\":\""+args[1]+"\"}"),bannedUser, new ReplaceOptions().upsert(true));
						List<Guild> guilds = event.getClient().getGuilds().collectList().block();
						List<Long> bannedUsers = new ArrayList<>();
						while(guilds.size() > 0){
							
							Guild guild = guilds.get(0);
							Document guildOpts = db.getCollection("guilds").find(Document.parse("{\"id\":"+event.getGuild().block().getId().asString()+"}")).first();
							if(guildOpts == null) guildOpts = initGuild(event.getGuild().block());
							List<Member> users = guild.getMembers().collectList().block();
							while(users.size() > 0){
								User user = users.get(0);
								if(user.getId().asString().equals(msg.split(" ")[1])){
									sendMessage(event.getClient().getChannelByID(
											Long.parseLong()), "User " + 
											user.getName() + "#" + user.getDiscriminator() + 
											" Is banned by GlobalBans for " + 
											msg.split("\\d{17,18}")[1]);
											bannedUsers.add(guild.getLongID());
									if(!guildOpts.getBoolean("warnonly", false)){
										justBanned.add(user.getStringID());
										try {
											guild.banUser(user);
										} catch(@SuppressWarnings("unused") MissingPermissionsException e) {
											//ok
										}
										new Thread(new Runnable() {

											@Override
											public void run() {
												try {
													Thread.sleep(5000);
												} catch (InterruptedException e) {
													e.printStackTrace();
												}
												justBanned.remove(user.getStringID());
											}
											
										}).start();
									}
								}
								users.remove(0);
							}
							guilds.remove(0);
						}
						sendMessage(channel,"Ban affected " + bannedUsers.size() + " Guilds");
						sendMessage(event.getClient().getChannelByID(526097580539641866l),event.getAuthor().getName() + " Banned " + msg.split(" ")[1] + " For " + msg.split(msg.split(" ")[1])[1] + "\n" + bannedUsers.toString());
					} catch (Exception e ) {
						sendMessageError((TextChannel)channel,event,e,true);
					}
				}
				else {
					User user = event.getAuthor();
					if(checkPerms(event,Permission.BAN)[2] == true){
	    				try{
	    					msg.split(" ")[2].length();
	    					if(event.getMessage().getMentions().size() > 0) {
	    						event.getGuild().banUser(event.getMessage().getMentions().get(0),"[" + user.getName() + "#" + user.getDiscriminator() + "] "+ msg.split("<@!?\\d{17,18}>")[1]);
	    						sendMessage(channel,"Banned user.");
	    					}
	    					else {
	    						event.getGuild().banUser(event.getClient().fetchUser(Long.parseLong(msg.split(" ")[1])),"[" + user.getName() + "#" + user.getDiscriminator() + "] " + msg.split("\\d{17,18}")[1]);
	    						sendMessage(channel,"Banned user.");
	    					}
	    				} catch(@SuppressWarnings("unused") MissingPermissionsException e) {
	    					sendMessage(channel,"GlobalBans does not have permissions to ban users above it or equal to it in the heirarchy!");
	    				} catch(@SuppressWarnings("unused") ArrayIndexOutOfBoundsException e){
	    					if(event.getMessage().getMentions().size() > 0) {
	    						event.getGuild().banUser(event.getMessage().getMentions().get(0),"[" + user.getName() + "#" + user.getDiscriminator() + "] ");
	    						sendMessage(channel,"Banned user.");
	    					}
	    					else {
	    						event.getGuild().banUser(event.getClient().fetchUser(Long.parseLong(msg.split(" ")[1])),"[" + user.getName() + "#" + user.getDiscriminator() + "] ");
	    						sendMessage(channel,"Banned user.");
	    					}
	    				}
	    			} else if(checkPerms(event,Permission.BAN)[0] == false){
	    				sendMessage(channel, "You do not have permissions to ban!");
	    			} else if(checkPerms(event,Permission.BAN)[1] == false){
	    				sendMessage(channel, "GlobalBans does not have permissions to ban!");
	    			}
				}
			}
			if(cmd.equalsIgnoreCase("b;kick")) {
				User user = event.getAuthor();
				if(checkPerms(event,Permission.KICK)[2] == true){
    				try{
    					msg.split(" ")[2].length();
    					if(event.getMessage().getMentions().size() > 0) {
    						event.getGuild().kickUser(event.getMessage().getMentions().get(0),"[" + user.getName() + "#" + user.getDiscriminator() + "] " + msg.split("<@!?\\d{17,18}>")[1]);
    						sendMessage(channel,"Kicked user.");
    					}
    					else {
    						event.getGuild().kickUser(event.getClient().fetchUser(Long.parseLong(msg.split(" ")[1])),"[" + user.getName() + "#" + user.getDiscriminator() + "] " +msg.split("\\d{17,18}")[1]);
    						sendMessage(channel,"Kicked user.");
    					}
    				} catch(@SuppressWarnings("unused") MissingPermissionsException e) {
    					sendMessage(channel,"GlobalBans does not have permissions to kick users above it or equal to it in the heirarchy!");
    				} catch(@SuppressWarnings("unused") ArrayIndexOutOfBoundsException e){
    					if(event.getMessage().getMentions().size() > 0) {
    						event.getGuild().kickUser(event.getMessage().getMentions().get(0),"[" + user.getName() + "#" + user.getDiscriminator() + "] ");
    						sendMessage(channel,"Kicked user.");
    					}
    					else {
    						event.getGuild().kickUser(event.getClient().fetchUser(Long.parseLong(msg.split(" ")[1])),"[" + user.getName() + "#" + user.getDiscriminator() + "] ");
    						sendMessage(channel,"Kicked user.");
    					}
    				}
    			} else if(checkPerms(event,Permission.KICK)[0] == false){
    				sendMessage(channel, "You do not have permissions to kick!");
    			} else if(checkPerms(event,Permission.KICK)[1] == false){
    				sendMessage(channel, "GlobalBans does not have permissions to kick!");
    			}
			}
			if(cmd.equalsIgnoreCase("b;warn") && author.getId().asString().equals("153353572711530496")){
				try {
					Document bannedUser = Document.parse("{\"id\":\""+args[1]+"\",\"type\":\"warn\",\"\":\""+msg.split(args[1]+" ")[1]+"\"}");
					db.getCollection("bans").replaceOne(Document.parse(""),bannedUser,new ReplaceOptions().upsert(true));
					List<Long> affected = new ArrayList<>();
					for(Object gl : event.getClient().getGuilds().toArray()){
						
						Guild guild = (Guild) gl;
						Document guildOpts = db.getCollection("guilds").find(Document.parse("{\"id\":"+event.getGuild().block().getId().asString()+"}")).first();
						if(guildOpts == null) guildOpts = initGuild(event.getGuild().block());
						for(Object ul : guild.getMembers()){
							User user = (User) ul;
							if(user.getStringID().equals(msg.split(" ")[1])){
								Channel channel = guild.getChannelByID(Long.parseLong(guildOpts.getString("notifychannel")));
								sendMessage(channel, "User " + user.getName() + "#" + user.getDiscriminator() + " has a warning in the GlobalBans system for "
								+ msg.split("\\d{17,18}")[1]);
								affected.add(user.getLongID());
							}
						}
					}
					sendMessage(channel, "Warn affected " + affected.size() + " servers.");
					sendMessage(event.getClient().getChannelByID(526097580539641866l),event.getAuthor().getName() + " Warned " + msg.split(" ")[1] + " For " + msg.split(msg.split(" ")[1])[1] + "\n" + affected.toString());
					
				} catch (Exception e ) {
					sendMessageError((TextChannel)channel,event,e,true);
					sendMessage(event.getClient().getApplicationOwner().getOrCreatePMChannel(),"CRITICAL: failed to load bans database!");
				}
			}
			if(cmd.equalsIgnoreCase("b;eval") && author.getId().asLong() == 153353572711530496L){
    			ScriptEngineManager factory = new ScriptEngineManager();
    			ScriptEngine engine = factory.getEngineByName("JavaScript");
    			engine.put("event", event);
    			new Thread() {
				    @Override
					public void run() {
				    	try {
		    				if(event.getMessage().getContent().toLowerCase().contains(".gettoken()")){
		    					sendMessage(channel,"lol no");
		    					return;
		    				}
		    				engine.put("Long", Long.class);
		    				Message m = sendMessage(channel,"Evaluating");
		    				
		    			    Object object = engine.eval("var cthis = Packages.com.craftxbox.main.AnnotationListener;" + msg.substring(7));
		    			    if(object == null){
		    			    	 m.edit("```[class java.lang.Object] null```");
		    			    	 return;
		    			    }
		    			    if(object.toString().length() > 1800) {
		    			    	PrintWriter out = new PrintWriter("lasteval.txt");
		    			    	out.println(object.toString());
		    			    	out.close();
		    			    	m.edit("Uploading");
		    			    	channel().sendFile(new File("lasteval.txt"));
	    			    		m.delete();
		    			    }
		    			    else
		    			    	m.edit( "```[" + object.getClass() +"] " + object.toString() + "```");
		    			} catch (Exception ex) {
		    			     sendMessageError((TextChannel)channel, event,ex,false);
		    			    
		    			}
				    }  
				}.start();
    		}
			if(cmd.equalsIgnoreCase("b;cullfarms") && author.getId().asLong() == 153353572711530496l){
				Runnable r = new Runnable(){
					@Override
					public void run(){
						List<String> out = new ArrayList<>();
						for(Guild i : event.getClient().getGuilds()){
							out.add(i.getName().substring(0, 1));
						}
						String.join("", out);
						int culled = 0;
						for(Guild guild : event.getClient().getGuilds()){
							if((guild.getMembers().stream().filter(u -> u.isBot()).collect(Collectors.toList()).size() * 100.0f) / guild.getMembers().size() > 55f){
			    				guild.leave();
			    				culled++;
			    			}
						}
						sendMessage(channel,"Culled " + culled + " Botfarms");
					}
				};
				new Thread(r).start();
			}
			if(cmd.equalsIgnoreCase("b;deny")  && author.getId().asLong() == 153353572711530496l){
				if((args.length >= 3)) {
					User user = event.getClient().fetchUser(Long.parseLong(args[2]));
					sendMessage(event.getClient().getChannelByID(Long.parseLong(args[1])),
							"Hello! Your report for " + user.getName() + "#" + user.getDiscriminator() + "(" + args[2] + ") has been declined for:\n`" +
							msg.split(args[2])[1]+"`");
				}
			}
			if(cmd.equalsIgnoreCase("b;update")  && author.getId().asLong() == 153353572711530496l){
				if((args.length >= 3)) {
					User user = event.getClient().fetchUser(Long.parseLong(args[2]));
					sendMessage(event.getClient().getChannelByID(Long.parseLong(args[1])),
							"Hello! Your report for " + user.getName() + "#" + user.getDiscriminator() + "(" + args[2] + ") has been updated:\n`" +
							msg.split(args[2])[1]+"`");
				}
			}
			if(cmd.equalsIgnoreCase("b;accept")  && author.getId().asLong() == 153353572711530496l){
				if((args.length >= 3)) {
					User user = event.getClient().fetchUser(Long.parseLong(args[2]));
					sendMessage(event.getClient().getChannelByID(Long.parseLong(args[1])),
							"Hello! Your report for " + user.getName() + "#" + user.getDiscriminator() + "(" + args[2] + ") has been accepted. Thank you for helping to make discord a better place");
				}
			}
    	}
    }
    private Document initGuild(Guild Guild) {
    	Document guild = db.getCollection("guilds").find(Document.parse("{\"id\":\""+Guild.getStringID()+"\"}")).first();
    	if(guild == null) {
    		guild = Document.parse(("{'id':'"+Guild.getStringID()+"','notifychannel':'"+Guild.getDefaultChannel().getStringID()+"','warnonly':false}").replaceAll("'", "\""));
    		db.getCollection("guilds").replaceOne(Document.parse("{\"id\":\""+Guild.getStringID()+"\"}"), guild, new ReplaceOptions().upsert(true));
    		return guild;
    	}
		return guild;
	}

	
    public void onMessageSent(MessageCreateEvent event){
    	String msg = event.getMessage().toString();
		if(msg.split(" ")[0].equalsIgnoreCase("b;kill")){
			if(msg.split(" ")[1].equals(instance)){
				System.out.println("Ignoring kill from my own instance, " + instance);
				
			}
			else{
				sendMessage(channel, instance + " shutting down :ok_hand:");
				System.exit(0);
			}
		}
    }
    
    public void onJoin(GuildCreateEvent event) throws Exception{
    	if(event.getClient().isReady()){
    		Runnable r = new Runnable() {
    				@Override
					public void run() {
			    		try {
			    			if(db.getCollection("blacklist").find(Document.parse("{}")).first() != null) {
			    				sendMessage(event.getGuild().getChannels().ofType(MessageChannel.class).blockFirst(),"This server has been banned from using GlobalBans.\nIf you believe this was in error, please contact craftxbox at <https://discord.gg/jcYq5ch>");
			        			event.getGuild().leave();
			        		}
			    			Document guildOpts = db.getCollection("guilds").find(Document.parse("{\"id\":"+event.getGuild().block().getId().asString()+"}")).first();
							if(guildOpts == null){
								Guild guild = event.getGuild();
								initGuild(guild);
								if((guild.getMembers().stream().filter(u -> u.isBot()).collect(Collectors.toList()).size() * 100.0f) / guild.getMembers().size() > 55f){
			        				guild.leave();
			        				return;
			        			}
								EmbedBuilder em = new EmbedBuilder();
								em.withAuthorIcon(guild.getIconURL()); 
								em.withAuthorName(guild.getName());
								em.appendField("Bots", Integer.toString(guild.getMembers().stream().filter(u -> u.isBot()).collect(Collectors.toList()).size()), true);
								em.appendField("Humans", Integer.toString(guild.getMembers().stream().filter(u -> !u.isBot()).collect(Collectors.toList()).size()), true);
								em.appendField("Total Users", Integer.toString(guild.getMembers().size()), true);
								em.appendField("Bot Percentage", "%" + Float.toString((guild.getMembers().stream().filter(u -> u.isBot()).collect(Collectors.toList()).size() * 100.0f) / guild.getMembers().size()), true );
								em.withColor(Color.green);
								em.appendField("Owner", guild.getOwner().getName() + "#" + guild.getOwner().getDiscriminator(), true);
								em.appendField("ID", Long.toString(guild.getLongID()), true);
								sendMessage(event.getClient().getChannelByID(330218133707292672L), em.build());
								sendMessage(event.getGuild().getDefaultChannel(), "To set the channel you want this bot to notify to, run b;setnotifychannel in the channel you want to set it to."
										+ "\nRun b;togglewarnonly to toggle if you want the bot to auto-ban or not."
										+ "\nUse b;whitelist <User ID> to whitelist already globalbanned users from the auto ban."
										+ "\nUse b;report <User ID> <Reason + Proof> to report users to be reviewed."
										+ "\nCriteria for reporting is if they 1: Advertise 2: Spam 3: Raid 4: Harrass. Please include proof."
										+ "\nUse b;userinfo <Mention or ID> to get information about a specific user.");
							}
							event.getClient().changePresence(StatusType.ONLINE, ActivityType.PLAYING, "GlobalBans | Banning people from " + event.getClient().getGuilds().size() + " servers | b;help");
							
							System.out.println(event.getClient().getGuilds().size());
							Main.textField.setText(Integer.toString(event.getClient().getGuilds().size()));
						} catch (Exception e) {
							logger.error("Error in join guild:",e);
						}
    				}
    		}; new Thread(r).start();
    	}
    	
    }
    
    public void onLeave(GuildLeaveEvent event){
    	Runnable r = new Runnable(){
			@Override
			public void run(){
		    	if(event.getClient().isReady()){
		    		event.getClient().changePresence(StatusType.ONLINE, ActivityType.PLAYING, "GlobalBans | Banning people from " + event.getClient().getGuilds().size() + " servers | b;help");
		        	System.out.println(event.getClient().getGuilds().size());
		        	Main.textField.setText(Integer.toString(event.getClient().getGuilds().size()));
					Guild guild = event.getGuild();
					EmbedBuilder em = new EmbedBuilder();
					em.withAuthorIcon(guild.getIconURL()); 
					em.withAuthorName(guild.getName());
					em.appendField("Bots", Integer.toString(guild.getUsers().stream().filter(u -> u.isBot()).collect(Collectors.toList()).size()), true);
					em.appendField("Humans", Integer.toString(guild.getUsers().stream().filter(u -> !u.isBot()).collect(Collectors.toList()).size()), true);
					em.appendField("Total Users", Integer.toString(guild.getUsers().size()), true);
					em.appendField("Bot Percentage", "%" + Float.toString((guild.getUsers().stream().filter(u -> u.isBot()).collect(Collectors.toList()).size() * 100.0f) / guild.getUsers().size()), true );
					em.withColor(Color.red);
					em.appendField("ID", Long.toString(guild.getLongID()), true);
					db.getCollection("guilds").deleteOne(Document.parse("{\"id\":"+event.getGuild().block().getId().asString()+"}"));
					sendMessage(event.getClient().getChannelByID(330218133707292672L), em.build());
		    	}
			}
		}; new Thread(r).start();
    }
    
    public void onUserJoin(UserJoinEvent event) {
    	Runnable r = new Runnable(){
			@Override
			public void run(){
		    	try {
		    		Document guildOpts = db.getCollection("guilds").find(Document.parse("{\"id\":"+event.getGuild().block().getId().asString()+"}")).first();
					if(guildOpts == null) guildOpts = initGuild(event.getGuild().block());
					Document user = db.getCollection("bans").find(Document.parse("{\"id\":"+event.getUser().getStringID()+"}")).first();
					try{
						@SuppressWarnings("unchecked")
						ArrayList<String> whitelist = ((ArrayList<String>)guildOpts.get("whitelisted"));
						if(whitelist.contains(event.getUser().getStringID())){
					    	//user is whitelisted, do nothing
						}
					} catch (@SuppressWarnings("unused") NullPointerException e){
						try{
							if(user.getString("type") == "ban"){
					    		if(guildOpts.getBoolean("warnonly", false)){
					    			try {
					    				justBanned.add(event.getUser().getStringID());
										event.getGuild().banUser(event.getUser(), user.getString("reason"),1);
										new Thread(new Runnable() {

											@Override
											public void run() {
												try {
													Thread.sleep(5000);
												} catch (InterruptedException e) {
													e.printStackTrace();
												}
												justBanned.remove(event.getUser().getStringID());
											}
											
										}).start();
									} catch (@SuppressWarnings("unused") MissingPermissionsException e1) {
										//cant ban, dont bother
									}
					    		}
						    	if(guildOpts.getString("notifychannel") == null){
						    		sendMessage(event.getGuild().getDefaultChannel(), "User " + event.getUser().getName().replaceAll("discord.gg", "") + "#" + event.getUser().getDiscriminator() +
						    				"("+event.getUser().getStringID()+")" + " is banned by GlobalBans for " + user.getString("reason"));
						    		return;
						    	}
								sendMessage(event.getGuild().getChannelByID(Long.parseLong(guildOpts.getString("notifychannel"))), "User " + event.getUser().getName().replaceAll("discord.gg", "") + "#" + event.getUser().getDiscriminator() +
										"("+event.getUser().getStringID()+")" + " is banned by GlobalBans for " + user.getString("reason"));
								return;
					    	}
							else if(user.getString("type").equals("warn")){
								if (guildOpts.getString("notifychannel") == null) {
									sendMessage(event.getGuild().getDefaultChannel(),
											"User " + event.getUser().getName().replaceAll("discord.gg", "") + "#"
													+ event.getUser().getDiscriminator() + "("
													+ event.getUser().getStringID() + ")"
													+ " has a warning in the GlobalBans System for "
													+ user.getString("reason"));
									return;
								}
								guildOpts.getString("notifychannel");
								sendMessage(event.getGuild().getChannelByID(Long.parseLong(guildOpts.getString("notifychannel"))),
										"User " + event.getUser().getName().replaceAll("discord.gg", "") + "#"
												+ event.getUser().getDiscriminator() + "("
												+ event.getUser().getStringID() + ")"
												+ " has a warning in the GlobalBans System for "
												+ user.getString("reason"));
								return;
							}
						} catch(@SuppressWarnings("unused") NullPointerException ex){
							// user isnt beaned, do nothing
						} catch(@SuppressWarnings("unused") MissingPermissionsException ex) {
							for(Channel i : event.getGuild().getChannels()) {
								if(i.getModifiedPermissions(event.getClient().getOurUser()).contains(Permission.SEND_MESSAGES)) {
									try {
										Message check = sendMessage(i, "Warning! Globalbans cannot send messages to the set notification channel! Please address this issue with b;setnotifychannel!");
										if(check != null) {
											return;
										}
									} catch(MissingPermissionsException e1) {
										logger.error("Unexpected permissions error in send message",e1);
										//still unable to send even when we have send permission, discord bug?
									} 
								}
							}
						}
					}
				} catch (Exception e) {
					logger.error("presumed critical error:", e);
					sendMessage(event.getClient().getApplicationOwner().getOrCreatePMChannel(),"CRITICAL: failed to load bans database!");
				}
		    	Guild guild = event.getGuild();
				if((guild.getUsers().stream().filter(u -> u.isBot()).collect(Collectors.toList()).size() * 100.0f) / guild.getUsers().size() > 55f){
					guild.leave();
					return;
				}
				if(event.getUser().getName().contains("discord.gg")) {
					try {
						Document bannedUser = Document.parse("{\"id\":\""+event.getUser().getStringID()+"\",\"type\":\"ban\",\"\":\"(AUTOBAN) Invite in username\",\"autoban\":true}");
						db.getCollection("bans").replaceOne(Document.parse("{\"id\":\""+event.getUser().getStringID()+"\"}"), bannedUser);
						List<Guild> guilds = event.getClient().getGuilds();
						int bannedUsers = 0;
						while(guilds.size() > 0){
							
							guild = guilds.get(0);
							if(guild.getChannels().size() < 1) guild.leave();
							Document guildOpts = db.getCollection("guilds").find(Document.parse("{\"id\":"+event.getGuild().block().getId().asString()+"}")).first();
							if(guildOpts == null) guildOpts = initGuild(event.getGuild().block());
							List<User> users = guild.getUsers();
							while(users.size() > 0){
								User user = users.get(0);
								if(user.getId().asString().equals(event.getUser().getStringID())){
									sendMessage(event.getClient().getChannelByID(
											Long.parseLong(guildOpts.getString("notifychannel"))), "User " + 
											event.getUser().getLongID() +" Was autobanned for: Invite in username.");
									bannedUsers++;
									if(guildOpts.getBoolean("warnonly", false)){
										justBanned.add(user.getId().asString());
										guild.banUser(user,"(AUTOBAN) Invite in username.", 1);
										new Thread(new Runnable() {

											@Override
											public void run() {
												try {
													Thread.sleep(5000);
												} catch (InterruptedException e) {
													e.printStackTrace();
												}
												justBanned.remove(user.getId().asString());
											}
											
										}).start();
									}
								}
								users.remove(0);
							}
							guilds.remove(0);
						}
						sendMessage(event.getClient().getChannelByID(356298368919797760l),"Autoban report: " + event.getUser().getStringID() + " was banned for: invite in username. \n"
								+ "Affected " + bannedUsers + " servers.");
					} catch (Exception e) {
						logger.error("presumed critical error:", e);
						sendMessage(event.getClient().getApplicationOwner().getOrCreatePMChannel(),"CRITICAL: failed to load bans database!");
					}
				}
		    }
		}; new Thread(r).start();
		
    }
    /* //TODO
    public void onUserLeftG(UserLeaveEvent event ){
    	Runnable r = new Runnable() {
    		@Override
			public void run() {
    			if((event.getGuild().getUsers().stream().filter(u -> u.isBot()).collect(Collectors.toList()).size() * 100.0f) / event.getGuild().getUsers().size() > 55f){
    				event.getGuild().leave();
    			}
    		}
    	}; new Thread(r).start();
    }*/
    
    private void voidMessage(MessageChannel channel,String message) {
    	channel.createMessage(message).subscribe();
    }
    
    private Message sendMessage(MessageChannel channel, String message){
        Message out;
        try{
        	out = channel.createMessage(message).block();
            return out;
        } catch (Exception e) {
            e.printStackTrace();
            out = null;
        }
        return out;
    }
    private void sendMessageError(TextChannel channel,MessageCreateEvent event, Exception e, boolean report){
        try{
			logger.error("Exception", e);      
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String sStackTrace = sw.toString(); 
            channel.createMessage("<:vpRedTick:319191455589728256> Looks like there was an error:```" + e + "```"
            		+ "\nThis error was automatically reported. No further action is required.").subscribe();
            if(report) {
            	channel.getClient().getUserById(Snowflake.of(153353572711530496l)).block().getPrivateChannel().block().createMessage("```" + sStackTrace.substring(0,Math.min(sStackTrace.length(), 1990)) + "```\n");
            	channel.getClient().getUserById(Snowflake.of(153353572711530496l)).block().getPrivateChannel().block()
                .createMessage("```Diagnostics Information:\n     Guild:" + event.getGuild().block().getName() + "(" + event.getGuildId() 
                		+ ")\n     Channel:" + channel.getName() + "(" + channel.getId().asString() 
                		+ ")\n     Message:" + event.getMessage().getContent() +  "(" + event.getMessage().getId().asString()
                		+ ")\n     Author:" + event.getMember().get().getUsername() + "(" + event.getMember().get().getId().asString() + ")\nEnd of diagnostics information```");
            }
            
        } catch (Exception e1){
            System.out.println(channel.getGuild().block().getName() + "," + channel.getName() +  ". msg error send fail:");
            logger.error("Exception", e1);
        }
    }
    public <T> T ifnull(T input,T ifnull) {
    	return (input != null ? input : ifnull);
    }
    
}

