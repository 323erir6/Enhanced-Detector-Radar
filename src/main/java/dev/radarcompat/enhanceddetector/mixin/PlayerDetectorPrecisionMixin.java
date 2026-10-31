package dev.radarcompat.enhanceddetector.mixin;

import de.srendi.advancedperipherals.common.addons.computercraft.peripheral.PlayerDetectorPeripheral;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Uses milliblock precision when getPlayerPos is called without a precision argument. */
@Mixin(value = PlayerDetectorPeripheral.class, remap = false)
public abstract class PlayerDetectorPrecisionMixin {
    @ModifyArg(
        method = "getPlayerPos",
        at = @At(
            value = "INVOKE",
            target = "Ldan200/computercraft/api/lua/IArguments;optInt(II)I",
            remap = false
        ),
        index = 1,
        remap = false
    )
    private int enhancedDetectorRadar$useMilliblockPrecisionByDefault(int originalDefault) {
        return 3;
    }
}
