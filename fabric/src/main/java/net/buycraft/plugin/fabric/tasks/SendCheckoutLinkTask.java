package net.buycraft.plugin.fabric.tasks;

import net.buycraft.plugin.data.responses.CheckoutUrlResponse;
import net.buycraft.plugin.fabric.BuycraftPlugin;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

public class SendCheckoutLinkTask implements Runnable {
    @NotNull
    private final BuycraftPlugin plugin;
    private final int pkgId;
    @NotNull
    private final CommandSourceStack player;

    public SendCheckoutLinkTask(@NotNull final BuycraftPlugin plugin, final int pkgId, @NotNull final CommandSourceStack player) {
        this.plugin = Objects.requireNonNull(plugin);
        this.pkgId = pkgId;
        this.player = Objects.requireNonNull(player);
    }

    @Override
    public void run() {
        CheckoutUrlResponse response;
        try {
            response = plugin.getApiClient().getCheckoutUri(player.getTextName(), pkgId).execute().body();
        } catch (IOException e) {
            player.sendSystemMessage(Component.literal(plugin.getI18n().get("cant_check_out")).withStyle(ChatFormatting.RED));
            return;
        }
        if (response != null) {
                player.sendSystemMessage(Component.literal("                                            ").withStyle(ChatFormatting.STRIKETHROUGH));
            Arrays.asList(
                    Component.literal(plugin.getI18n().get("to_buy_this_package")).withStyle(ChatFormatting.GREEN),
                    Component.literal(response.getUrl())
                            .withStyle(ChatFormatting.BLUE, ChatFormatting.UNDERLINE)
                            .withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, response.getUrl())))
            ).forEach(player::sendSystemMessage);
            player.sendSystemMessage(Component.literal("                                            ").withStyle(ChatFormatting.STRIKETHROUGH));
        }
    }
}