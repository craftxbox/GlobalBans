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

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.bson.Document;
import org.ini4j.Wini;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.ReplaceOptions;

import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.guild.GuildCreateEvent;
import sx.blah.discord.handle.impl.events.guild.GuildLeaveEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageSendEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserJoinEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserLeaveEvent;
import sx.blah.discord.handle.obj.ActivityType;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IMessage.Attachment;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.handle.obj.StatusType;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.MissingPermissionsException;


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
	
	public static Boolean[] checkPerms(MessageReceivedEvent event, Permissions perm){
    	List<Boolean> out = new ArrayList<>();
    	if(event.getAuthor().getPermissionsForGuild(event.getGuild()).contains(perm)){
    		out.add(true);
    	} else{
    		out.add(false);
    	}
    	if(event.getClient().getOurUser().getPermissionsForGuild(event.getGuild()).contains(perm)){
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
	
    @EventSubscriber
    public void onReadyEvent(ReadyEvent event) {
    	sendMessage(event.getClient().getChannelByID(322462068475428864L), "b;kill " + instance);
    	event.getClient().changePresence(StatusType.ONLINE, ActivityType.PLAYING, "GlobalBans | Banning people from " + event.getClient().getGuilds().size() + " servers | b;help");
    	System.out.println(event.getClient().getGuilds().size());
    	Main.txtRunning.setText("Started");
    	Main.textField.setText(Integer.toString(event.getClient().getGuilds().size()));
    }
    @EventSubscriber
    public void onMessageReceive(MessageReceivedEvent event) {
    	Runnable r = new Runnable(){
    		@Override
			public void run(){
    			processCommand(event);
    		}
    	};
    	new Thread(r).start();
    }
    public void processCommand(MessageReceivedEvent event) {
    	String msg = event.getMessage().toString();
    	String cmd = msg.split(" ")[0];
    	String[] args = msg.split(" ");
    	Wini help;
    	if(lastActionTime.get(event.getAuthor().getLongID()) == null) {
    		lastActionTime.put(event.getAuthor().getLongID(), 0l);
    	}
    	if(userMisuses.get(event.getAuthor().getLongID()) == null) {
    		userMisuses.put(event.getAuthor().getLongID(), 0);
    	}
    	
    	if(event.getAuthor().isBot()){
			return;
		}
    	if(msg.toLowerCase().startsWith("b;")){
    		if(!commands.contains(cmd)){
        		//sendRespondMessage(event.getChannel(), "DEBUG: invalid command", event.getAuthor());
    			return;
    		}

    		if(blacklisted.contains(event.getAuthor().getStringID())){
    			return;
    		}
			if(cmd.equalsIgnoreCase("b;about")){
				sendRespondMessage(event.getChannel(), "<:vpGreenTick:319191454331437058> GlobalBans, A database of rulebreakers. \nSupport: https://discord.gg/jcYq5ch", event.getAuthor());
			}
			if(cmd.equalsIgnoreCase("b;help")){
				if(msg.split(" ").length > 1){
					try {
						help = new Wini(new File("help.ini"));
						sendRespondMessage(event.getChannel(), help.get("commands", msg.split(" ")[1].replace("b;", "")), event.getAuthor());
					} catch (FileNotFoundException e) {
						File inicheck = new File("help.ini");
						try {
							inicheck.createNewFile();
							help = new Wini(new File("help.ini"));
							sendRespondMessage(event.getChannel(), help.get("commands", msg.split(" ")[1].replace("b;", "")), event.getAuthor());
						} catch (Exception e1) {
							sendMessageError(event.getChannel(), event, e1,true);
						}
						sendMessageError(event.getChannel(), event, e,true);
					} catch(Exception e){
						sendMessageError(event.getChannel(), event, e,true);
					} finally{
						
					}
				}
				else{
					sendRespondMessage(event.getChannel(), "<:vpGreenTick:319191454331437058> Commands:\n```"+ Arrays.toString(visible).replace("[", "").replace("]", "")+"```\nUsage: b;help <command>", event.getAuthor());
				}
				
			}
			if(cmd.equalsIgnoreCase("b;invite")){
				sendRespondMessage(event.getChannel(), "<:vpGreenTick:319191454331437058> Invite me with https://discordapp.com/oauth2/authorize?client_id=" + event.getClient().getApplicationClientID() +"&scope=bot&permissions=70757577 !", event.getAuthor());
			}
			if(cmd.equalsIgnoreCase("b;legal")){
				sendMessage(event.getChannel(), "By having this bot in your discord server you agree that "
						+ "\n(i) usage of this bot may be terminated at any time without warning"
						+ "\n(ii) bot usage, responses and errors may be logged"
						+ "\n(iii) any data sent to the bot may be indefinately stored"
						+ "\n(iv) staff of this bot may look through your discord guild to gather evidence on reports"
						+ "\nNo warranty is provided for using this bot and I(craftxbox) disclaim responsibility for any damages that are caused as a result of use of this bot");
			}
			if(cmd.equalsIgnoreCase("b;userinfo")){
				IUser mentioned;
				if(event.getMessage().getMentions().size() < 1) {
					try {
						mentioned = event.getClient().fetchUser(Long.parseLong(msg.substring(("b;userinfo").length()).split(" ")[1]));
					}
					catch(Exception e) {
						e.printStackTrace();
						sendMessage(event.getChannel(),"<:vpRedTick:319191455589728256> User doesnt exist! Usage: c;userinfo <mention or id>");
						return;
					}
				} else {
					mentioned = event.getMessage().getMentions().get(0);
				}
				MongoCollection<Document> bans = db.getCollection("bans");
				String nick = ifnull(mentioned.getDisplayName(event.getGuild()),"N/A");	
				Instant joinTime;
				try {
					joinTime = event.getGuild().getJoinTimeForUser(mentioned);
				} catch(@SuppressWarnings("unused") Exception e) {
					joinTime = Instant.EPOCH;
				}
				LocalDateTime userJoined = LocalDateTime.ofInstant(joinTime,ZoneId.of("UTC"));
				LocalDateTime userCreated = LocalDateTime.ofInstant(mentioned.getCreationDate(), ZoneId.of("UTC"));
				String formattedJoin = userJoined.getHour() + ":" + userJoined.getMinute() 
						+ " " +userJoined.getDayOfMonth()+"/"+userJoined.getMonthValue() + "/" +
						userJoined.getYear();
				String formattedCreate = userCreated.getHour() + ":" + userCreated.getMinute() 
						+ " " +userCreated.getDayOfMonth()+"/"+userCreated.getMonthValue() + "/" +
						userCreated.getYear();
				EmbedBuilder em = new EmbedBuilder();
				em.withAuthorName(mentioned.getName());
				em.withAuthorIcon(mentioned.getAvatarURL()); 
				try{
					em.withColor(
							mentioned.getRolesForGuild(event.getGuild()).get(0).getColor().getRed(),
							mentioned.getRolesForGuild(event.getGuild()).get(0).getColor().getGreen(), 
							mentioned.getRolesForGuild(event.getGuild()).get(0).getColor().getBlue()
						);
				} catch(@SuppressWarnings("unused") Exception e) {
					//doesnt have one of those colors
				}
				em.appendField("Nickname:", nick, true);
				em.appendField("Discriminator:", mentioned.getDiscriminator(), true);
				em.appendField("Date Registered:", formattedCreate.toString(), true); 
				if(userJoined.getYear() > 2014) em.appendField("Date Joined:", formattedJoin, true);
				em.appendField("Is Bot:", Boolean.toString(mentioned.isBot()), true);
				em.appendField("Status:", mentioned.getPresence().getStatus().toString(), true);
				em.appendField("ID:",mentioned.getStringID(),true);
				if(bans.find(Document.parse("{\"id\":"+event.getGuild().getStringID()+"}")).first() != null) {
					if(bans.find(Document.parse("{\"id\":"+event.getGuild().getStringID()+"}")).first().getString("type").equals("ban")) {
						em.appendField("GlobalBans Listed","Banned", true);
					}
					else if(bans.find(Document.parse("{\"id\":"+event.getGuild().getStringID()+"}")).first().getString("type").equals("ban")) {
						em.appendField("GlobalBans Listed","Warned", true);
					}
				}
				em.appendField("Roles:", ifnull(mentioned.getRolesForGuild(event.getGuild()).toString(),"Not in server."), true);
				sendMessage(event.getChannel(), em.build());
			}

    		if(cmd.equalsIgnoreCase("b;ping")){
    			sendMessage(event.getChannel(), "<:vpGreenTick:319191454331437058> " +  event.getGuild().getShard().getResponseTime() + "ms");
    		}
    		if(cmd.equalsIgnoreCase("b;recount") && event.getAuthor().getLongID() == 153353572711530496L){
    			event.getClient().changePresence(StatusType.ONLINE, ActivityType.PLAYING, "GlobalBans | Banning people from " + event.getClient().getGuilds().size() + " servers | b;help");
    	    	System.out.println("Recounted: " + event.getClient().getGuilds().size());
    		}
    		if(cmd.equalsIgnoreCase("b;leave") && event.getAuthor().getLongID() == 153353572711530496L){
    			event.getClient().getGuildByID(Long.parseLong(msg.split(" ")[1])).leave();
    		}
    		if(cmd.equalsIgnoreCase("b;getGuild") && event.getAuthor().getLongID() == 153353572711530496L){
    			IGuild guild = event.getClient().getGuildByID(Long.parseLong(msg.split(" ")[1]));
				EmbedBuilder em = new EmbedBuilder();
				em.withAuthorIcon(guild.getIconURL()); 
				em.withAuthorName(guild.getName());
				em.appendField("Bots", Integer.toString(guild.getUsers().stream().filter(u -> u.isBot()).collect(Collectors.toList()).size()), true);
				em.appendField("Humans", Integer.toString(guild.getUsers().stream().filter(u -> !u.isBot()).collect(Collectors.toList()).size()), true);
				em.appendField("Total Users", Integer.toString(guild.getUsers().size()), true);
				em.appendField("Bot Percentage", "%" + Float.toString((guild.getUsers().stream().filter(u -> u.isBot()).collect(Collectors.toList()).size() * 100.0f) / guild.getUsers().size()), true );
				em.appendField("Owner", guild.getOwner().getName() + "#" + guild.getOwner().getDiscriminator(), true);
    			sendMessage(event.getChannel(), em.build());
    		}
    		if(cmd.equalsIgnoreCase("b;report")){
    			if(lastActionTime.get(event.getAuthor().getLongID()) + 60000 < System.currentTimeMillis()) {
	    			if(msg.split(" ").length > 2){
	    				if(checkPerms(event,Permissions.KICK)[0] == true){
	    					if(event.getMessage().getMentions().size() > 0) {
	    						sendMessage(event.getClient().getChannelByID(356298368919797760L), "Report from: " + event.getAuthor().getStringID() 
	    								+ "\nGuild: " + event.getGuild().getStringID()
	    								+ "\nChannel: " + event.getChannel().getStringID()
		    							+ "\nReported user: " + msg.split(" ")[1] 
		        						+ "\nReason: " +msg.substring(msg.split(" ")[1].length() + 2 + msg.split(" ")[0].length()  ));
		    					for(Attachment i : event.getMessage().getAttachments()) {
		    						try {
		    							HttpURLConnection urlconn = (HttpURLConnection)new URL(i.getUrl()).openConnection();
		    							urlconn.setRequestProperty ("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/40.0.2214.85 Safari/537.36");
		    							urlconn.setRequestMethod("GET");
		    							InputStream istream = urlconn.getInputStream();
										event.getClient().getChannelByID(356298368919797760L).sendFile("", istream,i.getFilename());
										Thread.sleep(200);
									} catch (IOException e) {
										sendMessageError(event.getChannel(), event, e,true);
									} catch (InterruptedException e) {
										sendMessageError(event.getChannel(), event, e,true);
									}
		    					}
		    					sendMessage(event.getChannel(), "Successfully reported user " + event.getMessage().getMentions().get(0).getStringID() + ". Thank you.");
		    					lastActionTime.put(event.getAuthor().getLongID(), System.currentTimeMillis());
	    					} else {
	    						try {
	    							Long.parseLong(msg.split(" ")[1]);
	    						} catch(@SuppressWarnings("unused") NumberFormatException e) {
	    							sendRespondMessage(event.getChannel(),"Please enter a valid id or mention!",event.getAuthor());
	    						}
	    						if(event.getClient().fetchUser(Long.parseLong(msg.split(" ")[1])) == null) {
	    							sendRespondMessage(event.getChannel(),"User doesnt exist!", event.getAuthor());
	    							return;
	    						}
	    						sendMessage(event.getClient().getChannelByID(356298368919797760L), "Report from:" + event.getAuthor().getStringID() 
	    								+ "\nGuild: " + event.getGuild().getStringID()
	    								+ "\nChannel: " + event.getChannel().getStringID()
		    							+ "\nReported user: " + msg.split(" ")[1] 
		        						+ "\nReason:" +msg.substring(msg.split(" ")[1].length() + 2 + msg.split(" ")[0].length()  ));
		    					for(Attachment i : event.getMessage().getAttachments()) {
		    						try {
										event.getClient().getChannelByID(356298368919797760L).sendFile("", new URL(i.getUrl()).openStream(),i.getFilename());
										Thread.sleep(200);
									} catch (IOException e) {
										sendMessageError(event.getChannel(), event, e,true);
									} catch (InterruptedException e) {
										sendMessageError(event.getChannel(), event, e,true);
									}
		    					}
		    					sendMessage(event.getChannel(), "Successfully reported user " + msg.split(" ")[1] + ". Thank you.");
		    					lastActionTime.put(event.getAuthor().getLongID(), System.currentTimeMillis());
	    					}
	    				} else {
	    					sendRespondMessage(event.getChannel(),"You do not have permission!",event.getAuthor());
	    				}
	    			} else {
	    				sendRespondMessage(event.getChannel(),"Not enough arguments! Usage: b;report <@ mention or id> <reason and proof>", event.getAuthor());
	    			}
    			} else {
    				if(userMisuses.get(event.getAuthor().getLongID()) > 0) {
    					if(userMisuses.get(event.getAuthor().getLongID()) > 5) {
    						sendRespondMessage(event.getChannel(),"You have been Auto-Blacklisted for exceeding maximum misuses. This will expire in 2 hours",event.getAuthor());
    						Runnable r = new Runnable() {
    							@Override
								public void run() {
    								blacklisted.add(event.getAuthor().getStringID());
    								try {
										Thread.sleep(7200000);
									} catch (Exception e) {
										sendMessageError(event.getChannel(),event,e,true);
									}
    							}
    						};
    						new Thread(r).start();
    					} else {
    						sendRespondMessage(event.getChannel(),"You are being ratelimited! You are at " + userMisuses.get(event.getAuthor().getLongID()) + " out of 5 misuses!",event.getAuthor());
    						userMisuses.put(event.getAuthor().getLongID(), userMisuses.get(event.getAuthor().getLongID()) + 1);
    					}
    				} else {
    					sendRespondMessage(event.getChannel(),"You are being ratelimited!",event.getAuthor());
    					userMisuses.put(event.getAuthor().getLongID(), 1);
    				}
    				lastActionTime.put(event.getAuthor().getLongID(), System.currentTimeMillis());
    			}
    		}
			if(cmd.equalsIgnoreCase("b;whitelist")){
				if(msg.split(" ").length > 1){
					IUser toWhitelist = null;
					try {
						toWhitelist = event.getClient().fetchUser(Long.parseLong(msg.replaceAll("<>@!", "").split(" ")[1]));
					} catch (@SuppressWarnings("unused") NumberFormatException e1) {
						sendRespondMessage(event.getChannel(), "This user doesnt exist!", event.getAuthor());
						return;
					}
					if(toWhitelist == null){
						sendRespondMessage(event.getChannel(), "This user doesnt exist!", event.getAuthor());
						return;
					}
					try{
						if(db.getCollection("bans").find(Document.parse("{\"id\":\""+args[1]+"\"}")).first() != null){
							if(checkPerms(event,Permissions.KICK)[0] == true){
								if(db.getCollection("bans").find(Document.parse("{\"id\":\""+args[1]+"\"}")).first().getString("type").equals("ban")){
									Document guildOpts = db.getCollection("guilds").find(Document.parse("{\"id\":"+event.getGuild().getStringID()+"}")).first();
									if(guildOpts == null) guildOpts = initGuild(event.getGuild());
									if(guildOpts.containsKey("whitelist")) {
										@SuppressWarnings("unchecked")
										ArrayList<String> whitelist = (ArrayList<String>) guildOpts.get("whitelist");
										whitelist.add(args[1]);
										guildOpts.put("whitelist", whitelist);
									} else {
										guildOpts.put("whitelist", Arrays.asList(new String[]{args[1]}));
									}
									sendMessage(event.getChannel(), "Successfully whitelisted " + msg.split(" ")[1]);
									return;
								}
								sendRespondMessage(event.getChannel(), "This user isnt banned in the GlobalBan System!", event.getAuthor());
								return;
							}
							sendRespondMessage(event.getChannel(), "You dont have the permission to do this! (missing kick permissions)", event.getAuthor());
							return;
						}
						sendRespondMessage(event.getChannel(), "This user isnt banned in the GlobalBan System!", event.getAuthor());
						return;
					}catch(NullPointerException e){
						sendMessageError(event.getChannel(),event,e,true);
						return;
					}
				}
				sendRespondMessage(event.getChannel(), "Usage: b;whitelist <user id>", event.getAuthor());
				return;
			}
			if(cmd.equalsIgnoreCase("b;setnotifychannel")){
				if(checkPerms(event,Permissions.MANAGE_CHANNELS)[0]  || event.getAuthor().getStringID().equals("153353572711530496")){
					Document guildOpts = db.getCollection("guilds").find(Document.parse("{\"id\":"+event.getGuild().getStringID()+"}")).first();
					if(guildOpts == null) guildOpts = initGuild(event.getGuild());
					try {
						if(event.getGuild().getChannelByID(Long.parseLong(args[1].replaceAll("[^\\d]",""))) == null) {
							guildOpts.put("notifychannel", Long.toString(event.getChannel().getLongID()));
						} else {
							guildOpts.put("notifychannel",args[1].replaceAll("[^\\d]",""));
						}
					} catch (@SuppressWarnings("unused") NumberFormatException e) {
						sendMessage(event.getChannel(),"That isnt a channel! Got:"+ args[1]);
						return;
					} catch (@SuppressWarnings("unused") IndexOutOfBoundsException e) {
						guildOpts.put("notifychannel", Long.toString(event.getChannel().getLongID()));
					}
				    db.getCollection("guilds").replaceOne(Document.parse("{\"id\":"+event.getGuild().getStringID()+"}"), guildOpts);
				    sendMessage(event.getChannel(), "Successfully set the Notification Channel to " + event.getGuild().getChannelByID(Long.parseLong(guildOpts.getString("notifychannel"))));
				    return;
				}
				sendMessage(event.getChannel(), "You do not have permission! (missing manage channels)");
				return;
			}
			if(cmd.equalsIgnoreCase("b;togglewarnonly")){
				if(checkPerms(event,Permissions.MANAGE_CHANNELS)[0] || event.getAuthor().getStringID().equals("153353572711530496")){
					Document guildOpts = db.getCollection("guilds").find(Document.parse("{\"id\":"+event.getGuild().getStringID()+"}")).first();
					if(guildOpts == null) guildOpts = initGuild(event.getGuild());
					if(!guildOpts.getBoolean("warnonly", false)) {
						guildOpts.put("warnonly", true);
					    sendMessage(event.getChannel(), "Successfully set the bot to Warn Only Mode");
					} else {
						guildOpts.put("warnonly", false);
					    sendMessage(event.getChannel(), "Successfully set the bot to Ban Mode");
					}
					db.getCollection("guilds").replaceOne(Document.parse("{\"id\":"+event.getGuild().getStringID()+"}"), guildOpts);
				}
				else {
					sendMessage(event.getChannel(), "You do not have permission! (missing manage channels)");
					return;
				}
			}
			
			if(cmd.equalsIgnoreCase("b;ban")){
				if(event.getAuthor().getStringID().equals("153353572711530496")) {
					try {
						Document bannedUser = Document.parse("{\"id\":\""+args[1]+"\",\"type\":\"ban\",\"reason\":\""+msg.split(args[1]+" ")[1]+"\"}");
						db.getCollection("bans").replaceOne(Document.parse("{\"id\":\""+args[1]+"\"}"),bannedUser, new ReplaceOptions().upsert(true));
						List<IGuild> guilds = event.getClient().getGuilds();
						List<Long> bannedUsers = new ArrayList<>();
						while(guilds.size() > 0){
							
							IGuild guild = guilds.get(0);
							Document guildOpts = db.getCollection("guilds").find(Document.parse("{\"id\":"+event.getGuild().getStringID()+"}")).first();
							if(guildOpts == null) guildOpts = initGuild(event.getGuild());
							List<IUser> users = guild.getUsers();
							while(users.size() > 0){
								IUser user = users.get(0);
								if(user.getStringID().equals(msg.split(" ")[1])){
									sendMessage(event.getClient().getChannelByID(
											Long.parseLong(guildOpts.getString("notifychannel"))), "User " + 
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
						sendMessage(event.getChannel(),"Ban affected " + bannedUsers.size() + " Guilds");
						sendMessage(event.getClient().getChannelByID(526097580539641866l),event.getAuthor().getName() + " Banned " + msg.split(" ")[1] + " For " + msg.split(msg.split(" ")[1])[1] + "\n" + bannedUsers.toString());
					} catch (Exception e ) {
						sendMessageError(event.getChannel(),event,e,true);
					}
				}
				else {
					IUser user = event.getAuthor();
					if(checkPerms(event,Permissions.BAN)[2] == true){
	    				try{
	    					msg.split(" ")[2].length();
	    					if(event.getMessage().getMentions().size() > 0) {
	    						event.getGuild().banUser(event.getMessage().getMentions().get(0),"[" + user.getName() + "#" + user.getDiscriminator() + "] "+ msg.split("<@!?\\d{17,18}>")[1]);
	    						sendMessage(event.getChannel(),"Banned user.");
	    					}
	    					else {
	    						event.getGuild().banUser(event.getClient().fetchUser(Long.parseLong(msg.split(" ")[1])),"[" + user.getName() + "#" + user.getDiscriminator() + "] " + msg.split("\\d{17,18}")[1]);
	    						sendMessage(event.getChannel(),"Banned user.");
	    					}
	    				} catch(@SuppressWarnings("unused") MissingPermissionsException e) {
	    					sendMessage(event.getChannel(),"GlobalBans does not have permissions to ban users above it or equal to it in the heirarchy!");
	    				} catch(@SuppressWarnings("unused") ArrayIndexOutOfBoundsException e){
	    					if(event.getMessage().getMentions().size() > 0) {
	    						event.getGuild().banUser(event.getMessage().getMentions().get(0),"[" + user.getName() + "#" + user.getDiscriminator() + "] ");
	    						sendMessage(event.getChannel(),"Banned user.");
	    					}
	    					else {
	    						event.getGuild().banUser(event.getClient().fetchUser(Long.parseLong(msg.split(" ")[1])),"[" + user.getName() + "#" + user.getDiscriminator() + "] ");
	    						sendMessage(event.getChannel(),"Banned user.");
	    					}
	    				}
	    			} else if(checkPerms(event,Permissions.BAN)[0] == false){
	    				sendMessage(event.getChannel(), "You do not have permissions to ban!");
	    			} else if(checkPerms(event,Permissions.BAN)[1] == false){
	    				sendMessage(event.getChannel(), "GlobalBans does not have permissions to ban!");
	    			}
				}
			}
			if(cmd.equalsIgnoreCase("b;kick")) {
				IUser user = event.getAuthor();
				if(checkPerms(event,Permissions.KICK)[2] == true){
    				try{
    					msg.split(" ")[2].length();
    					if(event.getMessage().getMentions().size() > 0) {
    						event.getGuild().kickUser(event.getMessage().getMentions().get(0),"[" + user.getName() + "#" + user.getDiscriminator() + "] " + msg.split("<@!?\\d{17,18}>")[1]);
    						sendMessage(event.getChannel(),"Kicked user.");
    					}
    					else {
    						event.getGuild().kickUser(event.getClient().fetchUser(Long.parseLong(msg.split(" ")[1])),"[" + user.getName() + "#" + user.getDiscriminator() + "] " +msg.split("\\d{17,18}")[1]);
    						sendMessage(event.getChannel(),"Kicked user.");
    					}
    				} catch(@SuppressWarnings("unused") MissingPermissionsException e) {
    					sendMessage(event.getChannel(),"GlobalBans does not have permissions to kick users above it or equal to it in the heirarchy!");
    				} catch(@SuppressWarnings("unused") ArrayIndexOutOfBoundsException e){
    					if(event.getMessage().getMentions().size() > 0) {
    						event.getGuild().kickUser(event.getMessage().getMentions().get(0),"[" + user.getName() + "#" + user.getDiscriminator() + "] ");
    						sendMessage(event.getChannel(),"Kicked user.");
    					}
    					else {
    						event.getGuild().kickUser(event.getClient().fetchUser(Long.parseLong(msg.split(" ")[1])),"[" + user.getName() + "#" + user.getDiscriminator() + "] ");
    						sendMessage(event.getChannel(),"Kicked user.");
    					}
    				}
    			} else if(checkPerms(event,Permissions.KICK)[0] == false){
    				sendMessage(event.getChannel(), "You do not have permissions to kick!");
    			} else if(checkPerms(event,Permissions.KICK)[1] == false){
    				sendMessage(event.getChannel(), "GlobalBans does not have permissions to kick!");
    			}
			}
			if(cmd.equalsIgnoreCase("b;warn") && event.getAuthor().getStringID().equals("153353572711530496")){
				try {
					Document bannedUser = Document.parse("{\"id\":\""+args[1]+"\",\"type\":\"warn\",\"\":\""+msg.split(args[1]+" ")[1]+"\"}");
					db.getCollection("bans").replaceOne(Document.parse(""),bannedUser,new ReplaceOptions().upsert(true));
					List<Long> affected = new ArrayList<>();
					for(Object gl : event.getClient().getGuilds().toArray()){
						
						IGuild guild = (IGuild) gl;
						Document guildOpts = db.getCollection("guilds").find(Document.parse("{\"id\":"+event.getGuild().getStringID()+"}")).first();
						if(guildOpts == null) guildOpts = initGuild(event.getGuild());
						for(Object ul : guild.getUsers()){
							IUser user = (IUser) ul;
							if(user.getStringID().equals(msg.split(" ")[1])){
								IChannel channel = guild.getChannelByID(Long.parseLong(guildOpts.getString("notifychannel")));
								sendMessage(channel, "User " + user.getName() + "#" + user.getDiscriminator() + " has a warning in the GlobalBans system for "
								+ msg.split("\\d{17,18}")[1]);
								affected.add(user.getLongID());
							}
						}
					}
					sendMessage(event.getChannel(), "Warn affected " + affected.size() + " servers.");
					sendMessage(event.getClient().getChannelByID(526097580539641866l),event.getAuthor().getName() + " Warned " + msg.split(" ")[1] + " For " + msg.split(msg.split(" ")[1])[1] + "\n" + affected.toString());
					
				} catch (Exception e ) {
					sendMessageError(event.getChannel(),event,e,true);
					sendMessage(event.getClient().getApplicationOwner().getOrCreatePMChannel(),"CRITICAL: failed to load bans database!");
				}
			}
			if(cmd.equalsIgnoreCase("b;eval") && event.getAuthor().getLongID() == 153353572711530496L){
    			ScriptEngineManager factory = new ScriptEngineManager();
    			ScriptEngine engine = factory.getEngineByName("JavaScript");
    			engine.put("event", event);
    			new Thread() {
				    @Override
					public void run() {
				    	try {
		    				if(event.getMessage().getContent().toLowerCase().contains(".gettoken()")){
		    					sendMessage(event.getChannel(),"lol no");
		    					return;
		    				}
		    				engine.put("Long", Long.class);
		    				IMessage m = sendMessage(event.getChannel(),"Evaluating");
		    				
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
		    			    	event.getChannel().sendFile(new File("lasteval.txt"));
	    			    		m.delete();
		    			    }
		    			    else
		    			    	m.edit( "```[" + object.getClass() +"] " + object.toString() + "```");
		    			} catch (Exception ex) {
		    			     sendMessageError(event.getChannel(), event,ex,false);
		    			    
		    			}
				    }  
				}.start();
    		}
			if(cmd.equalsIgnoreCase("b;cullfarms") && event.getAuthor().getLongID() == 153353572711530496l){
				Runnable r = new Runnable(){
					@Override
					public void run(){
						List<String> out = new ArrayList<>();
						for(IGuild i : event.getClient().getGuilds()){
							out.add(i.getName().substring(0, 1));
						}
						String.join("", out);
						int culled = 0;
						for(IGuild guild : event.getClient().getGuilds()){
							if((guild.getUsers().stream().filter(u -> u.isBot()).collect(Collectors.toList()).size() * 100.0f) / guild.getUsers().size() > 55f){
			    				guild.leave();
			    				culled++;
			    			}
						}
						sendMessage(event.getChannel(),"Culled " + culled + " Botfarms");
					}
				};
				new Thread(r).start();
			}
			if(cmd.equalsIgnoreCase("b;deny")  && event.getAuthor().getLongID() == 153353572711530496l){
				if((args.length >= 3)) {
					IUser user = event.getClient().fetchUser(Long.parseLong(args[2]));
					sendMessage(event.getClient().getChannelByID(Long.parseLong(args[1])),
							"Hello! Your report for " + user.getName() + "#" + user.getDiscriminator() + "(" + args[2] + ") has been declined for:\n`" +
							msg.split(args[2])[1]+"`");
				}
			}
			if(cmd.equalsIgnoreCase("b;update")  && event.getAuthor().getLongID() == 153353572711530496l){
				if((args.length >= 3)) {
					IUser user = event.getClient().fetchUser(Long.parseLong(args[2]));
					sendMessage(event.getClient().getChannelByID(Long.parseLong(args[1])),
							"Hello! Your report for " + user.getName() + "#" + user.getDiscriminator() + "(" + args[2] + ") has been updated:\n`" +
							msg.split(args[2])[1]+"`");
				}
			}
			if(cmd.equalsIgnoreCase("b;accept")  && event.getAuthor().getLongID() == 153353572711530496l){
				if((args.length >= 3)) {
					IUser user = event.getClient().fetchUser(Long.parseLong(args[2]));
					sendMessage(event.getClient().getChannelByID(Long.parseLong(args[1])),
							"Hello! Your report for " + user.getName() + "#" + user.getDiscriminator() + "(" + args[2] + ") has been accepted. Thank you for helping to make discord a better place");
				}
			}
    	}
    }
    private Document initGuild(IGuild iGuild) {
    	Document guild = db.getCollection("guilds").find(Document.parse("{\"id\":\""+iGuild.getStringID()+"\"}")).first();
    	if(guild == null) {
    		guild = Document.parse(("{'id':'"+iGuild.getStringID()+"','notifychannel':'"+iGuild.getDefaultChannel().getStringID()+"','warnonly':false}").replaceAll("'", "\""));
    		db.getCollection("guilds").replaceOne(Document.parse("{\"id\":\""+iGuild.getStringID()+"\"}"), guild, new ReplaceOptions().upsert(true));
    		return guild;
    	}
		return guild;
	}

	@EventSubscriber
    public void onMessageSent(MessageSendEvent event){
    	String msg = event.getMessage().toString();
		if(msg.split(" ")[0].equalsIgnoreCase("b;kill")){
			if(msg.split(" ")[1].equals(instance)){
				System.out.println("Ignoring kill from my own instance, " + instance);
				
			}
			else{
				sendMessage(event.getChannel(), instance + " shutting down :ok_hand:");
				System.exit(0);
			}
		}
    }
    @EventSubscriber
    public void onJoin(GuildCreateEvent event) throws Exception{
    	if(event.getClient().isReady()){
    		Runnable r = new Runnable() {
    				@Override
					public void run() {
			    		try {
			    			if(db.getCollection("blacklist").find(Document.parse("{}")).first() != null) {
			    				sendMessage(event.getGuild().getDefaultChannel(),"This server has been banned from using GlobalBans.\nIf you believe this was in error, please contact craftxbox at <https://discord.gg/jcYq5ch>");
			        			event.getGuild().leave();
			        		}
			    			Document guildOpts = db.getCollection("guilds").find(Document.parse("{\"id\":"+event.getGuild().getStringID()+"}")).first();
							if(guildOpts == null){
								IGuild guild = event.getGuild();
								initGuild(guild);
								if((guild.getUsers().stream().filter(u -> u.isBot()).collect(Collectors.toList()).size() * 100.0f) / guild.getUsers().size() > 55f){
			        				guild.leave();
			        				return;
			        			}
								EmbedBuilder em = new EmbedBuilder();
								em.withAuthorIcon(guild.getIconURL()); 
								em.withAuthorName(guild.getName());
								em.appendField("Bots", Integer.toString(guild.getUsers().stream().filter(u -> u.isBot()).collect(Collectors.toList()).size()), true);
								em.appendField("Humans", Integer.toString(guild.getUsers().stream().filter(u -> !u.isBot()).collect(Collectors.toList()).size()), true);
								em.appendField("Total Users", Integer.toString(guild.getUsers().size()), true);
								em.appendField("Bot Percentage", "%" + Float.toString((guild.getUsers().stream().filter(u -> u.isBot()).collect(Collectors.toList()).size() * 100.0f) / guild.getUsers().size()), true );
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
    @EventSubscriber
    public void onLeave(GuildLeaveEvent event){
    	Runnable r = new Runnable(){
			@Override
			public void run(){
		    	if(event.getClient().isReady()){
		    		event.getClient().changePresence(StatusType.ONLINE, ActivityType.PLAYING, "GlobalBans | Banning people from " + event.getClient().getGuilds().size() + " servers | b;help");
		        	System.out.println(event.getClient().getGuilds().size());
		        	Main.textField.setText(Integer.toString(event.getClient().getGuilds().size()));
					IGuild guild = event.getGuild();
					EmbedBuilder em = new EmbedBuilder();
					em.withAuthorIcon(guild.getIconURL()); 
					em.withAuthorName(guild.getName());
					em.appendField("Bots", Integer.toString(guild.getUsers().stream().filter(u -> u.isBot()).collect(Collectors.toList()).size()), true);
					em.appendField("Humans", Integer.toString(guild.getUsers().stream().filter(u -> !u.isBot()).collect(Collectors.toList()).size()), true);
					em.appendField("Total Users", Integer.toString(guild.getUsers().size()), true);
					em.appendField("Bot Percentage", "%" + Float.toString((guild.getUsers().stream().filter(u -> u.isBot()).collect(Collectors.toList()).size() * 100.0f) / guild.getUsers().size()), true );
					em.withColor(Color.red);
					em.appendField("ID", Long.toString(guild.getLongID()), true);
					db.getCollection("guilds").deleteOne(Document.parse("{\"id\":"+event.getGuild().getStringID()+"}"));
					sendMessage(event.getClient().getChannelByID(330218133707292672L), em.build());
		    	}
			}
		}; new Thread(r).start();
    }
    @EventSubscriber
    public void onUserJoin(UserJoinEvent event) {
    	Runnable r = new Runnable(){
			@Override
			public void run(){
		    	try {
		    		Document guildOpts = db.getCollection("guilds").find(Document.parse("{\"id\":"+event.getGuild().getStringID()+"}")).first();
					if(guildOpts == null) guildOpts = initGuild(event.getGuild());
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
							for(IChannel i : event.getGuild().getChannels()) {
								if(i.getModifiedPermissions(event.getClient().getOurUser()).contains(Permissions.SEND_MESSAGES)) {
									try {
										IMessage check = sendMessage(i, "Warning! Globalbans cannot send messages to the set notification channel! Please address this issue with b;setnotifychannel!");
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
		    	IGuild guild = event.getGuild();
				if((guild.getUsers().stream().filter(u -> u.isBot()).collect(Collectors.toList()).size() * 100.0f) / guild.getUsers().size() > 55f){
					guild.leave();
					return;
				}
				if(event.getUser().getName().contains("discord.gg")) {
					try {
						Document bannedUser = Document.parse("{\"id\":\""+event.getUser().getStringID()+"\",\"type\":\"ban\",\"\":\"(AUTOBAN) Invite in username\",\"autoban\":true}");
						db.getCollection("bans").replaceOne(Document.parse("{\"id\":\""+event.getUser().getStringID()+"\"}"), bannedUser);
						List<IGuild> guilds = event.getClient().getGuilds();
						int bannedUsers = 0;
						while(guilds.size() > 0){
							
							guild = guilds.get(0);
							if(guild.getChannels().size() < 1) guild.leave();
							Document guildOpts = db.getCollection("guilds").find(Document.parse("{\"id\":"+event.getGuild().getStringID()+"}")).first();
							if(guildOpts == null) guildOpts = initGuild(event.getGuild());
							List<IUser> users = guild.getUsers();
							while(users.size() > 0){
								IUser user = users.get(0);
								if(user.getStringID().equals(event.getUser().getStringID())){
									try {
										sendMessage(event.getClient().getChannelByID(
												Long.parseLong(guildOpts.getString("notifychannel"))), "User " + 
												event.getUser().getLongID() +" Was autobanned for: Invite in username.");
									} catch (@SuppressWarnings("unused") MissingPermissionsException e) {
										for(IChannel i : event.getGuild().getChannels()) {
											if(i.getModifiedPermissions(event.getClient().getOurUser()).contains(Permissions.SEND_MESSAGES)) {
												try {
													IMessage check = sendMessage(i, "Warning! Globalbans cannot send messages to the set notification channel! Please address this issue with b;setnotifychannel!");
													if(check != null) {
														break;
													}
												} catch(MissingPermissionsException e1) {
													logger.error("Unexpected permissions error in send message",e1);
													//still unable to send even when we have send permission, discord bug?
												} 
											}
										}
									}
											bannedUsers++;
									if(guildOpts.getBoolean("warnonly", false)){
										justBanned.add(user.getStringID());
										guild.banUser(user,"(AUTOBAN) Invite in username.", 1);
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
    @EventSubscriber
    public void onUserLeftG(UserLeaveEvent event ){
    	Runnable r = new Runnable() {
    		@Override
			public void run() {
    			if((event.getGuild().getUsers().stream().filter(u -> u.isBot()).collect(Collectors.toList()).size() * 100.0f) / event.getGuild().getUsers().size() > 55f){
    				event.getGuild().leave();
    			}
    		}
    	}; new Thread(r).start();
    }
    
    private IMessage sendMessage(IChannel channel, String message) throws MissingPermissionsException{
        IMessage out;
        try{
        	out = channel.sendMessage(message);
            return out;
        } catch(MissingPermissionsException e) {
        	throw e;
        } catch (Exception e) {
        	System.err.println(channel.getGuild().getName() + "," + channel.getName() +  ". msg send fail:");
            e.printStackTrace();
            out = null;
        }
        return out;
    }
    private IMessage sendMessage(IChannel channel, EmbedObject em)  throws MissingPermissionsException{
    	IMessage out;
	    try{
	    	out = channel.sendMessage(em);
        	return out;
	    } catch(MissingPermissionsException e) {
	    	throw e;
	    }
	    catch (Exception e){
	    	System.err.println(channel.getGuild().getName() + "," + channel.getName() +  ". msg send fail:");
	    	e.printStackTrace();
	    	out = null;
	    }
	    return out;
    }
    private IMessage sendRespondMessage(IChannel channel, String message, IUser author){
    	IMessage out;
    	try{
    		out = channel.sendMessage(author + ": " + message);
            return out;
        } catch (DiscordException e){
        	System.err.println(channel.getGuild().getName() + "," + channel.getName() +  ". msg respond send fail:");
            e.printStackTrace();
            out = null;
        }
        return out;
    }
    /*private IMessage sendMessageError(IChannel channel, Exception e,true){
        	IMessage out;
            try{
				e.printStackTrace();
                
                channel.sendMessage("<:vpRedTick:319191455589728256> Error detected, sending traceback:");
                out = channel.sendMessage("```" + e + "```");
                
            } catch (DiscordException e1){
                System.out.println(channel.getGuild().getName() + "," + channel.getName() +  ". msg error send fail:");
                e1.printStackTrace();
                out = null;
            }
            return out;
    }*/
    private IMessage sendMessageError(IChannel channel,MessageEvent event, Exception e, boolean report){
    	IMessage out;
        try{
			logger.error("Exception", e);      
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String sStackTrace = sw.toString(); 
            out = channel.sendMessage("<:vpRedTick:319191455589728256> Looks like there was an error:```" + e + "```"
            		+ "\nThis error was automatically reported. No further action is required.");
            if(report) {
            	channel.getClient().fetchUser(153353572711530496l).getOrCreatePMChannel().sendMessage("```" + sStackTrace.substring(0,Math.min(sStackTrace.length(), 1999)) + "```\n");
                channel.getClient().fetchUser(153353572711530496l).getOrCreatePMChannel()
                .sendMessage("```Diagnostics Information:\n     Guild:" + event.getGuild().getName() + "(" + event.getGuild().getStringID() 
                		+ ")\n     Channel:" + channel.getName() + "(" + channel.getStringID() 
                		+ ")\n     Message:" + event.getMessage().getContent() +  "(" + event.getMessage().getStringID() 
                		+ ")\n     Author:" + event.getAuthor().getName() + "(" + event.getAuthor().getStringID() + ")\nEnd of diagnostics information```");
            }
            
        } catch (DiscordException e1){
            System.out.println(channel.getGuild().getName() + "," + channel.getName() +  ". msg error send fail:");
            logger.error("Exception", e1);
            out = null;
        }
        return out;
    }
    public <T> T ifnull(T input,T ifnull) {
    	return (input != null ? input : ifnull);
    }
    
}

