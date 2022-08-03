package su.plo.voice;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import su.plo.voice.common.packets.Packet;
import su.plo.voice.data.ServerMutedEntity;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface PlasmoVoiceAPI {
    /**
     * Mute the player
     *
     * @param player       Player UUID
     * @param duration     Duration of the mute in durationTime
     * @param durationUnit DurationUnit, can be null, if duration is 0
     * @param reason       Reason for the mute
     * @param silent       If true, the player won't see the message about the mute
     */
    void mute(UUID player, long duration, @Nullable DurationUnit durationUnit, @Nullable String reason, boolean silent);

    /**
     * Unmute the player
     *
     * @param player Player UUID
     * @param silent if true, player won't see the message that they are no longer muted
     * @return returns true, if the player is no longer muted. false, if the player wasn't muted, to begin with
     */
    boolean unmute(UUID player, boolean silent);

    /**
     * @param player Player UUID
     * @return null is no reason given
     */
    @Nullable String getMuteReason(UUID player);

    /**
     * @param player Player UUID
     * @return returns timestamp when the mute is over
     */
    long getTimestampOfMuteEnd(UUID player);

    /**
     * @return list of players with voice chat
     */
    Set<Player> getConnectedPlayers();

    /**
     * Send voice chat server packet to player. Useful for redirecting packets.
     *
     * @param packet Voice Chat Server Packet
     * @param recipient Recipient
     * @return true if the packet was sent successfully, false if the packet was not sent
     */
    boolean sendVoicePacketToPlayer(@NotNull Packet packet, @NotNull Player recipient);

    /**
     * Check if the player is muted
     *
     * @param player Player UUID
     */
    boolean isMuted(UUID player);

    /**
     * @return list of muted players uuids
     */
    List<UUID> getMutedPlayersUUIDs();

    /**
     * Map of the muted players
     *
     * @return Map (key - Player UUID, value - ServerMutedEntity)
     */
    Map<UUID, ServerMutedEntity> getMutedMap();

    /**
     * @param player Online player UUID
     * @return true, if the player has the mod installed
     */
    boolean hasVoiceChat(UUID player);

    /**
     * @param player Online player UUID
     * @return true if the player is talking
     */
    boolean isTalking(UUID player);

    /**
     * Returns ModLoader of the player
     *
     * @param player Online Player UUID
     * @return fabric/forge or null, if the player doesn't have the mod installed
     */
    @Nullable
    String getPlayerModLoader(UUID player);

    /**
     * @return List of UUIDs of the players with the mod installed
     */
    List<UUID> getConnectedPlayersUUIDs();

    /**
     * Set player voice distances
     */
    void setVoiceDistances(UUID playerId, List<Integer> distances, Integer defaultDistance, Integer fadeDivisor);

    enum DurationUnit {
        SECONDS,
        MINUTES,
        HOURS,
        DAYS,
        WEEKS,
        TIMESTAMP;

        public long multiply(long duration) {
            switch (this) {
                case MINUTES:
                    return duration * 60;
                case HOURS:
                    return duration * 3600;
                case DAYS:
                    return duration * 86400;
                case WEEKS:
                    return duration * 604800;
                default:
                    return duration;
            }
        }

        public String format(long duration) {
            switch (this) {
                case MINUTES:
                    return String.format(PlasmoVoice.getInstance().getMessage("mute_durations.minutes"), duration);
                case HOURS:
                    return String.format(PlasmoVoice.getInstance().getMessage("mute_durations.hours"), duration);
                case DAYS:
                    return String.format(PlasmoVoice.getInstance().getMessage("mute_durations.days"), duration);
                case WEEKS:
                    return String.format(PlasmoVoice.getInstance().getMessage("mute_durations.weeks"), duration);
                default:
                    return String.format(PlasmoVoice.getInstance().getMessage("mute_durations.seconds"), duration);
            }
        }
    }
}
