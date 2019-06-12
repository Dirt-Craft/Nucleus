/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.teleport.commands;

import io.github.nucleuspowered.nucleus.internal.annotations.RunAsync;
import io.github.nucleuspowered.nucleus.internal.annotations.command.NoModifiers;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.internal.docgen.annotations.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.internal.userprefs.UserPreferenceService;
import io.github.nucleuspowered.nucleus.modules.teleport.TeleportUserPrefKeys;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.HashMap;
import java.util.Map;

@Permissions(prefix = "teleport", suggestedLevel = SuggestedLevel.USER)
@NoModifiers
@NonnullByDefault
@RegisterCommand({"tptoggle"})
@RunAsync
@EssentialsEquivalent("tptoggle")
public class TeleportToggleCommand extends AbstractCommand<Player> {

    @Override
    public Map<String, PermissionInformation> permissionSuffixesToRegister() {
        Map<String, PermissionInformation> m = new HashMap<>();
        m.put("exempt", PermissionInformation.getWithTranslation("permission.tptoggle.exempt", SuggestedLevel.ADMIN));
        return m;
    }

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
                NucleusParameters.OPTIONAL_ONE_TRUE_FALSE
        };
    }

    @Override
    public CommandResult executeCommand(Player src, CommandContext args, Cause cause) {
        UserPreferenceService ups = getServiceUnchecked(UserPreferenceService.class);
        boolean toggle = ups.get(src.getUniqueId(), TeleportUserPrefKeys.TELEPORT_TARGETABLE).get(); // we know it's always there
        boolean flip = args.<Boolean>getOne(NucleusParameters.Keys.BOOL).orElseGet(() -> !toggle);
        ups.set(src.getUniqueId(), TeleportUserPrefKeys.TELEPORT_TARGETABLE, flip);
        src.sendMessage(Text.builder().append(
                getMessageFor(src, "command.tptoggle.success",
                        getMessageFor(src, flip ? "standard.enabled" : "standard.disabled")))
                .build());
        return CommandResult.success();
    }
}
