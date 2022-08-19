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

package pl.fratik.fratikcoiny.commands;

import com.google.common.eventbus.EventBus;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.SubCommand;
import pl.fratik.core.entity.*;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.*;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("squid:S1192")
public class SklepCommand extends Command {

    private final GuildDao guildDao;
    private final MemberDao memberDao;
    private final EventWaiter eventWaiter;
    private final ShardManager shardManager;
    private final EventBus eventBus;

    public SklepCommand(GuildDao guildDao, MemberDao memberDao, EventWaiter eventWaiter, ShardManager shardManager, EventBus eventBus) {
        this.guildDao = guildDao;
        this.memberDao = memberDao;
        this.eventWaiter = eventWaiter;
        this.shardManager = shardManager;
        this.eventBus = eventBus;
        name = "sklep";
        category = CommandCategory.MONEY;
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
        LinkedHashMap<String, String> hmap = new LinkedHashMap<>();
        hmap.put("rola", "role");
        hmap.put("kwota", "long");
        hmap.put("opis", "string");
        hmap.put("[...]", "string");
        uzycie = new Uzycie(hmap, new boolean[] {false, false, false, false});
        uzycieDelim = " ";
        allowPermLevelChange = false;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        CommonErrors.usage(context);
        return false;
    }

    @SubCommand(name="kup", aliases = "buy")
    public boolean kup(CommandContext context) {
        GuildConfig gc = guildDao.get(context.getGuild());
        if (context.getArgs().length == 0 || (context.getArgs().length > 0 && context.getArgs()[0] == null)) {
            context.reply(context.getTranslated("sklep.kup.invalidrole"));
            return false;
        }
        Role rola = (Role) context.getArgs()[0];
        if (!gc.getRoleDoKupienia().containsKey(rola.getId())) {
            context.reply(context.getTranslated("sklep.kup.cantbuythis"));
            return false;
        }
        long kasa = gc.getRoleDoKupienia().get(rola.getId());
        MemberConfig mc = memberDao.get(context.getMember());
        if (context.getMember().getRoles().contains(rola)) {
            EmbedBuilder eb = generateEmbed(Typ.KUPOWANIE, rola, gc.getRoleDoKupieniaOpisy().get(rola.getId()),
                    kasa, mc.getFratikCoiny(), true, context.getTlumaczenia(), context.getLanguage());
            context.reply(eb.build());
            context.send(context.getTranslated("sklep.kup.hasrole"));
            return false;
        }
        if (mc.getFratikCoiny() < gc.getRoleDoKupienia().get(rola.getId())) {
            EmbedBuilder eb = generateEmbed(Typ.KUPOWANIE, rola, gc.getRoleDoKupieniaOpisy().get(rola.getId()),
                    kasa, mc.getFratikCoiny(), false, context.getTlumaczenia(), context.getLanguage());
            context.reply(eb.build());
            context.send(context.getTranslated("sklep.kup.brakhajsu"));
            return false;
        }
        EmbedBuilder eb = generateEmbed(Typ.KUPOWANIE, rola, gc.getRoleDoKupieniaOpisy().get(rola.getId()),
                kasa, mc.getFratikCoiny(), false, context.getTlumaczenia(), context.getLanguage());
        Message msgTmp = context.reply(eb.build(), ActionRow.of(
                Button.success("BUY", context.getTranslated("sklep.przycisk.kup")),
                Button.danger("CANCEL", context.getTranslated("sklep.przycisk.anuluj"))
        ));
        ButtonWaiter rw = new ButtonWaiter(eventWaiter, context, msgTmp.getIdLong(), ButtonWaiter.ResponseType.REPLY);
        rw.setButtonHandler(event -> {
            msgTmp.editMessage(msgTmp).setActionRows(Collections.emptySet()).queue();
            if (event.getComponentId().equals("BUY")) {
                try {
                    context.getGuild().addRoleToMember(context.getMember(), rola).queue(aVoid -> {
                        MemberConfig mc2 = memberDao.get(Objects.requireNonNull(event.getMember()));
                        if (mc2.getFratikCoiny() < kasa) {
                            event.getHook().editOriginal(context.getTranslated("sklep.kup.brakhajsu")).queue();
                            return;
                        }
                        mc2.setFratikCoiny(mc2.getFratikCoiny() - kasa);
                        memberDao.save(mc2);
                        event.getHook().editOriginal(context.getTranslated("sklep.kup.success")).queue();
                    }, err -> event.getHook().editOriginal(context.getTranslated("sklep.kup.failed")).queue());
                } catch (Exception e) {
                    event.getHook().editOriginal(context.getTranslated("sklep.kup.failed")).queue();
                }
            }
            if (event.getComponentId().equals("CANCEL"))
                event.getHook().editOriginal(context.getTranslated("sklep.kup.canceled")).queue();
        });
        rw.setTimeoutHandler(() -> {
            msgTmp.editMessage(msgTmp).setActionRows(Collections.emptySet()).queue();
            context.send(context.getTranslated("sklep.kup.canceled"), m -> {});
        });
        rw.create();
        return true;
    }

    @SubCommand(name="sprzedaj", aliases = "sell")
    public boolean sprzedaj(CommandContext context) {
        GuildConfig gc = guildDao.get(context.getGuild());
        if (context.getArgs().length == 0 || (context.getArgs().length > 0 && context.getArgs()[0] == null)) {
            context.reply(context.getTranslated("sklep.sprzedaj.invalidrole"));
            return false;
        }
        Role rola = (Role) context.getArgs()[0];
        if (!gc.getRoleDoKupienia().containsKey(rola.getId())) {
            context.reply(context.getTranslated("sklep.sprzedaj.cantsellthis"));
            return false;
        }
        long kasa = gc.getRoleDoKupienia().get(rola.getId());
        MemberConfig mc = memberDao.get(context.getMember());
        if (!context.getMember().getRoles().contains(rola)) {
            EmbedBuilder eb = generateEmbed(Typ.SPRZEDAZ, rola, gc.getRoleDoKupieniaOpisy().get(rola.getId()),
                    kasa, mc.getFratikCoiny(), false, context.getTlumaczenia(), context.getLanguage());
            context.reply(eb.build());
            context.send(context.getTranslated("sklep.sprzedaj.hasrole"));
            return false;
        }
        EmbedBuilder eb = generateEmbed(Typ.SPRZEDAZ, rola, gc.getRoleDoKupieniaOpisy().get(rola.getId()),
                kasa, mc.getFratikCoiny(), true, context.getTlumaczenia(), context.getLanguage());
        Message msgTmp = context.reply(eb.build(), ActionRow.of(
                Button.success("SELL", context.getTranslated("sklep.przycisk.sprzedaj")),
                Button.danger("CANCEL", context.getTranslated("sklep.przycisk.anuluj"))
        ));
        ButtonWaiter rw = new ButtonWaiter(eventWaiter, context, msgTmp.getIdLong(), ButtonWaiter.ResponseType.REPLY);
        rw.setButtonHandler(event -> {
            msgTmp.editMessage(msgTmp).setActionRows(Collections.emptySet()).queue();
            if (event.getComponentId().equals("SELL")) {
                context.getGuild().removeRoleFromMember(context.getMember(), rola)
                        .queue(
                                aVoid -> {
                                    MemberConfig mc2 = memberDao.get(Objects.requireNonNull(event.getMember()));
                                    mc2.setFratikCoiny(mc2.getFratikCoiny() + (int) Math.floor((double) kasa / 2));
                                    memberDao.save(mc2);
                                    event.getHook().editOriginal(context.getTranslated("sklep.sprzedaj.success")).queue();
                                },
                                err -> event.getHook().editOriginal(context.getTranslated("sklep.sprzedaj.failed")).queue());
            }
            if (event.getComponentId().equals("CANCEL"))
                event.getHook().editOriginal(context.getTranslated("sklep.sprzedaj.canceled")).queue();
        });
        rw.setTimeoutHandler(() -> {
            msgTmp.editMessage(msgTmp).setActionRows(Collections.emptySet()).queue();
            context.send(context.getTranslated("sklep.sprzedaj.canceled"), m -> {});
        });
        rw.create();
        return true;
    }

    @SubCommand(name="lista",aliases={"list"})
    public boolean lista(CommandContext context) {
        Message wiadomosc = context.reply(context.getTranslated("generic.loading"));
        GuildConfig gc = guildDao.get(context.getGuild());
        MemberConfig mc = memberDao.get(context.getMember());
        List<EmbedBuilder> strony = new ArrayList<>();
        for (String rId : gc.getRoleDoKupienia().keySet()) {
            long kasa = gc.getRoleDoKupienia().get(rId);
            Role rola = context.getGuild().getRoleById(rId);
            if (rola == null) continue;
            EmbedBuilder eb = generateEmbed(Typ.INFO, rola, gc.getRoleDoKupieniaOpisy().get(rId), kasa,
                    mc.getFratikCoiny(), context.getMember().getRoles().contains(rola), context.getTlumaczenia(),
                    context.getLanguage());
            strony.add(eb);
        }
        if (strony.isEmpty()) {
            context.reply(context.getTranslated("sklep.list.empty"));
            return false;
        }
        ClassicEmbedPaginator paginator = new ClassicEmbedPaginator(eventWaiter, strony, context.getSender(),
                context.getLanguage(), context.getTlumaczenia(), eventBus);
        paginator.create(wiadomosc);
        return true;
    }

    @SubCommand(name="ustaw", aliases = "set")
    public boolean dodaj(CommandContext context) {
        if (UserUtil.getPermlevel(context.getMember(), guildDao, shardManager).getNum() < 2) {
            context.reply(context.getTranslated("sklep.ustaw.noperms"));
            return false;
        }
        GuildConfig gc = guildDao.get(context.getGuild());
        if (context.getArgs().length == 0 || (context.getArgs().length > 0 && context.getArgs()[0] == null)) {
            context.reply(context.getTranslated("sklep.ustaw.invalidrole"));
            return false;
        }
        if (context.getArgs().length == 1 || (context.getArgs().length > 1 && context.getArgs()[1] == null)) {
            context.reply(context.getTranslated("sklep.ustaw.invalidprice"));
            return false;
        }
        Role rola = (Role) context.getArgs()[0];
        long kasa = (Long) context.getArgs()[1];
        String opis = null;
        if (!(context.getArgs().length == 2 || (context.getArgs().length > 2 && context.getArgs()[2] == null))) {
            opis = Arrays.stream(Arrays.copyOfRange(context.getArgs(), 2, context.getArgs().length))
                    .map(o -> o == null ? "" : o.toString()).collect(Collectors.joining(uzycieDelim));
        }
        if (gc.getRoleDoKupienia().containsKey(rola.getId())) {
            context.reply(context.getTranslated("sklep.ustaw.alreadyset"));
            return false;
        }
        if (!context.getMember().getRoles().get(0).canInteract(rola)) {
            EmbedBuilder eb = generateEmbed(Typ.DODAWANIE, rola, opis, kasa, 0, true,
                    context.getTlumaczenia(), context.getLanguage());
            context.reply(eb.build());
            context.send(context.getTranslated("sklep.ustaw.noperms"));
        }
        EmbedBuilder eb = generateEmbed(Typ.DODAWANIE, rola, opis, kasa, 0, false,
                context.getTlumaczenia(), context.getLanguage());
        Message msgTmp = context.reply(eb.build(), ActionRow.of(
                Button.success("ADD", context.getTranslated("sklep.przycisk.dodaj")),
                Button.danger("CANCEL", context.getTranslated("sklep.przycisk.anuluj"))
        ));
        ButtonWaiter rw = new ButtonWaiter(eventWaiter, context, msgTmp.getIdLong(), ButtonWaiter.ResponseType.REPLY);
        String finalOpis = opis;
        rw.setButtonHandler(event -> {
            msgTmp.editMessage(msgTmp).setActionRows(Collections.emptySet()).queue();
            if (event.getComponentId().equals("ADD")) {
                GuildConfig gc2 = guildDao.get(event.getGuild());
                Map<String, Long> roleDoKupienia = gc.getRoleDoKupienia();
                roleDoKupienia.put(rola.getId(), kasa);
                Map<String, String> roleDoKupieniaOpisy = gc.getRoleDoKupieniaOpisy();
                roleDoKupieniaOpisy.put(rola.getId(), finalOpis);
                gc2.setRoleDoKupienia(roleDoKupienia);
                gc2.setRoleDoKupieniaOpisy(roleDoKupieniaOpisy);
                guildDao.save(gc2);
                event.getHook().editOriginal(context.getTranslated("sklep.ustaw.success")).queue();
            }
            if (event.getComponentId().equals("CANCEL"))
                event.getHook().editOriginal(context.getTranslated("sklep.ustaw.canceled")).queue();
        });
        rw.setTimeoutHandler(() -> {
            msgTmp.editMessage(msgTmp).setActionRows(Collections.emptySet()).queue();
            context.send(context.getTranslated("sklep.ustaw.canceled"), m -> {});
        });
        rw.create();
        return true;
    }

    @SubCommand(name="usun", aliases = "delete")
    public boolean usun(CommandContext context) {
        if (UserUtil.getPermlevel(context.getMember(), guildDao, shardManager).getNum() < 2) {
            context.reply(context.getTranslated("sklep.usun.noperms"));
            return false;
        }
        GuildConfig gc = guildDao.get(context.getGuild());
        if (context.getArgs().length == 0 || (context.getArgs().length > 0 && context.getArgs()[0] == null)) {
            context.reply(context.getTranslated("sklep.usun.invalidrole"));
            return false;
        }
        Role rola = (Role) context.getArgs()[0];
        if (!gc.getRoleDoKupienia().containsKey(rola.getId())) {
            context.reply(context.getTranslated("sklep.usun.alreadyset"));
            return false;
        }
        if (!context.getMember().getRoles().get(0).canInteract(rola)) {
            EmbedBuilder eb = generateEmbed(Typ.USUWANIE, rola, gc.getRoleDoKupieniaOpisy().get(rola.getId()),
                    gc.getRoleDoKupienia().get(rola.getId()), 0, true,
                    context.getTlumaczenia(), context.getLanguage());
            context.reply(eb.build());
            context.send(context.getTranslated("sklep.usun.noperms"));
        }
        EmbedBuilder eb = generateEmbed(Typ.USUWANIE, rola, gc.getRoleDoKupieniaOpisy().get(rola.getId()),
                gc.getRoleDoKupienia().get(rola.getId()), 0, false,
                context.getTlumaczenia(), context.getLanguage());
        Message msgTmp = context.reply(eb.build(), ActionRow.of(
                Button.secondary("DELETE", context.getTranslated("sklep.przycisk.usun")),
                Button.danger("CANCEL", context.getTranslated("sklep.przycisk.anuluj"))
        ));
        ButtonWaiter rw = new ButtonWaiter(eventWaiter, context, msgTmp.getIdLong(), ButtonWaiter.ResponseType.REPLY);
        rw.setButtonHandler(event -> {
            msgTmp.editMessage(msgTmp).setActionRows(Collections.emptySet()).queue();
            if (event.getComponentId().equals("DELETE")) {
                GuildConfig gc2 = guildDao.get(event.getGuild());
                Map<String, Long> roleDoKupienia = gc.getRoleDoKupienia();
                roleDoKupienia.remove(rola.getId());
                Map<String, String> roleDoKupieniaOpisy = gc.getRoleDoKupieniaOpisy();
                roleDoKupieniaOpisy.remove(rola.getId());
                gc2.setRoleDoKupienia(roleDoKupienia);
                gc2.setRoleDoKupieniaOpisy(roleDoKupieniaOpisy);
                guildDao.save(gc2);
                event.getHook().editOriginal(context.getTranslated("sklep.usun.success")).queue();
            }
            if (event.getComponentId().equals("CANCEL"))
                event.getHook().editOriginal(context.getTranslated("sklep.usun.canceled")).queue();
        });
        rw.setTimeoutHandler(() -> {
            msgTmp.editMessage(msgTmp).setActionRows(Collections.emptySet()).queue();
            context.send(context.getTranslated("sklep.usun.canceled"), m -> {});
        });
        rw.create();
        return true;
    }

    @SuppressWarnings("squid:S00107")
    private EmbedBuilder generateEmbed(Typ typ, Role rola, String opis, long cena, long hajs, boolean hasRole, Tlumaczenia tlumaczenia, Language l) {
        EmbedBuilder eb = new EmbedBuilder();
        switch (typ) {
            case KUPOWANIE:
                eb.setTitle(tlumaczenia.get(l, "sklep.embed.kup"));
                eb.addField(tlumaczenia.get(l, "sklep.embed.nazwa"), rola.getName(), false);
                eb.addField(tlumaczenia.get(l, "sklep.embed.opis"),
                        opis == null || opis.isEmpty() ? tlumaczenia.get(l, "sklep.embed.opis.pusty") : opis,
                        false);
                eb.addField(tlumaczenia.get(l, "sklep.embed.cena.kupna"), String.valueOf(cena), true);
                eb.addField(tlumaczenia.get(l, "sklep.embed.cena.sprzedazy"), String.valueOf((int) Math.floor((double) cena / 2)), true);
                eb.addField(tlumaczenia.get(l, "sklep.embed.hajs"), String.valueOf(hajs), true);
                if (hajs < cena) {
                    eb.setFooter(tlumaczenia.get(l, "sklep.embed.zamalokasy"), null);
                    eb.setColor(Color.RED);
                    return eb;
                }
                if (hasRole) {
                    eb.setFooter(tlumaczenia.get(l, "sklep.embed.hasrole"), null);
                    eb.setColor(Color.RED);
                    return eb;
                }
                eb.setColor(Color.GREEN);
                return eb;
            case SPRZEDAZ:
                eb.setTitle(tlumaczenia.get(l, "sklep.embed.sprzedaj"));
                eb.addField(tlumaczenia.get(l, "sklep.embed.nazwa"), rola.getName(), false);
                eb.addField(tlumaczenia.get(l, "sklep.embed.opis"),
                        opis == null || opis.isEmpty() ? tlumaczenia.get(l, "sklep.embed.opis.pusty") : opis,
                        false);
                eb.addField(tlumaczenia.get(l, "sklep.embed.sprzedaj.cena"), String.valueOf((int) Math.floor((double) cena / 2)), true);
                eb.addField(tlumaczenia.get(l, "sklep.embed.hajs"), String.valueOf(hajs), true);
                if (!hasRole) {
                    eb.setFooter(tlumaczenia.get(l, "sklep.embed.hasrole.nie"), null);
                    eb.setColor(Color.RED);
                    return eb;
                }
                eb.setColor(Color.GREEN);
                return eb;
            case INFO:
                eb.setTitle(tlumaczenia.get(l, "sklep.embed.info", rola.getName()));
                eb.addField(tlumaczenia.get(l, "sklep.embed.opis"),
                        opis == null || opis.isEmpty() ? tlumaczenia.get(l, "sklep.embed.opis.pusty") : opis,
                        false);
                eb.addField(tlumaczenia.get(l, "sklep.embed.info.posiadanie"), hasRole ?
                        Objects.requireNonNull(shardManager.getEmoteById(Ustawienia.instance.emotki.greenTick)).getAsMention() :
                        Objects.requireNonNull(shardManager.getEmoteById(Ustawienia.instance.emotki.redTick)).getAsMention(), false);
                eb.addField(tlumaczenia.get(l, "sklep.embed.cena.kupna"), String.valueOf(cena), true);
                eb.addField(tlumaczenia.get(l, "sklep.embed.cena.sprzedazy"), String.valueOf((int) Math.floor((double) cena / 2)), true);
                eb.addField(tlumaczenia.get(l, "sklep.embed.hajs"), String.valueOf(hajs), true);
                eb.setColor(rola.getColor());
                return eb;
            case DODAWANIE:
                eb.setTitle(tlumaczenia.get(l, "sklep.embed.dodaj"));
                eb.addField(tlumaczenia.get(l, "sklep.embed.nazwa"), rola.getName(), false);
                eb.addField(tlumaczenia.get(l, "sklep.embed.cena.kupna"), String.valueOf(cena), true);
                eb.addField(tlumaczenia.get(l, "sklep.embed.cena.sprzedazy"), String.valueOf((int) Math.floor((double) cena / 2)), true);
                eb.addField(tlumaczenia.get(l, "sklep.embed.opis"),
                        opis == null || opis.isEmpty() ? tlumaczenia.get(l, "sklep.embed.opis.pusty") : opis,
                        false);
                if (hasRole) { //hasRole == nie ma permow w tym przypadku
                    eb.setColor(Color.RED);
                    eb.setFooter(tlumaczenia.get(l, "sklep.embed.dodaj.niemozna"), null);
                }
                eb.setColor(Color.GREEN);
                return eb;
            case USUWANIE:
                eb.setTitle(tlumaczenia.get(l, "sklep.embed.usun"));
                eb.addField(tlumaczenia.get(l, "sklep.embed.nazwa"), rola.getName(), false);
                eb.addField(tlumaczenia.get(l, "sklep.embed.cena.kupna"), String.valueOf(cena), true);
                eb.addField(tlumaczenia.get(l, "sklep.embed.cena.sprzedazy"), String.valueOf((int) Math.floor((double) cena / 2)), true);
                eb.addField(tlumaczenia.get(l, "sklep.embed.opis"),
                        opis == null || opis.isEmpty() ? tlumaczenia.get(l, "sklep.embed.opis.pusty") : opis,
                        false);
                if (hasRole) { //hasRole == nie ma permow w tym przypadku
                    eb.setColor(Color.RED);
                    eb.setFooter(tlumaczenia.get(l, "sklep.embed.usun.niemozna"), null);
                }
                eb.setColor(Color.GREEN);
                return eb;
        }
        throw new IllegalStateException("typ nieprawidłowy");
    }

    private enum Typ {
        KUPOWANIE, SPRZEDAZ, INFO, DODAWANIE, USUWANIE
    }

}
