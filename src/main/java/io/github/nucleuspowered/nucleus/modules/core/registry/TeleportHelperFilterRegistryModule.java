package io.github.nucleuspowered.nucleus.modules.core.registry;

import io.github.nucleuspowered.nucleus.api.catalogkeys.NucleusTeleportHelperFilters;
import io.github.nucleuspowered.nucleus.internal.annotations.Registry;
import io.github.nucleuspowered.nucleus.internal.registry.NucleusRegistryModule;
import io.github.nucleuspowered.nucleus.internal.teleport.filters.NoCheckFilter;
import io.github.nucleuspowered.nucleus.internal.teleport.filters.WallCheckFilter;
import org.spongepowered.api.world.teleport.TeleportHelperFilter;

import javax.inject.Singleton;

@Singleton
@Registry(NucleusTeleportHelperFilters.class)
public class TeleportHelperFilterRegistryModule extends NucleusRegistryModule<TeleportHelperFilter> {

    @Override
    public Class<TeleportHelperFilter> catalogClass() {
        return TeleportHelperFilter.class;
    }

    @Override
    public void registerModuleDefaults() {
        registerAdditionalCatalog(new NoCheckFilter());
        registerAdditionalCatalog(new WallCheckFilter());
    }
}
