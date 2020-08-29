/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.vanish.services;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.internal.annotations.ReregisterService;
import io.github.nucleuspowered.nucleus.internal.interfaces.Reloadable;
import io.github.nucleuspowered.nucleus.internal.interfaces.ServiceBase;
import io.github.nucleuspowered.nucleus.internal.services.PlayerOnlineService;
import io.github.nucleuspowered.nucleus.internal.traits.InternalServiceManagerTrait;
import io.github.nucleuspowered.nucleus.internal.traits.PermissionTrait;
import io.github.nucleuspowered.nucleus.modules.vanish.VanishModule;
import io.github.nucleuspowered.nucleus.modules.vanish.config.VanishConfigAdapter;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.User;

import java.time.Instant;
import java.util.Optional;

@ReregisterService(PlayerOnlineService.class)
public class VanishPlayerOnlineService implements PlayerOnlineService, ServiceBase, PermissionTrait,
        InternalServiceManagerTrait, Reloadable {

    private boolean allCanSee = false;

    @Override
    public boolean isOnline(CommandSource src, User player) {
        if (player.isOnline()) {
            if (getServiceUnchecked(VanishService.class).isVanished(player)) {
                return this.allCanSee || hasPermission(src, VanishModule.CAN_SEE_PERMISSION);
            }

            return true;
        }

        return false;
    }

    @Override
    public Optional<Instant> lastSeen(CommandSource src, User player) {
        if (isOnline(src, player) || !player.isOnline() || !getServiceUnchecked(VanishService.class).getLastVanishTime(player.getUniqueId()).isPresent()) {
            return player.get(Keys.LAST_DATE_PLAYED);
        } else {
            return getServiceUnchecked(VanishService.class).getLastVanishTime(player.getUniqueId());
        }

    }

    @Override
    public void onReload() throws Exception {
        this.allCanSee = !Nucleus.getNucleus()
                .getInternalServiceManager()
                .getServiceUnchecked(VanishConfigAdapter.class)
                .getNodeOrDefault()
                .isTryHidePlayers();
    }
}
