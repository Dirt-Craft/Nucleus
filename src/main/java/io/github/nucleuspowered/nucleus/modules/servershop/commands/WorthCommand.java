/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.servershop.commands;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.configurate.datatypes.ItemDataNode;
import io.github.nucleuspowered.nucleus.dataservices.ItemDataService;
import io.github.nucleuspowered.nucleus.internal.EconHelper;
import io.github.nucleuspowered.nucleus.internal.annotations.RunAsync;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.docgen.annotations.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.internal.messages.MessageProvider;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@RunAsync
@RegisterCommand("worth")
@Permissions(suggestedLevel = SuggestedLevel.USER)
@EssentialsEquivalent({"worth", "price"})
@NonnullByDefault
public class WorthCommand extends AbstractCommand<CommandSource> {

    private final String item = "item";
    private final ItemDataService itemDataService = Nucleus.getNucleus().getItemDataService();
    private final EconHelper econHelper = Nucleus.getNucleus().getEconHelper();

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
                GenericArguments.optionalWeak(GenericArguments.string(Text.of(this.item)))
        };
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args, Cause cause) throws Exception {
        if (!(src instanceof Player))
            throw new CommandException(Util.format("&cOnly a player can use this command!"));
        //CatalogType type = getCatalogTypeFromHandOrArgs(src, this.item, args);
        //String id = type.getId();
        String itemID = getItemIDFromHandOrArgs(src, this.item, args);

        // Get the item from the system.
        ItemDataNode node = this.itemDataService.getDataForItem(itemID);

        // Get the current item worth.
        MessageProvider provider = Nucleus.getNucleus().getMessageProvider();
        if (!this.econHelper.economyServiceExists()) {
            src.sendMessage(provider.getTextMessageWithFormat("command.setworth.noeconservice"));
        }

        double buyPrice = node.getServerBuyPrice();
        double sellPrice = node.getServerSellPrice();


        Player player = (Player) src;
        int quantity = player.getItemInHand(HandTypes.MAIN_HAND).get().getQuantity();
        if(quantity == 0) quantity++;

        String price = this.econHelper.getCurrencySymbol(node.getServerBuyPrice() * quantity);

        if (buyPrice > 0 || sellPrice > 0) {
            if (quantity > 1) {
                src.sendMessage(Util.format(
                        "&6&l" + quantity + " &b" + player.getItemInHand(HandTypes.MAIN_HAND).get().getType().getName() + "&7's are worth &a$" + price));
            } else {
                src.sendMessage(Util.format(
                        "&7A &b" + itemID + "&7 is worth &a$" + price));
            }
        } else {
            src.sendMessage(Util.format(
                    "&b" + itemID + "&7 does &cnot&7 have a worth set!" +
                            "\nContact a moderator or above to set one!"));
        }

        /*if (stringBuilder.length() == 0) {
            //src.sendMessage(provider.getTextMessageWithFormat("command.worth.nothing", Util.getTranslatableIfPresentOnCatalogType(type)));
            //src.sendMessage(provider.getTextMessageWithFormat("command.worth.nothing", itemID));
            src.sendMessage(Util.format("&7This item does &cnot&7 have a worth set!"));
        } else {
            //src.sendMessage(provider.getTextMessageWithFormat("command.worth.something", Util.getTranslatableIfPresentOnCatalogType(type), stringBuilder.toString()));
            //src.sendMessage(provider.getTextMessageWithFormat("command.worth.something", itemID, stringBuilder.toString()));
            src.sendMessage(Util.format(stringBuilder.toString()));
        }*/

        return CommandResult.success();
    }

}
