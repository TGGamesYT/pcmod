package dev.tggamesyt.pcmod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Unit;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.Set;
import java.util.function.Function;


public class Pcmod implements ModInitializer {

    static public String MOD_ID = "pcmod";

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    /** Sounds like an iron block, breaks like a shulker box (quickly, with anything, fastest with a pickaxe). */
    private static BlockBehaviour.Properties pcProperties(ResourceKey<Block> key) {
        return BlockBehaviour.Properties.of().setId(key).mapColor(MapColor.METAL).strength(2.0F)
                .sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.POPPED);
    }

    private static Item item(String name, Function<Item.Properties, Item.Properties> properties) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id(name));
        return Registry.register(BuiltInRegistries.ITEM, key, new Item(properties.apply(new Item.Properties().setId(key))));
    }

    private static Item item(String name) {
        return item(name, p -> p);
    }

    /** On a CPU: thermal paste was put on it. */
    public static final DataComponentType<Unit> THERMAL_PASTE_APPLIED = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
            id("thermal_paste"),
            DataComponentType.<Unit>builder().persistent(Unit.CODEC).networkSynchronized(StreamCodec.unit(Unit.INSTANCE)).build());

    static Item CPU = item("cpu");
    static Item GPU = item("gpu");
    static Item RAM = item("ram");
    static Item COOLERS = item("coolers");
    static Item MOTHERBOARD = item("motherboard");
    static Item SSD = item("ssd");
    static Item PSU = item("psu");
    static Item OLD_MOTHERBOARD = item("old_motherboard");
    static Item OLD_RAM = item("old_ram");
    static Item HDD = item("hdd");
    /** Each use on a CPU costs one durability. */
    static Item THERMAL_PASTE = item("thermal_paste", p -> p.durability(8));

    static ResourceKey<Block> PC_BLOCK_KEY = ResourceKey.create(Registries.BLOCK, id("pc"));
    static Block PC_BLOCK = Registry.register(BuiltInRegistries.BLOCK, PC_BLOCK_KEY, new CaseBlock(pcProperties(PC_BLOCK_KEY)) {
        @Override
        public PcLayout layout() {
            return PcLayout.REGULAR;
        }
    });
    static ResourceKey<Item> PC_ITEM_KEY = ResourceKey.create(Registries.ITEM, id("pc"));
    static Item PC_ITEM = Registry.register(BuiltInRegistries.ITEM, PC_ITEM_KEY,
            new CaseItem(PC_BLOCK, new Item.Properties().setId(PC_ITEM_KEY), "block.pcmod.pc", "item.pcmod.pc_case"));

    static ResourceKey<Block> VILLAGER_PC_BLOCK_KEY = ResourceKey.create(Registries.BLOCK, id("villager_pc"));
    static Block VILLAGER_PC_BLOCK = Registry.register(BuiltInRegistries.BLOCK, VILLAGER_PC_BLOCK_KEY, new CaseBlock(pcProperties(VILLAGER_PC_BLOCK_KEY)) {
        @Override
        public PcLayout layout() {
            return PcLayout.VILLAGER;
        }
    });
    static ResourceKey<Item> VILLAGER_PC_ITEM_KEY = ResourceKey.create(Registries.ITEM, id("villager_pc"));
    static Item VILLAGER_PC_ITEM = Registry.register(BuiltInRegistries.ITEM, VILLAGER_PC_ITEM_KEY,
            new CaseItem(VILLAGER_PC_BLOCK, new Item.Properties().setId(VILLAGER_PC_ITEM_KEY), "block.pcmod.villager_pc", "item.pcmod.villager_pc_case"));

    public static final BlockEntityType<CaseBlockEntity> CASE_BLOCK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
            id("pc"), new BlockEntityType<>(CaseBlockEntity::new, Set.of(PC_BLOCK, VILLAGER_PC_BLOCK)));

    /** A PC item with every part in it. */
    private static ItemStack finished(Item pc, PcLayout layout) {
        ItemStack stack = new ItemStack(pc);
        stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(layout.filled()));
        return stack;
    }

    public static final ResourceKey<CreativeModeTab> CUSTOM_CREATIVE_TAB_KEY = ResourceKey.create(
            BuiltInRegistries.CREATIVE_MODE_TAB.key(), id("pc_tab")
    );
    public static final CreativeModeTab CUSTOM_CREATIVE_TAB = FabricCreativeModeTab.builder()
            .icon(() -> new ItemStack(PC_ITEM))
            .title(Component.translatable("creativeTab.pcmod"))
            .displayItems((params, output) -> {
                output.accept(finished(PC_ITEM, PcLayout.REGULAR));
                output.accept(PC_ITEM);
                output.accept(finished(VILLAGER_PC_ITEM, PcLayout.VILLAGER));
                output.accept(VILLAGER_PC_ITEM);
                output.accept(MOTHERBOARD);
                output.accept(OLD_MOTHERBOARD);
                output.accept(CPU);
                output.accept(COOLERS);
                output.accept(THERMAL_PASTE);
                output.accept(GPU);
                output.accept(RAM);
                output.accept(OLD_RAM);
                output.accept(SSD);
                output.accept(HDD);
                output.accept(PSU);
            })
            .build();

    @Override
    public void onInitialize() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, CUSTOM_CREATIVE_TAB_KEY, CUSTOM_CREATIVE_TAB);
        Gamers.init();
    }
}
