package com.example.storytell.init.start;

import com.example.storytell.init.HologramConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "storytell", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerStartManager {

    // Карта для отслеживания предыдущих позиций игроков
    private static final Map<UUID, PlayerPosition> previousPositions = new HashMap<>();

    // Внутренний класс для хранения позиции игрока
    private static class PlayerPosition {
        public final double x;
        public final double y;
        public final double z;
        public final float yaw;
        public final float pitch;

        public PlayerPosition(double x, double y, double z, float yaw, float pitch) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            String playerName = player.getGameProfile().getName();
            UUID playerId = player.getUUID();

            // Проверяем, видел ли игрок cutscene ранее
            if (!HologramConfig.hasPlayerSeenCutscene(playerName)) {
                // Добавляем игрока в список ожидающих подтверждения
                HologramConfig.addPendingPlayer(playerName);

                // Сохраняем начальную позицию игрока
                previousPositions.put(playerId, new PlayerPosition(
                        player.getX(), player.getY(), player.getZ(),
                        player.getYRot(), player.getXRot()
                ));

                // Отправляем сообщение с инструкцией
                player.sendSystemMessage(Component.literal("§6Добро пожаловать! Нажмите любую кнопку или переместитесь для просмотра вступительной сцены."));
                System.out.println("Player " + playerName + " is pending confirmation for cutscene");
            } else {
                System.out.println("Player " + playerName + " has already seen the cutscene");
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            String playerName = player.getGameProfile().getName();

            // Проверяем, ожидает ли игрок подтверждения
            if (HologramConfig.isPlayerPending(playerName)) {
                System.out.println("Player " + playerName + " interacted, triggering cutscene");
                triggerCutscene(player, playerName);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            String playerName = player.getGameProfile().getName();
            UUID playerId = player.getUUID();

            // Проверяем, ожидает ли игрок подтверждения
            if (HologramConfig.isPlayerPending(playerName)) {
                // Получаем текущую и предыдущую позиции
                PlayerPosition prevPos = previousPositions.get(playerId);
                PlayerPosition currentPos = new PlayerPosition(
                        player.getX(), player.getY(), player.getZ(),
                        player.getYRot(), player.getXRot()
                );

                // Если у нас нет предыдущей позиции, сохраняем текущую
                if (prevPos == null) {
                    previousPositions.put(playerId, currentPos);
                    return;
                }

                // Вычисляем разницу по всем осям
                double deltaX = Math.abs(currentPos.x - prevPos.x);
                double deltaY = Math.abs(currentPos.y - prevPos.y);
                double deltaZ = Math.abs(currentPos.z - prevPos.z);
                double deltaYaw = Math.abs(currentPos.yaw - prevPos.yaw);
                double deltaPitch = Math.abs(currentPos.pitch - prevPos.pitch);

                // Увеличиваем порог для движения и добавляем проверку поворота камеры
                boolean hasMoved = deltaX > 0.05 || deltaY > 0.05 || deltaZ > 0.05;
                boolean hasRotated = deltaYaw > 1.0 || deltaPitch > 1.0;

                // Если игрок переместился или повернул камеру
                if (hasMoved || hasRotated) {
                    System.out.println("Player " + playerName + " moved or rotated, triggering cutscene");
                    System.out.println("Movement: X=" + deltaX + ", Y=" + deltaY + ", Z=" + deltaZ);
                    System.out.println("Rotation: Yaw=" + deltaYaw + ", Pitch=" + deltaPitch);
                    triggerCutscene(player, playerName);
                } else {
                    // Обновляем предыдущую позицию для следующего тика
                    previousPositions.put(playerId, currentPos);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Удаляем игрока из карты позиций при выходе
            previousPositions.remove(player.getUUID());
        }
    }

    private static void triggerCutscene(ServerPlayer player, String playerName) {
        // Убираем игрока из списка ожидающих
        HologramConfig.removePendingPlayer(playerName);

        // Удаляем игрока из карты позиций
        previousPositions.remove(player.getUUID());

        // Запускаем катсцену
        String cutsceneCommand = "cutscene @a[name=" + playerName + "] 1";
        int cutsceneResult = player.getServer().getCommands().performPrefixedCommand(
                player.getServer().createCommandSourceStack(),
                cutsceneCommand
        );

        System.out.println("Cutscene command result for " + playerName + ": " + cutsceneResult);

        // Команда считается успешной, если возвращает положительное значение
        boolean success = cutsceneResult > 0;

        if (success) {
            // Помечаем игрока как видевшего cutscene только если команда выполнена успешно
            HologramConfig.markPlayerAsSeen(playerName);
            System.out.println("Cutscene successfully triggered for player: " + playerName);
        } else {
            System.out.println("Failed to trigger cutscene for player: " + playerName);
            // Если команда не сработала, оставляем игрока в pending для повторной попытки
            HologramConfig.addPendingPlayer(playerName);
        }
    }
}