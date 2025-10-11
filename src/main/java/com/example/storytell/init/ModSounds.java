// ModSounds.java
package com.example.storytell.init;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, "storytell");

    public static final RegistryObject<SoundEvent> RADIO_STATIC =
            registerSoundEvent("radio_static");

    public static final RegistryObject<SoundEvent> RADIO_MUSIC =
            registerSoundEvent("radio_music");

    public static final RegistryObject<SoundEvent> RADIO_ENABLE =
            registerSoundEvent("radio_enable");

    public static final RegistryObject<SoundEvent> RADIO_DISABLE =
            registerSoundEvent("radio_disable");

    public static final RegistryObject<SoundEvent> HOLOGRAM_APPEAR =
            registerSoundEvent("hologram_appear");

    public static final RegistryObject<SoundEvent> HOLOGRAM_DISAPPEAR =
            registerSoundEvent("hologram_disappear");

    public static final RegistryObject<SoundEvent> HOLOGRAM_AMBIENT =
            registerSoundEvent("hologram_ambient");

    public static final RegistryObject<SoundEvent> MUSIC_1 =
            registerSoundEvent("music_1");

    public static final RegistryObject<SoundEvent> MUSIC =
            registerSoundEvent("music");

    public static final RegistryObject<SoundEvent> EVENT1 =
            registerSoundEvent("event1");

    private static RegistryObject<SoundEvent> registerSoundEvent(String name) {
        ResourceLocation location = new ResourceLocation("storytell", name);
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(location));
    }
}