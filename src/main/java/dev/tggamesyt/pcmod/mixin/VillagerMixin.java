package dev.tggamesyt.pcmod.mixin;

import dev.tggamesyt.pcmod.Gamers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Villagers will not trade with a player who stole from a gamer's PC. */
@Mixin(Villager.class)
public abstract class VillagerMixin {
    @Shadow
    private void setUnhappy() {
        throw new AssertionError();
    }

    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void pcmod$refuseThieves(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Villager self = (Villager) (Object) this;
        if (player instanceof ServerPlayer serverPlayer && self.isAlive() && !self.isBaby() && !self.isSleeping()
                && Gamers.isHated(serverPlayer)) {
            if (hand == InteractionHand.MAIN_HAND) {
                setUnhappy();
            }
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }
}
