package de.xletxzx.visuals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Counts how many players currently run the mod. Every player sends an anonymous random id once a minute
 * to a Firebase Realtime Database. Leave BASE_URL empty to disable the whole feature.
 */
public class OnlineCounter {
    // e.g. "https://my-project-default-rtdb.europe-west1.firebasedatabase.app"  (no trailing slash)
    public static final String BASE_URL = "";

    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private static volatile int count = -1;
    private static volatile boolean started = false;

    public static boolean configured() { return !BASE_URL.isEmpty(); }

    public static int getCount() { return count; }

    public static synchronized void start() {
        if (started || !configured()) return;
        started = true;
        Thread t = new Thread(OnlineCounter::loop, "XLetxzX-Online");
        t.setDaemon(true);
        t.start();
        Runtime.getRuntime().addShutdownHook(new Thread(OnlineCounter::leave));
    }

    /** Applies the current setting immediately (used by the toggle in the settings page). */
    public static void refresh() {
        if (!configured()) return;
        Thread t = new Thread(OnlineCounter::cycle, "XLetxzX-Online-Refresh");
        t.setDaemon(true);
        t.start();
    }

    private static void loop() {
        while (true) {
            cycle();
            try { Thread.sleep(60000); } catch (InterruptedException e) { return; }
        }
    }

    private static void cycle() {
        try {
            if (Config.get().onlineCount) {
                ping();
                fetch();
            } else {
                leave();
                count = -1;
            }
        } catch (Exception ignored) {
        }
    }

    private static String base() {
        return BASE_URL.endsWith("/") ? BASE_URL.substring(0, BASE_URL.length() - 1) : BASE_URL;
    }

    private static void ping() throws Exception {
        HttpRequest r = HttpRequest.newBuilder(URI.create(base() + "/presence/" + Config.get().clientId() + ".json"))
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString("{\"t\":{\".sv\":\"timestamp\"}}"))
                .build();
        HTTP.send(r, HttpResponse.BodyHandlers.discarding());
    }

    private static void fetch() throws Exception {
        long since = System.currentTimeMillis() - 150000L;
        HttpRequest r = HttpRequest.newBuilder(URI.create(base() + "/presence.json?orderBy=%22t%22&startAt=" + since))
                .timeout(Duration.ofSeconds(8)).GET().build();
        HttpResponse<String> resp = HTTP.send(r, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) return;
        Matcher m = Pattern.compile("\"t\":").matcher(resp.body());
        int n = 0;
        while (m.find()) n++;
        count = n;
    }

    private static void leave() {
        try {
            HttpRequest r = HttpRequest.newBuilder(URI.create(base() + "/presence/" + Config.get().clientId() + ".json"))
                    .timeout(Duration.ofSeconds(3)).DELETE().build();
            HTTP.send(r, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {
        }
    }
}
