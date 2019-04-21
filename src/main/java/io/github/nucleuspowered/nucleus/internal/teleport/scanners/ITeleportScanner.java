/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.internal.teleport.scanners;

import com.flowpowered.math.vector.Vector3i;
import org.spongepowered.api.CatalogType;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.teleport.TeleportHelperFilter;

import java.util.Optional;

public interface ITeleportScanner extends CatalogType {

    Optional<Location<World>> scanFrom(World wold, Vector3i position, TeleportHelperFilter filter, TeleportHelperFilter... filters);

}
