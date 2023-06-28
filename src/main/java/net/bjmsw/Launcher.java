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
import net.bjmsw.io.SDConfigFile;
import net.bjmsw.listener.JDAEventListener;
import net.bjmsw.manager.GuildPlayerManager;
import net.bjmsw.model.SDConfig;
import net.bjmsw.util.MusicScheduler;
import net.bjmsw.util.StableDiffusion;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class Launcher {

    private static AudioPlayerManager apm;
    private static JDA jda;
    private static StableDiffusion stableDiffusion;
    private static SDConfigFile sdConfigFile;
    private static Config config;
    private static JDAEventListener listener;

    private static MusicScheduler scheduler;


    public static final boolean DEBUG = true;
    public static final String VERSION = "2.0.0";


    public static void main(String[] args) {
        System.out.println("Hello world!");

        config = new Config();
        sdConfigFile = new SDConfigFile();

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

        stableDiffusion = new StableDiffusion(config.getSD_url());

        if (config.getToken().equalsIgnoreCase("PUT-YOUR-TOKEN-HERE")) {
            System.out.println("Please put your token in the config.json file!");
            System.exit(0);
        }

        scheduler = new MusicScheduler(apm, gpm);


        listener = new JDAEventListener(apm, gpm, scheduler);
        JDABuilder builder = JDABuilder.createDefault(config.getToken())
                .setActivity(Activity.listening(" some great music | momobot2"))
                .addEventListeners(listener);

        jda = builder.build();

        jda.updateCommands().addCommands(
                Commands.slash("eq", "Manipulate the eq"),
                Commands.slash("play", "Play a song now (skip current song and insert in queue)")
                        .addOption(
                                OptionType.STRING, "query", "The search query or youtube link", true
                        ),
                Commands.slash("add", "Add a song to the queue (if queue is empty, it will play the song)")
                        .addOption(OptionType.STRING, "query", "The search query or youtube link", true),
                Commands.slash("test", "Test command"),
                Commands.slash("txt2img", "Create a stable diffusion image from text (only works when the provider server is online)"),
                Commands.slash("sd-config", "Configure Stable-Diffusion Settings fot your guild"),
                Commands.slash("queue", "Show the current queue"),
                Commands.slash("inspiro", "Generate a random inspiro bot image"),
                Commands.slash("help", "Show commands & usage"),
                Commands.slash("configure-channels", "Configure the channels for the bot for this guild")
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

    public static StableDiffusion getStableDiffusion() {
        return stableDiffusion;
    }

    public static SDConfigFile getSdConfigFile() {
        return sdConfigFile;
    }

    public static Config getConfig() {
        return config;
    }

    public static JDAEventListener getListener() {
        return listener;
    }
}