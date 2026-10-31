# Enhanced Detector Radar

Small compatibility add-on for the exact mod versions used by pack 6.9.2.

## Player detector

`getPlayerPos(name)` and `getPlayer(name)` now default to three decimal places.
The original optional precision argument is preserved.

## Neo radar

Every row returned by `scanForSubLevels` additionally contains:

- `velocity_x`, `velocity_y`, `velocity_z`: world-axis velocity in blocks/second;
- `size_x`, `size_y`, `size_z`: local construction dimensions in blocks.

All original fields and normal/creative radar range/error behavior are preserved.

## Sable engine

The shared impulse-command rate limiter is replaced with NeoPeripheral's
unlimited limiter. `sable_engine.max_rate` and `sable_engine.rate_window` stay
in the original config for compatibility but no longer affect engine calls.
The original `sable_engine.enable` switch remains active and still controls
whether the peripheral is exposed to CC:Tweaked.

Two optional atomic methods are added for high-frequency flight controllers:

- `getPhysicsState()` reads pose, velocities and mass from one physics step;
- `applyControlImpulse(lx, ly, lz, ax, ay, az)` applies both world-axis impulses
  together during one physics step.
- `stepControl(lx, ly, lz, ax, ay, az)` applies the previous combined command
  and returns the resulting coherent physics state in one call. This permits a
  controller update on every Sable physics substep instead of alternating read
  and write substeps.
