package net.bjmsw;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import net.bjmsw.io.Config;
import net.bjmsw.listener.JDAEventListener;
import net.bjmsw.manager.GuildPlayerManager;
import net.bjmsw.util.MusicScheduler;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class Launcher {

    private static AudioPlayerManager apm;
    private static JDA jda;

    private static MusicScheduler scheduler;


    public static final boolean DEBUG = true;


    public static void main(String[] args) {
        System.out.println("Hello world!");

        Config config = new Config();

        apm = new DefaultAudioPlayerManager();
        apm.registerSourceManager(new YoutubeAudioSourceManager());
        apm.registerSourceManager(SoundCloudAudioSourceManager.createDefault());
        apm.registerSourceManager(new BandcampAudioSourceManager());
        apm.registerSourceManager(new VimeoAudioSourceManager());
        apm.registerSourceManager(new TwitchStreamAudioSourceManager());
        apm.registerSourceManager(new BeamAudioSourceManager());
        apm.registerSourceManager(new HttpAudioSourceManager());
        apm.registerSourceManager(new LocalAudioSourceManager());

        GuildPlayerManager gpm = new GuildPlayerManager(apm);

        if (config.getToken().equalsIgnoreCase("PUT-YOUR-TOKEN-HERE")) {
            System.out.println("Please put your token in the config.json file!");
            System.exit(0);
        }

        scheduler = new MusicScheduler(apm, gpm);


        JDABuilder builder = JDABuilder.createDefault(config.getToken())
                .setActivity(Activity.listening(" some great music | momobot2"))
                .addEventListeners(new JDAEventListener(apm, gpm, scheduler));

        jda = builder.build();

        jda.updateCommands().addCommands(
                Commands.slash("eq", "Manipulate the eq"),
                Commands.slash("play", "Play a song from youtube")
                        .addOption(
                                OptionType.STRING, "query", "The search query or youtube link", true
                        )
                        .addOption(
                                OptionType.BOOLEAN, "skip-queue", "Whether to skip the queue or not", false
                        ),
                Commands.slash("add", "Add a song to the queue (if queue is empty, it will play the song)")
                        .addOption(OptionType.STRING, "query", "The search query or youtube link", true),
                Commands.slash("test", "Test command")
        ).queue();


    }

    public static JDA getJda() {
        return jda;
    }

    public static AudioPlayerManager getApm() {
        return apm;
    }

    public static MusicScheduler getScheduler() {
        return scheduler;
    }
}