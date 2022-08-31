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

package pl.fratik.commands.images;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.util.NetworkUtil;

import java.io.IOException;
import java.net.URLEncoder;

public class ChainCommand extends NewCommand {
    public ChainCommand() {
        name = "chain";
        usage = "<nalozku:user> [obok:user]";
        cooldown = 5;
        allowInDMs = true;
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        User nalozku = context.getArguments().get("nalozku").getAsUser();
        User obok = context.getArgumentOr("obok", context.getSender(), OptionMapping::getAsUser);
        if (nalozku.getId().equals(obok.getId())) {
            context.replyEphemeral(context.getTranslated("chain.selfchain"));
            return;
        }
        context.deferAsync(false);
        try {
            JSONObject zdjecie = NetworkUtil.getJson(String.format("%s/api/image/chain?avatarURL=%s&avatarURL2=%s",
                    Ustawienia.instance.apiUrls.get("image-server"),
                    URLEncoder.encode(nalozku.getEffectiveAvatarUrl().replace(".webp", ".png")
                            + "?size=2048", "UTF-8"),
                    URLEncoder.encode(obok.getEffectiveAvatarUrl().replace(".webp", ".png")
                            + "?size=2048", "UTF-8")), Ustawienia.instance.apiKeys.get("image-server"));
            if (zdjecie == null || !zdjecie.getBoolean("success")) {
                context.sendMessage("Wystąpił błąd ze zdobyciem zdjęcia!");
                return;
            }
            byte[] img = NetworkUtil.getBytesFromBufferArray(zdjecie.getJSONObject("image").getJSONArray("data"));
            context.sendMessage("chain.png", img);
        } catch (IOException | NullPointerException e) {
            context.reply(context.getTranslated("image.server.fail"));
        }


    }
}
