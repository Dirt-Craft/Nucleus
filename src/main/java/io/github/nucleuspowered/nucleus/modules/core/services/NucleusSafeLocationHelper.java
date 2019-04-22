/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.core.services;

import com.flowpowered.math.vector.Vector3d;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.api.service.NucleusSafeTeleportService;
import io.github.nucleuspowered.nucleus.api.teleport.TeleportResult;
import io.github.nucleuspowered.nucleus.api.teleport.TeleportResults;
import io.github.nucleuspowered.nucleus.api.teleport.TeleportScanner;
import io.github.nucleuspowered.nucleus.internal.annotations.APIService;
import io.github.nucleuspowered.nucleus.internal.interfaces.Reloadable;
import io.github.nucleuspowered.nucleus.internal.interfaces.ServiceBase;
import io.github.nucleuspowered.nucleus.internal.teleport.NucleusTeleportHandler;
import io.github.nucleuspowered.nucleus.modules.core.config.CoreConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.core.config.SafeTeleportConfig;
import io.github.nucleuspowered.nucleus.modules.teleport.events.AboutToTeleportEvent;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.text.channel.MessageReceiver;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.TeleportHelper;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.teleport.TeleportHelperFilter;

import java.util.Optional;

import javax.inject.Singleton;

@Singleton
@APIService(NucleusSafeTeleportService.class)
public class NucleusSafeLocationHelper implements NucleusSafeTeleportService, Reloadable, ServiceBase {

    private SafeTeleportConfig config = new SafeTeleportConfig();

    @Override
    public TeleportResult teleportPlayer(Player player,
            Location<World> location,
            boolean centreBlock,
            TeleportScanner scanner,
            TeleportHelperFilter filter,
            TeleportHelperFilter... filters) {
        return teleportPlayer(
                player,
                location,
                player.getRotation(),
                centreBlock,
                scanner,
                filter,
                filters
        );
    }

    @Override
    public TeleportResult teleportPlayer(Player player,
            Location<World> location,
            Vector3d rotation,
            boolean centreBlock,
            TeleportScanner scanner,
            TeleportHelperFilter filter,
            TeleportHelperFilter... filters) {

        Optional<Transform<World>> optionalWorldTransform = getSafeTransform(
                location,
                rotation,
                scanner,
                filter,
                filters
        );

        Cause cause = Sponge.getCauseStackManager().getCurrentCause();
        if (optionalWorldTransform.isPresent()) {
            Transform<World> targetLocation = optionalWorldTransform.get();
            AboutToTeleportEvent event = new AboutToTeleportEvent(
                    cause,
                    targetLocation,
                    player
            );

            if (Sponge.getEventManager().post(event)) {
                event.getCancelMessage().ifPresent(x -> {
                    Object o = cause.root();
                    if (o instanceof MessageReceiver) {
                        ((MessageReceiver) o).sendMessage(x);
                    }
                });
                return TeleportResults.FAIL_CANCELLED;
            }

            Optional<Entity> oe = player.getVehicle();
            if (oe.isPresent()) {
                player.setVehicle(null);
            }

            // Do it, tell the routine if it worked.
            NucleusTeleportHandler.TeleportResult tr;
            if (centreBlock) {
                targetLocation = new Transform<>(
                        targetLocation.getExtent(),
                        targetLocation.getLocation().getBlockPosition().toDouble().add(0.5, 0.5, 0.5),
                        targetLocation.getRotation());
            }

            if (player.setTransform(targetLocation)) {
                player.setSpectatorTarget(null);
                return TeleportResults.SUCCESS;
            }

            oe.ifPresent(player::setVehicle);
        }

        return TeleportResults.FAIL_NO_LOCATION;
    }

    @Override public Optional<Location<World>> getSafeLocation(
            Location<World> location,
            TeleportScanner scanner,
            TeleportHelperFilter filter,
            TeleportHelperFilter... filters) {
        return scanner.scanFrom(
                location.getExtent(),
                location.getBlockPosition(),
                this.config.getHeight(),
                this.config.getWidth(),
                TeleportHelper.DEFAULT_FLOOR_CHECK_DISTANCE,
                filter,
                filters
        );
    }

    @Override public Optional<Transform<World>> getSafeTransform(
            Location<World> location,
            Vector3d rotation,
            TeleportScanner scanner,
            TeleportHelperFilter filter,
            TeleportHelperFilter... filters) {
        return getSafeLocation(location, scanner, filter, filters)
                .map(x -> new Transform<>(location.getExtent(), location.getPosition(), rotation));
    }

    @Override
    public void onReload() {
        this.config = Nucleus.getNucleus().getInternalServiceManager().getServiceUnchecked(CoreConfigAdapter.class)
                .getNodeOrDefault().getSafeTeleportConfig();
    }
}
