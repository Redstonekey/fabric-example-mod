package de.joan_code.ai;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.text.Text;
import net.minecraft.network.message.SignedMessage;
import com.mojang.authlib.GameProfile;
import java.time.Instant;
import net.minecraft.client.MinecraftClient;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.util.ScreenshotRecorder;
import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import net.minecraft.client.session.Session;
import java.util.UUID;
import net.minecraft.client.gui.screen.TitleScreen;
import java.util.Optional;
import net.minecraft.entity.player.PlayerEntity;

public class ExampleModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Register chat listener for plugin defeat/win messages
        ClientReceiveMessageEvents.CHAT.register((Text message, SignedMessage signedMessage, GameProfile sender, net.minecraft.network.message.MessageType.Parameters params, Instant receptionTimestamp) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) return;
            String msg = message.getString();
            String playerName = client.player.getName().getString();
            // only handle the plugin’s final '[Duels]' defeated message
            if (!msg.contains("[Duels]") || !msg.contains("defeated")) return;
            int idxPlayer = msg.indexOf(playerName);
            int idxDef = msg.indexOf("defeated");
            boolean isWin = idxPlayer < idxDef;
            client.getNetworkHandler().sendCommand("queue join nothing");
            client.getNetworkHandler().sendChatMessage(isWin ? "WON" : "LOST");
        });
        // Also listen to system messages
        ClientReceiveMessageEvents.GAME.register((Text message, boolean overlay) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) return;
            String msg = message.getString();
            String playerName = client.player.getName().getString();
            // only handle the plugin’s final '[Duels]' defeated message
            if (!msg.contains("[Duels]") || !msg.contains("defeated")) return;
            int idxPlayer = msg.indexOf(playerName);
            int idxDef = msg.indexOf("defeated");
            boolean isWin = idxPlayer < idxDef;
            client.getNetworkHandler().sendCommand("queue join nothing");
            client.getNetworkHandler().sendChatMessage(isWin ? "WON" : "LOST");
        });
        // Capture screenshot each tick without debug logs
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            File runDir = client.runDirectory;
            File screenshotDir = new File(runDir, "screenshots"); screenshotDir.mkdirs();
            ScreenshotRecorder.saveScreenshot(runDir, client.getFramebuffer(), msg -> {});
            File[] pics = screenshotDir.listFiles((d, name) -> name.toLowerCase().endsWith(".png"));
            if (pics != null && pics.length > 0) {
                Arrays.sort(pics, Comparator.comparingLong(File::lastModified).reversed());
                File latest = pics[0];
                File aiDir = new File(runDir, "ai"); aiDir.mkdirs();
                File dest = new File(aiDir, "frame.png");
                if (dest.exists()) dest.delete();
                latest.renameTo(dest);
            }
        });
        // Register offline-only username switch and health command
        net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            // ...existing su registration...

            // health command
            dispatcher.register(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("hp")
                .executes(ctx -> {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc.player == null || mc.world == null) return 1;
                    for (PlayerEntity player : mc.world.getPlayers()) {
                        float health = player.getHealth();
                        float max = player.getMaxHealth();
                        mc.player.sendMessage(Text.literal(player.getName().getString() + ": " + health + "/" + max), false);
                    }
                    return 1;
                })
            );
        });
    }
}