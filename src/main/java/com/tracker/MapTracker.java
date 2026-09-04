package com.tracker;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class MapTracker implements ModInitializer {
    // This points to where your website will listen for coordinates
    private static final String TARGET_URL = "http://localhost:3000/api/coordinates";
    
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    private int tickCounter = 0;

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            // 20 ticks = exactly 1 second
            if (tickCounter >= 20) {
                tickCounter = 0;

                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    sendPlayerData(player);
                }
            }
        });
    }

    private void sendPlayerData(ServerPlayerEntity player) {
        String jsonPayload = String.format(
            "{\"player\":\"%s\",\"x\":%.2f,\"y\":%.2f,\"z\":%.2f,\"dimension\":\"%s\"}",
            player.getName().getString(),
            player.getX(),
            player.getY(),
            player.getZ(),
            player.getWorld().getRegistryKey().getValue().toString()
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TARGET_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .timeout(Duration.ofSeconds(2))
                .build();

        // Send in background so the game never lags or freezes
        HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.discarding());
    }
}
