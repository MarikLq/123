package su.plo.voice.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import su.plo.voice.PlasmoVoice;
import su.plo.voice.PlasmoVoiceConfig;
import su.plo.voice.common.packets.tcp.ConfigPacket;
import su.plo.voice.common.packets.tcp.PacketTCP;
import su.plo.voice.events.PlayerConfigEvent;
import su.plo.voice.socket.SocketServerUDP;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;

public class VoiceReload implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, String[] args) {
        PlasmoVoice.getInstance().reloadConfig();
        PlasmoVoice.getInstance().updateConfig();

        PlasmoVoiceConfig config = PlasmoVoice.getInstance().getVoiceConfig();

        Bukkit.getScheduler().runTaskAsynchronously(PlasmoVoice.getInstance(), () -> {
            try {
                Enumeration<Player> it = SocketServerUDP.clients.keys();
                while (it.hasMoreElements()) {
                    Player player = it.nextElement();

                    ConfigPacket configPacket = new ConfigPacket(
                            config.getSampleRate(),
                            new ArrayList<>(config.getDistances()),
                            config.getDefaultDistance(),
                            config.getMaxPriorityDistance(),
                            config.getFadeDivisor(),
                            config.getPriorityFadeDivisor(),
                            config.isDisableVoiceActivation() || !player.hasPermission("voice.activation")
                    );

                    PlayerConfigEvent event = new PlayerConfigEvent(player, configPacket, PlayerConfigEvent.Cause.RELOAD);
                    Bukkit.getPluginManager().callEvent(event);
                    if (event.isCancelled()) {
                        continue;
                    }

                    byte[] pkt = PacketTCP.write(configPacket);

                    player.sendPluginMessage(PlasmoVoice.getInstance(), "plasmo:voice", pkt);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        sender.sendMessage(PlasmoVoice.getInstance().getMessagePrefix("reloaded"));
        return true;
    }
}
