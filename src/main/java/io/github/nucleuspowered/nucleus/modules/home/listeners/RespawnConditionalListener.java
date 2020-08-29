/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.home.listeners;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.api.EventContexts;
import io.github.nucleuspowered.nucleus.api.nucleusdata.Home;
import io.github.nucleuspowered.nucleus.api.service.NucleusHomeService;
import io.github.nucleuspowered.nucleus.internal.interfaces.ListenerBase;
import io.github.nucleuspowered.nucleus.modules.home.HomeModule;
import io.github.nucleuspowered.nucleus.modules.home.config.HomeConfig;
import io.github.nucleuspowered.nucleus.modules.home.config.HomeConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.home.datamodules.HomeUserDataModule;
import io.github.nucleuspowered.nucleus.modules.spawn.events.SendToSpawnEvent;
import io.github.nucleuspowered.nucleus.util.CauseStackHelper;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.event.entity.living.humanoid.player.RespawnPlayerEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.world.World;

import java.util.Optional;

public class RespawnConditionalListener implements ListenerBase.Conditional {

    @Listener
    public void onRespawn(final RespawnPlayerEvent event, @Getter("getTargetEntity") final Player player) {
        Optional<Home> oh = Nucleus.getNucleus().getUserDataManager().getUnchecked(player)
            .get(HomeUserDataModule.class)
            .getHome(NucleusHomeService.DEFAULT_HOME_NAME);

        Optional<Transform<World>> ot = oh.flatMap(Home::getTransform);

        if(ot.isPresent()) {
            EventContext context = EventContext.builder().add(EventContexts.SPAWN_EVENT_TYPE,SendToSpawnEvent.Type.HOME_ON_DEATH).build();
            SendToSpawnEvent sEvent = new SendToSpawnEvent(ot.get(), event.getTargetEntity(), CauseStackHelper.createCause(context, event.getTargetEntity()));
            if (Sponge.getEventManager().post(sEvent)) {
                event.getTargetEntity().sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.home.fail", oh.get().getName()));
                return;
            }

            event.setToTransform(sEvent.getTransformTo());
        }
    }

    @Override public boolean shouldEnable() {
        return Nucleus.getNucleus().getConfigValue(HomeModule.ID, HomeConfigAdapter.class, HomeConfig::isRespawnAtHome).orElse(false);
    }


}
