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

package pl.fratik.commands.system;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import pl.fratik.commands.entity.Blacklist;
import pl.fratik.commands.entity.BlacklistDao;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.util.CommonUtil;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

public class BlacklistPopCommand extends Command {

    private final BlacklistDao blacklistDao;

    public BlacklistPopCommand(BlacklistDao blacklistDao) {
        this.blacklistDao = blacklistDao;
        name = "blacklistpop";
        category = CommandCategory.UTILITY;
        permLevel = PermLevel.GADMIN;
        LinkedHashMap<String, String> hmap = new LinkedHashMap<>();
        hmap.put("id", "string");
        hmap.put("powod", "string");
        hmap.put("[...]", "string");
        uzycie = new Uzycie(hmap, new boolean[] {true, true, false});
        uzycieDelim = " ";
        allowInDMs = true;
        allowPermLevelChange = false;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        String id = (String) context.getArgs()[0];
        String powod = Arrays.stream(Arrays.copyOfRange(context.getArgs(), 1, context.getArgs().length))
                .map(o -> o == null ? "" : o.toString()).collect(Collectors.joining(uzycieDelim));
        User user = null;
        Guild server = null;
        try {
            user = context.getShardManager().retrieveUserById(id).complete();
            if (user == null) throw new NullPointerException("jebać pis");
        } catch (Exception e) {
            server = context.getShardManager().getGuildById(id);
        }
        if (user == null && server == null) {
            if (!CommonUtil.ID_REGEX.matcher(id).matches()) {
                context.reply(context.getTranslated("blacklistpop.invalid.id"));
                return false;
            }
        }
        Blacklist xd = blacklistDao.get(id);
        if (xd.isBlacklisted()) {
            xd.setBlacklisted(false);
            xd.setReason(null);
            xd.setExecutor(null);
            context.reply(context.getTranslated("blacklistpop.success.removed"));
        } else {
            xd.setBlacklisted(true);
            xd.setReason(powod);
            xd.setExecutor(context.getSender().getId());
            context.reply(context.getTranslated("blacklistpop.success.added"));
        }
        blacklistDao.save(xd);
        return true;
    }
}
