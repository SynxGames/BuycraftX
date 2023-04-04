package net.buycraft.plugin.fabric.command;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.buycraft.plugin.data.Category;
import net.buycraft.plugin.data.Package;
import net.buycraft.plugin.fabric.BuycraftPlugin;
import net.buycraft.plugin.shared.util.Node;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class BuyCommand {
    private final BuycraftPlugin plugin;

    public BuyCommand(BuycraftPlugin plugin) {
        this.plugin = plugin;
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("buy")
                .executes(context -> {
                    CommandSourceStack source = context.getSource();

                    if (plugin.getApiClient() == null) {
                        source.sendSystemMessage(Component.literal(plugin.getI18n().get("generic_api_operation_error")).withStyle(ChatFormatting.RED));
                        return 1;
                    }

                    if (plugin.getListingUpdateTask().getListing() == null) {
                        source.sendSystemMessage(Component.literal("We're currently retrieving the listing. Sit tight!").withStyle(ChatFormatting.RED));
                        return 1;
                    }

                    sendPaginatedMessage(new Node(plugin.getListingUpdateTask().getListing().getCategories(), ImmutableList.of(), plugin.getI18n().get("categories"), null), source);

                    return 1;
                })
        );
    }

    public void sendPaginatedMessage(Node node, CommandSourceStack source) {
        List<Category> subcategories = node.getSubcategories();

        List<Component> contents;
        if(subcategories.size() > 0) {
            contents = subcategories.stream().map(category -> Component.literal("> " + category.getName())
                    .withStyle(ChatFormatting.GRAY)
                    .withStyle(style -> style
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tebex packages " + category.getId()))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to view packages in this category")))
                    )).collect(Collectors.toList());
        } else {
            contents = new ArrayList<>();
            for (Package p : node.getPackages()) {
                contents.add(
                        Component.literal(p.getName())
                                .withStyle(ChatFormatting.WHITE)
                                .append(Component.literal(" - ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal("$x".replace("$", plugin.getServerInformation().getAccount().getCurrency().getSymbol())
                                                .replace("x", String.valueOf(p.getEffectivePrice()))).withStyle(ChatFormatting.GREEN))
                                        .withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tebex checkout " + p.getId()))))
                                );
            }
        }

        Component packageHeader = Component.literal(plugin.getI18n().get("sponge_listing")).withStyle(ChatFormatting.BLUE);
        source.sendSystemMessage(packageHeader);
        contents.forEach(source::sendSystemMessage);
    }
}
