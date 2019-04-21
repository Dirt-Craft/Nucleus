/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.internal.teleport.scanners;

import com.flowpowered.math.vector.Vector3i;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.TeleportHelper;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.teleport.TeleportHelperFilter;
import org.spongepowered.plugin.meta.util.NonnullByDefault;

import java.util.Optional;

@NonnullByDefault
public class NoTeleportScanner implements ITeleportScanner {

    @Override
    public Optional<Location<World>> scanFrom(World wold,
            Vector3i position,
            TeleportHelperFilter filter,
            TeleportHelperFilter... filters) {

        TeleportHelper teleportHelper = Sponge.getTeleportHelper();
        return teleportHelper.getSafeLocation(
                new Location<>(wold, position),
                teleportHelper.DEFAULT_HEIGHT,
                teleportHelper.DEFAULT_WIDTH,
                teleportHelper.DEFAULT_FLOOR_CHECK_DISTANCE,
                filter,
                filters
        );
    }

    @Override
    public String getId() {
        return "nucleus:no_scan";
    }

    @Override
    public String getName() {
        return "Nucleus No Scan";
    }

}
