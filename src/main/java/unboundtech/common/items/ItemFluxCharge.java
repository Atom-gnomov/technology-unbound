package unboundtech.common.items;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import thaumcraft.common.config.ConfigBlocks;

/**
 * Флюкс-Заряд (`05_objects/flux_condenser.md` §4.2): сгущённая порча,
 * единственный способ носить флюкс в кармане. Сырьё флюкс-патрона.
 *
 * Заведён по находке PROG-3/CAN-3: флюкс-патрон был непроизводим — в порте
 * нет предмета-флюкса. Делается только Флюкс-Конденсатором в режиме
 * сгущения (4 Praecantatio + 2 000 EU), эквивалент 10 000 EU.
 *
 * «Держать можно. Ронять не стоит»: предмет, погибший в лаве или в огне,
 * ставит на своё место блок флюкс-газа. Ловится в {@link #onEntityItemUpdate}
 * — хука на смерть от взрыва у EntityItem нет, эта часть карточки
 * сознательно сужена до огня и лавы.
 */
public class ItemFluxCharge extends Item {

    private static final String TOOLTIP_KEY = "unboundtech.tooltip.flux_charge";

    public ItemFluxCharge() {
        this.setMaxStackSize(16);
    }

    @Override
    public boolean onEntityItemUpdate(EntityItem entity) {
        World world = entity.world;
        if (world.isRemote || !(entity.isInLava() || entity.isBurning())) {
            return false;
        }
        BlockPos pos = new BlockPos(entity);
        // Газ легче лавы: встаёт в первую проходимую клетку над точкой гибели.
        for (int up = 0; up < 3; up++) {
            BlockPos at = pos.up(up);
            if (world.isAirBlock(at)) {
                world.setBlockState(at, ConfigBlocks.blockFluxGas.getDefaultState(), 3);
                break;
            }
        }
        entity.setDead();
        return true;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world,
                               List<String> lines, ITooltipFlag flag) {
        lines.add("§5" + I18n.translateToLocal(TOOLTIP_KEY));
    }
}
