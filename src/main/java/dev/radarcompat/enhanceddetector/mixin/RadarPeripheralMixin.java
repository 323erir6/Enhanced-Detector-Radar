package dev.radarcompat.enhanceddetector.mixin;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.MethodResult;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import io.p2vman.neoperipheral.Config;
import io.p2vman.neoperipheral.IPrefSource;
import io.p2vman.neoperipheral.lua.Table;
import io.p2vman.neoperipheral.lua.TableArray;
import io.p2vman.neoperipheral.lua.UUIDTable;
import io.p2vman.neoperipheral.peripheral.RadarPeripheral;
import java.util.Random;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Extends NeoPeripheral radar rows while remaining compatible with the chunk-loader radar fix. */
@Mixin(value = RadarPeripheral.class, remap = false)
public abstract class RadarPeripheralMixin {
    @Shadow @Final private Random random;
    @Shadow @Final private boolean creative;

    @Inject(method = "scanForSubLevels", at = @At("HEAD"), cancellable = true, remap = false)
    private void enhancedDetectorRadar$scanWithMotionAndSize(
        IArguments arguments,
        CallbackInfoReturnable<MethodResult> callback
    ) throws LuaException {
        // The chunk-loader mod supplies the same extended fields together with
        // its stable construction coordinates. Let its single implementation
        // own the call instead of racing two cancellable HEAD injections.
        if (ModList.get().isLoaded("sable_autonomous_chunk_loader")) {
            return;
        }

        RadarPeripheral self = (RadarPeripheral) (Object) this;
        IPrefSource source = self.getSource();

        int radius = this.creative
            ? arguments.optInt(0, 16)
            : Math.max(16, Math.min(
                Config._RADAR_RANGE_LIMIT,
                arguments.optInt(0, Config._RADAR_DEFAULT_RANGE)
            ));
        boolean absolute = arguments.optBoolean(1, false) && this.creative;

        TableArray results = new TableArray();
        int index = 1;
        Vec3 radarPosition = Sable.HELPER.projectOutOfSubLevel(
            source.getLevel(),
            source.getPos().getCenter()
        );

        BoundingBox3dc searchBounds = new BoundingBox3d(
            radarPosition.x - radius,
            radarPosition.y - radius,
            radarPosition.z - radius,
            radarPosition.x + radius,
            radarPosition.y + radius,
            radarPosition.z + radius
        );

        for (SubLevel candidate : Sable.HELPER.getAllIntersecting(source.getLevel(), searchBounds)) {
            if (!(candidate instanceof ServerSubLevel subLevel)) {
                continue;
            }
            Table row = new Table();
            row.put("id", new UUIDTable(subLevel.getUniqueId()));

            String name = subLevel.getName();
            if (name != null) {
                row.put("name", name);
            }

            Pose3d pose = subLevel.logicalPose();
            Vector3d position = pose.position();
            double dx = radarPosition.x - position.x;
            double dy = radarPosition.y - position.y;
            double dz = radarPosition.z - position.z;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (absolute) {
                row.put("x", position.x);
                row.put("y", position.y);
                row.put("z", position.z);
                row.put("distance", distance);
            } else {
                double error = distance / Math.pow(Math.PI, 3.0);
                row.put("x", dx + randomError(error));
                row.put("y", dy + randomError(error));
                row.put("z", dz + randomError(error));
                row.put("distance", distance + randomError(error));
            }

            Quaterniond orientation = pose.orientation();
            row.put("q_x", orientation.x);
            row.put("q_y", orientation.y);
            row.put("q_z", orientation.z);
            row.put("q_w", orientation.w);

            Vector3d velocity = subLevel.latestLinearVelocity;
            row.put("velocity_x", finiteOrZero(velocity.x));
            row.put("velocity_y", finiteOrZero(velocity.y));
            row.put("velocity_z", finiteOrZero(velocity.z));

            BoundingBox3ic localBounds = subLevel.getPlot().getBoundingBox();
            row.put("size_x", localBounds.width());
            row.put("size_y", localBounds.height());
            row.put("size_z", localBounds.length());

            results.put(index++, row);
        }

        callback.setReturnValue(MethodResult.of(results));
    }

    private double randomError(double magnitude) {
        return (this.random.nextDouble() * 2.0 - 1.0) * magnitude;
    }

    private static double finiteOrZero(double value) {
        return Double.isFinite(value) ? value : 0.0;
    }
}
