package net.bjmsw.manager;

import com.sedmelluq.discord.lavaplayer.filter.equalizer.EqualizerFactory;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import net.bjmsw.Launcher;
import net.bjmsw.util.SeekbarMessage;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GuildPlayerManager {

    AudioPlayerManager apm;

    private BidiMap<String, AudioPlayer> guildPlayers;
    private BidiMap<String, EqualizerFactory> guildEqualizers;

    private BidiMap<String, SeekbarMessage> seekbarMessages;

    public GuildPlayerManager(AudioPlayerManager apm) {
        this.apm = apm;
        this.guildPlayers = new DualHashBidiMap<>();
        this.guildEqualizers = new DualHashBidiMap<>();
        this.seekbarMessages = new DualHashBidiMap<>();
    }

    public AudioPlayer getPlayerForGuild(String guildId) {
        if (!guildPlayers.containsKey(guildId)) {
            var player = apm.createPlayer();
            player.addListener(Launcher.getScheduler());
            guildPlayers.put(guildId, player);
        }
        return guildPlayers.get(guildId);
    }

    public String getGuildForPlayer(AudioPlayer player) {
        return guildPlayers.getKey(player);
    }

    public EqualizerFactory getEqualizerForGuild(String guildId) {
        if (!guildEqualizers.containsKey(guildId)) {
            var equalizer = new EqualizerFactory();
            guildEqualizers.put(guildId, equalizer);
        }
        return guildEqualizers.get(guildId);
    }

    public String getGuildForEqualizer(EqualizerFactory equalizer) {
        return guildEqualizers.getKey(equalizer);
    }

    public SeekbarMessage getSeekbarMessageForGuild(String guildID) {
        return seekbarMessages.get(guildID);
    }

    public void setSeekbarMessageForGuild(String guildID, SeekbarMessage seekbarMessage) {
        seekbarMessages.put(guildID, seekbarMessage);
    }
}
