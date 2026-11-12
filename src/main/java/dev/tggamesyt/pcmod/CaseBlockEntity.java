package dev.tggamesyt.pcmod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Holds the parts in a PC, one slot per {@link PcLayout.Slot}. They go with the PC item when it is broken. */
public class CaseBlockEntity extends BlockEntity {
    private final NonNullList<ItemStack> items;
    /** Generated in a village: it belongs to the gamer villagers, and taking from it is stealing. */
    private boolean villageOwned;

    public CaseBlockEntity(BlockPos pos, BlockState state) {
        super(Pcmod.CASE_BLOCK_ENTITY, pos, state);
        int size = state.getBlock() instanceof CaseBlock block ? block.layout().size() : 0;
        this.items = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    public ItemStack get(PcLayout.Slot slot) {
        return items.get(slot.index());
    }

    /** Puts a part in (or takes it out with an empty stack) and updates the block so it is drawn. */
    public void set(PcLayout.Slot slot, ItemStack stack) {
        items.set(slot.index(), stack);
        setChanged();
        if (level != null && getBlockState().getBlock() instanceof CaseBlock block) {
            BlockState state = getBlockState();
            BlockState updated = block.withParts(state, items);
            if (updated != state) {
                level.setBlock(getBlockPos(), updated, Block.UPDATE_ALL);
            }
        }
    }

    public boolean isVillageOwned() {
        return villageOwned;
    }

    /** Every slot has its part. */
    public boolean isFull() {
        return items.stream().noneMatch(ItemStack::isEmpty);
    }

    /** Fills a PC placed by world generation in a village (the block state already shows the parts). */
    void fillForVillage(java.util.List<ItemStack> parts) {
        for (int i = 0; i < items.size() && i < parts.size(); i++) {
            items.set(i, parts.get(i));
        }
        villageOwned = true;
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        if (villageOwned) {
            output.putBoolean("VillageOwned", true);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int i = 0; i < items.size(); i++) {
            items.set(i, ItemStack.EMPTY);
        }
        ContainerHelper.loadAllItems(input, items);
        villageOwned = input.getBooleanOr("VillageOwned", false);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        components.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("Items");
    }
}
