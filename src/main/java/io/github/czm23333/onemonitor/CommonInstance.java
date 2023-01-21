package io.github.czm23333.onemonitor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.net.http.HttpClient;

public class CommonInstance {
    public static final Gson GSON = new Gson();
    public static final Gson GSON_PRETTY = new GsonBuilder().setPrettyPrinting().create();
    public static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().build();
}