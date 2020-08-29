/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.vanish.listener;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.internal.interfaces.ListenerBase;
import io.github.nucleuspowered.nucleus.internal.interfaces.Reloadable;
import io.github.nucleuspowered.nucleus.internal.userprefs.UserPreferenceService;
import io.github.nucleuspowered.nucleus.modules.vanish.VanishUserPrefKeys;
import io.github.nucleuspowered.nucleus.modules.vanish.commands.VanishCommand;
import io.github.nucleuspowered.nucleus.modules.vanish.config.VanishConfig;
import io.github.nucleuspowered.nucleus.modules.vanish.config.VanishConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.vanish.datamodules.VanishUserDataModule;
import io.github.nucleuspowered.nucleus.modules.vanish.services.VanishService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.action.TextActions;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class VanishListener implements Reloadable, ListenerBase {

    private VanishConfig vanishConfig = new VanishConfig();
    private VanishService service = getServiceUnchecked(VanishService.class);

    private final String permission = getPermissionHandlerFor(VanishCommand.class).getPermissionWithSuffix("persist");
    public static final String LOGIN_VANISH_PERMISSION = "nucleus.vanish.onlogin";

    @Listener
    public void onAuth(ClientConnectionEvent.Auth auth) {
        if (this.vanishConfig.isTryHidePlayers()) {
            UUID uuid = auth.getProfile().getUniqueId();
            CompletableFuture<Void> future = new CompletableFuture<>();
            Task.builder().execute(
                    () -> {
                        Sponge.getServiceManager()
                                .provideUnchecked(UserStorageService.class)
                                .get(uuid)
                                .map(x -> x.get(Keys.LAST_DATE_PLAYED))
                                .ifPresent(x -> x.ifPresent(y -> this.service.setLastVanishedTime(uuid, y)));
                        future.complete(null);
                    }
            ).submit(Nucleus.getNucleus());

            future.join();
        }
    }

    @Listener
    public void onLogin(ClientConnectionEvent.Join event, @Getter("getTargetEntity") Player player) {
        boolean persist = this.service.isVanished(player);

        boolean shouldVanish = (hasPermission(player, LOGIN_VANISH_PERMISSION)
                && getServiceUnchecked(UserPreferenceService.class).get(player.getUniqueId(), VanishUserPrefKeys.VANISH_ON_LOGIN).orElse(false))
                || persist;

        if (shouldVanish) {
            if (!hasPermission(player, this.permission)) {
                // No permission, no vanish.
                this.service.unvanishPlayer(player);
                return;
            } else if (this.vanishConfig.isSuppressMessagesOnVanish()) {
                event.setMessageCancelled(true);
            }

            this.service.vanishPlayer(player, true);
            sendMessageTo(player, "vanish.login");

            if (!persist) {
                // on login
                player.sendMessage(getMessageFor(player, "vanish.onlogin.prefs").toBuilder()
                        .onClick(TextActions.runCommand("/nuserprefs vanish-on-login false")).build());
            }
        } else if (this.vanishConfig.isForceNucleusVanish()) {
            // unvanish
            this.service.unvanishPlayer(player);
        }
    }

    @Listener
    public void onQuit(ClientConnectionEvent.Disconnect event, @Getter("getTargetEntity") Player player) {
        if (player.get(Keys.VANISH).orElse(false)) {
            Nucleus.getNucleus().getUserDataManager().getUnchecked(player).get(VanishUserDataModule.class).setVanished(true);
            if (this.vanishConfig.isSuppressMessagesOnVanish()) {
                event.setMessageCancelled(true);
            }
        }

        this.service.clearLastVanishTime(player.getUniqueId());
    }

    @Override
    public void onReload() {
        this.vanishConfig = getServiceUnchecked(VanishConfigAdapter.class).getNodeOrDefault();
    }
}
