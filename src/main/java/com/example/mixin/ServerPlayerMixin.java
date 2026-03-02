package com.example.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {
    @Inject(method = "displayClientMessage", at = @At("HEAD"), cancellable = true)
    private void onDisplayClientMessage(Component message, boolean actionBar, CallbackInfo ci) {
        if (actionBar && message != null) {
            String text = message.getString().toLowerCase();
            if (text.contains("space")) {
                ServerPlayer player = (ServerPlayer) (Object) this;
                if (com.example.ExampleMod.hudTimers.containsKey(player.getUUID())) {
                    ci.cancel();
                }
            }
        }
    }
}
