/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.playerinfo.commands;

import com.flowpowered.math.vector.Vector3d;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.docgen.annotations.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.internal.interfaces.Reloadable;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.internal.services.PlayerOnlineService;
import io.github.nucleuspowered.nucleus.modules.playerinfo.config.PlayerInfoConfigAdapter;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.text.NumberFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Permissions(suggestedLevel = SuggestedLevel.USER, supportsOthers = true)
@RegisterCommand("near")
@EssentialsEquivalent("near")
@NonnullByDefault
public class NearCommand extends AbstractCommand.SimpleTargetOtherUser implements Reloadable {

    private static final NumberFormat formatter =  NumberFormat.getInstance();
    private final String radiusKey = "radius";
    private int maxRadius;

    static {
        formatter.setMaximumFractionDigits(2);
    }

    @Override
    protected Map<String, PermissionInformation> permissionSuffixesToRegister() {
        final Map<String, PermissionInformation> pi = super.permissionSuffixesToRegister();
        pi.put("maxexempt", PermissionInformation.getWithTranslation("permission.near.maxexempt", SuggestedLevel.MOD));
        return pi;
    }

    @Override
    protected CommandElement[] additionalArguments() {
        return new CommandElement[] {
                GenericArguments.optionalWeak(GenericArguments.integer(Text.of(this.radiusKey)))
        };
    }

    //near radius
    //near player radius
    @Override
    protected CommandResult executeWithPlayer(CommandSource src, User user, CommandContext args, boolean isSelf) throws Exception {
        final Location<World> location;
        final Vector3d position;
        if (user.isOnline()) {
            location = user.getPlayer().get().getLocation();
            position = location.getPosition();
        } else {
            World world = user.getWorldUniqueId()
                    .flatMap(x -> Sponge.getServer().getWorld(x))
                    .orElseThrow((() -> ReturnMessageException.fromKey(src,"command.near.location.nolocation", user.getName())));
            position = user.getPosition();
            location = new Location<>(world, position);
        }

        int radius = this.maxRadius;
        final Optional<Integer> radiusOpt = args.getOne(this.radiusKey);
        if (radiusOpt.isPresent()) {
            int inputRadius = radiusOpt.get();
            // Check if executor has max radius override permission
            if (inputRadius > this.maxRadius && this.permissions.testSuffix(src, "maxexempt")) {
                radius = inputRadius;
            } else {
                sendMessageTo(src, "command.near.radiustoobig", this.maxRadius);
            }
        }

        final PlayerOnlineService playerOnlineService = getServiceManager().getServiceUnchecked(PlayerOnlineService.class);
        final List<Text> messagesToSend =
                location.getExtent()
                        .getNearbyEntities(location.getPosition(), radius)
                        .stream()
                        .filter(Player.class::isInstance)
                        .map(Player.class::cast)
                        .filter(e -> e.getUniqueId() != user.getUniqueId() && playerOnlineService.isOnline(src, e))
                        .map(x -> Tuple.of(x, position.distance(x.getPosition())))
                        .sorted(Comparator.comparingDouble(Tuple::getSecond))
                        .map(tuple -> createPlayerLine(src, tuple))
                        .collect(Collectors.toList());

        Util.getPaginationBuilder(src)
                        .title(getMessageFor(src, "command.near.playersnear", user.getName()))
                        .contents(messagesToSend)
                        .sendTo(src);

        return CommandResult.success();
    }

    private Text createPlayerLine(CommandSource src, Tuple<Player, Double> player) {
        Text.Builder line = Text.builder();
        getMessageFor(src, "command.near.playerdistancefrom", player.getFirst().getName());
        line.append(getMessageFor(src, "command.near.playerdistancefrom", player.getFirst().getName(),
                formatter.format(Math.abs(player.getSecond()))))
                .onClick(TextActions.runCommand("/tp " + player.getFirst().getName()))
                .onHover(TextActions.showText(getMessageFor(src,"command.near.tpto", player.getFirst().getName())));
        return line.build();
    }

    @Override
    public void onReload() {
        PlayerInfoConfigAdapter configAdapter = getServiceUnchecked(PlayerInfoConfigAdapter.class);
        this.maxRadius = configAdapter.getNodeOrDefault().getNear().getMaxRadius();
    }

}
