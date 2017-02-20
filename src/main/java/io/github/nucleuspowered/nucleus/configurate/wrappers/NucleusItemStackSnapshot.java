/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.configurate.wrappers;

import org.spongepowered.api.item.inventory.ItemStackSnapshot;

public class NucleusItemStackSnapshot {

    private ItemStackSnapshot snapshot;

    public NucleusItemStackSnapshot(ItemStackSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public ItemStackSnapshot getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(ItemStackSnapshot snapshot) {
        this.snapshot = snapshot;
    }
}
