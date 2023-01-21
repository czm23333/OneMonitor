package io.github.czm23333.onemonitor.telegram;

import io.github.czm23333.onemonitor.Config;
import io.github.czm23333.onemonitor.minecraft.oneprobe.ProbeRequest;
import io.github.czm23333.onemonitor.minecraft.oneprobe.elements.Element;
import org.barfuin.texttree.api.DefaultNode;
import org.barfuin.texttree.api.TextTree;
import org.barfuin.texttree.api.TreeOptions;
import org.barfuin.texttree.api.style.TreeStyles;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class TelegramBot extends TelegramLongPollingBot {
    private static final Logger LOGGER = Logger.getLogger("TelegramBot");
    private static final TextTree RENDERER;

    static {
        TreeOptions options = new TreeOptions();
        options.setStyle(TreeStyles.UNICODE_ROUNDED);
        RENDERER = TextTree.newInstance(options);
    }

    private final String botUsername;
    private final String botToken;
    private final Set<Long> chatWhitelist;
    private final BlockingQueue<ProbeRequest> channel;

    public TelegramBot(String botUsername, String botToken, Collection<Long> chatWhitelist,
            BlockingQueue<ProbeRequest> channel) {
        this.botUsername = botUsername;
        this.botToken = botToken;
        this.chatWhitelist = new HashSet<>(chatWhitelist);
        this.channel = channel;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage()) return;
        Message msg = update.getMessage();
        long chatId = msg.getChatId();
        int msgId = msg.getMessageId();
        if (!(chatWhitelist.isEmpty() || chatWhitelist.contains(chatId)) || !msg.hasText()) return;

        String[] args = msg.getText().split(" ");
        if (args[0].equals("/probe") || args[0].equals("/probe@" + Config.INSTANCE.telegram.botUsername)) {
            int dim, x, y, z;
            if (args.length == 5) {
                try {
                    dim = Integer.parseInt(args[1]);
                    x = Integer.parseInt(args[2]);
                    y = Integer.parseInt(args[3]);
                    z = Integer.parseInt(args[4]);
                    channel.add(new ProbeRequest(dim, x, y, z, probeResponse -> {
                        SendMessage send = new SendMessage();
                        send.setChatId(chatId);
                        if (probeResponse.timedOut) send.setText("Probe timed out");
                        else if (!probeResponse.hasElements) send.setText("No element");
                        else send.setText(RENDERER.render(new DefaultNode("-", null, null, null,
                                    probeResponse.elements.stream().map(Element::toTree)
                                            .collect(Collectors.toList()))));
                        send.setReplyToMessageId(msgId);
                        try {
                            execute(send);
                        } catch (TelegramApiException ex) {
                            LOGGER.log(Level.WARNING, "Error sending message: ", ex);
                        }
                    }));
                } catch (NumberFormatException e) {
                    SendMessage send = new SendMessage();
                    send.setChatId(chatId);
                    send.setText("Illegal arguments: not an integer");
                    send.setReplyToMessageId(msgId);
                    try {
                        execute(send);
                    } catch (TelegramApiException ex) {
                        LOGGER.log(Level.WARNING, "Error sending message: ", ex);
                    }
                }

            } else {
                SendMessage send = new SendMessage();
                send.setChatId(chatId);
                send.setText("Illegal arguments: illegal length");
                send.setReplyToMessageId(msgId);
                try {
                    execute(send);
                } catch (TelegramApiException ex) {
                    LOGGER.log(Level.WARNING, "Error sending message: ", ex);
                }
            }
        }
    }
}