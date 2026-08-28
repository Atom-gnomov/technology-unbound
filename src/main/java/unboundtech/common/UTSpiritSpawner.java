package unboundtech.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import thaumcraft.api.nodes.INode;
import unboundtech.UnboundTech;
import unboundtech.common.blocks.BlockMachineBase;
import unboundtech.common.entities.EntityTechnoSpirit;

/**
 * Спавн и эскалация техно-духов (`techno_spirit.md` §4, §4.1):
 *
 *  - машина копит «наработку» пока АКТИВНА и узел в 16 блоках (та же
 *    мерка, что у интерференции); каждые 6 000 тиков наработки — новый
 *    дух рядом (первый дух и каждый следующий — это и есть эскалация:
 *    «не трогай — само рассосётся» не работает);
 *  - удар по ЛЮБОМУ духу сбрасывает счётчики всех машин в радиусе 16 —
 *    важно «отогнал», а не «убил» (§10);
 *  - потолок 5 духов на группу машин в радиусе 16 (§4);
 *  - мирный режим — не спавнятся вовсе.
 *
 * Наработка держится в памяти, не в NBT: после перезапуска сервера
 * первые духи придут через те же 5 минут — терпимо для паразита.
 */
@Mod.EventBusSubscriber(modid = UnboundTech.MODID)
public final class UTSpiritSpawner {

    /** §4/§5. */
    public static final int WORK_TICKS_PER_SPIRIT = 6_000;
    public static final int GROUP_RADIUS = 16;
    public static final int GROUP_CAP = 5;
    private static final int SCAN_INTERVAL = 100;

    /** Наработка по машинам: измерение → позиция → тики. */
    private static final Map<Integer, Map<BlockPos, Integer>> WORKED = new HashMap<>();

    private UTSpiritSpawner() {
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote
                || event.world.getTotalWorldTime() % SCAN_INTERVAL != 0) {
            return;
        }
        World world = event.world;
        if (world.getDifficulty() == EnumDifficulty.PEACEFUL) {
            return;
        }
        // Один проход по загруженным тайлам: наши активные машины + узлы.
        List<BlockPos> machines = new ArrayList<>();
        List<BlockPos> nodes = new ArrayList<>();
        for (TileEntity te : world.loadedTileEntityList) {
            if (te.isInvalid()) {
                continue;
            }
            if (te instanceof INode) {
                nodes.add(te.getPos());
            } else if (te instanceof unboundtech.common.tiles.IMachineStatus) {
                IBlockState state = world.getBlockState(te.getPos());
                if (state.getBlock() instanceof BlockMachineBase
                        && state.getValue(BlockMachineBase.ACTIVE)) {
                    machines.add(te.getPos());
                }
            }
        }
        Map<BlockPos, Integer> byPos = WORKED.computeIfAbsent(
                world.provider.getDimension(), d -> new HashMap<>());
        // подчистка: машины, которых больше нет в работе, остывают из карты
        Iterator<BlockPos> it = byPos.keySet().iterator();
        while (it.hasNext()) {
            if (!machines.contains(it.next())) {
                it.remove();
            }
        }
        for (BlockPos machine : machines) {
            boolean nearNode = false;
            for (BlockPos node : nodes) {
                if (node.distanceSq(machine) <= GROUP_RADIUS * GROUP_RADIUS) {
                    nearNode = true;
                    break;
                }
            }
            if (!nearNode) {
                continue;
            }
            int total = byPos.merge(machine, SCAN_INTERVAL, Integer::sum);
            if (total < WORK_TICKS_PER_SPIRIT) {
                continue;
            }
            byPos.put(machine, 0);
            trySpawn(world, machine);
        }
    }

    private static void trySpawn(World world, BlockPos machine) {
        AxisAlignedBB box = new AxisAlignedBB(machine).grow(GROUP_RADIUS);
        if (world.getEntitiesWithinAABB(EntityTechnoSpirit.class, box)
                .size() >= GROUP_CAP) {
            return;   // §4: потолок 5, дальше растёт только замедление
        }
        for (int attempt = 0; attempt < 8; attempt++) {
            BlockPos at = machine.add(world.rand.nextInt(7) - 3,
                    1 + world.rand.nextInt(3), world.rand.nextInt(7) - 3);
            if (world.isAirBlock(at)) {
                EntityTechnoSpirit spirit = new EntityTechnoSpirit(world);
                spirit.setPosition(at.getX() + 0.5, at.getY() + 0.5,
                        at.getZ() + 0.5);
                world.spawnEntity(spirit);
                return;
            }
        }
    }

    /** §4.1: удар по любому духу группы сбрасывает эскалацию в ноль. */
    public static void onSpiritHurt(World world, BlockPos at) {
        Map<BlockPos, Integer> byPos = WORKED.get(world.provider.getDimension());
        if (byPos == null) {
            return;
        }
        byPos.keySet().removeIf(
                pos -> pos.distanceSq(at) <= GROUP_RADIUS * GROUP_RADIUS);
    }
}
