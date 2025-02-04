package com.cleanroommc.multiblocked.common.capability;

import com.cleanroommc.multiblocked.api.capability.IO;
import com.cleanroommc.multiblocked.api.capability.MultiblockCapability;
import com.cleanroommc.multiblocked.api.capability.proxy.CapCapabilityProxy;
import com.cleanroommc.multiblocked.api.capability.trait.CapabilityTrait;
import com.cleanroommc.multiblocked.api.gui.widget.imp.recipe.ContentWidget;
import com.cleanroommc.multiblocked.api.json.FluidStackTypeAdapter;
import com.cleanroommc.multiblocked.api.pattern.util.BlockInfo;
import com.cleanroommc.multiblocked.api.recipe.ContentModifier;
import com.cleanroommc.multiblocked.api.recipe.Recipe;
import com.cleanroommc.multiblocked.api.registry.MbdCapabilities;
import com.cleanroommc.multiblocked.common.capability.trait.FluidCapabilityTrait;
import com.cleanroommc.multiblocked.common.capability.widget.FluidContentWidget;
import com.cleanroommc.multiblocked.jei.IJeiIngredientAdapter;
import com.cleanroommc.multiblocked.util.world.DummyWorld;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IIngredientType;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nonnull;
import java.awt.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public class FluidMultiblockCapability extends MultiblockCapability<FluidStack> {
    public static final FluidMultiblockCapability CAP = new FluidMultiblockCapability();

    private  FluidMultiblockCapability() {
        super("fluid", new Color(0x3C70EE).getRGB(), new FluidJeiAdapter());
    }

    @Override
    public FluidStack defaultContent() {
        return new FluidStack(FluidRegistry.WATER, 1000);
    }

    @Override
    public boolean isBlockHasCapability(@Nonnull IO io, @Nonnull TileEntity tileEntity) {
        return !getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, tileEntity).isEmpty();
    }

    @Override
    public FluidStack copyInner(FluidStack content) {
        return content.copy();
    }

    @Override
    public FluidStack copyInnerByModifier(FluidStack content, ContentModifier modifier) {
        FluidStack copy = content.copy();
        copy.amount = ((int) modifier.apply(content.amount));
        return copy;
    }

    @Override
    public FluidCapabilityProxy createProxy(@Nonnull IO io, @Nonnull TileEntity tileEntity) {
        return new FluidCapabilityProxy(tileEntity);
    }

    @Override
    public ContentWidget<? super FluidStack> createContentWidget() {
        return new FluidContentWidget();
    }

    @Override
    public boolean hasTrait() {
        return true;
    }

    @Override
    public CapabilityTrait createTrait() {
        return new FluidCapabilityTrait();
    }

    @Override
    public BlockInfo[] getCandidates() {
        List<BlockInfo> list = new ArrayList<>();
        DummyWorld dummyWorld = new DummyWorld();
        for (Block block : ForgeRegistries.BLOCKS.getValuesCollection()) {
            if (block.getRegistryName() != null) {
                String path = block.getRegistryName().getPath();
                if (path.contains("tank") || path.contains("fluid") || path.contains("liquid")) {
                    try {
                        if (block.hasTileEntity(block.getDefaultState())) {
                            TileEntity tileEntity = block.createTileEntity(dummyWorld, block.getDefaultState());
                            if (tileEntity != null  && isBlockHasCapability(IO.BOTH, tileEntity)) {
                                list.add(new BlockInfo(block.getDefaultState(), tileEntity));
                            }
                        }
                    } catch (Throwable ignored) { }
                }
            }
        }
        return list.toArray(new BlockInfo[0]);
    }

    @Override
    public FluidStack deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        return FluidStackTypeAdapter.INSTANCE.deserialize(jsonElement, type, jsonDeserializationContext);
    }

    @Override
    public JsonElement serialize(FluidStack fluidStack, Type type, JsonSerializationContext jsonSerializationContext) {
        return FluidStackTypeAdapter.INSTANCE.serialize(fluidStack, type, jsonSerializationContext);
    }

    public static class FluidCapabilityProxy extends CapCapabilityProxy<IFluidHandler, FluidStack> {

        public FluidCapabilityProxy(TileEntity tileEntity) {
            super(MbdCapabilities.FLUID, tileEntity, CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY);
        }

        @Override
        protected List<FluidStack> handleRecipeInner(IO io, Recipe recipe, List<FluidStack> left, boolean simulate) {
            IFluidHandler capability = getCapability();
            if (capability == null) return left;
            Iterator<FluidStack> iterator = left.iterator();
            if (io == IO.IN) {
                while (iterator.hasNext()) {
                    FluidStack fluidStack = iterator.next();
                    boolean found = false;
                    for (IFluidTankProperties tankProperty : capability.getTankProperties()) {
                        FluidStack stored = tankProperty.getContents();
                        if (stored == null || !stored.isFluidEqual(fluidStack)) {
                            continue;
                        }
                        found = true;
                    }
                    if (!found) continue;
                    FluidStack drained = capability.drain(fluidStack.copy(), !simulate);
                    if (drained == null) continue;
                    fluidStack.amount = fluidStack.amount - drained.amount;
                    if (fluidStack.amount <= 0) {
                        iterator.remove();
                    }
                }
            } else if (io == IO.OUT){
                while (iterator.hasNext()) {
                    FluidStack fluidStack = iterator.next();
                    int filled = capability.fill(fluidStack.copy(), !simulate);
                    fluidStack.amount = fluidStack.amount - filled;
                    if (fluidStack.amount <= 0) {
                        iterator.remove();
                    }
                }
            }
            return left.isEmpty() ? null : left;
        }

        FluidStack[] lastContents = new FluidStack[0];
        int[] lastCaps = new int[0];

        @Override
        protected boolean hasInnerChanged() {
            IFluidHandler capability = getCapability();
            if (capability == null) return false;
            IFluidTankProperties[] tanks = capability.getTankProperties();
            boolean same = true;
            if (tanks.length == lastContents.length) {
                for (int i = 0; i < tanks.length; i++) {
                    IFluidTankProperties tank = tanks[i];
                    FluidStack content = tank.getContents();
                    FluidStack lastContent = lastContents[i];
                    if (content == null) {
                        if (lastContent != null) {
                            same = false;
                            break;
                        }
                    } else {
                        if (lastContent == null) {
                            same = false;
                            break;
                        } else if (!content.isFluidEqual(lastContent) || content.amount != lastContent.amount) {
                            same = false;
                            break;
                        }
                    }
                    int cap = tank.getCapacity();
                    int lastCap = lastCaps[i];
                    if (cap != lastCap) {
                        same = false;
                        break;
                    }
                }
            } else {
                same = false;
            }

            if (same) {
                return false;
            }
            lastContents = new FluidStack[tanks.length];
            lastCaps = new int[tanks.length];
            for (int i = 0; i < tanks.length; i++) {
                IFluidTankProperties tank = tanks[i];
                FluidStack content = tank.getContents();
                lastContents[i] = content == null ? null : content.copy();
                lastCaps[i] = tank.getCapacity();
            }
            return true;
        }
    }

    public static class FluidJeiAdapter implements IJeiIngredientAdapter<FluidStack, FluidStack> {

        @Override
        public Class<FluidStack> getInternalIngredientType() {
            return FluidStack.class;
        }

        @Override
        public IIngredientType<FluidStack> getJeiIngredientType() {
            return VanillaTypes.FLUID;
        }

        @Override
        public Stream<FluidStack> apply(FluidStack fluidStack) {
            return Stream.of(fluidStack);
        }
    }
}
