/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.teleport;

import com.google.common.collect.ImmutableMap;
import io.github.nucleuspowered.nucleus.internal.qsml.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.modules.core.CoreModule;
import io.github.nucleuspowered.nucleus.modules.teleport.config.TeleportConfigAdapter;
import uk.co.drnaylor.quickstart.annotations.ModuleData;

@ModuleData(id = "teleport", name = "Teleport", dependencies = CoreModule.ID)
public class TeleportModule extends ConfigurableModule<TeleportConfigAdapter> {

    @Override
    public TeleportConfigAdapter createAdapter() {
        return new TeleportConfigAdapter();
    }

    @Override protected ImmutableMap<String, String> remapCommand() {
        return ImmutableMap.<String, String>builder()
                .put("tpn", "minecraft:tp")
                .build();
    }
}
