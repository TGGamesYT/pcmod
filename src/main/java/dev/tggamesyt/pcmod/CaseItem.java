package dev.tggamesyt.pcmod;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;

/** A PC item. Called a case while nothing is in it. */
public class CaseItem extends BlockItem {
    private final String fullName;
    private final String emptyName;

    public CaseItem(Block block, Properties properties, String fullName, String emptyName) {
        super(block, properties);
        this.fullName = fullName;
        this.emptyName = emptyName;
    }

    @Override
    public Component getName(ItemStack stack) {
        boolean hasParts = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItemCopyStream().findAny().isPresent();
        return Component.translatable(hasParts ? fullName : emptyName);
    }
}
