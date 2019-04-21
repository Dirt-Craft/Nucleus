package io.github.nucleuspowered.nucleus.internal.teleport.scanners;

import com.flowpowered.math.vector.Vector3i;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.teleport.TeleportHelperFilter;

import java.util.Optional;

public class AscendingTeleportScanner implements ITeleportScanner {

    @Override
    public Optional<Location<World>> scanFrom(World wold,
            Vector3i position,
            TeleportHelperFilter filter,
            TeleportHelperFilter... filters) {
        return null;
    }

    @Override
    public String getId() {
        return "nucleus:ascending_scan";
    }

    @Override
    public String getName() {
        return "Nucleus Ascending Scan";
    }

}
