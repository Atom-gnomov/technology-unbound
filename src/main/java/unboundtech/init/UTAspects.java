package unboundtech.init;

import net.minecraft.item.ItemStack;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/**
 * Аспекты предметов и блоков самого мода: без них таумометр молчит,
 * а вещи не плавятся в эссенцию — для контента магического мода это дыра.
 * Значения зеркалят стоимость крафта (таум-сталь = металл + магия).
 */
public final class UTAspects {

    private UTAspects() {
    }

    public static void register() {
        ThaumcraftApi.registerObjectTag(new ItemStack(UTItems.thaumSteelIngot),
                new AspectList().add(Aspect.METAL, 4).add(Aspect.MAGIC, 2).add(Aspect.ORDER, 2));

        ThaumcraftApi.registerObjectTag(new ItemStack(UTBlocks.thaumGenerator),
                new AspectList().add(Aspect.MECHANISM, 8).add(Aspect.ENERGY, 6)
                        .add(Aspect.AURA, 4).add(Aspect.METAL, 4));

        ThaumcraftApi.registerObjectTag(new ItemStack(UTBlocks.aethericEngine),
                new AspectList().add(Aspect.MECHANISM, 8).add(Aspect.AURA, 6)
                        .add(Aspect.ORDER, 4).add(Aspect.METAL, 4));
    }
}
