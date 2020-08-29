/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.environment.commands;

import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.argumentparsers.WorldTimeArgument;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.internal.docgen.annotations.EssentialsEquivalent;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.function.LongFunction;

@Permissions(prefix = "time")
@RegisterCommand(value = "set", subcommandOf = TimeCommand.class, rootAliasRegister = { "settime", "timeset" })
@EssentialsEquivalent(value = {"time", "day", "night"}, isExact = false, notes = "A time MUST be specified.")
@NonnullByDefault
public class SetTimeCommand extends AbstractCommand<CommandSource> {
    private final String time = "time";

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
            NucleusParameters.OPTIONAL_WEAK_WORLD_PROPERTIES_ENABLED_ONLY,
            GenericArguments.onlyOne(new WorldTimeArgument(Text.of(this.time)))
        };
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args, Cause cause) {
        WorldProperties pr = getWorldPropertiesOrDefault(src, NucleusParameters.Keys.WORLD, args);

        LongFunction<Long> tick = args.requireOne(this.time);
        long time = tick.apply(pr.getWorldTime());
        pr.setWorldTime(time);
        sendMessageTo(src, "command.settime.done2",
                pr.getWorldName(),
                Util.getTimeFromTicks(time));
        return CommandResult.success();
    }
}
