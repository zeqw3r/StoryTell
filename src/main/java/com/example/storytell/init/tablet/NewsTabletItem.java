package com.example.storytell.init.tablet;

import com.example.storytell.init.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NewsTabletItem extends Item {
    private static final String IMAGE_TAG = "TabletImage";
    private static final String DEFAULT_IMAGE = "storytell:textures/gui/tablet/default.png";
    private static final Logger LOGGER = LogManager.getLogger();

    public NewsTabletItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Воспроизводим звук на обеих сторонах
        level.playSound(player, player.getX(), player.getY(), player.getZ(),
                ModSounds.TABLET1.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

        // Открываем GUI только на клиенте - БЕЗОПАСНЫЙ ВЫЗОВ
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> openTabletGuiClient(stack));
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    // Безопасный метод для открытия GUI на клиенте
    private void openTabletGuiClient(ItemStack stack) {
        try {
            String imagePath = getImagePath(stack);
            // Используем reflection для безопасной загрузки клиентского класса
            Class<?> screenClass = Class.forName("com.example.storytell.init.tablet.TabletScreen");
            Object screenInstance = screenClass.getConstructor(String.class).newInstance(imagePath);
            net.minecraft.client.Minecraft.getInstance().execute(() -> {
                net.minecraft.client.Minecraft.getInstance().setScreen((net.minecraft.client.gui.screens.Screen) screenInstance);
            });
        } catch (Exception e) {
            LOGGER.error("Failed to open tablet GUI on client", e);
        }
    }

    public static String getImagePath(ItemStack stack) {
        try {
            CompoundTag tag = stack.getOrCreateTag();
            return tag.contains(IMAGE_TAG) ? tag.getString(IMAGE_TAG) : DEFAULT_IMAGE;
        } catch (Exception e) {
            LOGGER.error("Failed to get image path from tablet", e);
            return DEFAULT_IMAGE;
        }
    }

    public static void setImagePath(ItemStack stack, String imagePath) {
        try {
            CompoundTag tag = stack.getOrCreateTag();
            tag.putString(IMAGE_TAG, imagePath);
        } catch (Exception e) {
            LOGGER.error("Failed to set image path for tablet", e);
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.storytell.news_tablet");
    }
}