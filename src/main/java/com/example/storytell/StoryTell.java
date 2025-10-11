// StoryTell.java
package com.example.storytell;

import com.example.storytell.init.HologramConfig;
import com.example.storytell.init.boss.BossCommands;
import com.example.storytell.init.blocks.*;
import com.example.storytell.init.cutscene.CutsceneCommand;
import com.example.storytell.init.cutscene.CutsceneNetworkHandler;
import com.example.storytell.init.event.Event1Command;
import com.example.storytell.init.network.NetworkHandler;
import com.example.storytell.init.radio.ModRadioBlockEntities;
import com.example.storytell.init.radio.ModRadioBlocks;
import com.example.storytell.init.radio.ModRadioItems;
import com.example.storytell.init.radio.RadioCommand;
import com.example.storytell.init.shake.CameraBreathCommand;
import com.example.storytell.init.shake.ScreenShakeCommand;
import com.example.storytell.init.star.StarMoveCommand;
import com.example.storytell.init.strike.OrbitalStrikeCommand;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import com.example.storytell.init.ModSounds;
import com.example.storytell.init.ModItems;
import com.example.storytell.init.ModEntities;

@Mod(StoryTell.MODID)
public class StoryTell {
    public static final String MODID = "storytell";
    public static final Logger LOGGER = LogUtils.getLogger();

    public StoryTell() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Инициализируем конфиг
        HologramConfig.init();

        // Регистрируем все компоненты в правильном порядке
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);
        CutsceneNetworkHandler.register();

        // Регистрируем обработчики событий
        MinecraftForge.EVENT_BUS.register(this);

        ModRadioBlocks.BLOCKS.register(modEventBus);
        ModRadioBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModRadioItems.ITEMS.register(modEventBus);

        NetworkHandler.register();
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CutsceneNetworkHandler.register();
            System.out.println("StoryTell mod common setup completed");
        });
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        HoloCommand.register(event.getDispatcher());
        BossCommands.register(event.getDispatcher());
        RadioCommand.register(event.getDispatcher());
        CutsceneCommand.register(event.getDispatcher());
        OrbitalStrikeCommand.register(event.getDispatcher());
        ScreenShakeCommand.register(event.getDispatcher());
        Event1Command.register(event.getDispatcher());
        StarMoveCommand.register(event.getDispatcher());
        CameraBreathCommand.register(event.getDispatcher());
    }
}