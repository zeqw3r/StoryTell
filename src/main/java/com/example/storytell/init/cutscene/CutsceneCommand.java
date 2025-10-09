// CutsceneCommand.java
package com.example.storytell.init.cutscene;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.Collection;

public class CutsceneCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("cutscene")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("target", EntityArgument.players())
                        .then(Commands.argument("folder", StringArgumentType.string())
                                .executes(context -> {
                                    String folderName = StringArgumentType.getString(context, "folder");
                                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "target");

                                    // Определяем звук в зависимости от катсцены
                                    String soundName = "storytell:music"; // по умолчанию
                                    if ("1".equals(folderName)) {
                                        soundName = "storytell:music_1";
                                    }

                                    // Воспроизводим музыку для выбранных игроков
                                    for (ServerPlayer player : targets) {
                                        // Используем execute для правильного позиционирования звука
                                        String playSoundCommand = String.format(
                                                "execute as %s at @s run playsound %s master @s ~ ~ ~ 1 1 1",
                                                player.getGameProfile().getName(),
                                                soundName
                                        );

                                        // Выполняем команду playsound на сервере
                                        context.getSource().getServer().getCommands()
                                                .performPrefixedCommand(context.getSource(), playSoundCommand);

                                        System.out.println("Playing sound " + soundName + " for player " + player.getGameProfile().getName());
                                    }

                                    // Отправляем пакет выбранным игрокам для запуска катсцены
                                    for (ServerPlayer player : targets) {
                                        CutsceneNetworkHandler.INSTANCE.send(
                                                PacketDistributor.PLAYER.with(() -> player),
                                                new CutsceneStartPacket(folderName, player.getGameProfile().getName())
                                        );

                                        System.out.println("Sent cutscene " + folderName + " to player " + player.getGameProfile().getName());
                                    }

                                    return targets.size();
                                }))));
    }
}