package com.example.storytell;

import com.example.storytell.init.HologramConfig;
import com.example.storytell.init.boss.BossCommands;
import com.example.storytell.init.blocks.*;
import com.example.storytell.init.radio.ModRadioBlockEntities;
import com.example.storytell.init.radio.ModRadioBlocks;
import com.example.storytell.init.radio.ModRadioItems;
import com.example.storytell.init.radio.RadioCommand;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import com.example.storytell.init.ModSounds;

@Mod(StoryTell.MODID)
public class StoryTell {
    public static final String MODID = "storytell";
    private static final Logger LOGGER = LogUtils.getLogger();

    public StoryTell() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Инициализируем конфиг
        HologramConfig.init();

        // Регистрируем все компоненты в правильном порядке
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModEntities.register(); // Регистрация сущностей
        ModSounds.SOUND_EVENTS.register(modEventBus); // Регистрация звуков


        // Регистрируем обработчики событий
        MinecraftForge.EVENT_BUS.register(this);


        ModRadioBlocks.BLOCKS.register(modEventBus);
        ModRadioBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModRadioItems.ITEMS.register(modEventBus);

        LOGGER.info("StoryTell mod initialized with star system");
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        // Регистрируем все команды
        HoloCommand.register(event.getDispatcher());
        BossCommands.register(event.getDispatcher()); // Регистрируем команды для боссов
        RadioCommand.register(event.getDispatcher());
    }
}