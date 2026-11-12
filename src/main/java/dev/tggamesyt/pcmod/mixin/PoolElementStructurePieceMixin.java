package dev.tggamesyt.pcmod.mixin;

import dev.tggamesyt.pcmod.Gamers;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

/** After a village house is placed, it may get villager PCs. */
@Mixin(PoolElementStructurePiece.class)
public abstract class PoolElementStructurePieceMixin {
    @Shadow
    @Final
    protected StructurePoolElement element;

    @Inject(method = "place", at = @At("TAIL"))
    private void pcmod$addVillagerPcs(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                                      RandomSource random, BoundingBox chunkBB, BlockPos referencePos, boolean keepJigsaws,
                                      CallbackInfo ci) {
        if (!(element instanceof SinglePoolElement single)) {
            return;
        }
        Optional<Identifier> template = ((SinglePoolElementAccessor) single).pcmod$getTemplate().left();
        if (template.isPresent() && template.get().getPath().startsWith("village/") && template.get().getPath().contains("/houses/")) {
            Gamers.decorateHouse(level, chunkBB, ((StructurePiece) (Object) this).getBoundingBox());
        }
    }
}
