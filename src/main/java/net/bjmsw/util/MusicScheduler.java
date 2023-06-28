package net.bjmsw.util;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import net.bjmsw.Launcher;
import net.bjmsw.manager.GuildPlayerManager;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

public class MusicScheduler extends AudioEventAdapter {

    private boolean skipEvent = false;
    private AudioPlayerManager playerManager;
    private GuildPlayerManager gpm;
    private Map<String, Queue<AudioTrack>> guildQueues;
    public MusicScheduler(AudioPlayerManager playerManager, GuildPlayerManager gpm) {
        this.playerManager = playerManager;
        this.gpm = gpm;
        guildQueues = new HashMap<>();
    }

    public void addTrack(AudioTrack track, String guildId) {
        checkQueue(guildId);
        guildQueues.get(guildId).add(track);
        if (gpm.getPlayerForGuild(guildId).getPlayingTrack() == null)
            playNextTrack(guildId);
    }

    public void playNextTrack(String guildId, ButtonInteractionEvent event) {
        if (guildQueues.get(guildId).isEmpty()) {
            skipEvent = true;
            event.reply("There are no more tracks in the queue!").queue();
            return;
        }
        event.reply("Skipped " + gpm.getPlayerForGuild(guildId).getPlayingTrack().getInfo().title).queue();
        playNextTrack(guildId);
    }

    public boolean playNextTrack(String guildId) {
        checkQueue(guildId);
        if (!guildQueues.get(guildId).isEmpty()) {
            gpm.getPlayerForGuild(guildId).startTrack(guildQueues.get(guildId).poll(), false);
            Launcher.getJda().getGuildById(guildId).getDefaultChannel().asTextChannel().sendMessage(String.format("Now playing %s (%s).", gpm.getPlayerForGuild(guildId).getPlayingTrack().getInfo().title, gpm.getPlayerForGuild(guildId).getPlayingTrack().getInfo().uri))
                    .addActionRow(
                            Button.primary("pause", "Pause"),
                            Button.danger("stop", "Stop"),
                            Button.primary("skip", "Skip")
                    )
                    .queue();
            return true;
        }
        return false;
    }

    public void insertTrack(AudioTrack track, String guildID) {
        skipEvent = true;
        checkQueue(guildID);
        gpm.getPlayerForGuild(guildID).startTrack(track, false);
    }

    private void checkQueue(String guildId) {
        if (!guildQueues.containsKey(guildId))
            guildQueues.put(guildId, new ArrayBlockingQueue<>(500));
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (endReason == AudioTrackEndReason.REPLACED) return;
        if (endReason == AudioTrackEndReason.LOAD_FAILED) {
            Launcher.getJda().getGuildById(gpm.getGuildForPlayer(player)).getDefaultChannel().asTextChannel().sendMessage("Failed to load track " + track.getInfo().title).queue();
            return;
        }
        if (skipEvent) {
            skipEvent = false;
            return;
        }
        System.out.println("Track ended for " + Launcher.getJda().getGuildById(gpm.getGuildForPlayer(player)).getName() + " (" + Launcher.getJda().getGuildById(gpm.getGuildForPlayer(player)).getId() + ").");
        String guildId = gpm.getGuildForPlayer(player);

        if (guildQueues.get(guildId).isEmpty() && player.getPlayingTrack() == null) {
            Launcher.getJda().getGuildById(guildId).getAudioManager().closeAudioConnection();
            Launcher.getJda().getGuildById(guildId).getDefaultChannel().asTextChannel().sendMessage("Queue finished.").queue();
        } else {
            Launcher.getJda().getGuildById(guildId).getDefaultChannel().asTextChannel().sendMessage(String.format("Track %s finished", track.getInfo().title, guildQueues.get(guildId).peek().getInfo().uri)).queue();
            playNextTrack(guildId);
        }

    }

    public Queue<AudioTrack> getQueue(String guildId) {
        checkQueue(guildId);
        return guildQueues.get(guildId);
    }
}
