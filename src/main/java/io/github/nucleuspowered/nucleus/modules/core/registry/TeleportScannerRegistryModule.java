package io.github.nucleuspowered.nucleus.modules.core.registry;

import io.github.nucleuspowered.nucleus.internal.registry.NucleusRegistryModule;
import io.github.nucleuspowered.nucleus.internal.teleport.scanners.AscendingTeleportScanner;
import io.github.nucleuspowered.nucleus.internal.teleport.scanners.ITeleportScanner;
import io.github.nucleuspowered.nucleus.internal.teleport.scanners.NoTeleportScanner;

public class TeleportScannerRegistryModule extends NucleusRegistryModule<ITeleportScanner> {

    @Override
    public Class<ITeleportScanner> catalogClass() {
        return ITeleportScanner.class;
    }

    @Override
    public void registerModuleDefaults() {
        registerAdditionalCatalog(new NoTeleportScanner());
        registerAdditionalCatalog(new AscendingTeleportScanner());
    }
}
