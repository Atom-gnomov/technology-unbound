package unboundtech.common;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import unboundtech.UnboundTech;

/**
 * Свои звуки мода (`.ogg` в `assets/unboundtech/sounds`).
 *
 * Карточки оружия и машин прямо требуют СВОЙ SoundEvent, а не вызов
 * {@code IC2.audioManager}: файлы IC2 не объявлены в его sounds.json и
 * ванильным {@code world.playSound} их не позвать, а жёсткая привязка к
 * чужой аудио-подсистеме роняла бы наши блоки без неё.
 */
@Mod.EventBusSubscriber(modid = UnboundTech.MODID)
public final class UTSounds {

    public static SoundEvent revolverShot;
    public static SoundEvent revolverReload;
    public static SoundEvent arquebusShot;
    public static SoundEvent mortarFire;
    public static SoundEvent mortarImpact;
    public static SoundEvent teslaDischarge;
    public static SoundEvent singulatorCharge;

    private UTSounds() {
    }

    private static SoundEvent make(String name,
                                   RegistryEvent.Register<SoundEvent> event) {
        ResourceLocation id = new ResourceLocation(UnboundTech.MODID, name);
        SoundEvent sound = new SoundEvent(id).setRegistryName(id);
        event.getRegistry().register(sound);
        return sound;
    }

    @SubscribeEvent
    public static void register(RegistryEvent.Register<SoundEvent> event) {
        revolverShot = make("revolver_shot", event);
        revolverReload = make("revolver_reload", event);
        arquebusShot = make("arquebus_shot", event);
        mortarFire = make("mortar_fire", event);
        mortarImpact = make("mortar_impact", event);
        teslaDischarge = make("tesla_discharge", event);
        singulatorCharge = make("singulator_charge", event);
    }
}
