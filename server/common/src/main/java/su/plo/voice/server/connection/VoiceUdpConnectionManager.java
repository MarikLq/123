package su.plo.voice.server.connection;

import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.plo.voice.api.event.EventSubscribe;
import su.plo.voice.api.server.PlasmoVoiceServer;
import su.plo.voice.api.server.connection.UdpServerConnectionManager;
import su.plo.voice.api.server.event.connection.UdpConnectEvent;
import su.plo.voice.api.server.event.connection.UdpConnectedEvent;
import su.plo.voice.api.server.event.connection.UdpDisconnectEvent;
import su.plo.voice.api.server.player.VoicePlayer;
import su.plo.voice.api.server.socket.UdpConnection;
import su.plo.voice.proto.packets.Packet;
import su.plo.voice.proto.packets.udp.clientbound.ClientPacketUdpHandler;
import su.plo.voice.server.event.player.PlayerQuitEvent;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

@AllArgsConstructor
public final class VoiceUdpConnectionManager implements UdpServerConnectionManager {

    private static final Logger LOGGER = LogManager.getLogger();

    private final PlasmoVoiceServer server;

    private final Map<UUID, UUID> secretByPlayerId = Maps.newConcurrentMap();
    private final Map<UUID, UUID> playerIdBySecret = Maps.newConcurrentMap();

    private final Map<UUID, UdpConnection> connectionBySecret = Maps.newConcurrentMap();
    private final Map<UUID, UdpConnection> connectionByPlayerId = Maps.newConcurrentMap();

    @Override
    public Optional<UUID> getPlayerIdBySecret(UUID secret) {
        return Optional.ofNullable(playerIdBySecret.get(secret));
    }

    @Override
    public UUID getSecretByPlayerId(UUID playerUUID) {
        if (secretByPlayerId.containsKey(playerUUID)) {
            return secretByPlayerId.get(playerUUID);
        }

        UUID secret = UUID.randomUUID();
        secretByPlayerId.put(playerUUID, secret);
        playerIdBySecret.put(secret, playerUUID);

        return secret;
    }

    @Override
    public void addConnection(UdpConnection connection) {
        UdpConnectEvent connectEvent = new UdpConnectEvent(connection);
        server.getEventBus().call(connectEvent);
        if (connectEvent.isCancelled()) return;

        UdpConnection bySecret = connectionBySecret.put(connection.getSecret(), connection);
        UdpConnection byPlayer = connectionByPlayerId.put(connection.getPlayer().getInstance().getUUID(), connection);

        if (bySecret != null) bySecret.disconnect();
        if (byPlayer != null) byPlayer.disconnect();

        server.getEventBus().call(new UdpConnectedEvent(connection));
    }

    @Override
    public boolean removeConnection(UdpConnection connection) {
        UdpConnection bySecret = connectionBySecret.remove(connection.getSecret());
        UdpConnection byPlayer = connectionByPlayerId.remove(connection.getPlayer().getInstance().getUUID());

        if (bySecret != null) disconnect(bySecret);
        if (byPlayer != null && !byPlayer.equals(bySecret)) disconnect(byPlayer);

        return bySecret != null || byPlayer != null;
    }

    @Override
    public boolean removeConnection(VoicePlayer player) {
        UdpConnection connection = connectionByPlayerId.remove(player.getInstance().getUUID());
        if (connection != null) disconnect(connection);

        return connection != null;
    }

    @Override
    public boolean removeConnection(UUID secret) {
        UdpConnection connection = connectionBySecret.remove(secret);
        if (connection != null) disconnect(connection);

        return connection != null;
    }

    @Override
    public Optional<UdpConnection> getConnectionBySecret(UUID secret) {
        return Optional.ofNullable(connectionBySecret.get(secret));
    }

    @Override
    public Optional<UdpConnection> getConnectionByUUID(UUID playerUUID) {
        return Optional.ofNullable(connectionByPlayerId.get(playerUUID));
    }

    @Override
    public Collection<UdpConnection> getConnections() {
        return connectionByPlayerId.values();
    }

    @Override
    public void clearConnections() {
        getConnections().forEach(this::removeConnection);
    }

    private void disconnect(UdpConnection connection) {
        connection.disconnect();

        secretByPlayerId.remove(connection.getPlayer().getInstance().getUUID());
        playerIdBySecret.remove(connection.getSecret());

        // LOGGER.info("{} disconnected", connection.getPlayer());
        server.getEventBus().call(new UdpDisconnectEvent(connection));
    }

    @Override
    public void broadcast(@NotNull Packet<ClientPacketUdpHandler> packet, @Nullable Predicate<VoicePlayer> filter) {
        for (UdpConnection connection : getConnections()) {
            if (filter == null || filter.test(connection.getPlayer()))
                connection.sendPacket(packet);
        }
    }

    @Override
    public void broadcast(@NotNull Packet<ClientPacketUdpHandler> packet) {
        broadcast(packet, null);
    }

    @EventSubscribe
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        getConnectionByUUID(event.getPlayerId()).ifPresent(this::removeConnection);
    }
}
