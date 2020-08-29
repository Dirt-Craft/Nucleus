/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.staffchat;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.api.chat.NucleusChatChannel;
import io.github.nucleuspowered.nucleus.internal.text.NucleusTextTemplateImpl;
import io.github.nucleuspowered.nucleus.internal.traits.InternalServiceManagerTrait;
import io.github.nucleuspowered.nucleus.internal.traits.PermissionTrait;
import io.github.nucleuspowered.nucleus.internal.userprefs.UserPreferenceService;
import io.github.nucleuspowered.nucleus.modules.staffchat.commands.StaffChatCommand;
import io.github.nucleuspowered.nucleus.modules.staffchat.config.StaffChatConfigAdapter;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.source.ProxySource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.service.permission.SubjectReference;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.text.channel.MessageReceiver;
import org.spongepowered.api.text.channel.MutableMessageChannel;
import org.spongepowered.api.text.chat.ChatType;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class StaffChatMessageChannel
        implements NucleusChatChannel.StaffChat, PermissionTrait, InternalServiceManagerTrait {

    private static boolean formatting = false;
    private static NucleusTextTemplateImpl template;
    private static TextColor colour;

    public static StaffChatMessageChannel getInstance() {
        Preconditions.checkState(ImmutableStaffChatMessageChannel.INSTANCE != null, "StaffChatMessageChannel#Instance");
        return ImmutableStaffChatMessageChannel.INSTANCE;
    }

    @Override
    public void send(@Nullable Object sender, Text original, ChatType type) {
        CommandSource source;
        if (!(sender instanceof CommandSource)) {
            if (sender instanceof String) {
                source = new NamedSource((String) sender);
            } else {
                source = Sponge.getServer().getConsole();
            }
        } else {
            source = (CommandSource) sender;
        }

        NucleusChatChannel.StaffChat.super.send(sender, format(source, Sponge.getServer().getConsole(), original), type);
    }

    @Override
    public Text format(CommandSource sender, MessageReceiver receiver, Text text) {
        return Text.of(template.getForCommandSource(sender), colour, text);
    }

    @Override
    public boolean formatMessages() {
        return formatting;
    }

    private static void onReload() {
        Nucleus.getNucleus().getConfigAdapter(StaffChatModule.ID, StaffChatConfigAdapter.class)
                .ifPresent(x -> {
                    formatting = x.getNodeOrDefault().isIncludeStandardChatFormatting();
                    template = x.getNodeOrDefault().getMessageTemplate();
                    colour = x.getNodeOrDefault().getColour();
                });
    }

    public static class ImmutableStaffChatMessageChannel extends StaffChatMessageChannel {

        private static StaffChatMessageChannel INSTANCE = new ImmutableStaffChatMessageChannel();

        private final String basePerm;

        private ImmutableStaffChatMessageChannel() {
            Nucleus.getNucleus().registerReloadable(StaffChatMessageChannel::onReload);
            onReload();
            this.basePerm = Nucleus.getNucleus().getPermissionRegistry().getPermissionsForNucleusCommand(StaffChatCommand.class).getBase();
        }

        @Override
        @Nonnull
        public Collection<MessageReceiver> getMembers() {
            List<MessageReceiver> c =
                    Sponge.getServer().getOnlinePlayers().stream()
                            .filter(this::test)
                            .collect(Collectors.toList());
            c.add(Sponge.getServer().getConsole());
            return c;
        }

        @Override
        public MutableMessageChannel asMutable() {
            return new MutableStaffChatMessageChannel(getMembers());
        }

        private boolean test(Player player) {
            if (hasPermission(player, this.basePerm)) {
                return getServiceUnchecked(UserPreferenceService.class)
                        .getPreferenceFor(player, StaffChatUserPrefKeys.VIEW_STAFF_CHAT)
                        .orElse(true);
            }

            return false;
        }

    }

    public static class MutableStaffChatMessageChannel extends StaffChatMessageChannel implements MutableMessageChannel {

        private final List<MessageReceiver> messageReceivers = new ArrayList<>();

        MutableStaffChatMessageChannel(Collection<MessageReceiver> receivers) {
            this.messageReceivers.addAll(receivers);
        }

        @Override
        public Collection<MessageReceiver> getMembers() {
            return ImmutableList.copyOf(this.messageReceivers);
        }

        @Override
        public boolean addMember(MessageReceiver member) {
            return this.messageReceivers.add(member);
        }

        @Override
        public boolean removeMember(MessageReceiver member) {
            return this.messageReceivers.remove(member);
        }

        @Override
        public void clearMembers() {
            this.messageReceivers.clear();
        }

        @Override
        public MutableMessageChannel asMutable() {
            return this;
        }
    }

    @NonnullByDefault
    private class NamedSource implements ProxySource {

        private final String name;

        NamedSource(String name) {
            this.name = name;
        }

        @Override
        public CommandSource getOriginalSource() {
            return Sponge.getServer().getConsole();
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public Optional<CommandSource> getCommandSource() {
            return Optional.of(getOriginalSource());
        }

        @Override
        public SubjectCollection getContainingCollection() {
            return getOriginalSource().getContainingCollection();
        }

        @Override
        public SubjectReference asSubjectReference() {
            return getOriginalSource().asSubjectReference();
        }

        @Override
        public boolean isSubjectDataPersisted() {
            return getOriginalSource().isSubjectDataPersisted();
        }

        @Override
        public SubjectData getSubjectData() {
            return getOriginalSource().getSubjectData();
        }

        @Override
        public SubjectData getTransientSubjectData() {
            return getOriginalSource().getTransientSubjectData();
        }

        @Override
        public Tristate getPermissionValue(Set<Context> contexts, String permission) {
            return getOriginalSource().getPermissionValue(contexts, permission);
        }

        @Override
        public boolean isChildOf(Set<Context> contexts, SubjectReference parent) {
            return getOriginalSource().isChildOf(contexts, parent);
        }

        @Override
        public List<SubjectReference> getParents(Set<Context> contexts) {
            return getOriginalSource().getParents();
        }

        @Override
        public Optional<String> getOption(Set<Context> contexts, String key) {
            return getOriginalSource().getOption(contexts, key);
        }

        @Override
        public String getIdentifier() {
            return getName();
        }

        @Override
        public Set<Context> getActiveContexts() {
            return getOriginalSource().getActiveContexts();
        }

        @Override
        public void sendMessage(Text message) {
            getOriginalSource().sendMessage(message);
        }

        @Override
        public MessageChannel getMessageChannel() {
            return getOriginalSource().getMessageChannel();
        }

        @Override
        public void setMessageChannel(MessageChannel channel) {
            getOriginalSource().setMessageChannel(channel);
        }
    }

}
