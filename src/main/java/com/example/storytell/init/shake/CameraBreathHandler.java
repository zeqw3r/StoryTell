package com.example.storytell.init.shake;

import com.example.storytell.init.network.NetworkHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;

@Mod.EventBusSubscriber
public class CameraBreathHandler {

    private static final Map<UUID, BreathData> ACTIVE_BREATHS = new HashMap<>();

    public static void applyCameraBreath(ServerPlayer player, float power, int duration) {
        // Отправляем пакет активации на клиент
        NetworkHandler.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> player),
                new CameraBreathPacket(power, duration, true)
        );

        ACTIVE_BREATHS.put(player.getUUID(), new BreathData(power, duration));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            processCameraBreaths();
        }
    }

    private static void processCameraBreaths() {
        Iterator<Map.Entry<UUID, BreathData>> iterator = ACTIVE_BREATHS.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, BreathData> entry = iterator.next();
            UUID playerId = entry.getKey();
            BreathData data = entry.getValue();

            ServerPlayer player = getPlayerByUUID(playerId);
            if (player == null) {
                iterator.remove();
                continue;
            }

            if (data.timer <= 0) {
                // Отправляем пакет деактивации на клиент
                NetworkHandler.INSTANCE.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new CameraBreathPacket(data.power, 0, false)
                );
                iterator.remove();
                continue;
            }

            data.timer--;
        }
    }

    private static ServerPlayer getPlayerByUUID(UUID uuid) {
        try {
            return ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(uuid);
        } catch (Exception e) {
            return null;
        }
    }

    private static class BreathData {
        public float power;
        public int duration;
        public int timer;

        public BreathData(float power, int duration) {
            this.power = power;
            this.duration = duration;
            this.timer = duration;
        }
    }
}