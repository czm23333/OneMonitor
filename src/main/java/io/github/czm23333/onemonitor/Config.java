package io.github.czm23333.onemonitor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Config {
    private static final Logger LOGGER = Logger.getLogger("Config");
    private static final String CONFIG = "config.json";
    public static Config INSTANCE;
    public _Telegram telegram = new _Telegram();
    public _Minecraft minecraft = new _Minecraft();

    public static void init() {
        Path configPath = Path.of(CONFIG);
        if (Files.exists(configPath)) try {
            INSTANCE = CommonInstance.GSON.fromJson(Files.newBufferedReader(configPath), Config.class);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error loading config: ", e);
        }
        else try {
            Files.createFile(configPath);
            Files.writeString(configPath, CommonInstance.GSON_PRETTY.toJson(new Config()));

            LOGGER.log(Level.SEVERE, "Cannot find config file, default config file has been created");
            System.exit(0);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error creating default config: ", e);
        }
    }

    public static class _Telegram {
        public String botUsername = "username of your bot";
        public String botToken = "token of your bot";
        public List<Long> chatWhitelist = List.of(123L, 456L);
    }

    public static class _Minecraft {
        public String serverAddress = "server address";
        public int serverPort = 25565;
        public boolean autoReconnect = true;
        public long timeOutMillis = 5000;
    }
}