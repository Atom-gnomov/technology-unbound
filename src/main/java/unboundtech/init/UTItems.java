package unboundtech.init;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.OreDictionary;
import unboundtech.UTCreativeTab;
import unboundtech.UnboundTech;

/**
 * Предметы мода. Регистрация безусловна (см. пояснение в {@link UTBlocks}).
 */
@Mod.EventBusSubscriber(modid = UnboundTech.MODID)
public final class UTItems {

    /** Слиток Таум-Стали — основа ветки инструментов и корпусов машин. */
    public static Item thaumSteelIngot;

    private UTItems() {
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        thaumSteelIngot = new Item()
                .setRegistryName(UnboundTech.MODID, "thaum_steel_ingot")
                .setTranslationKey(UnboundTech.MODID + ".thaum_steel_ingot")
                .setCreativeTab(UTCreativeTab.INSTANCE);

        event.getRegistry().register(thaumSteelIngot);
    }

    /** Вызывается из init: оредикт должен быть готов до рецептов в postInit. */
    public static void registerOreDictionary() {
        OreDictionary.registerOre("ingotThaumSteel", new ItemStack(thaumSteelIngot));
    }
}
