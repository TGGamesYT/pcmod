package dev.tggamesyt.pcmod.client;

import dev.tggamesyt.pcmod.Pcmod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class PcmodClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (stack.has(Pcmod.THERMAL_PASTE_APPLIED)) {
                lines.add(Component.translatable("tooltip.pcmod.thermal_paste").withStyle(ChatFormatting.GRAY));
            }
        });
    }
}
