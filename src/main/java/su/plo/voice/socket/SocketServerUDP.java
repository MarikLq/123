package su.plo.voice.socket;

import org.bukkit.entity.Player;
import su.plo.voice.PlasmoVoice;
import su.plo.voice.common.packets.Packet;
import su.plo.voice.common.packets.udp.PacketUDP;

import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SocketServerUDP extends Thread {
    public static final ConcurrentHashMap<Player, SocketClientUDP> clients = new ConcurrentHashMap<>();
    public static final Map<UUID, Long> talking = new HashMap<>();
    private final SocketAddress addr;

    public static boolean started;
    private final SocketServerUDPQueue queue;
    private static DatagramSocket socket;

    public SocketServerUDP(String ip, int port) {
        this.addr = new InetSocketAddress(ip, port);
        this.queue = new SocketServerUDPQueue();
        this.queue.start();
    }

    public static void sendToNearbyPlayers(Packet packet, Player player, double maxDistance) {
        double maxDistanceSquared = maxDistance * maxDistance * 1.25F;

        byte[] bytes;
        try {
            bytes = PacketUDP.write(packet);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        SocketServerUDP.clients.forEach((p, sock) -> {
            if (!player.getUniqueId().equals(p.getUniqueId())) {
                if (maxDistanceSquared > 0) {
                    if (!player.getLocation().getWorld().getName().equals(p.getLocation().getWorld().getName())) {
                        return;
                    }

                    try {
                        if (player.getLocation().distanceSquared(p.getLocation()) > maxDistanceSquared) {
                            return;
                        }
                    } catch (IllegalArgumentException ignored) {
                        return;
                    }
                }

                try {
                    SocketServerUDP.sendTo(bytes, sock);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static SocketClientUDP getSender(PacketUDP packet) {
        return clients.values().stream()
                .filter(connection -> connection.getAddress()
                        .equals(packet.getAddress()) && connection.getPort() == packet.getPort())
                .findAny().orElse(null);
    }

    public static void sendTo(byte[] data, SocketClientUDP connection) throws IOException {
        socket.send(new DatagramPacket(data, data.length, connection.getAddress(), connection.getPort()));
    }

    public void close() {
        if (socket != null) {
            socket.close();
            queue.interrupt();
        }
    }

    @Override
    public void run() {
        try {
            socket = new DatagramSocket(this.addr);
            socket.setTrafficClass(0x04); // IPTOS_RELIABILITY
        } catch (SocketException e) {
            e.printStackTrace();
            PlasmoVoice.getVoiceLogger().info(String.format("Failed to bind socket. Check if port %d UDP is open",
                    PlasmoVoice.getInstance().getVoiceConfig().getPort()));
            return;
        }

        started = true;
        PlasmoVoice.getVoiceLogger().info("Voice UDP server started on " + addr.toString());

        while (!socket.isClosed()) {
            try {
                PacketUDP message = PacketUDP.read(socket);
                this.queue.queue.offer(message);

                synchronized (this.queue) {
                    this.queue.notify();
                }
            } catch (IOException | InstantiationException | IllegalStateException | IllegalArgumentException e) { // bad packet? just ignore it 4HEad
                if (PlasmoVoice.getInstance().getConfig().getBoolean("debug")) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
