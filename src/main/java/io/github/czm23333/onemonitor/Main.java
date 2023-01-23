package io.github.czm23333.onemonitor;

import io.github.czm23333.onemonitor.minecraft.MinecraftBot;
import io.github.czm23333.onemonitor.minecraft.XBoxAuth;
import io.github.czm23333.onemonitor.minecraft.oneprobe.ProbeRequest;
import io.github.czm23333.onemonitor.stats.expression.FunctionManager;
import io.github.czm23333.onemonitor.stats.expression.exception.IllegalExpressionException;
import io.github.czm23333.onemonitor.telegram.TelegramBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    private static final Logger LOGGER = Logger.getLogger("Main");
    private static final BlockingQueue<ProbeRequest> requestQueue = new ArrayBlockingQueue<>(100);

    public static void main(String[] args) {
        FunctionManager.register("sum", para -> {
            if (para.size() != 1) throw new IllegalExpressionException("Illegal argument length");
            if (para.get(0) instanceof List<?> list) {
                double result = 0;
                for (Object v : list) {
                    if (v instanceof Double d) result += d;
                    else throw new IllegalExpressionException("Illegal element type");
                }
                return result;
            } else throw new IllegalExpressionException("Illegal argument type");
        });
        FunctionManager.register("min", para -> {
            if (para.size() != 1) throw new IllegalExpressionException("Illegal argument length");
            if (para.get(0) instanceof List<?> list) {
                if (list.size() == 0) throw new IllegalExpressionException("Illegal list length");
                double result = Double.MAX_VALUE;
                for (Object v : list) {
                    if (v instanceof Double d) result = Math.min(result, d);
                    else throw new IllegalExpressionException("Illegal element type");
                }
                return result;
            } else throw new IllegalExpressionException("Illegal argument type");
        });
        FunctionManager.register("max", para -> {
            if (para.size() != 1) throw new IllegalExpressionException("Illegal argument length");
            if (para.get(0) instanceof List<?> list) {
                if (list.size() == 0) throw new IllegalExpressionException("Illegal list length");
                double result = Double.MIN_VALUE;
                for (Object v : list) {
                    if (v instanceof Double d) result = Math.max(result, d);
                    else throw new IllegalExpressionException("Illegal element type");
                }
                return result;
            } else throw new IllegalExpressionException("Illegal argument type");
        });

        Config.init();

        try {
            new TelegramBotsApi(DefaultBotSession.class).registerBot(
                    new TelegramBot(Config.INSTANCE.telegram.botUsername, Config.INSTANCE.telegram.botToken,
                            Config.INSTANCE.telegram.chatWhitelist, requestQueue));
        } catch (TelegramApiException e) {
            LOGGER.log(Level.SEVERE, "Failed to run telegram bot", e);
            return;
        }
        LOGGER.info("Registered telegram bot");

        var loginResult = XBoxAuth.login();
        MinecraftBot bot = new MinecraftBot(requestQueue, loginResult.first(), loginResult.second());
        bot.setAutoReconnect(Config.INSTANCE.minecraft.autoReconnect);
        bot.setTimeoutMillis(Config.INSTANCE.minecraft.timeOutMillis);
        bot.initModList(Config.INSTANCE.minecraft.serverAddress, Config.INSTANCE.minecraft.serverPort);
        bot.connect(Config.INSTANCE.minecraft.serverAddress, Config.INSTANCE.minecraft.serverPort);
    }
}