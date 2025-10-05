// ModSounds.java
package com.example.storytell.init.blocks;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, "storytell");

    // Правильная регистрация звуков для современных версий Minecraft
    public static final RegistryObject<SoundEvent> HOLOGRAM_APPEAR =
            SOUND_EVENTS.register("hologram_appear",
                    () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("storytell", "hologram_appear")));

    public static final RegistryObject<SoundEvent> HOLOGRAM_DISAPPEAR =
            SOUND_EVENTS.register("hologram_disappear",
                    () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("storytell", "hologram_disappear")));

    public static final RegistryObject<SoundEvent> HOLOGRAM_AMBIENT =
            SOUND_EVENTS.register("hologram_ambient",
                    () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("storytell", "hologram_ambient")));
}