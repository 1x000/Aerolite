package net.ccbluex.liquidbounce.injection.forge.mixins.optimize;

import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.pathfinder.NodeProcessor;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(NodeProcessor.class)
public class MixinNodeProcessor {

    @Shadow protected IBlockAccess blockaccess;

    @Inject(method = "postProcess", at = @At("HEAD"))
    private void patcher$cleanupBlockAccess(CallbackInfo ci) {
        this.blockaccess = null;
    }

}