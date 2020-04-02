/*
 * Copyright (c) GlobalBans Team 2020
 *
 * The code this file loads is closed source
 */
package com.craftxbox.globalbans.util;

import discord4j.core.DiscordClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class BlackBoxModuleLoader {

    private static Logger raidModuleLogger = LoggerFactory.getLogger(BlackBoxModuleLoader.class);

    public void loadBlackBlockModule(DiscordClient discordClient) {
        try {
            File file = new File("./BlackBoxModule.jar");

            if (file.exists()) {
                URLClassLoader classLoader = new URLClassLoader(
                        new URL[] { file.toURI().toURL() }
                );

                // Kick Start the Module
                Class<?> initClass = classLoader.loadClass("com.craftxbox.globalbans.blackbox.Blackbox");
                Object initInstance = initClass.getConstructor().newInstance();
                Method initMethod = initClass.getDeclaredMethod("init", DiscordClient.class);
                initMethod.invoke(initInstance, discordClient);
            } else {
                raidModuleLogger.info("Raid Module is not present.");
            }
        } catch (ReflectiveOperationException | MalformedURLException e) {
            raidModuleLogger.warn("There was an issue loading the raid module.", e);
        }
    }
}
