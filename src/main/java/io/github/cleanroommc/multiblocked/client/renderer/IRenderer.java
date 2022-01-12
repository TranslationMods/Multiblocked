package io.github.cleanroommc.multiblocked.client.renderer;

import crafttweaker.annotations.ZenRegister;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import stanhebben.zenscript.annotations.ZenClass;

@ZenClass("mods.multiblocked.client.IRenderer")
@ZenRegister
public interface IRenderer {

    @SideOnly(Side.CLIENT)
    void renderItem(ItemStack stack);

    @SideOnly(Side.CLIENT)
    default void renderBlockDamage(IBlockState state, BlockPos pos, TextureAtlasSprite texture, IBlockAccess blockAccess) {

    }

    @SideOnly(Side.CLIENT)
    boolean renderBlock(IBlockState state, BlockPos pos, IBlockAccess blockAccess, BufferBuilder buffer);

    @SideOnly(Side.CLIENT)
    default void register(TextureMap map) {

    }

    @SideOnly(Side.CLIENT)
    default TextureAtlasSprite getParticleTexture() {
        return Minecraft.getMinecraft().getTextureMapBlocks().getMissingSprite();
    }
}