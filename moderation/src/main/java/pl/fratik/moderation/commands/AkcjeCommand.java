/*
 * Copyright (C) 2019-2021 FratikB0T Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package pl.fratik.moderation.commands;

import com.google.common.eventbus.EventBus;
import io.sentry.Sentry;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.command.SubCommand;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.Kara;
import pl.fratik.core.util.ButtonWaiter;
import pl.fratik.core.util.ClassicEmbedPaginator;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.core.util.UserUtil;
import pl.fratik.moderation.entity.Case;
import pl.fratik.moderation.entity.CaseDao;
import pl.fratik.moderation.utils.ModLogBuilder;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class AkcjeCommand extends ModerationCommand {

    private final CaseDao caseDao;
    private final ShardManager shardManager;
    private final EventWaiter eventWaiter;
    private final EventBus eventBus;
    private final ManagerKomend managerKomend;
    private final GuildDao guildDao;

    public AkcjeCommand(CaseDao caseDao, ShardManager shardManager, EventWaiter eventWaiter, EventBus eventBus, ManagerKomend managerKomend, GuildDao guildDao) {
        super(true);
        this.caseDao = caseDao;
        this.shardManager = shardManager;
        this.eventWaiter = eventWaiter;
        this.eventBus = eventBus;
        this.managerKomend = managerKomend;
        this.guildDao = guildDao;
        name = "akcje";
        aliases = new String[] {"administracyjne", "adm", "listawarnow", "listakickow", "listabanow", "ostrzezenia", "kicki", "bany", "ilewarnów"};
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
        permissions.add(Permission.MESSAGE_HISTORY);
        category = CommandCategory.MODERATION;
        uzycieDelim = " ";
        uzycie = new Uzycie("czlonek", "user");
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        Message m = context.reply(context.getTranslated("generic.loading"));
        User user = null;
        Object[] args = context.getArgs();
        if (args.length > 0 && args[0] != null) user = (User) args[0];
        if (user == null) user = context.getSender();
        return sendCases(context, user, caseDao.getCasesByMember(user, context.getGuild()), m);
    }

    @SubCommand(name = "admin", aliases = {"adm"})
    public boolean adminMode(@NotNull CommandContext context) {
        Message m = context.reply(context.getTranslated("generic.loading"));
        User user = null;
        Object[] args = context.getArgs();
        if (args.length > 0 && args[0] != null) user = (User) args[0];
        if (user == null) user = context.getSender();
        User finalUser = user;
        return sendCases(context, finalUser, caseDao.getCasesByGuild(context.getGuild()).stream()
                .filter(c -> Objects.equals(c.getIssuerId(), finalUser.getIdLong())).collect(Collectors.toList()), m);
    }

    public boolean sendCases(CommandContext context, User user, List<Case> mcases, Message m) {
        Collections.sort(mcases);
        List<Case> warnCases = mcases.stream().filter(c -> c.getType() == Kara.WARN).collect(Collectors.toList());
        List<Case> unwarnCases = mcases.stream().filter(c -> c.getType() == Kara.UNWARN).collect(Collectors.toList());
        long warnow = 0;
        long unwarnow = 0;
        for (Case c : warnCases) warnow += c.getIleRazy();
        for (Case c : unwarnCases) unwarnow += c.getIleRazy();
        long kickow = mcases.stream().filter(c -> c.getType() == Kara.KICK).count();
        long banow = mcases.stream().filter(c -> c.getType() == Kara.BAN).count();
        long mutow = mcases.stream().filter(c -> c.getType() == Kara.MUTE).count();
        long unmutow = mcases.stream().filter(c -> c.getType() == Kara.UNMUTE).count();
        List<EmbedBuilder> strony = new ArrayList<>();
        strony.add(context.getBaseEmbed(UserUtil.formatDiscrim(user), user.getEffectiveAvatarUrl()
                .replace(".webp", ".png"))
                .addField(context.getTranslated("akcje.embed.warns"),
                        String.format(context.getTranslated("akcje.embed.warns.content"),
                                (warnow - unwarnow), warnow, unwarnow),
                        true)
                .addField(context.getTranslated("akcje.embed.kicks"), String.valueOf(kickow), true)
                .addField(context.getTranslated("akcje.embed.bans"), String.valueOf(banow), true)
                .addField(context.getTranslated("akcje.embed.mutes"), String.valueOf(mutow), true)
                .addField(context.getTranslated("akcje.embed.unmutes"), String.valueOf(unmutow), true)
                .setDescription(context.getTranslated("akcje.embed.description")).setFooter("%s/%s", null));
        for (Case aCase : mcases) {
            EmbedBuilder eb = ModLogBuilder.generateEmbed(aCase, context.getGuild(), shardManager,
                    context.getLanguage(), managerKomend, false, true);
            eb.setFooter(Objects.requireNonNull(eb.build().getFooter()).getText() + " (%s/%s)", null);
            strony.add(eb);
        }
        new ClassicEmbedPaginator(eventWaiter, strony, context.getSender(), context.getLanguage(),
                context.getTlumaczenia(), eventBus).setCustomFooter(true).create(m);
        return true;
    }

    @SubCommand(name = "reset")
    public boolean reset(CommandContext context) {
        if (UserUtil.getPermlevel(context.getMember(), guildDao, shardManager, PermLevel.OWNER).getNum() < PermLevel.OWNER.getNum()) {
            context.reply(context.getTranslated("akcje.reset.perms"));
            return false;
        }
        Message msg = context.reply(context.getTranslated("akcje.reset.confirmation"), ActionRow.of(
                Button.danger("YES", context.getTranslated("generic.yes")),
                Button.secondary("NO", context.getTranslated("generic.no"))
        ));
        ButtonWaiter waiter = new ButtonWaiter(eventWaiter, context, msg.getIdLong(), ButtonWaiter.ResponseType.REPLY);
        waiter.setTimeoutHandler(() -> {
            msg.editMessage(msg.getContentRaw()).setActionRows(Collections.emptySet()).queue();
            context.send(context.getTranslated("akcje.reset.cancelled"));
        });
        waiter.setButtonHandler(e -> {
            msg.editMessage(msg.getContentRaw()).setActionRows(Collections.emptySet()).queue();
            if (!e.getComponentId().equals("YES")) {
                e.getHook().editOriginal(context.getTranslated("akcje.reset.cancelled")).queue();
                return;
            }
            try {
                caseDao.reset(context.getGuild().getIdLong());
                e.getHook().editOriginal(context.getTranslated("akcje.reset.complete")).completeAfter(1, TimeUnit.SECONDS);
            } catch (Exception err) {
                Sentry.getContext().setUser(new io.sentry.event.User(context.getSender().getId(),
                        UserUtil.formatDiscrim(context.getSender()), null, null));
                Sentry.capture(err);
                Sentry.clearContext();
                try {
                    e.getHook().editOriginal(context.getTranslated("akcje.reset.error")).complete();
                } catch (ErrorResponseException ed) {
                    if (ed.getErrorResponse() == ErrorResponse.UNKNOWN_MESSAGE) {
                        context.reply(context.getTranslated("akcje.reset.error"));
                        return;
                    }
                }
                throw err;
            }
        });
        waiter.create();
        return true;
    }
}
