package dev.tggamesyt.pcmod;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PoiHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Gamer villagers: the villager PC is their workstation, villages come with villager PCs, and stealing from those
 * PCs makes every villager hate you for a while (they will not trade, and iron golems attack you).
 * <p>
 * Stealing is opening a village PC, taking parts out of it or breaking it while a gamer villager sees you or the PC.
 * A gamer who comes back to their PC and finds parts missing also gets mad at a player they see holding PC parts.
 * Their trades are data driven: data/pcmod/trade_set/gamer and data/pcmod/villager_trade/gamer.
 */
public final class Gamers {
    public static final ResourceKey<PoiType> GAMER_POI = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, id("gamer"));
    public static final ResourceKey<VillagerProfession> GAMER = ResourceKey.create(Registries.VILLAGER_PROFESSION, id("gamer"));

    /** How long the villagers stay mad: 5 minutes, counted again from every new theft. */
    private static final int HATE_TICKS = 5 * 60 * 20;
    /** Gamers further than this do not see what you do. */
    private static final double WITNESS_RANGE = 24.0;
    /** Iron golems this close to a hated player go for them. */
    private static final double GOLEM_RANGE = 32.0;
    /** A gamer this close to their PC is "at" it and notices missing parts. */
    private static final double AT_PC_RANGE = 3.0;
    /** Chance for each fitting spot in a village house to get a villager PC. */
    private static final float PC_CHANCE = 0.02F;

    /** Game time (overworld) until which the player is hated by villagers. */
    private static final AttachmentType<Long> HATED_UNTIL = AttachmentRegistry.create(id("hated_until"),
            builder -> builder.persistent(Codec.LONG).copyOnDeath());
    /** On a gamer villager: they found their PC looted, and watch for players holding parts. */
    private static final AttachmentType<Boolean> NOTICED_LOOT = AttachmentRegistry.create(id("noticed_loot"),
            builder -> builder.persistent(Codec.BOOL));

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Pcmod.MOD_ID, path);
    }

    private Gamers() {
    }

    static void init() {
        PoiHelper.register(GAMER_POI.identifier(), 1, 1, Pcmod.VILLAGER_PC_BLOCK);
        Int2ObjectMap<ResourceKey<TradeSet>> trades = new Int2ObjectOpenHashMap<>();
        for (int level = 1; level <= 5; level++) {
            trades.put(level, ResourceKey.create(Registries.TRADE_SET, id("gamer/level_" + level)));
        }
        Registry.register(BuiltInRegistries.VILLAGER_PROFESSION, GAMER, new VillagerProfession(
                Component.translatable("entity.pcmod.villager.gamer"),
                poi -> poi.is(GAMER_POI), poi -> poi.is(GAMER_POI),
                ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_WORK_LIBRARIAN, trades));
        ServerTickEvents.END_LEVEL_TICK.register(Gamers::tick);
    }

    public static boolean isGamer(Villager villager) {
        return villager.getVillagerData().profession().is(GAMER);
    }

    private static long now(ServerLevel level) {
        return level.getServer().overworld().getGameTime();
    }

    public static boolean isHated(ServerPlayer player) {
        Long until = player.getAttached(HATED_UNTIL);
        return until != null && until > now(player.level());
    }

    private static boolean ignored(ServerPlayer player) {
        return player.isCreative() || player.isSpectator();
    }

    // -------------------------------------------------------------------------------------------------------------
    // Getting caught
    // -------------------------------------------------------------------------------------------------------------

    /** The player opened, took from or broke the village PC at the given place. Hated if a gamer saw it. */
    static void onTamper(ServerLevel level, BlockPos pc, ServerPlayer player) {
        if (ignored(player)) {
            return;
        }
        AABB area = new AABB(pc).inflate(WITNESS_RANGE);
        for (Villager villager : level.getEntitiesOfClass(Villager.class, area, v -> isGamer(v) && !v.isSleeping())) {
            if (villager.hasLineOfSight(player) || sees(villager, pc)) {
                hate(level, player, villager);
                return;
            }
        }
    }

    /** A clear line from the villager's eyes to the block. */
    private static boolean sees(Villager villager, BlockPos pos) {
        Vec3 eyes = villager.getEyePosition();
        Vec3 target = Vec3.atCenterOf(pos);
        if (eyes.distanceTo(target) > WITNESS_RANGE) {
            return false;
        }
        BlockHitResult hit = villager.level().clip(new ClipContext(eyes, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, villager));
        return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(pos);
    }

    private static void hate(ServerLevel level, ServerPlayer player, Villager witness) {
        boolean already = isHated(player);
        player.setAttached(HATED_UNTIL, now(level) + HATE_TICKS);
        witness.makeSound(SoundEvents.VILLAGER_NO);
        angryParticles(level, witness);
        if (!already) {
            player.sendOverlayMessage(Component.translatable("message.pcmod.caught").withStyle(ChatFormatting.RED));
        }
    }

    private static void angryParticles(ServerLevel level, Villager villager) {
        level.sendParticles(ParticleTypes.ANGRY_VILLAGER, villager.getX(), villager.getEyeY() + 0.5, villager.getZ(), 1, 0.2, 0.1, 0.2, 0.0);
    }

    // -------------------------------------------------------------------------------------------------------------
    // Every second: angry villagers and golems, gamers checking their PCs
    // -------------------------------------------------------------------------------------------------------------

    private static void tick(ServerLevel level) {
        if (level.getGameTime() % 20 != 0) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            Long until = player.getAttached(HATED_UNTIL);
            if (until == null) {
                continue;
            }
            if (until <= now(level)) {
                player.removeAttached(HATED_UNTIL);
                calmGolems(level, player);
            } else if (!ignored(player)) {
                punish(level, player);
            }
        }
        for (Villager villager : level.getEntities(EntityTypes.VILLAGER, Gamers::isGamer)) {
            checkPc(level, villager);
        }
    }

    private static void punish(ServerLevel level, ServerPlayer player) {
        for (IronGolem golem : level.getEntitiesOfClass(IronGolem.class, player.getBoundingBox().inflate(GOLEM_RANGE),
                golem -> !golem.isPlayerCreated() && golem.getTarget() != player)) {
            golem.setTarget(player);
        }
        if (level.getGameTime() % 40 == 0) {
            for (Villager villager : level.getEntitiesOfClass(Villager.class, player.getBoundingBox().inflate(16.0),
                    villager -> !villager.isSleeping() && villager.hasLineOfSight(player))) {
                angryParticles(level, villager);
            }
        }
    }

    private static void calmGolems(ServerLevel level, ServerPlayer player) {
        for (IronGolem golem : level.getEntitiesOfClass(IronGolem.class, player.getBoundingBox().inflate(GOLEM_RANGE * 2),
                golem -> !golem.isPlayerCreated() && golem.getTarget() == player)) {
            golem.setTarget(null);
        }
    }

    /** A gamer at their village PC notices missing parts, and then looks out for whoever is holding parts. */
    private static void checkPc(ServerLevel level, Villager villager) {
        Optional<GlobalPos> jobSite = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
        if (jobSite.isPresent() && jobSite.get().dimension() == level.dimension()) {
            BlockPos pos = jobSite.get().pos();
            if (villager.position().closerThan(Vec3.atCenterOf(pos), AT_PC_RANGE)
                    && level.getBlockEntity(pos) instanceof CaseBlockEntity pc && pc.isVillageOwned()) {
                if (pc.isFull()) {
                    villager.removeAttached(NOTICED_LOOT);
                } else {
                    villager.setAttached(NOTICED_LOOT, true);
                }
            }
        }
        if (!Boolean.TRUE.equals(villager.getAttached(NOTICED_LOOT)) || villager.isSleeping()) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            if (!ignored(player) && player.distanceTo(villager) <= 16.0 && holdsParts(player) && villager.hasLineOfSight(player)) {
                hate(level, player, villager);
            }
        }
    }

    private static final Set<Item> PART_ITEMS = new HashSet<>();

    private static boolean holdsParts(ServerPlayer player) {
        if (PART_ITEMS.isEmpty()) {
            for (PcLayout layout : List.of(PcLayout.REGULAR, PcLayout.VILLAGER)) {
                for (PcLayout.Slot slot : layout.slots) {
                    PART_ITEMS.add(slot.item());
                }
            }
        }
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (PART_ITEMS.contains(stack.getItem())) {
                return true;
            }
        }
        return false;
    }

    // -------------------------------------------------------------------------------------------------------------
    // Village houses get villager PCs
    // -------------------------------------------------------------------------------------------------------------

    /**
     * Called after a village house piece was placed in a chunk. Every full block in the house with air (or something
     * that breaks instantly, like grass or flowers) on it and a roof over it has a small chance of getting a finished
     * villager PC on it, facing the middle of the house. Spots next to doors are skipped.
     */
    public static void decorateHouse(WorldGenLevel level, BoundingBox chunkBox, BoundingBox house) {
        CaseBlock block = (CaseBlock) Pcmod.VILLAGER_PC_BLOCK;
        BlockPos.MutableBlockPos floor = new BlockPos.MutableBlockPos();
        int minX = Math.max(house.minX(), chunkBox.minX());
        int maxX = Math.min(house.maxX(), chunkBox.maxX());
        int minZ = Math.max(house.minZ(), chunkBox.minZ());
        int maxZ = Math.min(house.maxZ(), chunkBox.maxZ());
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = house.minY(); y < house.maxY(); y++) {
                    floor.set(x, y, z);
                    BlockPos spot = floor.above();
                    RandomSource random = RandomSource.create(level.getSeed() ^ spot.asLong() * 341873128712L);
                    if (random.nextFloat() >= PC_CHANCE || !fits(level, floor, spot)) {
                        continue;
                    }
                    Direction facing = towards(spot, house, random);
                    BlockState state = block.withParts(block.defaultBlockState().setValue(CaseBlock.FACING, facing),
                            block.layout().filled());
                    level.setBlock(spot, state, Block.UPDATE_CLIENTS);
                    if (level.getBlockEntity(spot) instanceof CaseBlockEntity pc) {
                        pc.fillForVillage(block.layout().filled());
                    }
                }
            }
        }
    }

    private static boolean fits(WorldGenLevel level, BlockPos floor, BlockPos spot) {
        BlockState below = level.getBlockState(floor);
        if (!below.isCollisionShapeFullBlock(level, floor)) {
            return false;
        }
        BlockState here = level.getBlockState(spot);
        boolean replaceable = here.isAir() || here.getDestroySpeed(level, spot) == 0.0F
                && !here.isCollisionShapeFullBlock(level, spot) && here.getFluidState().isEmpty() && !here.hasBlockEntity();
        if (!replaceable) {
            return false;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(spot.relative(direction)).getBlock() instanceof DoorBlock) {
                return false;
            }
        }
        // Inside: there is a roof (or a floor above) not far up.
        for (int up = 2; up <= 6; up++) {
            if (!level.getBlockState(spot.above(up)).isAir()) {
                return true;
            }
        }
        return false;
    }

    private static Direction towards(BlockPos spot, BoundingBox house, RandomSource random) {
        double dx = (house.minX() + house.maxX()) / 2.0 - spot.getX();
        double dz = (house.minZ() + house.maxZ()) / 2.0 - spot.getZ();
        if (Math.abs(dx) < 0.5 && Math.abs(dz) < 0.5) {
            return Direction.Plane.HORIZONTAL.getRandomDirection(random);
        }
        if (Math.abs(dx) >= Math.abs(dz)) {
            return dx > 0 ? Direction.EAST : Direction.WEST;
        }
        return dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }
}
