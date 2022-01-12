package io.github.cleanroommc.multiblocked;

import io.github.cleanroommc.multiblocked.api.block.BlockComponent;
import io.github.cleanroommc.multiblocked.api.block.ItemComponent;
import io.github.cleanroommc.multiblocked.api.definition.ControllerDefinition;
import io.github.cleanroommc.multiblocked.api.pattern.FactoryBlockPattern;
import io.github.cleanroommc.multiblocked.api.pattern.TraceabilityPredicate;
import io.github.cleanroommc.multiblocked.api.registry.MultiblockComponents;
import io.github.cleanroommc.multiblocked.client.renderer.impl.IModelRenderer;
import io.github.cleanroommc.multiblocked.client.renderer.impl.SidedOverlayRenderer;
import io.github.cleanroommc.multiblocked.events.Listeners;
import io.github.cleanroommc.multiblocked.network.MultiblockedNetworking;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

import static io.github.cleanroommc.multiblocked.api.pattern.TraceabilityPredicate.blocks;

@Mod.EventBusSubscriber(modid = Multiblocked.MODID)
public class CommonProxy {

    public void preInit() {
        MinecraftForge.EVENT_BUS.register(Listeners.class);
        MultiblockedNetworking.initializeC2S();
        MultiblockedNetworking.initializeS2C();
        ControllerDefinition definition = new ControllerDefinition(new ResourceLocation("multiblocked:test_block"), component -> FactoryBlockPattern.start()
                .aisle("XXX")
                .aisle("X#X")
                .aisle("XYX")
                .where('X', blocks(Blocks.STONE))
                .where('#', TraceabilityPredicate.AIR)
                .where('Y', component.selfPredicate())
                .build());
        definition.formedRenderer = new IModelRenderer(new ResourceLocation(Multiblocked.MODID,"block/emitter"));
        Map<SidedOverlayRenderer.RelativeDirection, String> map = new EnumMap<>(SidedOverlayRenderer.RelativeDirection.class);
        map.put(SidedOverlayRenderer.RelativeDirection.UP, Multiblocked.MODID + ":test/u");
        map.put(SidedOverlayRenderer.RelativeDirection.DOWN, Multiblocked.MODID + ":test/d");
        map.put(SidedOverlayRenderer.RelativeDirection.LEFT, Multiblocked.MODID + ":test/w");
        map.put(SidedOverlayRenderer.RelativeDirection.RIGHT, Multiblocked.MODID + ":test/e");
        map.put(SidedOverlayRenderer.RelativeDirection.FRONT, Multiblocked.MODID + ":test/n");
        map.put(SidedOverlayRenderer.RelativeDirection.BACK, Multiblocked.MODID + ":test/s");
        definition.baseRenderer = new SidedOverlayRenderer(map);
        definition.isOpaqueCube = false;
        MultiblockComponents.registerComponent(definition);
    }

    public void init() {

    }

    public void postInit() {

    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        IForgeRegistry<Block> registry = event.getRegistry();
        MultiblockComponents.COMPONENT_BLOCKS_REGISTRY.values().forEach(registry::register);
        MultiblockComponents.registerTileEntity();
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        IForgeRegistry<Item> registry = event.getRegistry();
        for (BlockComponent block : MultiblockComponents.COMPONENT_BLOCKS_REGISTRY
                .values()) {
            registry.register(createItemBlock(block, ItemComponent::new));
        }
    }

    @SuppressWarnings("ConstantConditions")
    private static <T extends Block> ItemBlock createItemBlock(T block, Function<T, ItemBlock> producer) {
        ItemBlock itemBlock = producer.apply(block);
        itemBlock.setRegistryName(block.getRegistryName());
        return itemBlock;
    }

}
