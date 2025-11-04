package com.example.storytell.init.cutscene;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;

public class CutsceneCommand {
    private static final Logger LOGGER = LogManager.getLogger();

    // Кэш для звуков чтобы не создавать строки каждый раз
    private static final String DEFAULT_SOUND = "storytell:music";
    private static final String CUTSCENE_1_SOUND = "storytell:music_1";
    private static final String CUTSCENE_2_SOUND = "storytell:music_2";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("cutscene")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("target", EntityArgument.players())
                        .then(Commands.argument("folder", StringArgumentType.string())
                                .executes(context -> {
                                    String folderName = StringArgumentType.getString(context, "folder");
                                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "target");

                                    // Оптимизированный выбор звука
                                    String soundName = getSoundForFolder(folderName);

                                    // Воспроизводим музыку и запускаем катсцены
                                    processCutsceneForPlayers(targets, folderName, soundName, context);

                                    LOGGER.debug("Sent cutscene {} to {} players", folderName, targets.size());
                                    return targets.size();
                                }))));
    }

    private static String getSoundForFolder(String folderName) {
        switch (folderName) {
            case "1":
                return CUTSCENE_1_SOUND;
            case "2":
                return CUTSCENE_2_SOUND;
            default:
                return DEFAULT_SOUND;
        }
    }

    private static void processCutsceneForPlayers(Collection<ServerPlayer> targets, String folderName,
                                                  String soundName, com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) {
        for (ServerPlayer player : targets) {
            // Оптимизированная команда звука - создаем строку один раз
            String playSoundCommand = String.format(
                    "execute as %s at @s run playsound %s master @s ~ ~ ~ 1 1 1",
                    player.getGameProfile().getName(),
                    soundName
            );

            // Выполняем команду playsound на сервере
            context.getSource().getServer().getCommands()
                    .performPrefixedCommand(context.getSource(), playSoundCommand);

            // Отправляем пакет катсцены
            CutsceneNetworkHandler.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new CutsceneStartPacket(folderName, player.getGameProfile().getName())
            );
        }
    }
}