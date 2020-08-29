/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kit.services;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.events.NucleusKitEvent;
import io.github.nucleuspowered.nucleus.api.exceptions.KitRedeemException;
import io.github.nucleuspowered.nucleus.api.nucleusdata.Kit;
import io.github.nucleuspowered.nucleus.api.service.NucleusKitService;
import io.github.nucleuspowered.nucleus.dataservices.KitService;
import io.github.nucleuspowered.nucleus.internal.CommandPermissionHandler;
import io.github.nucleuspowered.nucleus.internal.PermissionRegistry;
import io.github.nucleuspowered.nucleus.internal.annotations.APIService;
import io.github.nucleuspowered.nucleus.internal.interfaces.Reloadable;
import io.github.nucleuspowered.nucleus.internal.interfaces.ServiceBase;
import io.github.nucleuspowered.nucleus.internal.text.NucleusTextTemplateFactory;
import io.github.nucleuspowered.nucleus.internal.traits.InternalServiceManagerTrait;
import io.github.nucleuspowered.nucleus.internal.traits.MessageProviderTrait;
import io.github.nucleuspowered.nucleus.internal.traits.PermissionTrait;
import io.github.nucleuspowered.nucleus.modules.kit.commands.kit.KitCommand;
import io.github.nucleuspowered.nucleus.modules.kit.config.KitConfig;
import io.github.nucleuspowered.nucleus.modules.kit.config.KitConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.kit.datamodules.KitUserDataModule;
import io.github.nucleuspowered.nucleus.modules.kit.events.KitEvent;
import io.github.nucleuspowered.nucleus.modules.kit.misc.KitRedeemResult;
import io.github.nucleuspowered.nucleus.modules.kit.misc.SingleKit;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Container;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.transaction.InventoryTransactionResult;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.util.Tuple;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

@APIService(NucleusKitService.class)
public class KitHandler implements NucleusKitService, Reloadable, InternalServiceManagerTrait, MessageProviderTrait, PermissionTrait, ServiceBase {

    private static final InventoryTransactionResult EMPTY_ITR =
            InventoryTransactionResult.builder().type(InventoryTransactionResult.Type.SUCCESS).build();

    private static final Pattern inventory = Pattern.compile("\\{\\{.+?}}");

    public static String getPermissionForKit(String kitName) {
        return PermissionRegistry.PERMISSIONS_PREFIX + "kits." + kitName.toLowerCase();
    }

    private boolean isProcessTokens = false;
    private boolean isMustGetAll = false;

    private final CommandPermissionHandler cph = Nucleus.getNucleus()
            .getPermissionRegistry().getPermissionsForNucleusCommand(KitCommand.class);

    private final List<Container> viewers = Lists.newArrayList();
    private final Map<Container, Tuple<Kit, Inventory>> inventoryKitMap = Maps.newHashMap();

    private final KitService store = Nucleus.getNucleus().getKitService();

    public boolean exists(String name, boolean includeHidden) {
        return this.store.getKitNames(includeHidden).stream().anyMatch(x -> x.equalsIgnoreCase(name));
    }

    @Override
    public Set<String> getKitNames() {
        return getKitNames(true);
    }

    public Set<String> getKitNames(boolean showHidden) {
        return this.store.getKitNames(showHidden);
    }

    @Override
    public Optional<Kit> getKit(String name) {
        return this.store.getKit(name).map(x -> new SingleKit(name, x));
    }

    @Override
    public Collection<ItemStack> getItemsForPlayer(Kit kit, Player player) {
        Collection<ItemStack> cis = kit.getStacks().stream().map(ItemStackSnapshot::createStack).collect(Collectors.toList());
        if (this.isProcessTokens) {
            processTokensInItemStacks(player, cis);
        }

        return cis;
    }

    @Override
    public RedeemResult redeemKit(Kit kit, Player player, boolean performChecks) throws KitRedeemException {
        return redeemKit(kit, player, performChecks, performChecks, this.isMustGetAll, false,
                Nucleus.getNucleus().getUserDataManager().getUnchecked(player.getUniqueId()).get(KitUserDataModule.class));
    }

    @Override
    public RedeemResult redeemKit(Kit kit, Player player, boolean performChecks, boolean mustRedeemAll) throws KitRedeemException {
        return redeemKit(kit, player, performChecks, performChecks, mustRedeemAll, false,
                Nucleus.getNucleus().getUserDataManager().getUnchecked(player.getUniqueId()).get(KitUserDataModule.class));
    }

    public RedeemResult redeemKit(Kit kit,
            Player player,
            boolean checkOneTime,
            boolean checkCooldown,
            boolean isMustGetAll,
            boolean isFirstJoin,
            KitUserDataModule user) throws KitRedeemException {
        Instant timeOfLastUse = user.getLastRedeemedTime(kit.getName());
        Instant now = Instant.now();

        try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
            frame.pushCause(player);

            // If the kit was used before...
            // Get original list
            Collection<ItemStackSnapshot> original = getItems(kit, this.isProcessTokens, player);
            Collection<String> commands = kit.getCommands();
            if ((checkOneTime || checkCooldown) && timeOfLastUse != null) {

                // if it's one time only and the user does not have an exemption...
                if (checkOneTime && !checkOneTime(kit, player)) {
                    Sponge.getEventManager().post(
                            new KitEvent.FailedRedeem(frame.getCurrentCause(), timeOfLastUse, kit, player,
                                    original, null, commands, null, KitRedeemException.Reason.ALREADY_REDEEMED));
                    throw new KitRedeemException("Already redeemed", KitRedeemException.Reason.ALREADY_REDEEMED);
                }

                // If we have a cooldown for the kit, and we don't have permission to
                // bypass it...
                if (checkCooldown) {
                    Optional<Duration> duration = checkCooldown(kit, player, timeOfLastUse);
                    if (duration.isPresent()) {
                        Sponge.getEventManager().post(
                                new KitEvent.FailedRedeem(frame.getCurrentCause(), timeOfLastUse, kit, player,
                                        original, null, commands, null, KitRedeemException.Reason.COOLDOWN_NOT_EXPIRED));
                        throw new KitRedeemException.Cooldown("Cooldown not expired", duration.get());
                    }
                }
            }

            NucleusKitEvent.Redeem.Pre preEvent = new KitEvent.PreRedeem(frame.getCurrentCause(), timeOfLastUse, kit, player, original, commands);
            if (Sponge.getEventManager().post(preEvent)) {
                Sponge.getEventManager().post(
                        new KitEvent.FailedRedeem(frame.getCurrentCause(), timeOfLastUse, kit, player, original,
                                preEvent.getStacksToRedeem().orElse(null),
                                commands,
                                preEvent.getCommandsToExecute().orElse(null),
                                KitRedeemException.Reason.PRE_EVENT_CANCELLED));
                throw new KitRedeemException.PreCancelled(preEvent.getCancelMessage().orElse(null));
            }

            List<Optional<ItemStackSnapshot>> slotList = Lists.newArrayList();
            Util.getStandardInventory(player).slots().forEach(x -> slotList.add(x.peek().map(ItemStack::createSnapshot)));

            InventoryTransactionResult inventoryTransactionResult = EMPTY_ITR;
            KitRedeemException ex = null;
            if (!kit.getStacks().isEmpty()) {
                inventoryTransactionResult = addToStandardInventory(player, preEvent.getStacksToRedeem().orElseGet(preEvent::getOriginalStacksToRedeem));
                if (!isFirstJoin && !inventoryTransactionResult.getRejectedItems().isEmpty() && isMustGetAll) {
                    Inventory inventory = Util.getStandardInventory(player);

                    // Slots
                    Iterator<Inventory> slot = inventory.slots().iterator();

                    // Slots to restore
                    slotList.forEach(x -> {
                        Inventory i = slot.next();
                        i.clear();
                        x.ifPresent(y -> i.offer(y.createStack()));
                    });

                    // My friend was playing No Man's Sky, I almost wrote "No free slots in suit inventory".
                    ex = new KitRedeemException("No free slots in player inventory", KitRedeemException.Reason.NO_SPACE);
                }
            }

            // If something was consumed, consider a success.
            if (ex == null && inventoryTransactionResult.getType() == InventoryTransactionResult.Type.SUCCESS) {
                redeemKitCommands(preEvent.getCommandsToExecute().orElse(commands), player);

                // Register the last used time. Do it for everyone, in case
                // permissions or cooldowns change later
                if (checkCooldown) {
                    user.addKitLastUsedTime(kit.getName(), now);
                }

                Sponge.getEventManager().post(new KitEvent.PostRedeem(frame.getCurrentCause(), timeOfLastUse, kit, player, original,
                        preEvent.getStacksToRedeem().orElse(null),
                        commands,
                        preEvent.getCommandsToExecute().orElse(null)));
                return new KitRedeemResult(inventoryTransactionResult.getRejectedItems(), slotList.stream()
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toList()));
            } else {
                // Failed.
                ex = ex == null ? new KitRedeemException("No items were redeemed", KitRedeemException.Reason.UNKNOWN) : ex;
                Sponge.getEventManager().post(new KitEvent.FailedRedeem(frame.getCurrentCause(), timeOfLastUse, kit, player, original,
                        preEvent.getStacksToRedeem().orElse(null),
                        commands,
                        preEvent.getCommandsToExecute().orElse(null),
                        ex.getReason()));
                throw ex;
            }
        }
    }

    private void redeemKitCommands(Collection<String> commands, Player player) {
        ConsoleSource source = Sponge.getServer().getConsole();
        String playerName = player.getName();
        commands.forEach(x -> Sponge.getCommandManager().process(source, x.replace("{{player}}", playerName)));
    }

    public boolean checkOneTime(Kit kit, Player player) {
        // if it's one time only and the user does not have an exemption...
        return !kit.isOneTime() || hasPermission(player, this.cph.getPermissionWithSuffix("exempt.onetime"));
    }

    public Optional<Duration> checkCooldown(Kit kit, Player player, Instant timeOfLastUse) {
        Instant now = Instant.now();

        // If the kit was used before...
        if (timeOfLastUse != null) {

            // If we have a cooldown for the kit, and we don't have permission to
            // bypass it...
            if (!this.cph.testCooldownExempt(player) && kit.getCooldown().map(Duration::getSeconds).orElse(0L) > 0) {

                // ...and we haven't reached the cooldown point yet...
                Instant timeForNextUse = timeOfLastUse.plus(kit.getCooldown().get());
                if (timeForNextUse.isAfter(now)) {
                    return Optional.of(Duration.between(now, timeForNextUse));
                }
            }
        }

        return Optional.empty();
    }

    @Override
    public boolean removeKit(String kitName) {
        if (this.store.removeKit(kitName)) {
            this.store.save();
            return true;
        }

        return false;
    }

    @Override
    public void saveKit(Kit kit) {
        saveKitInternal(kit.getName(), kit);
    }

    private synchronized void saveKitInternal(String name, Kit kit) {
        Preconditions.checkArgument(kit instanceof SingleKit);
        Util.getKeyIgnoreCase(this.store.getKitNames(true), name).ifPresent(this.store::removeKit);
        this.store.addKit(name, kit);
        this.store.save();
    }

    @Override
    public Kit createKit(String name) throws IllegalArgumentException {
        Optional<String> key = Util.getKeyIgnoreCase(this.store.getKitNames(true), name);
        key.ifPresent(s -> {
            throw new IllegalArgumentException("Kit " + name + " already exists!");
        });
        return new SingleKit(name);
    }

    @Override
    public void renameKit(final String kitName, final String newKitName) throws IllegalArgumentException {
        Kit targetKit = getKit(kitName).orElseThrow(() -> new IllegalArgumentException(getMessageString("kit.noexists", kitName)));
        if (Util.getKeyIgnoreCase(getKitNames(), newKitName).isPresent()) {
            throw new IllegalArgumentException(getMessageString("kit.cannotrename", kitName, newKitName));
        }

        saveKitInternal(newKitName, targetKit);
        removeKit(kitName);
    }

    public Optional<Tuple<Kit, Inventory>> getCurrentlyOpenInventoryKit(Container inventory) {
        return Optional.ofNullable(this.inventoryKitMap.get(inventory));
    }

    public boolean isOpen(String kitName) {
        return this.inventoryKitMap.values().stream().anyMatch(x -> x.getFirst().getName().equalsIgnoreCase(kitName));
    }

    public void addKitInventoryToListener(Tuple<Kit, Inventory> kit, Container inventory) {
        Preconditions.checkState(!this.inventoryKitMap.containsKey(inventory));
        this.inventoryKitMap.put(inventory, kit);
    }

    public void removeKitInventoryFromListener(Container inventory) {
        this.inventoryKitMap.remove(inventory);
    }

    public void addViewer(Container inventory) {
        this.viewers.add(inventory);
    }

    @Nullable private Boolean hasViewersWorks = null;

    public void removeViewer(Container inventory) {
        this.viewers.remove(inventory);
        if (this.hasViewersWorks == null) {
            try {
                inventory.hasViewers();
                this.hasViewersWorks = true;
            } catch (Throwable throwable) {
                this.hasViewersWorks = false;
                return;
            }
        }

        if (this.hasViewersWorks) {
            this.viewers.removeIf(x -> !x.hasViewers());
        }
    }

    public boolean isViewer(Container inventory) {
        return this.viewers.contains(inventory);
    }

    public void processTokensInItemStacks(Player player, Collection<ItemStack> stacks) {
        final Matcher m = inventory.matcher("");
        for (ItemStack x : stacks) {
            x.get(Keys.DISPLAY_NAME).ifPresent(text -> {
                if (m.reset(text.toPlain()).find()) {
                    x.offer(Keys.DISPLAY_NAME,
                            NucleusTextTemplateFactory.createFromAmpersandString(TextSerializers.FORMATTING_CODE.serialize(text))
                                    .getForCommandSource(player, null, null));
                }
            });

            x.get(Keys.ITEM_LORE).ifPresent(text -> {
                if (text.stream().map(Text::toPlain).anyMatch(y -> m.reset(y).find())) {
                    x.offer(Keys.ITEM_LORE,
                            text.stream().map(y ->
                                    NucleusTextTemplateFactory.createFromAmpersandString(TextSerializers.FORMATTING_CODE.serialize(y))
                                            .getForCommandSource(player, null, null)).collect(Collectors.toList()));
                }
            });
        }
    }

    private ImmutableList<ItemStackSnapshot> getItems(Kit kit, boolean replaceTokensInLore, Player targetPlayer) {
        Collection<ItemStack> toOffer = kit.getStacks().stream()
                .filter(x -> x.getType() != ItemTypes.NONE)
                .map(ItemStackSnapshot::createStack)
                .collect(Collectors.toList());

        if (replaceTokensInLore) {
            processTokensInItemStacks(targetPlayer, toOffer);
        }

        return toOffer.stream().map(ItemStack::createSnapshot).collect(ImmutableList.toImmutableList());
    }

    /**
     * Adds items to a {@link Player}s {@link Inventory}
     * @param player The {@link Player}
     * @param itemStacks The {@link ItemStackSnapshot}s to add.
     * @return {@link Tristate#TRUE} if everything is successful, {@link Tristate#FALSE} if nothing was added, {@link Tristate#UNDEFINED}
     * if some stacks were added.
     */
    private InventoryTransactionResult addToStandardInventory(
            Player player, Collection<ItemStackSnapshot> itemStacks) {

        Inventory target = Util.getStandardInventory(player);
        InventoryTransactionResult.Builder resultBuilder = InventoryTransactionResult.builder();

        Collection<ItemStack> toOffer = itemStacks.stream()
                .filter(x -> x.getType() != ItemTypes.NONE)
                .map(ItemStackSnapshot::createStack)
                .collect(Collectors.toList());

        boolean success = false;
        for (ItemStack stack : toOffer) {
            InventoryTransactionResult itr = target.offer(stack);
            success = success || itr.getType() == InventoryTransactionResult.Type.SUCCESS;
            for (ItemStackSnapshot iss : itr.getRejectedItems()) {
                resultBuilder.reject(iss.createStack());
            }

        }

        return resultBuilder.type(success ? InventoryTransactionResult.Type.SUCCESS : InventoryTransactionResult.Type.FAILURE).build();
    }

    @Override
    public void onReload() {
        KitConfig kitConfig = this.getServiceUnchecked(KitConfigAdapter.class).getNodeOrDefault();
        this.isMustGetAll = kitConfig.isMustGetAll();
        this.isProcessTokens = kitConfig.isProcessTokens();
    }
}
