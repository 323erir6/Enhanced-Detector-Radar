package dev.radarcompat.enhanceddetector.mixin;

import dan200.computercraft.api.lua.LuaFunction;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import io.p2vman.neoperipheral.IPrefSource;
import io.p2vman.neoperipheral.peripheral.SableEnginePeripheral;
import io.p2vman.neoperipheral.util.CallLimiter;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Removes only NeoPeripheral's shared command-rate gate from the Sable engine. */
@Mixin(value = SableEnginePeripheral.class, remap = false)
public abstract class SableEnginePeripheralMixin {
    @Shadow
    private CallLimiter callLimiter;

    @Shadow
    @Nullable
    private ServerSubLevel getSubLevel() {
        throw new AssertionError();
    }

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void enhancedDetectorRadar$removeCommandRateLimit(
        Direction direction,
        IPrefSource source,
        CallbackInfo callback
    ) {
        this.callLimiter = new CallLimiter.EmptyCallLimiter();
    }

    /** Reads one coherent physics snapshot in a single peripheral call. */
    @LuaFunction
    @Nullable
    public final Map<String, Double> getPhysicsState() {
        ServerSubLevel subLevel = this.getSubLevel();
        if (subLevel == null) return null;

        RigidBodyHandle handle = RigidBodyHandle.of(subLevel);
        MassData massData = subLevel.getMassTracker();
        if (handle == null || massData == null) return null;

        return enhancedDetectorRadar$physicsState(subLevel, handle, massData);
    }

    /**
     * Applies the previous controller output and returns the resulting state in
     * one server-thread call. A Lua controller can therefore run once per Sable
     * substep without alternating between read-only and write-only substeps.
     */
    @LuaFunction
    @Nullable
    public final Map<String, Double> stepControl(
        double linearX,
        double linearY,
        double linearZ,
        double angularX,
        double angularY,
        double angularZ
    ) {
        if (!enhancedDetectorRadar$finite(linearX, linearY, linearZ,
            angularX, angularY, angularZ)) return null;

        ServerSubLevel subLevel = this.getSubLevel();
        if (subLevel == null) return null;

        RigidBodyHandle handle = RigidBodyHandle.of(subLevel);
        MassData massData = subLevel.getMassTracker();
        if (handle == null || massData == null) return null;

        handle.applyLinearAndAngularImpulse(
            new Vector3d(linearX, linearY, linearZ),
            new Vector3d(angularX, angularY, angularZ)
        );
        return enhancedDetectorRadar$physicsState(subLevel, handle, massData);
    }

    @Unique
    private static Map<String, Double> enhancedDetectorRadar$physicsState(
        ServerSubLevel subLevel,
        RigidBodyHandle handle,
        MassData massData
    ) {

        Vector3dc position = subLevel.logicalPose().position();
        Quaterniondc orientation = subLevel.logicalPose().orientation();
        Vector3d linearVelocity = handle.getLinearVelocity(new Vector3d());
        Vector3d angularVelocity = handle.getAngularVelocity(new Vector3d());

        Map<String, Double> state = new HashMap<>(16);
        state.put("x", position.x());
        state.put("y", position.y());
        state.put("z", position.z());
        state.put("qx", orientation.x());
        state.put("qy", orientation.y());
        state.put("qz", orientation.z());
        state.put("qw", orientation.w());
        state.put("vx", linearVelocity.x);
        state.put("vy", linearVelocity.y);
        state.put("vz", linearVelocity.z);
        state.put("wx", angularVelocity.x);
        state.put("wy", angularVelocity.y);
        state.put("wz", angularVelocity.z);
        state.put("mass", massData.getMass());
        return state;
    }

    @Unique
    private static boolean enhancedDetectorRadar$finite(double... values) {
        for (double value : values) {
            if (!Double.isFinite(value)) return false;
        }
        return true;
    }

    /** Applies translation and rotation together during one Sable physics step. */
    @LuaFunction
    public final boolean applyControlImpulse(
        double linearX,
        double linearY,
        double linearZ,
        double angularX,
        double angularY,
        double angularZ
    ) {
        if (!enhancedDetectorRadar$finite(linearX, linearY, linearZ,
            angularX, angularY, angularZ)) return false;

        ServerSubLevel subLevel = this.getSubLevel();
        if (subLevel == null) return false;

        RigidBodyHandle handle = RigidBodyHandle.of(subLevel);
        if (handle == null) return false;
        handle.applyLinearAndAngularImpulse(
            new Vector3d(linearX, linearY, linearZ),
            new Vector3d(angularX, angularY, angularZ)
        );
        return true;
    }
}
