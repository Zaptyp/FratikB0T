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

import jersey.repackaged.com.google.common.collect.Lists;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.requests.restaction.PermissionOverrideAction;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;

import java.util.List;

public class LockCommand extends ModerationCommand {

    private final GuildDao guildDao;
    private final ManagerKomend managerKomend;

    public LockCommand(GuildDao guildDao, ManagerKomend managerKomend) {
        super(false);
        this.guildDao = guildDao;
        this.managerKomend = managerKomend;
        name = "lock";
        aliases = new String[] {"unlock", "zamknij", "zablokuj", "bloknij", "koniecrozmowy", "koniecpogadanki", "zablokujpisanienakanale"};
        category = CommandCategory.MODERATION;
        permLevel = PermLevel.ADMIN;
        permissions.add(Permission.MANAGE_PERMISSIONS);
    }

    @Override
    public boolean preExecute(CommandContext context) {
        if (context.getMessageChannel().getType() != ChannelType.TEXT) {
            context.reply(context.getTranslated("generic.text.only"));
            return false;
        }
        return super.preExecute(context);
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        GuildConfig gc = guildDao.get(context.getGuild());
        if (gc.getAdminRole() == null || gc.getAdminRole().equals("") || (gc.getAdminRole() != null &&
                !gc.getAdminRole().equals("") && context.getGuild().getRoleById(gc.getAdminRole()) == null)) {
            context.reply(context.getTranslated("lock.no.adminrole", managerKomend.getPrefixes(context.getGuild()).get(0)));
            return false;
        }
        Role adminRole = context.getGuild().getRoleById(gc.getAdminRole());
        if (adminRole == null) {
            context.reply(context.getTranslated("lock.no.adminrole", managerKomend.getPrefixes(context.getGuild()).get(0)));
            return false;
        }
        PermissionOverrideAction overrides = context.getTextChannel().upsertPermissionOverride(context.getGuild().getPublicRole());
        if (overrides.getDeniedPermissions().contains(Permission.MESSAGE_SEND)) {
            PermissionOverrideAction publicOverrides = context.getTextChannel().upsertPermissionOverride(context.getGuild().getPublicRole());
            PermissionOverrideAction adminOverrides = context.getTextChannel().upsertPermissionOverride(adminRole);
            List<Permission> publicDeny = Lists.newArrayList(publicOverrides.getDeniedPermissions());
            List<Permission> adminAllow = Lists.newArrayList(adminOverrides.getAllowedPermissions());
            publicDeny.remove(Permission.MESSAGE_SEND);
            publicDeny.remove(Permission.MESSAGE_ADD_REACTION);
            adminAllow.remove(Permission.MESSAGE_SEND);
            adminAllow.remove(Permission.MESSAGE_ADD_REACTION);
            try {
                publicOverrides.setDeny(publicDeny).complete();
                adminOverrides.setAllow(adminAllow).complete();
                context.reply(context.getTranslated("lock.unlock.success"));
            } catch (Exception e) {
                context.reply(context.getTranslated("lock.unlock.fail"));
            }
        } else {
            PermissionOverrideAction publicOverrides = context.getTextChannel().putPermissionOverride(context.getGuild().getPublicRole());
            PermissionOverrideAction adminOverrides = context.getTextChannel().putPermissionOverride(adminRole);
            List<Permission> publicDeny = Lists.newArrayList(publicOverrides.getDeniedPermissions());
            List<Permission> adminAllow = Lists.newArrayList(adminOverrides.getAllowedPermissions());
            publicDeny.add(Permission.MESSAGE_SEND);
            publicDeny.add(Permission.MESSAGE_ADD_REACTION);
            adminAllow.add(Permission.MESSAGE_SEND);
            adminAllow.add(Permission.MESSAGE_ADD_REACTION);
            try {
                publicOverrides.setDeny(publicDeny).complete();
                adminOverrides.setAllow(adminAllow).complete();
                context.reply(context.getTranslated("lock.lock.success"));
            } catch (Exception e) {
                context.reply(context.getTranslated("lock.lock.fail"));
            }
        }
        return true;
    }

    @Override
    public PermLevel getPermLevel() {
        return PermLevel.ADMIN;
    }
}
