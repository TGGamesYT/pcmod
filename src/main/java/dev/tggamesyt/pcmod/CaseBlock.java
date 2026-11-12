package dev.tggamesyt.pcmod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A PC parts can be put into (see {@link PcLayout} for which). Right-clicking it opens it (the closed panels stop
 * being drawn); while open, parts are put in by right-clicking with them and taken out by right-clicking them with an
 * empty hand or with the same kind of part. Right-clicking the case itself (or sneaking) closes it again.
 * <p>
 * The parts are kept in the block entity, so they stay with the PC when it is broken. The block state mirrors which
 * slots are filled, which is what the model and the click shapes use.
 */
public abstract class CaseBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    public static final BooleanProperty PASTE = BooleanProperty.create("paste");

    private final Map<BlockState, VoxelShape> shapes = new HashMap<>();

    protected CaseBlock(Properties properties) {
        super(properties);
        BlockState state = defaultBlockState().setValue(FACING, Direction.NORTH).setValue(OPEN, false).setValue(PASTE, false);
        for (PcLayout.Slot slot : layout().slots) {
            state = state.setValue(slot.property, false);
        }
        registerDefaultState(state);
        for (BlockState possible : getStateDefinition().getPossibleStates()) {
            shapes.put(possible, computeShape(possible));
        }
    }

    /** Which parts this PC takes. Returns a constant (it is asked for while the block is still being built). */
    public abstract PcLayout layout();

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN, PASTE);
        for (PcLayout.Slot slot : layout().slots) {
            builder.add(slot.property);
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CaseBlockEntity(pos, state);
    }

    /** Faces the player, and shows the parts its item carries. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        NonNullList<ItemStack> items = NonNullList.withSize(layout().size(), ItemStack.EMPTY);
        context.getItemInHand().getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        return withParts(state, items);
    }

    /** The state with the part properties matching what is in the slots. */
    BlockState withParts(BlockState state, List<ItemStack> items) {
        PcLayout layout = layout();
        for (PcLayout.Slot slot : layout.slots) {
            state = state.setValue(slot.property, !items.get(slot.index()).isEmpty());
        }
        ItemStack cpu = items.get(layout.cpu.index());
        return state.setValue(PASTE, !cpu.isEmpty() && cpu.has(Pcmod.THERMAL_PASTE_APPLIED));
    }

    // -------------------------------------------------------------------------------------------------------------
    // Shapes: closed it is a box; open it is the remaining panels plus the parts, so the parts can be clicked
    // -------------------------------------------------------------------------------------------------------------

    private VoxelShape computeShape(BlockState state) {
        Direction facing = state.getValue(FACING);
        if (!state.getValue(OPEN)) {
            return layout().closedShape(facing);
        }
        VoxelShape shape = layout().openBase(facing);
        for (PcLayout.Slot slot : layout().slots) {
            if (state.getValue(slot.property)) {
                shape = Shapes.or(shape, slot.shape(facing));
            }
        }
        return shape;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.get(state);
    }

    /** Closed it is solid; open only the panels that are still there (feet, bottom, left and back) are. */
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        return state.getValue(OPEN) ? layout().openBase(facing) : layout().closedShape(facing);
    }

    /** Where in the block the player clicked: just behind the clicked face, so inside what was clicked. */
    private static Vec3 clickPoint(BlockPos pos, BlockHitResult hit) {
        return hit.getLocation().subtract(Vec3.atLowerCornerOf(pos))
                .subtract(Vec3.atLowerCornerOf(hit.getDirection().getUnitVec3i()).scale(0.001));
    }

    /** The installed part the player clicked on, or null when they clicked the case itself. */
    private PcLayout.@Nullable Slot clickedSlot(BlockState state, Vec3 point) {
        Direction facing = state.getValue(FACING);
        for (PcLayout.Slot slot : layout().pickOrder) {
            if (state.getValue(slot.property) && distance(slot.shape(facing), point) < 1.0E-4) {
                return slot;
            }
        }
        return null;
    }

    private static double distance(VoxelShape shape, Vec3 point) {
        double best = Double.MAX_VALUE;
        for (AABB box : shape.toAabbs()) {
            double dx = Math.max(Math.max(box.minX - point.x, 0.0), point.x - box.maxX);
            double dy = Math.max(Math.max(box.minY - point.y, 0.0), point.y - box.maxY);
            double dz = Math.max(Math.max(box.minZ - point.z, 0.0), point.z - box.maxZ);
            best = Math.min(best, dx * dx + dy * dy + dz * dz);
        }
        return Math.sqrt(best);
    }

    // -------------------------------------------------------------------------------------------------------------
    // Using it
    // -------------------------------------------------------------------------------------------------------------

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        boolean isPart = !slotsFor(stack).isEmpty();
        boolean isPaste = stack.is(Pcmod.THERMAL_PASTE);
        if (!isPart && !isPaste) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!(level.getBlockEntity(pos) instanceof CaseBlockEntity pc)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!state.getValue(OPEN)) {
            setOpen(state, level, pos, player, true);
            witnessed(level, pos, player);
            return InteractionResult.SUCCESS;
        }
        if (isPaste) {
            applyPaste(stack, level, pos, player, hand, pc);
            return InteractionResult.SUCCESS;
        }
        // Clicking a part with the same kind of part takes it out instead.
        Vec3 point = clickPoint(pos, hit);
        PcLayout.Slot clicked = player.isShiftKeyDown() ? null : clickedSlot(state, point);
        if (clicked != null && stack.is(clicked.item())) {
            takeOut(clicked, point, state, level, pos, player, pc);
        } else {
            insert(stack, level, pos, player, pc);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof CaseBlockEntity pc)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!state.getValue(OPEN)) {
            setOpen(state, level, pos, player, true);
            witnessed(level, pos, player);
            return InteractionResult.SUCCESS;
        }
        Vec3 point = clickPoint(pos, hit);
        PcLayout.Slot slot = player.isShiftKeyDown() ? null : clickedSlot(state, point);
        if (slot == null) {
            setOpen(state, level, pos, player, false);
        } else {
            takeOut(slot, point, state, level, pos, player, pc);
        }
        return InteractionResult.SUCCESS;
    }

    /** Opening, looting or breaking a village PC is stealing if a gamer villager sees it. */
    private static void witnessed(Level level, BlockPos pos, Player player) {
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof CaseBlockEntity pc && pc.isVillageOwned()) {
            Gamers.onTamper(serverLevel, pos, serverPlayer);
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        witnessed(level, pos, player);
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** The slots that take this item, in the order they are filled. */
    private List<PcLayout.Slot> slotsFor(ItemStack stack) {
        return layout().slots.stream().filter(slot -> stack.is(slot.item())).toList();
    }

    private static void setOpen(BlockState state, Level level, BlockPos pos, Player player, boolean open) {
        level.setBlock(pos, state.setValue(OPEN, open), Block.UPDATE_ALL);
        level.playSound(null, pos, open ? SoundEvents.IRON_TRAPDOOR_OPEN : SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.gameEvent(player, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
    }

    private void insert(ItemStack stack, Level level, BlockPos pos, Player player, CaseBlockEntity pc) {
        PcLayout.Slot target = null;
        for (PcLayout.Slot slot : slotsFor(stack)) {
            if (pc.get(slot).isEmpty()) {
                target = slot;
                break;
            }
        }
        if (target == null) {
            player.sendOverlayMessage(Component.translatable("message.pcmod.slot_full"));
            return;
        }
        if (target.needsMotherboard && pc.get(layout().motherboard).isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.pcmod.needs_motherboard"));
            return;
        }
        pc.set(target, stack.consumeAndReturn(1, player));
        level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
    }

    private void takeOut(PcLayout.Slot slot, Vec3 point, BlockState state, Level level, BlockPos pos, Player player, CaseBlockEntity pc) {
        PcLayout layout = layout();
        if (slot == layout.motherboard) {
            // Something is still on the motherboard: take out what is on it closest to the click instead.
            PcLayout.Slot closest = null;
            double best = Double.MAX_VALUE;
            for (PcLayout.Slot other : layout.slots) {
                if (other.needsMotherboard && !pc.get(other).isEmpty()) {
                    double distance = distance(other.shape(state.getValue(FACING)), point);
                    if (distance < best) {
                        best = distance;
                        closest = other;
                    }
                }
            }
            if (closest != null) {
                slot = closest;
            }
        }
        if (slot == layout.cpu && !pc.get(layout.coolers).isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.pcmod.cooler_on_cpu"));
            return;
        }
        ItemStack removed = pc.get(slot);
        pc.set(slot, ItemStack.EMPTY);
        give(player, removed);
        witnessed(level, pos, player);
        level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
    }

    /**
     * Into the empty hand, unless the inventory already has some of it to stack with; then into the inventory
     * (and on the ground if there is no room).
     */
    private static void give(Player player, ItemStack stack) {
        Inventory inventory = player.getInventory();
        boolean canStack = false;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack other = inventory.getItem(i);
            if (ItemStack.isSameItemSameComponents(other, stack) && other.getCount() < other.getMaxStackSize()) {
                canStack = true;
                break;
            }
        }
        if (!canStack && player.getMainHandItem().isEmpty()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        } else if (!inventory.add(stack)) {
            Block.popResource(player.level(), player.blockPosition(), stack);
        }
    }

    private void applyPaste(ItemStack paste, Level level, BlockPos pos, Player player, InteractionHand hand, CaseBlockEntity pc) {
        PcLayout layout = layout();
        ItemStack cpu = pc.get(layout.cpu);
        if (cpu.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.pcmod.no_cpu"));
            return;
        }
        if (!pc.get(layout.coolers).isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.pcmod.cooler_on_cpu"));
            return;
        }
        if (cpu.has(Pcmod.THERMAL_PASTE_APPLIED)) {
            player.sendOverlayMessage(Component.translatable("message.pcmod.already_pasted"));
            return;
        }
        ItemStack pasted = cpu.copy();
        pasted.set(Pcmod.THERMAL_PASTE_APPLIED, net.minecraft.util.Unit.INSTANCE);
        pc.set(layout.cpu, pasted);
        paste.hurtAndBreak(1, player, hand);
        level.playSound(null, pos, SoundEvents.HONEY_BLOCK_PLACE, SoundSource.BLOCKS, 1.0F, 1.2F);
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
    }
}
