/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.teleport.commands;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.dataservices.modular.ModularUserService;
import io.github.nucleuspowered.nucleus.internal.annotations.command.NoModifiers;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.ContinueMode;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.docgen.annotations.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.internal.interfaces.Reloadable;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.core.datamodules.CoreUserDataModule;
import io.github.nucleuspowered.nucleus.modules.teleport.config.TeleportConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.teleport.services.TeleportHandler;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * NOTE: TeleportHere is considered an admin command, as there is a potential
 * for abuse for non-admin players trying to pull players. No cost or warmups
 * will be applied. /tpahere should be used instead in these circumstances.
 */
@Permissions(prefix = "teleport", suggestedLevel = SuggestedLevel.ADMIN, supportsSelectors = true)
@NoModifiers
@RegisterCommand({"tphere", "tph"})
@EssentialsEquivalent(value = {"tphere", "s", "tpohere"}, isExact = false,
        notes = "If you have permission, this will override '/tptoggle' automatically.")
@NonnullByDefault
public class TeleportHereCommand extends AbstractCommand<Player> implements Reloadable {

    private final TeleportHandler handler = getServiceUnchecked(TeleportHandler.class);

    private boolean isDefaultQuiet = false;

    @Override public void onReload() {
        this.isDefaultQuiet = getServiceUnchecked(TeleportConfigAdapter.class).getNodeOrDefault().isDefaultQuiet();
    }

    @Override
    public Map<String, PermissionInformation> permissionSuffixesToRegister() {
        Map<String, PermissionInformation> m = new HashMap<>();
        m.put("offline", PermissionInformation.getWithTranslation("permission.tphere.offline", SuggestedLevel.ADMIN));
        return m;
    }

    @Override protected ContinueMode preProcessChecks(Player source, CommandContext args) {
        UserStorageService userStorageService = Sponge.getServiceManager().provide(UserStorageService.class).get();
        Optional<User> user =  userStorageService.get(args.<String>getOne(Text.of("user")).get());
        if (!user.isPresent()) {
            source.sendMessage(Util.format("&cCould not get last location for user"));
            return ContinueMode.STOP;
        }
        return TeleportHandler.canTeleportTo(permissions, source, user.get()) ? ContinueMode.CONTINUE : ContinueMode.STOP;
    }

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
                GenericArguments.flags().flag("q", "-quiet").buildWith(
                                GenericArguments.string(Text.of("user")))
        };
    }

    @Override
    public CommandResult executeCommand(Player src, CommandContext args, Cause cause) throws Exception {
        boolean beQuiet = args.<Boolean>getOne("q").orElse(this.isDefaultQuiet);
        UserStorageService userStorageService = Sponge.getServiceManager().provide(UserStorageService.class).get();
        User target = userStorageService.get(args.<String>getOne("user").get()).get();

        if (target.getPlayer().isPresent()) {
            this.handler.getBuilder().setFrom(target.getPlayer().get()).setTo(src).setSilentSource(beQuiet).startTeleport();
        } else {
            this.permissions.checkSuffix(src, "offline", () -> ReturnMessageException.fromKey("command.tphere.noofflineperms"));

            // Update the offline player's next location
            ModularUserService mus = Nucleus.getNucleus().getUserDataManager().get(target)
                    .orElseThrow(() -> ReturnMessageException.fromKey("command.tphere.couldnotset", target.getName()));
            mus.get(CoreUserDataModule.class).sendToLocationOnLogin(src.getLocation());
            mus.save();

            src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.tphere.offlinesuccess", target.getName()));
        }

        return CommandResult.success();
    }
}
