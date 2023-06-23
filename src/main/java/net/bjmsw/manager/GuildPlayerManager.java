package net.bjmsw.manager;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import net.bjmsw.Launcher;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GuildPlayerManager {

    AudioPlayerManager apm;

    private BidiMap<String, AudioPlayer> guildPlayers;

    public GuildPlayerManager(AudioPlayerManager apm) {
        this.apm = apm;
        this.guildPlayers = new DualHashBidiMap<>();
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

}
