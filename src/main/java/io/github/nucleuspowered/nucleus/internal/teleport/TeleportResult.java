/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.internal.teleport;

public enum TeleportResult {

    /**
     * The teleportation was a success.
     */
    SUCCESS(true),

    /**
     * The teleportation failed due to no safe location.
     */
    FAILED_NO_LOCATION(false),

    /**
     * The teleportation failed due to a plugin cancellation.
     */
    FAILED_CANCELLED(false);

    private final boolean b;

    TeleportResult(boolean b) {
        this.b = b;
    }

    public boolean isSuccess() {
        return this.b;
    }
}
