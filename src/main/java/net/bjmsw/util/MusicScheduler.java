package net.bjmsw.util;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.bjmsw.manager.GuildPlayerManager;

import java.util.ArrayDeque;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public class MusicScheduler {

    private AudioPlayerManager playerManager;
    private GuildPlayerManager gpm;
    private Queue<AudioTrack> queue;
    public MusicScheduler(AudioPlayerManager playerManager, AudioPlayer player, GuildPlayerManager gpm) {
        this.playerManager = playerManager;
        this.gpm = gpm;
        queue = new ArrayBlockingQueue<>(100);
    }

    public void addTrack(AudioTrack track, String guildId) {
        if (!gpm.getPlayerForGuild(guildId).startTrack(track, true)) {
            queue.offer(track);
        }
    }

}
