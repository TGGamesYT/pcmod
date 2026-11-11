package dev.tggamesyt.pcmod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;


public class Pcmod implements ModInitializer {

    static public String MOD_ID = "pcmod";
    static ResourceKey<Block> BLOCK_KEY = ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, "pc"));
    static ResourceKey<Item> ITEM_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "pc"));
    static Block PC_BLOCK = Registry.register(BuiltInRegistries.BLOCK, BLOCK_KEY, new PCblock(BlockBehaviour.Properties.of().setId(BLOCK_KEY)));
    static BlockItem BLOCK_ITEM = new BlockItem(PC_BLOCK, new Item.Properties().setId(ITEM_KEY));
    static Item PC_ITEM = Registry.register(BuiltInRegistries.ITEM, ITEM_KEY, BLOCK_ITEM);

    static ResourceKey<Item> CPU_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "cpu"));
    static Item CPU = Registry.register(BuiltInRegistries.ITEM, CPU_KEY, new Item(new Item.Properties().setId(CPU_KEY)));
    static ResourceKey<Item> GPU_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "gpu"));
    static Item GPU = Registry.register(BuiltInRegistries.ITEM, GPU_KEY, new Item(new Item.Properties().setId(GPU_KEY)));
    static ResourceKey<Item> RAM_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "ram"));
    static Item RAM = Registry.register(BuiltInRegistries.ITEM, RAM_KEY, new Item(new Item.Properties().setId(RAM_KEY)));
    static ResourceKey<Item> COOLERS_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "coolers"));
    static Item COOLERS = Registry.register(BuiltInRegistries.ITEM, COOLERS_KEY, new Item(new Item.Properties().setId(COOLERS_KEY)));
    static ResourceKey<Item> MOTHERBOARD_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "motherboard"));
    static Item MOTHERBOARD = Registry.register(BuiltInRegistries.ITEM, MOTHERBOARD_KEY, new Item(new Item.Properties().setId(MOTHERBOARD_KEY)));

    public static final ResourceKey<CreativeModeTab> CUSTOM_CREATIVE_TAB_KEY = ResourceKey.create(
            BuiltInRegistries.CREATIVE_MODE_TAB.key(), Identifier.fromNamespaceAndPath(MOD_ID, "pc_tab")
    );
    public static final CreativeModeTab CUSTOM_CREATIVE_TAB = FabricCreativeModeTab.builder()
            .icon(() -> new ItemStack(PC_ITEM))
            .title(Component.translatable("creativeTab.pcmod"))
            .displayItems((params, output) -> {
                output.accept(PC_ITEM);
                output.accept(CPU);
                output.accept(GPU);
                output.accept(RAM);
                output.accept(COOLERS);
                output.accept(MOTHERBOARD);
            })
            .build();
    @Override
    public void onInitialize() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, CUSTOM_CREATIVE_TAB_KEY, CUSTOM_CREATIVE_TAB);
    }
}
