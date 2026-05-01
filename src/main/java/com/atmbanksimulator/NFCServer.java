package com.atmbanksimulator;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * NFCServer — listens for NFC card UIDs sent from the Android app.
 *
 * Uses a plain ServerSocket (no internal JDK packages) so it works
 * with all Java module configurations.
 *
 * The Android app sends an HTTP POST to localhost:8080/nfc with the
 * card UID as plain text. This server parses it and calls onUIDReceived.
 */
public class NFCServer {

    private final int port;
    private final Consumer<String> onUIDReceived;
    private volatile boolean running = false;
    private ServerSocket serverSocket;

    public NFCServer(int port, Consumer<String> onUIDReceived) {
        this.port = port;
        this.onUIDReceived = onUIDReceived;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;

        Thread thread = new Thread(() -> {
            System.out.println("[NFCServer] Listening on port " + port + " — waiting for card tap...");
            while (running) {
                try {
                    Socket client = serverSocket.accept();
                    new Thread(() -> handleRequest(client)).start();
                } catch (IOException e) {
                    if (running) {
                        System.out.println("[NFCServer] Accept error: " + e.getMessage());
                    }
                }
            }
        });
        thread.setDaemon(true);
        thread.setName("NFCServer-listener");
        thread.start();
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            // ignore on shutdown
        }
        System.out.println("[NFCServer] Stopped.");
    }

    private void handleRequest(Socket client) {
        try (
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
                OutputStream out = client.getOutputStream()
        ) {
            String requestLine = reader.readLine();
            if (requestLine == null) return;

            boolean isPost = requestLine.startsWith("POST");

            int contentLength = 0;
            String headerLine;
            while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
                if (headerLine.toLowerCase().startsWith("content-length:")) {
                    contentLength = Integer.parseInt(headerLine.split(":")[1].trim());
                }
            }

            if (isPost && contentLength > 0) {
                char[] body = new char[contentLength];
                int totalRead = 0;
                while (totalRead < contentLength) {
                    int read = reader.read(body, totalRead, contentLength - totalRead);
                    if (read == -1) break;
                    totalRead += read;
                }
                String uid = new String(body, 0, totalRead).trim();
                System.out.println("[NFCServer] Card tapped — UID: " + uid);

                String response = "HTTP/1.1 200 OK\r\n" +
                        "Content-Length: 2\r\n" +
                        "Connection: close\r\n\r\n" +
                        "OK";
                out.write(response.getBytes(StandardCharsets.UTF_8));
                out.flush();

                if (!uid.isEmpty()) {
                    onUIDReceived.accept(uid);
                }

            } else {
                String response = "HTTP/1.1 405 Method Not Allowed\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n\r\n";
                out.write(response.getBytes(StandardCharsets.UTF_8));
                out.flush();
            }

        } catch (IOException e) {
            System.out.println("[NFCServer] Request error: " + e.getMessage());
        } finally {
            try { client.close(); } catch (IOException ignored) {}
        }
    }
}