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

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Emote;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.MemberConfig;
import pl.fratik.core.entity.MemberDao;

import java.util.Calendar;
import java.util.Date;

public class DailyCommand extends MoneyCommand {

    private final MemberDao memberDao;

    public DailyCommand(MemberDao memberDao) {
        this.memberDao = memberDao;
        name = "daily";
        category = CommandCategory.MONEY;
        permissions.add(Permission.MESSAGE_EXT_EMOJI);
        aliases = new String[] {"dzienna", "dziennazaplata", "zaplatadzienna", "kasazadarmo", "kieszonkowe", "getfc", "wyplata"};
        allowPermLevelChange = false;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        Emote emotkaFc = getFratikCoin(context);
        MemberConfig mc = memberDao.get(context.getMember());
        Date dailyDate = mc.getDailyDate();
        Date teraz = new Date();
        if (mc.getDailyDate() != null) {
            long dist = dailyDate.getTime() - teraz.getTime();
            if (dist >= 0) {
                context.reply(context.getTranslated("daily.cooldown"));
                return false;
            }
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(teraz);
        cal.add(Calendar.DAY_OF_MONTH, 1);
        dailyDate = Date.from(cal.toInstant());
        long fc = isHoliday() ? mc.getFratikCoiny() + 500 : mc.getFratikCoiny() + 250;
        if (checkTooMuch(fc, context)) return false;
        String msg = isHoliday() ? "daily.success.holiday" : "daily.success";
        mc.setFratikCoiny(fc);
        mc.setDailyDate(dailyDate);
        memberDao.save(mc);
        context.reply(context.getTranslated(msg, emotkaFc.getAsMention(), mc.getFratikCoiny(), emotkaFc.getAsMention()));
        return true;
    }

    private static Boolean isHoliday() { // mozna to pozniej gdzies przeniesc
        Date teraz = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(teraz);
        return calendar.get(Calendar.MONTH) == Calendar.DECEMBER && calendar.get(Calendar.DAY_OF_MONTH) == 24;
        // TODO: dodac inne swieta
    }
}

