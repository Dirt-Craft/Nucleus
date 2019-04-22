/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.core.registry;

import io.github.nucleuspowered.nucleus.internal.registry.NucleusRegistryModule;
import io.github.nucleuspowered.nucleus.internal.teleport.scanners.NoTeleportScanner;
import io.github.nucleuspowered.nucleus.internal.teleport.scanners.TeleportScanner;
import io.github.nucleuspowered.nucleus.internal.teleport.scanners.VerticalTeleportScanner;
import org.spongepowered.plugin.meta.util.NonnullByDefault;

@NonnullByDefault
public class TeleportScannerRegistryModule extends NucleusRegistryModule<TeleportScanner> {

    @Override
    public Class<TeleportScanner> catalogClass() {
        return TeleportScanner.class;
    }

    @Override
    public void registerModuleDefaults() {
        registerAdditionalCatalog(new NoTeleportScanner());
        registerAdditionalCatalog(new VerticalTeleportScanner.Ascending());
        registerAdditionalCatalog(new VerticalTeleportScanner.Descending());
    }
}
