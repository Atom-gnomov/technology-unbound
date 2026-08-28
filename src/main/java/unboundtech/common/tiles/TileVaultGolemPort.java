package unboundtech.common.tiles;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import thaumcraft.api.TileThaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaTransport;

/**
 * Голем-порт (`essentia_vault.md` §4.1–4.2): прокси к контроллеру.
 * {@code AIEssentiaGather}/{@code AIEssentiaEmpty} порта ищут
 * {@code IAspectContainer}/{@code IEssentiaTransport} — порт отвечает
 * этими интерфейсами, пересылая всё контроллеру своей структуры.
 */
public class TileVaultGolemPort extends TileThaumcraft
        implements IAspectContainer, IEssentiaTransport {

    private static final int SEARCH_RADIUS = 2;

    /** Контроллер структуры: ищется в кубе 5×5×5, кэшируется по позиции. */
    private BlockPos controllerPos;

    private TileEssentiaVaultController controller() {
        if (this.world == null) {
            return null;
        }
        if (this.controllerPos != null) {
            TileEntity te = this.world.getTileEntity(this.controllerPos);
            if (te instanceof TileEssentiaVaultController
                    && ((TileEssentiaVaultController) te).isFormed()) {
                return (TileEssentiaVaultController) te;
            }
            this.controllerPos = null;
        }
        for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
            for (int dy = -SEARCH_RADIUS; dy <= SEARCH_RADIUS; dy++) {
                for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
                    TileEntity te = this.world.getTileEntity(
                            this.pos.add(dx, dy, dz));
                    if (te instanceof TileEssentiaVaultController
                            && ((TileEssentiaVaultController) te).isFormed()) {
                        this.controllerPos = te.getPos();
                        return (TileEssentiaVaultController) te;
                    }
                }
            }
        }
        return null;
    }

    // ================= IAspectContainer =================

    @Override
    public AspectList getAspects() {
        TileEssentiaVaultController c = this.controller();
        return c == null ? new AspectList() : c.getAspects();
    }

    @Override
    public void setAspects(AspectList list) {
    }

    @Override
    public boolean doesContainerAccept(Aspect aspect) {
        TileEssentiaVaultController c = this.controller();
        return c != null && c.doesContainerAccept(aspect);
    }

    @Override
    public int addToContainer(Aspect aspect, int amount) {
        TileEssentiaVaultController c = this.controller();
        return c == null ? amount : c.addToContainer(aspect, amount);
    }

    @Override
    public boolean takeFromContainer(Aspect aspect, int amount) {
        TileEssentiaVaultController c = this.controller();
        return c != null && c.takeFromContainer(aspect, amount);
    }

    @Override
    public boolean takeFromContainer(AspectList list) {
        TileEssentiaVaultController c = this.controller();
        return c != null && c.takeFromContainer(list);
    }

    @Override
    public boolean doesContainerContainAmount(Aspect aspect, int amount) {
        TileEssentiaVaultController c = this.controller();
        return c != null && c.doesContainerContainAmount(aspect, amount);
    }

    @Override
    public boolean doesContainerContain(AspectList list) {
        TileEssentiaVaultController c = this.controller();
        return c != null && c.doesContainerContain(list);
    }

    @Override
    public int containerContains(Aspect aspect) {
        TileEssentiaVaultController c = this.controller();
        return c == null ? 0 : c.containerContains(aspect);
    }

    // ================= IEssentiaTransport =================

    @Override
    public boolean isConnectable(EnumFacing face) {
        return true;
    }

    @Override
    public boolean canInputFrom(EnumFacing face) {
        return true;
    }

    @Override
    public boolean canOutputTo(EnumFacing face) {
        return true;
    }

    @Override
    public void setSuction(Aspect aspect, int amount) {
    }

    @Override
    public Aspect getSuctionType(EnumFacing face) {
        return null;
    }

    @Override
    public int getSuctionAmount(EnumFacing face) {
        TileEssentiaVaultController c = this.controller();
        return c == null ? 0 : c.getSuctionAmount(face);
    }

    @Override
    public int takeEssentia(Aspect aspect, int amount, EnumFacing face) {
        TileEssentiaVaultController c = this.controller();
        return c == null ? 0 : c.takeEssentia(aspect, amount, face);
    }

    @Override
    public int addEssentia(Aspect aspect, int amount, EnumFacing face) {
        TileEssentiaVaultController c = this.controller();
        return c == null ? 0 : c.addEssentia(aspect, amount, face);
    }

    @Override
    public Aspect getEssentiaType(EnumFacing face) {
        TileEssentiaVaultController c = this.controller();
        return c == null ? null : c.getEssentiaType(face);
    }

    @Override
    public int getEssentiaAmount(EnumFacing face) {
        TileEssentiaVaultController c = this.controller();
        return c == null ? 0 : c.getEssentiaAmount(face);
    }

    @Override
    public int getMinimumSuction() {
        return 0;
    }

    @Override
    public boolean renderExtendedTube() {
        return false;
    }
}
