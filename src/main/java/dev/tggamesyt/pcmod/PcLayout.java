package dev.tggamesyt.pcmod;

import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * What goes into one kind of PC: its parts (one slot each, in this order) with their boxes from the PC's model
 * (facing north), which of them need the motherboard, and the case's shapes when closed and when open.
 */
public final class PcLayout {
    /** One slot in a PC. Several slots can take the same item (the two RAM slots). */
    public static final class Slot {
        public final String name;
        public final BooleanProperty property;
        private final Supplier<Item> item;
        public final boolean needsMotherboard;
        int index;
        private final Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);

        Slot(String name, Supplier<Item> item, boolean needsMotherboard, VoxelShape... boxes) {
            this.name = name;
            this.property = BooleanProperty.create(name);
            this.item = item;
            this.needsMotherboard = needsMotherboard;
            VoxelShape north = Shapes.or(Shapes.empty(), boxes);
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                shapes.put(direction, rotate(direction, north));
            }
        }

        public Item item() {
            return item.get();
        }

        public int index() {
            return index;
        }

        public VoxelShape shape(Direction facing) {
            return shapes.get(facing);
        }
    }

    public static final PcLayout REGULAR;
    public static final PcLayout VILLAGER;

    static {
        Slot motherboard = new Slot("motherboard", () -> Pcmod.MOTHERBOARD, false,
                Block.box(4.05, 7, 6.45, 4.55, 17, 13.95),
                Block.box(4.05, 11.5, 12.95, 5.05, 16.5, 14.95),
                Block.box(4.55, 12, 7.95, 4.8, 16, 8.2),
                Block.box(4.05, 15, 8.95, 5.05, 16.5, 14.95),
                Block.box(4.55, 12, 7.45, 4.8, 16, 7.7),
                Block.box(4.55, 12.25, 9.2, 5.05, 14.75, 11.7));
        Slot psu = new Slot("psu", () -> Pcmod.PSU, false, Block.box(6, 1.05, 9.95, 11, 5.05, 14.95));
        Slot coolers = new Slot("coolers", () -> Pcmod.COOLERS, true,
                Block.box(6, 13.5, 13.9, 9, 16.5, 14.9),
                Block.box(5.05, 12.25, 9.2, 5.55, 14.75, 11.7));
        Slot gpu = new Slot("gpu", () -> Pcmod.GPU, true, Block.box(4.55, 10, 6.95, 9.05, 11, 14.95));
        Slot cpu = new Slot("cpu", () -> Pcmod.CPU, true, Block.box(4.55, 13, 9.95, 5.05, 14, 10.95));
        Slot ram1 = new Slot("ram1", () -> Pcmod.RAM, true, Block.box(4.55, 12.25, 8.0125, 5.3, 15.75, 8.1375));
        Slot ram2 = new Slot("ram2", () -> Pcmod.RAM, true, Block.box(4.55, 12.25, 7.5125, 5.3, 15.75, 7.6375));
        Slot ssd = new Slot("ssd", () -> Pcmod.SSD, true, Block.box(4.55, 11.5, 9.95, 4.8, 12.25, 12.95));
        REGULAR = new PcLayout(
                List.of(motherboard, psu, coolers, gpu, cpu, ram1, ram2, ssd),
                List.of(coolers, ram2, ram1, cpu, ssd, gpu, psu, motherboard),
                motherboard, cpu, coolers,
                Shapes.or(
                        Block.box(4, 0, 1, 5, 1, 3), Block.box(4, 0, 13, 5, 1, 15),
                        Block.box(11, 0, 13, 12, 1, 15), Block.box(11, 0, 1, 12, 1, 3),
                        Block.box(4, 1, 1, 12, 18, 15)),
                Shapes.or(
                        Block.box(4, 0, 1, 5, 1, 3), Block.box(4, 0, 13, 5, 1, 15),
                        Block.box(11, 0, 13, 12, 1, 15), Block.box(11, 0, 1, 12, 1, 3),
                        Block.box(4, 0.95, 1, 12, 1.05, 15),
                        Block.box(4, 1, 1, 4.05, 18, 15),
                        Block.box(4, 1, 14.95, 12, 18, 15)));

        // The villager PC lies flat: everything in it sits on the motherboard.
        Slot vMotherboard = new Slot("motherboard", () -> Pcmod.OLD_MOTHERBOARD, false,
                Block.box(3.05, 1.05, 3.05, 12.95, 2, 12.95),
                Block.box(3.05, 2, 3.05, 5.55, 3, 5.55),
                Block.box(8.4, 2, 12.05, 12.4, 2.25, 12.3),
                Block.box(8.4, 2, 11.55, 12.4, 2.25, 11.8));
        Slot vPsu = new Slot("psu", () -> Pcmod.PSU, true, Block.box(3.05, 1.95, 7.95, 8.05, 5.95, 12.95));
        Slot vCpu = new Slot("cpu", () -> Pcmod.CPU, true, Block.box(3.8, 2, 3.8, 4.7, 2.5, 4.7));
        Slot vCoolers = new Slot("coolers", () -> Pcmod.COOLERS, true, Block.box(3.05, 3, 3.05, 5.55, 4, 5.55));
        Slot vHdd = new Slot("hdd", () -> Pcmod.HDD, true, Block.box(8.05, 2, 3.05, 12.95, 3.5, 10.05));
        Slot vRam1 = new Slot("ram1", () -> Pcmod.OLD_RAM, true, Block.box(8.625, 2, 12.1125, 12.125, 2.75, 12.2375));
        Slot vRam2 = new Slot("ram2", () -> Pcmod.OLD_RAM, true, Block.box(8.625, 2, 11.6125, 12.125, 2.75, 11.7375));
        VILLAGER = new PcLayout(
                List.of(vMotherboard, vPsu, vCpu, vCoolers, vHdd, vRam1, vRam2),
                List.of(vCoolers, vRam2, vRam1, vCpu, vHdd, vPsu, vMotherboard),
                vMotherboard, vCpu, vCoolers,
                Shapes.or(
                        Block.box(3, 0, 3, 5, 1, 5), Block.box(3, 0, 11, 5, 1, 13),
                        Block.box(11, 0, 11, 13, 1, 13), Block.box(11, 0, 3, 13, 1, 5),
                        Block.box(3, 1, 3, 13, 6, 13)),
                Shapes.or(
                        Block.box(3, 0, 3, 5, 1, 5), Block.box(3, 0, 11, 5, 1, 13),
                        Block.box(11, 0, 11, 13, 1, 13), Block.box(11, 0, 3, 13, 1, 5),
                        Block.box(3, 0.95, 3, 13, 1.05, 13),
                        Block.box(3, 1, 3, 3.05, 6, 13),
                        Block.box(3, 1, 12.95, 13, 6, 13)));
    }

    public final List<Slot> slots;
    /** When clicking into the PC, parts on top of others are checked first. */
    public final List<Slot> pickOrder;
    public final Slot motherboard;
    public final Slot cpu;
    public final Slot coolers;
    private final Map<Direction, VoxelShape> closedShape = new EnumMap<>(Direction.class);
    private final Map<Direction, VoxelShape> openBase = new EnumMap<>(Direction.class);

    private PcLayout(List<Slot> slots, List<Slot> pickOrder, Slot motherboard, Slot cpu, Slot coolers,
                     VoxelShape closedNorth, VoxelShape openBaseNorth) {
        this.slots = slots;
        this.pickOrder = pickOrder;
        this.motherboard = motherboard;
        this.cpu = cpu;
        this.coolers = coolers;
        for (int i = 0; i < slots.size(); i++) {
            slots.get(i).index = i;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            closedShape.put(direction, rotate(direction, closedNorth));
            openBase.put(direction, rotate(direction, openBaseNorth));
        }
    }

    public int size() {
        return slots.size();
    }

    public VoxelShape closedShape(Direction facing) {
        return closedShape.get(facing);
    }

    /** What is left of the case when it is open (feet, bottom, left and back panels). */
    public VoxelShape openBase(Direction facing) {
        return openBase.get(facing);
    }

    /** Every slot filled, the CPU with thermal paste: what a finished PC holds. */
    public List<ItemStack> filled() {
        List<ItemStack> items = new ArrayList<>();
        for (Slot slot : slots) {
            ItemStack stack = new ItemStack(slot.item());
            if (slot == cpu) {
                stack.set(Pcmod.THERMAL_PASTE_APPLIED, net.minecraft.util.Unit.INSTANCE);
            }
            items.add(stack);
        }
        return items;
    }

    /** Turns a north-facing shape to face the given way (the same turn the block state model gets). */
    static VoxelShape rotate(Direction to, VoxelShape shape) {
        VoxelShape[] buffer = new VoxelShape[]{shape, Shapes.empty()};
        int times = (to.get2DDataValue() - Direction.NORTH.get2DDataValue() + 4) % 4;
        for (int i = 0; i < times; i++) {
            buffer[0].forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> buffer[1] = Shapes.or(
                    buffer[1],
                    Shapes.create(1 - maxZ, minY, minX, 1 - minZ, maxY, maxX)
            ));
            buffer[0] = buffer[1];
            buffer[1] = Shapes.empty();
        }
        return buffer[0];
    }
}
