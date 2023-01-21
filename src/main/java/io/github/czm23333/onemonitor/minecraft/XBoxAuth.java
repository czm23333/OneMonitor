package io.github.czm23333.onemonitor.minecraft;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import io.github.czm23333.onemonitor.CommonInstance;
import it.unimi.dsi.fastutil.Pair;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class XBoxAuth {
    // files
    private static final String REFRESH_TOKEN = "retoken";
    private static final String XBOX_ACCESS_TOKEN = "accToken";
    private static final String MINECRAFT_ACCESS_TOKEN = "mcAccess";
    private static final String URL_TOKEN = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";
    private static final String XBL = "https://user.auth.xboxlive.com/user/authenticate";
    private static final String XSTS_AUTHENTICATE = "https://xsts.auth.xboxlive.com/xsts/authorize";
    private static final String MINECRAFT_LOGIN = "https://api.minecraftservices.com/authentication/login_with_xbox";
    private static final String MINECRAFT_PROFILES = "https://api.minecraftservices.com/minecraft/profile";
    private static final Map<Long, String> ERROR_MESSAGE = Map.of(2148916233L,
            "The account doesn't have an Xbox account. Once they sign up for one (or login through minecraft.net to create one) then they can proceed with the login. This shouldn't happen with accounts that have purchased Minecraft with a Microsoft account, as they would've already gone through that Xbox signup process.",
            2148916235L, "The account is from a country where Xbox Live is not available/banned", 2148916236L,
            "The account needs adult verification on Xbox page. (South Korea)", 2148916237L,
            "The account needs adult verification on Xbox page. (South Korea)", 2148916238L,
            "The account is a child (under 18) and cannot proceed unless the account is added to a Family by an adult. This only seems to occur when using a custom Microsoft Azure application. When using the Minecraft launchers client id, this doesn't trigger.");
    private static final Logger LOGGER = Logger.getLogger("XBoxAuth");
    private static String accessToken;
    private static String userName;
    private static String uuid;
    private static String refreshToken;

    public static Pair<GameProfile, String> login() {
        Path accessTokenPath = Path.of(MINECRAFT_ACCESS_TOKEN);
        if (Files.exists(accessTokenPath)) try {
            var jo = JsonParser.parseReader(Files.newBufferedReader(accessTokenPath)).getAsJsonObject();
            accessToken = jo.get("token").getAsString();
            var time = jo.get("time").getAsLong();
            if (System.currentTimeMillis() - time > 80000L * 60 * 1000) { // update it.
                accessToken = null;
                LOGGER.warning("Invalidating outdated access tokens...");
                Files.deleteIfExists(accessTokenPath);
                Files.deleteIfExists(Path.of(XBOX_ACCESS_TOKEN));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (!checkProfile(accessToken)) auth();

        return Pair.of(new GameProfile(fixUUID(uuid), userName), accessToken);
    }

    private static void auth() {
        // load mc token
        readXBOXAccessTokenFromCache();
        if (!checkXBOXAccessToken(accessToken)) {
            if (refreshToken == null) {
                // update refreshToken.
                LOGGER.info("Can't find any valid accessToken, looking for refreshToken...");
                refreshToken = Optional.ofNullable(System.getProperty("onemonitor.refreshToken"))
                        .orElseGet(XBoxAuth::readRefreshToken);
            }
            try {
                updateXBOXAccessToken(refreshToken);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException("Can't update access token", e);
            }
        }
        System.out.println(accessToken);
        LOGGER.info("Authenticating with XBox Live (1/3)");
        // do login
        String xblToken;
        String userHash;
        {
            var payload = new JsonObject();
            payload.addProperty("TokenType", "JWT");
            payload.addProperty("RelyingParty", "http://auth.xboxlive.com");
            var prop = new JsonObject();
            prop.addProperty("AuthMethod", "RPS");
            prop.addProperty("SiteName", "user.auth.xboxlive.com");
            prop.addProperty("RpsTicket", "d=" + accessToken);
            payload.add("Properties", prop);
            var req = HttpRequest.newBuilder(URI.create(XBL)).header("Content-Type", "application/json")
                    .header("Accept", "application/json").POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();

            // send.
            try {
                var result = CommonInstance.HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
                if (result.statusCode() == 401) {
                    LOGGER.info("XBox access token is invalid, refreshing...");
                    accessToken = null;
                    Files.deleteIfExists(Path.of(XBOX_ACCESS_TOKEN));
                    auth();
                    return;
                }
                var resp = JsonParser.parseString(result.body());
                xblToken = resp.getAsJsonObject().get("Token").getAsString();
                userHash = resp.getAsJsonObject().get("DisplayClaims").getAsJsonObject().getAsJsonArray("xui").get(0)
                        .getAsJsonObject().get("uhs").getAsString();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException("Can't fetch XBL token: " + e);
            }
        }
        LOGGER.info("Authenticating with XSTS (2/3)");
        String xstsToken;
        {
            var payload = new JsonObject();
            payload.addProperty("TokenType", "JWT");
            payload.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
            var prop = new JsonObject();
            prop.addProperty("SandboxId", "RETAIL");
            var usrTks = new JsonArray();
            usrTks.add(xblToken);
            prop.add("UserTokens", usrTks);
            payload.add("Properties", prop);

            var req = HttpRequest.newBuilder(URI.create(XSTS_AUTHENTICATE)).header("Content-Type", "application/json")
                    .header("Accept", "application/json").POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();
            try {
                var resp = JsonParser.parseString(
                                CommonInstance.HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString()).body())
                        .getAsJsonObject();
                if (resp.has("XErr")) {
                    var errId = resp.get("XErr").getAsLong();
                    LOGGER.warning("Failed to get XSTS Token! Error: (" + errId + ") " + ERROR_MESSAGE.get(errId));
                    return;
                }
                xstsToken = resp.get("Token").getAsString();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException("Can't fetch XSTS Token: " + e);
            }
        }
        LOGGER.info("Authenticating with Minecraft! (3/3)");
        // auth mc
        var payload = new JsonObject();
        payload.addProperty("identityToken", "XBL3.0 x=" + userHash + ";" + xstsToken);
        var req = HttpRequest.newBuilder(URI.create(MINECRAFT_LOGIN)).header("Content-Type", "application/json")
                .header("Accept", "application/json").POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();
        try {
            var resp = JsonParser.parseString(
                            CommonInstance.HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString()).body())
                    .getAsJsonObject();
            accessToken = resp.get("access_token").getAsString();
            checkGameOwnership(accessToken);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Can't login to Minecraft! " + e);
        }
        LOGGER.info("Checking for profiles...");
        if (!checkProfile(accessToken))
            throw new RuntimeException("Can't fetch profile! Have you purchased Minecraft?");
    }

    private static boolean checkProfile(String accessToken) {
        if (accessToken == null) return false;
        var req = HttpRequest.newBuilder(URI.create(MINECRAFT_PROFILES))
                .header("Authorization", "Bearer " + accessToken).GET().build();
        try {
            var _raw = CommonInstance.HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString()).body();
            var resp = JsonParser.parseString(_raw).getAsJsonObject();
            var name = resp.get("name").getAsString();
            var id = resp.get("id").getAsString();

            uuid = id;
            userName = name;

            LOGGER.info("[" + name + "/" + id + "] Logged in!");
            var jo = new JsonObject();
            jo.addProperty("token", accessToken);
            jo.addProperty("time", System.currentTimeMillis());
            Files.writeString(Path.of(MINECRAFT_ACCESS_TOKEN), jo.toString());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void checkGameOwnership(String accessToken) throws IOException, InterruptedException {
        var req = HttpRequest.newBuilder(URI.create("https://api.minecraftservices.com/entitlements/mcstore"))
                .header("Authorization", "Bearer " + accessToken).GET().build();
        var resp = CommonInstance.HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString()).body();
        if (!resp.contains("game_minecraft"))
            throw new IOException("This account haven't purchase Minecraft yet! Dump: " + resp);
    }

    private static void readXBOXAccessTokenFromCache() {
        final var file = Path.of(XBOX_ACCESS_TOKEN);
        if (Files.exists(file)) try {
            LOGGER.info("AccessToken was found in cache!");
            accessToken = Files.readString(file);
        } catch (IOException ignored) {

        }
    }

    private static void updateXBOXAccessToken(String refreshToken) throws IOException, InterruptedException {
        var query = Map.of("grant_type", "refresh_token", "client_id", "6a3728d6-27a3-4180-99bb-479895b8f88e",
                // borrowed from HMCL, will delete if abused
                "refresh_token", refreshToken);
        var request = HttpRequest.newBuilder(URI.create(URL_TOKEN)).header("Accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(asForm(query))).build();
        var body = CommonInstance.HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString()).body();
        var resp = Objects.requireNonNull(CommonInstance.GSON.fromJson(body, RespRefresh.class));
        Objects.requireNonNull(accessToken = resp.accessToken);
        Files.writeString(Path.of(XBOX_ACCESS_TOKEN), accessToken);
    }

    private static String asForm(Map<String, String> query) {
        return query.entrySet().stream()
                .map(entry -> Map.entry(entry.getKey(), URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8)))
                .map(entry -> entry.getKey() + "=" + entry.getValue()).collect(Collectors.joining("&"));
    }

    private static String readRefreshToken() {
        var file = Path.of(REFRESH_TOKEN);
        if (!Files.exists(file)) throw new IllegalArgumentException(
                "Can't find refreshToken file! It should be `retoken` and under same directory.");
        try {
            return Files.readString(file).trim();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean checkXBOXAccessToken(String accessToken) {
        return accessToken != null;
    }

    private static String fixUUID(String mc_uuid) {
        return mc_uuid.substring(0, 8) + '-' + mc_uuid.substring(8, 8 + 4) + '-' + mc_uuid.substring(8 + 4, 8 + 4 + 4) +
                '-' + mc_uuid.substring(8 + 4 + 4, 8 + 4 + 4 + 4) + '-' +
                mc_uuid.substring(8 + 4 + 4 + 4, 8 + 4 + 4 + 4 + 12);
    }

    public static final class RespRefresh {
        @SerializedName("access_token")
        public String accessToken;
        @SerializedName("expires_in")
        int expiresIn;
        @SerializedName("refresh_token")
        String refreshToken;
    }
}