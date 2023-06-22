package net.bjmsw.manager;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GuildPlayerManager {

    AudioPlayerManager apm;

    private Map<String, AudioPlayer> guildPlayers;

    public GuildPlayerManager(AudioPlayerManager apm) {
        this.apm = apm;
        this.guildPlayers = new HashMap<>();
    }

    public AudioPlayer getPlayerForGuild(String guildId) {
        if (!guildPlayers.containsKey(guildId)) {
            guildPlayers.put(guildId, apm.createPlayer());
        }
        return guildPlayers.get(guildId);
    }

}
