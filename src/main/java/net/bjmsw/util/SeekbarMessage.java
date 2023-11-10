package net.bjmsw.util;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import net.bjmsw.Launcher;
import net.bjmsw.manager.GuildPlayerManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

public class SeekbarMessage extends Thread {

    private Message message;
    private final TextChannel channel;
    private final String guildID;
    private GuildPlayerManager gpm;
    private AudioPlayer player;

    public SeekbarMessage(TextChannel channel, String guildID, GuildPlayerManager gpm) {
        this.gpm = gpm;
        this.channel = channel;
        this.guildID = guildID;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(500);
            while (true) {
                if (Launcher.DEBUG) System.out.println("[Seekbar] Started seekbar thread");
                player = gpm.getPlayerForGuild(guildID);
                if (player == null) this.interrupt();
                if (Launcher.DEBUG)
                    System.out.println("[Seekbar] Position: " + player.getPlayingTrack().getPosition() + ", Duration: " + player.getPlayingTrack().getDuration());

                double percentage = (((double) player.getPlayingTrack().getPosition() / player.getPlayingTrack().getDuration()) * 50);

                if (Launcher.DEBUG) System.out.println("[Seekbar] Percentage: " + percentage);
                String msg = buildMessage(50, (int) percentage, player.getPlayingTrack().getInfo().title);
                if (message == null) {
                    if (Launcher.DEBUG) System.out.println("[Seekbar] Message is null, creating new one");
                    message = channel.sendMessage(msg).complete();
                } else {
                    message.editMessage(msg).queue();
                }
                if (Launcher.DEBUG) System.out.println("[Seekbar] Sleeping for 1 second");
                Thread.sleep(1000);
            }
        } catch (InterruptedException | NullPointerException e) {
            if (Launcher.DEBUG) System.out.println("[Seekbar] Interrupted");
            if (message != null) message.delete().complete();
        }
    }

    private String buildMessage(int length, int position, String title) {
        StringBuilder sb = new StringBuilder();
        sb.append("Playing: ").append(title).append("\n");
        sb.append("```[");
        for (int i = 0; i < length; i++) {
            if (i == position) {
                sb.append("X");
            } else {
                sb.append("-");
            }
        }
        sb.append("]```");
        return sb.toString();
    }
}
