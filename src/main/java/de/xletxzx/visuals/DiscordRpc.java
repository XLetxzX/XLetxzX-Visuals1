package de.xletxzx.visuals;

import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

/** Minimaler Discord-Rich-Presence-Client über die lokale IPC-Pipe (Windows), ohne Zusatz-Bibliothek. */
public class DiscordRpc {
    // Application ID aus dem Discord Developer Portal
    private static final String CLIENT_ID = "1570917829278965881";
    // Name des hochgeladenen Bilds unter Rich Presence > Art Assets (Discord speichert ihn klein geschrieben)
    private static final String LARGE_IMAGE = "xletxzx";

    private static volatile boolean running = false;
    private static Thread thread;

    public static synchronized void start() {
        if (running) return;
        running = true;
        thread = new Thread(DiscordRpc::loop, "XLetxzX-Discord");
        thread.setDaemon(true);
        thread.start();
    }

    public static synchronized void stop() { running = false; }

    private static void loop() {
        long start = System.currentTimeMillis() / 1000L;
        while (running) {
            try (RandomAccessFile pipe = open()) {
                if (pipe == null) { Thread.sleep(15000); continue; }
                send(pipe, 0, "{\"v\":1,\"client_id\":\"" + CLIENT_ID + "\"}");
                readFrame(pipe);
                while (running) {
                    String json = "{\"cmd\":\"SET_ACTIVITY\",\"args\":{\"pid\":" + ProcessHandle.current().pid()
                            + ",\"activity\":{\"details\":\"XLetxzX Visuals\",\"state\":\"Minecraft 1.21.11\","
                            + "\"timestamps\":{\"start\":" + start + "},"
                            + "\"assets\":{\"large_image\":\"" + LARGE_IMAGE + "\",\"large_text\":\"XLetxzX Visuals\"}}},"
                            + "\"nonce\":\"" + UUID.randomUUID() + "\"}";
                    send(pipe, 1, json);
                    readFrame(pipe);
                    Thread.sleep(15000);
                }
            } catch (Exception e) {
                try { Thread.sleep(15000); } catch (InterruptedException ignored) { return; }
            }
        }
    }

    private static RandomAccessFile open() {
        for (int i = 0; i < 10; i++) {
            try { return new RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + i, "rw"); } catch (Exception ignored) {}
        }
        return null;
    }

    private static void send(RandomAccessFile pipe, int op, String json) throws Exception {
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buf = ByteBuffer.allocate(8 + data.length).order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(op).putInt(data.length).put(data);
        pipe.write(buf.array());
    }

    private static void readFrame(RandomAccessFile pipe) throws Exception {
        byte[] head = new byte[8];
        pipe.readFully(head);
        int len = ByteBuffer.wrap(head).order(ByteOrder.LITTLE_ENDIAN).getInt(4);
        if (len < 0 || len > 1 << 20) throw new Exception("bad frame");
        pipe.readFully(new byte[len]);
    }
}
