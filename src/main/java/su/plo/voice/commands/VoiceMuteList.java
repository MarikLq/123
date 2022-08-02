package su.plo.voice.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import su.plo.voice.PlasmoVoice;

import java.text.SimpleDateFormat;
import java.util.Date;

public class VoiceMuteList implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (PlasmoVoice.getInstance().getMutedMap().isEmpty()) {
            sender.sendMessage(PlasmoVoice.getInstance().getMessagePrefix("muted_list_empty"));
            return true;
        }

        sender.sendMessage(PlasmoVoice.getInstance().getMessagePrefix("muted_list"));
        PlasmoVoice.getInstance().getMutedMap().forEach((uuid, muted) -> {
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            String expires = muted.getTo() > 0
                    ? new SimpleDateFormat(PlasmoVoice.getInstance().getMessage("mute_expires_format")).format(new Date(muted.getTo()))
                    : PlasmoVoice.getInstance().getMessage("mute_expires_never");
            String reason = muted.getReason() == null
                    ? PlasmoVoice.getInstance().getMessage("mute_no_reason")
                    : muted.getReason();
            sender.sendMessage(PlasmoVoice.getInstance().getMessage("muted_list_entry")
                    .replace("{player}", player.getName())
                    .replace("{expires}", expires)
                    .replace("{reason}", reason));
        });

        return true;
    }
}
