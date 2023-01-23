package io.github.czm23333.onemonitor.telegram;

import guru.nidi.graphviz.attribute.Rank;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.Factory;
import io.github.czm23333.onemonitor.Config;
import io.github.czm23333.onemonitor.minecraft.oneprobe.ProbeRequest;
import io.github.czm23333.onemonitor.minecraft.oneprobe.elements.Element;
import io.github.czm23333.onemonitor.minecraft.utils.GraphUtil;
import io.github.czm23333.onemonitor.stats.StatManager;
import org.barfuin.texttree.api.DefaultNode;
import org.barfuin.texttree.api.TextTree;
import org.barfuin.texttree.api.TreeOptions;
import org.barfuin.texttree.api.style.TreeStyles;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
    private final StatManager stat;

    public TelegramBot(String botUsername, String botToken, Collection<Long> chatWhitelist,
            BlockingQueue<ProbeRequest> channel, StatManager stat) {
        this.botUsername = botUsername;
        this.botToken = botToken;
        this.chatWhitelist = new HashSet<>(chatWhitelist);
        this.channel = channel;
        this.stat = stat;
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
            if (args.length == 6) {
                try {
                    dim = Integer.parseInt(args[1]);
                    x = Integer.parseInt(args[2]);
                    y = Integer.parseInt(args[3]);
                    z = Integer.parseInt(args[4]);
                    switch (args[5]) {
                        case "graph" -> channel.add(new ProbeRequest(dim, x, y, z, probeResponse -> {
                            if (probeResponse.timedOut) {
                                SendMessage send = new SendMessage();
                                send.setChatId(chatId);
                                send.setReplyToMessageId(msgId);
                                send.setText("Probe timed out");
                                try {
                                    execute(send);
                                } catch (TelegramApiException ex) {
                                    LOGGER.log(Level.WARNING, "Error sending message: ", ex);
                                }
                            } else if (!probeResponse.hasElements) {
                                SendMessage send = new SendMessage();
                                send.setChatId(chatId);
                                send.setReplyToMessageId(msgId);
                                send.setText("No element");
                                try {
                                    execute(send);
                                } catch (TelegramApiException ex) {
                                    LOGGER.log(Level.WARNING, "Error sending message: ", ex);
                                }
                            } else {
                                SendDocument send = new SendDocument();
                                send.setChatId(chatId);
                                send.setReplyToMessageId(msgId);
                                byte[] image;
                                try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                                    GraphUtil.reset();
                                    Graphviz.fromGraph(Factory.graph().directed().graphAttr()
                                                    .with(Rank.dir(Rank.RankDir.LEFT_TO_RIGHT))
                                                    .with(probeResponse.elements.stream().map(Element::toGraph)
                                                            .collect(Collectors.toList()))).render(Format.SVG)
                                            .toOutputStream(outputStream);
                                    image = outputStream.toByteArray();
                                } catch (IOException e) {
                                    LOGGER.log(Level.WARNING, "Error sending photo: ", e);
                                    return;
                                }
                                send.setDocument(new InputFile(new ByteArrayInputStream(image), "result.svg"));
                                try {
                                    execute(send);
                                } catch (TelegramApiException ex) {
                                    LOGGER.log(Level.WARNING, "Error sending photo: ", ex);
                                }
                            }
                        }));
                        case "text" -> channel.add(new ProbeRequest(dim, x, y, z, probeResponse -> {
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
                        default -> {
                            SendMessage send = new SendMessage();
                            send.setChatId(chatId);
                            send.setReplyToMessageId(msgId);
                            send.setText("Illegal arguments: unknown mode");
                            try {
                                execute(send);
                            } catch (TelegramApiException ex) {
                                LOGGER.log(Level.WARNING, "Error sending message: ", ex);
                            }
                        }
                    }
                } catch (NumberFormatException e) {
                    SendMessage send = new SendMessage();
                    send.setChatId(chatId);
                    send.setReplyToMessageId(msgId);
                    send.setText("Illegal arguments: not an integer");
                    try {
                        execute(send);
                    } catch (TelegramApiException ex) {
                        LOGGER.log(Level.WARNING, "Error sending message: ", ex);
                    }
                }
            } else {
                SendMessage send = new SendMessage();
                send.setChatId(chatId);
                send.setReplyToMessageId(msgId);
                send.setText("Illegal arguments: illegal length");
                try {
                    execute(send);
                } catch (TelegramApiException ex) {
                    LOGGER.log(Level.WARNING, "Error sending message: ", ex);
                }
            }
        } else if (args[0].equals("/display") || args[0].equals("/display@" + Config.INSTANCE.telegram.botUsername)) {
            if (args.length == 2) {
                BufferedImage image = stat.getChart(args[1]);
                if (image == null) {
                    SendMessage send = new SendMessage();
                    send.setChatId(chatId);
                    send.setReplyToMessageId(msgId);
                    send.setText("Cannot get image for display " + args[1]);
                    try {
                        execute(send);
                    } catch (TelegramApiException ex) {
                        LOGGER.log(Level.WARNING, "Error sending message: ", ex);
                    }
                } else {
                    SendPhoto send = new SendPhoto();
                    send.setChatId(chatId);
                    send.setReplyToMessageId(msgId);
                    byte[] imageBytes;
                    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                        ImageIO.write(image, "png", outputStream);
                        imageBytes = outputStream.toByteArray();
                    } catch (IOException e) {
                        LOGGER.log(Level.WARNING, "Error sending photo: ", e);
                        return;
                    }
                    send.setPhoto(new InputFile(new ByteArrayInputStream(imageBytes), "result.png"));
                    try {
                        execute(send);
                    } catch (TelegramApiException ex) {
                        LOGGER.log(Level.WARNING, "Error sending photo: ", ex);
                    }
                }
            } else {
                SendMessage send = new SendMessage();
                send.setChatId(chatId);
                send.setReplyToMessageId(msgId);
                send.setText("Illegal arguments: illegal length");
                try {
                    execute(send);
                } catch (TelegramApiException ex) {
                    LOGGER.log(Level.WARNING, "Error sending message: ", ex);
                }
            }
        }
    }
}