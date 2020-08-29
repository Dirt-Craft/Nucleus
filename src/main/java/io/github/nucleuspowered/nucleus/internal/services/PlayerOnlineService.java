/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.internal.services;

import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.User;

import java.time.Instant;
import java.util.Optional;

@FunctionalInterface
public interface PlayerOnlineService {

    PlayerOnlineService DEFAULT = (src, player) -> player.isOnline();

    boolean isOnline(CommandSource src, User player);

    default Optional<Instant> lastSeen(CommandSource src, User player) {
        return player.get(Keys.LAST_DATE_PLAYED);
    }

}
