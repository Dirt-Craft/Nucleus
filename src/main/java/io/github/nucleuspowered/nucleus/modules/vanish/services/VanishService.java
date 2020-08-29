/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.vanish.services;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.internal.interfaces.Reloadable;
import io.github.nucleuspowered.nucleus.internal.interfaces.ServiceBase;
import io.github.nucleuspowered.nucleus.internal.traits.PermissionTrait;
import io.github.nucleuspowered.nucleus.modules.vanish.VanishModule;
import io.github.nucleuspowered.nucleus.modules.vanish.config.VanishConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.vanish.datamodules.VanishUserDataModule;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.tab.TabListEntry;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class VanishService implements Reloadable, PermissionTrait, ServiceBase {

    private boolean isAlter = false;
    private final Map<UUID, Instant> lastVanish = new HashMap<>();

    @Override
    public void onReload() {
        String property = System.getProperty("nucleus.vanish.tablist.enable");
        this.isAlter = property != null && !property.isEmpty() &&
            Nucleus.getNucleus().getInternalServiceManager().getServiceUnchecked(VanishConfigAdapter.class).getNodeOrDefault().isAlterTabList();
    }

    public boolean isVanished(User player) {
        return Nucleus.getNucleus().getUserDataManager().getUnchecked(player).get(VanishUserDataModule.class).isVanished();
    }

    public void vanishPlayer(User player) {
        vanishPlayer(player, false);
    }

    public void vanishPlayer(User player, boolean delay) {
        VanishUserDataModule service = Nucleus.getNucleus().getUserDataManager().getUnchecked(player).get(VanishUserDataModule.class);
        service.setVanished(true);

        if (delay) {
            Task.builder().execute(() -> vanishPlayerInternal(player)).delayTicks(0).name("Nucleus Vanish runnable").submit(Nucleus.getNucleus());
        } else {
            this.lastVanish.put(player.getUniqueId(), Instant.now());
            vanishPlayerInternal(player);
        }
    }

    private void vanishPlayerInternal(User player) {
        VanishUserDataModule service = Nucleus.getNucleus().getUserDataManager().getUnchecked(player).get(VanishUserDataModule.class);
        if (service.isVanished()) {
            player.offer(Keys.VANISH, true);
            player.offer(Keys.VANISH_IGNORES_COLLISION, true);
            player.offer(Keys.VANISH_PREVENTS_TARGETING, true);

            if (this.isAlter) {
                Sponge.getServer().getOnlinePlayers().stream().filter(x -> !player.equals(x) || !hasPermission(x, VanishModule.CAN_SEE_PERMISSION))
                        .forEach(x -> x.getTabList().removeEntry(player.getUniqueId()));
            }
        }
    }

    public void unvanishPlayer(User user) {
        VanishUserDataModule service = Nucleus.getNucleus().getUserDataManager().getUnchecked(user).get(VanishUserDataModule.class);
        service.setVanished(false);
        user.offer(Keys.VANISH, false);
        user.offer(Keys.VANISH_IGNORES_COLLISION, false);
        user.offer(Keys.VANISH_PREVENTS_TARGETING, false);

        if (this.isAlter && user instanceof Player) {
            Player player = (Player) user;
            Sponge.getServer().getOnlinePlayers().forEach(x -> {
                if (!x.getTabList().getEntry(player.getUniqueId()).isPresent()) {
                    x.getTabList().addEntry(TabListEntry.builder()
                            .displayName(Text.of(player.getName()))
                            .profile(player.getProfile())
                            .gameMode(player.gameMode().get())
                            .latency(player.getConnection().getLatency())
                            .list(x.getTabList()).build());
                }
            });
        }
    }

    public void setLastVanishedTime(UUID pl, Instant instant) {
        this.lastVanish.put(pl, instant);
    }

    Optional<Instant> getLastVanishTime(UUID pl) {
        return Optional.ofNullable(this.lastVanish.get(pl));
    }

    public void clearLastVanishTime(UUID pl) {
        this.lastVanish.remove(pl);
    }

}
