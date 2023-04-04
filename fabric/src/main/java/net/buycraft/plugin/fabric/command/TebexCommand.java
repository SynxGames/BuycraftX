package net.buycraft.plugin.fabric.command;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.buycraft.plugin.BuyCraftAPI;
import net.buycraft.plugin.data.Category;
import net.buycraft.plugin.data.Package;
import net.buycraft.plugin.data.responses.ServerInformation;
import net.buycraft.plugin.fabric.BuycraftPlugin;
import net.buycraft.plugin.fabric.tasks.SendCheckoutLinkTask;
import net.buycraft.plugin.shared.util.Node;
import net.buycraft.plugin.shared.util.ReportBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

import static com.mojang.brigadier.builder.RequiredArgumentBuilder.argument;

public class TebexCommand {
    private final BuycraftPlugin plugin;

    public TebexCommand(BuycraftPlugin plugin) {
        this.plugin = plugin;
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerCommands(dispatcher, "tebex");
        registerCommands(dispatcher, "buycraft");
    }

    private void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, String command) {
        dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal(command).executes(context -> {
                    if (!checkPermission(context.getSource())) return 0;

                    onBaseCommand(context);
                    return 1;
                }).then(LiteralArgumentBuilder.<CommandSourceStack>literal("secret").then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("token", StringArgumentType.string()).executes(context -> {
                    if (!checkPermission((CommandSourceStack) context.getSource())) return 0;

                    onSecretCommand(context);
                    return 1;
                }))).then(LiteralArgumentBuilder.<CommandSourceStack>literal("forcecheck").executes(context -> {
                    if (!checkPermission(context.getSource())) return 0;

                    onForceCheckCommand(context);
                    return 1;
                })).then(LiteralArgumentBuilder.<CommandSourceStack>literal("info").executes(context -> {
                    if (!checkPermission(context.getSource())) return 0;

                    onInfoCommand(context);
                    return 1;
                })).then(LiteralArgumentBuilder.<CommandSourceStack>literal("refresh").executes(context -> {
                    if (!checkPermission(context.getSource())) return 0;

                    onRefreshCommand(context);
                    return 1;
                })).then(LiteralArgumentBuilder.<CommandSourceStack>literal("report").executes(context -> {
                    if (!checkPermission(context.getSource())) return 0;

                    onReportCommand(context);
                    return 1;
                })).then(LiteralArgumentBuilder.<CommandSourceStack>literal("packages").then(RequiredArgumentBuilder.<CommandSourceStack, Integer>argument("package", IntegerArgumentType.integer()).executes(context -> {
                    if (!checkPermission(context.getSource())) return 0;

                    onPackagesCommand(context);
                    return 1;
                }))).then(LiteralArgumentBuilder.<CommandSourceStack>literal("checkout").then(RequiredArgumentBuilder.<CommandSourceStack, Integer>argument("package", IntegerArgumentType.integer()).executes(context -> {
                    if (!checkPermission(context.getSource())) return 0;

                    onCheckoutCommand(context);
                    return 1;
                })))
        );
    }

    private boolean checkPermission(CommandSourceStack source) {
        if (!Permissions.check(source, "buycraft.admin", 4)) {
            source.sendFailure(Component.literal("You do not have permission to use this command."));
            return false;
        }

        return true;
    }

    private void onBaseCommand(CommandContext<CommandSourceStack> context) {
        String[][] commands = new String[][]{
                new String[]{"/tebex forcecheck", "Forces a purchase check."},
                new String[]{"/tebex secret <token>", "Sets the secret key to use for this server."},
                new String[]{"/tebex info", "Retrieves public information about the webstore this server is associated with."},
                new String[]{"/tebex refresh", "Refreshes the list of categories and packages."},
                new String[]{"/tebex signupdate", "Forces an update to your recent purchase signs."},
                new String[]{"/tebex report", "Generates a report with debugging information you can send to support."},
                new String[]{"/tebex coupon", "Manage server coupons."},
                new String[]{"/tebex sendlink", "Sends a package or category link to a player."},
        };

        context.getSource().sendSystemMessage(Component.literal("Usage for the Tebex plugin:").withStyle(ChatFormatting.BLUE, ChatFormatting.BOLD));
        for (String[] command : commands) {
            context.getSource().sendSystemMessage(Component.literal(command[0]).withStyle(ChatFormatting.GREEN).append(Component.literal(": " + command[1]).withStyle(ChatFormatting.GRAY)));
        }
    }

    private void onSecretCommand(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (source.getEntity() instanceof ServerPlayer) {
            source.sendSystemMessage(Component.literal(plugin.getI18n().get("secret_console_only")));
            return;
        }

        String token = context.getArgument("token", String.class);

        plugin.getPlatform().executeAsync(() -> {
            String currentKey = plugin.getConfiguration().getServerKey();
            BuyCraftAPI client;
            try {
                client = BuyCraftAPI.create(token, plugin.getHttpClient());
                plugin.updateInformation(client);
            } catch (IOException e) {
                plugin.getLogger().error("Unable to verify secret", e);
                source.sendSystemMessage(Component.literal(plugin.getI18n().get("secret_does_not_work")).withStyle(ChatFormatting.RED));
                return;
            }

            ServerInformation information = plugin.getServerInformation();
            plugin.setApiClient(client);
            plugin.getListingUpdateTask().run();
            plugin.getConfiguration().setServerKey(token);
            try {
                plugin.saveConfiguration();
            } catch (IOException e) {
                source.sendSystemMessage(Component.literal(plugin.getI18n().get("secret_cant_be_saved")).withStyle(ChatFormatting.RED));
            }
            source.sendSystemMessage(Component.literal(plugin.getI18n().get("secret_success",
                    information.getServer().getName(), information.getAccount().getName())).withStyle(ChatFormatting.GREEN));

            boolean repeatChecks = currentKey.equals("INVALID");

            plugin.getDuePlayerFetcher().run(repeatChecks);
        });
    }

    private void onForceCheckCommand(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (plugin.getApiClient() == null) {
            source.sendSystemMessage(Component.literal(plugin.getI18n().get("need_secret_key")).withStyle(ChatFormatting.RED));
            return;
        }

        if (plugin.getDuePlayerFetcher().inProgress()) {
            source.sendSystemMessage(Component.literal(plugin.getI18n().get("already_checking_for_purchases")).withStyle(ChatFormatting.RED));
            return;
        }

        plugin.getPlatform().executeAsync(() -> plugin.getDuePlayerFetcher().run(false));
        source.sendSystemMessage(Component.literal(plugin.getI18n().get("forcecheck_queued")).withStyle(ChatFormatting.GREEN));
    }

    private void onInfoCommand(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (plugin.getApiClient() == null) {
            source.sendSystemMessage(Component.literal(plugin.getI18n().get("generic_api_operation_error")).withStyle(ChatFormatting.RED));
            return;
        }

        if (plugin.getServerInformation() == null) {
            source.sendSystemMessage(Component.literal(plugin.getI18n().get("information_no_server")).withStyle(ChatFormatting.RED));
            return;
        }

        String webstoreURL = plugin.getServerInformation().getAccount().getDomain();
        Component webstore = Component.literal(webstoreURL)
                .withStyle(ChatFormatting.GREEN)
                .withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, webstoreURL)).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(webstoreURL))));

        Component server = Component.literal(plugin.getServerInformation().getServer().getName()).withStyle(ChatFormatting.GREEN);

        Arrays.asList(
                Component.literal(plugin.getI18n().get("information_title") + " ").withStyle(ChatFormatting.GRAY),
                Component.literal(plugin.getI18n().get("information_sponge_server") + " ").withStyle(ChatFormatting.GRAY).append(server),
                Component.literal(plugin.getI18n().get("information_currency", plugin.getServerInformation().getAccount().getCurrency().getIso4217())).withStyle(ChatFormatting.GRAY),
                Component.literal(plugin.getI18n().get("information_domain", "")).withStyle(ChatFormatting.GRAY).append(webstore)
        ).forEach(source::sendSystemMessage);
    }

    private void onRefreshCommand(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (plugin.getApiClient() == null) {
            source.sendSystemMessage(Component.literal(plugin.getI18n().get("need_secret_key")).withStyle(ChatFormatting.RED));
            return;
        }

        plugin.getPlatform().executeAsync(plugin.getListingUpdateTask());
        source.sendSystemMessage(Component.literal(plugin.getI18n().get("refresh_queued")).withStyle(ChatFormatting.GREEN));
    }

    private void onReportCommand(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSystemMessage(Component.literal(plugin.getI18n().get("report_wait")).withStyle(ChatFormatting.RED));

        plugin.getPlatform().executeAsync(() -> {
            String serverIP = plugin.getServer().getLocalIp();
            int serverPort = plugin.getServer().getPort();

            ReportBuilder builder = ReportBuilder.builder()
                    .client(plugin.getHttpClient())
                    .configuration(plugin.getConfiguration())
                    .platform(plugin.getPlatform())
                    .duePlayerFetcher(plugin.getDuePlayerFetcher())
                    .ip(serverIP)
                    .port(serverPort)
                    .listingUpdateTask(plugin.getListingUpdateTask())
                    .serverOnlineMode(plugin.getServer().usesAuthentication())
                    .build();

            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
            String filename = "report-" + f.format(new Date()) + ".txt";
            Path p = plugin.getBaseDirectory().resolve(filename);
            String generated = builder.generate();

            try (BufferedWriter w = Files.newBufferedWriter(p, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW)) {
                w.write(generated);
                source.sendSystemMessage(Component.literal(plugin.getI18n().get("report_saved", p.toAbsolutePath().toString())).withStyle(ChatFormatting.YELLOW));
            } catch (IOException e) {
                source.sendSystemMessage(Component.literal(plugin.getI18n().get("report_cant_save")).withStyle(ChatFormatting.RED));
                plugin.getLogger().info(generated);
            }
        });
    }

    private void onPackagesCommand(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Integer packageId = context.getArgument("package", Integer.class);

        Node categories = new Node(plugin.getListingUpdateTask().getListing().getCategories(), ImmutableList.of(), plugin.getI18n().get("categories"), null);
        Optional<Category> category = categories.getSubcategories().stream().filter(categoryId -> categoryId.getId() == packageId).findFirst();

        plugin.getBuyCommand().sendPaginatedMessage(categories.getChild(category.get()), source);
    }

    private void onCheckoutCommand(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Integer packageId = context.getArgument("package", Integer.class);

        try {
            Package packageById = plugin.getListingUpdateTask().getPackageById(packageId);
            plugin.getPlatform().executeAsync(new SendCheckoutLinkTask(plugin, packageById.getId(), source));
        } catch (Exception e) {
            source.sendSystemMessage(Component.literal("Could not find package with id " + packageId).withStyle(ChatFormatting.RED));
        }
    }
}
