package net.buycraft.plugin.fabric.util;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.buycraft.plugin.data.responses.Version;
import net.buycraft.plugin.fabric.BuycraftPlugin;
import net.buycraft.plugin.shared.util.VersionUtil;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static net.buycraft.plugin.shared.util.VersionUtil.isVersionGreater;

public class VersionCheck implements ServerPlayConnectionEvents.Join {
    private final BuycraftPlugin plugin;
    private final String pluginVersion;
    private final String secret;
    private Version lastKnownVersion;
    private boolean upToDate = true;

    public VersionCheck(final BuycraftPlugin plugin, final String pluginVersion, final String secret) {
        this.plugin = plugin;
        this.pluginVersion = pluginVersion;
        this.secret = secret;
    }

    public void verify() throws IOException {
        if (pluginVersion.endsWith("-SNAPSHOT")) {
            return; // SNAPSHOT versions ignore updates
        }

        lastKnownVersion = VersionUtil.getVersion(plugin.getHttpClient(), "sponge", secret);
        if (lastKnownVersion == null) {
            return;
        }

        // Compare versions
        String latestVersionString = lastKnownVersion.getVersion();
        if (!latestVersionString.equals(pluginVersion)) {
            upToDate = !isVersionGreater(pluginVersion, latestVersionString);
            if (!upToDate) {
                plugin.getLogger().info(plugin.getI18n().get("update_available", lastKnownVersion.getVersion()));
            }
        }
    }

    @Override
    public void onPlayReady(ServerGamePacketListenerImpl handler, PacketSender sender, MinecraftServer server) {
        ServerPlayer player = handler.player;
        if (Permissions.check(player, "buycraft.admin", 4) && !upToDate) {
            plugin.getPlatform().executeAsyncLater(() -> {
                player.sendSystemMessage(Component.literal(plugin.getI18n().get("update_available", lastKnownVersion.getVersion()))
                        .withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://server.tebex.io/plugins")))
                        .withStyle(ChatFormatting.YELLOW));
            }, 3, TimeUnit.SECONDS);
        }
    }

    public Version getLastKnownVersion() {
        return this.lastKnownVersion;
    }

    public boolean isUpToDate() {
        return this.upToDate;
    }
}
