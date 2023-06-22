package net.bjmsw.util;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.ArrayDeque;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public class MusicScheduler {

    private AudioPlayerManager playerManager;
    private AudioPlayer player;
    private Queue<AudioTrack> queue;
    public MusicScheduler(AudioPlayerManager playerManager, AudioPlayer player) {
        this.playerManager = playerManager;
        this.player = player;
        queue = new ArrayBlockingQueue<>(100);
    }

}
