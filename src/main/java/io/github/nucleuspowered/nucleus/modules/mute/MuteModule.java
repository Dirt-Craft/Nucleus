/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.mute;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.internal.qsml.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.internal.text.Tokens;
import io.github.nucleuspowered.nucleus.internal.traits.InternalServiceManagerTrait;
import io.github.nucleuspowered.nucleus.internal.traits.MessageProviderTrait;
import io.github.nucleuspowered.nucleus.modules.mute.commands.CheckMuteCommand;
import io.github.nucleuspowered.nucleus.modules.mute.config.MuteConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.mute.data.MuteData;
import io.github.nucleuspowered.nucleus.modules.mute.services.MuteHandler;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import uk.co.drnaylor.quickstart.annotations.ModuleData;

import java.util.Map;
import java.util.Optional;
/*
@ModuleData(id = MuteModule.ID, name = "Mute")
public class MuteModule extends ConfigurableModule<MuteConfigAdapter> implements MessageProviderTrait, InternalServiceManagerTrait {

    public static final String ID = "mute";

    @Override
    public MuteConfigAdapter createAdapter() {
        return new MuteConfigAdapter();
    }

    @Override
    public void performEnableTasks() {
        createSeenModule(CheckMuteCommand.class, (c, u) -> {

            // If we have a ban service, then check for a ban.
            MuteHandler jh = getServiceUnchecked(MuteHandler.class);
            if (jh.isMuted(u)) {
                MuteData jd = jh.getPlayerMuteData(u).get();
                // Lightweight checkban.
                Text.Builder m;
                if (jd.getRemainingTime().isPresent()) {
                    m = getMessageFor(c, "seen.ismuted.temp",
                            Util.getTimeStringFromSeconds(jd.getRemainingTime().get().getSeconds())).toBuilder();
                } else {
                    m = getMessageFor(c, "seen.ismuted.perm").toBuilder();
                }

                return Lists.newArrayList(
                        m.onClick(TextActions.runCommand("/checkmute " + u.getName()))
                                .onHover(TextActions.showText(
                                        getMessageFor(c.getLocale(), "standard.clicktoseemore"))).build(),
                            getMessageFor(c, "standard.reason", jd.getReason()));
            }

            return Lists.newArrayList(getMessageFor(c, "seen.notmuted"));
        });
    }

    @Override protected Map<String, Tokens.Translator> tokensToRegister() {
        return ImmutableMap.<String, Tokens.Translator>builder()
                .put("muted", new Tokens.TrueFalseVariableTranslator() {
                    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
                    final Optional<Text> def = Optional.of(Text.of(TextColors.GRAY, "[Muted]"));

                    @Override protected Optional<Text> getDefault() {
                        return this.def;
                    }

                    @Override protected boolean condition(CommandSource commandSource) {
                        return commandSource instanceof Player &&
                                getServiceUnchecked(MuteHandler.class).isMuted((Player) commandSource);
                    }
                })
                .build();
    }
}
 */
