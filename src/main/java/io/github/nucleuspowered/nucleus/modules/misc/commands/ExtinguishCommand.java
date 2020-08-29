/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.misc.commands;

import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;

@Permissions(supportsOthers = true, suggestedLevel = SuggestedLevel.MOD)
@RegisterCommand(value = { "extinguish", "ext" })
public class ExtinguishCommand extends AbstractCommand<CommandSource> {

    @Override
    protected CommandElement[] getArguments() {
        return new CommandElement[] {
            requirePermissionArg(NucleusParameters.OPTIONAL_ONE_PLAYER, this.permissions.getOthers())
        };
    }

    @Override
    protected CommandResult executeCommand(CommandSource src, CommandContext args, Cause cause) throws Exception {
        Player target = this.getUserFromArgs(Player.class, src, NucleusParameters.Keys.PLAYER, args);
        if (target.get(Keys.FIRE_TICKS).orElse(-1) > 0 && target.offer(Keys.FIRE_TICKS, 0).isSuccessful()) {
            sendMessageTo(src, "command.extinguish.success", target.getName());
            return CommandResult.success();
        }

        throw ReturnMessageException.fromKey("command.extinguish.failed", target.getName());
    }
}
