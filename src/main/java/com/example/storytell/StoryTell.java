package com.example.storytell;

import com.example.storytell.init.HologramConfig;
import com.example.storytell.init.altar.ModAltarBlocks;
import com.example.storytell.init.altar.ModAltarContainers;
import com.example.storytell.init.blocks.*;
import com.example.storytell.init.cutscene.CutsceneCommand;
import com.example.storytell.init.cutscene.CutsceneNetworkHandler;
import com.example.storytell.init.entity.RepoSpawnCommand;
import com.example.storytell.init.event.*;
import com.example.storytell.init.network.NetworkHandler;
import com.example.storytell.init.radio.ModRadioBlockEntities;
import com.example.storytell.init.radio.ModRadioBlocks;
import com.example.storytell.init.radio.ModRadioItems;
import com.example.storytell.init.radio.RadioCommand;
import com.example.storytell.init.shake.CameraBreathCommand;
import com.example.storytell.init.shake.ScreenShakeCommand;
import com.example.storytell.init.star.StarMoveCommand;
import com.example.storytell.init.star.StarVisibilityCommand;
import com.example.storytell.init.strike.OrbitalStrikeCommand;
import com.example.storytell.init.tablet.TabletCommand;
import com.example.storytell.init.world.WorldModelCommand;
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

        HologramConfig.init();

        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);

        ModRadioBlocks.BLOCKS.register(modEventBus);
        ModRadioBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModRadioItems.ITEMS.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::commonSetup);
        ModAltarContainers.CONTAINERS.register(modEventBus);
        ModAltarBlocks.BLOCKS.register(modEventBus);
        ModAltarBlocks.BLOCK_ENTITIES.register(modEventBus);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CutsceneNetworkHandler.register();
            NetworkHandler.register();
            LOGGER.info("StoryTell mod common setup completed");
        });
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        HoloCommand.register(dispatcher);
        RadioCommand.register(dispatcher);
        CutsceneCommand.register(dispatcher);
        OrbitalStrikeCommand.register(dispatcher);
        ScreenShakeCommand.register(dispatcher);
        Event1Command.register(dispatcher);
        StarMoveCommand.register(dispatcher);
        CameraBreathCommand.register(dispatcher);
        StarVisibilityCommand.register(dispatcher);
        Event2Command.register(dispatcher);
        RepoSpawnCommand.register(dispatcher);
        Event3Command.register(dispatcher);
        WorldModelCommand.register(dispatcher);
        Event4Command.register(dispatcher);
        Event5Command.register(dispatcher);
        Event6Command.register(dispatcher);
        Event7Command.register(dispatcher);
        Event8Command.register(dispatcher);
        Event9Command.register(dispatcher);
        Event10Command.register(dispatcher);
        Event11Command.register(dispatcher);
        TabletCommand.register(dispatcher);
        MeteorCommand.register(dispatcher);
    }
}