package net.bjmsw.listener;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.bjmsw.Launcher;
import net.bjmsw.manager.GuildPlayerManager;
import net.bjmsw.util.AudioPlayerSendHandler;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.util.ArrayList;
import java.util.List;

public class EventListener extends ListenerAdapter {

    private enum PlayerState {
        PLAYING,
        PAUSED,
        STOPPED
    }

    private AudioPlayerManager apm;
    private GuildPlayerManager gpm;
    EventListener.PlayerState state = EventListener.PlayerState.STOPPED;

    public EventListener(AudioPlayerManager apm, GuildPlayerManager gpm) {
        this.apm = apm;
        this.gpm = gpm;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equalsIgnoreCase("play")) {
            //event.deferReply().queue();
            var query = event.getOption("query").getAsString();
            event.deferReply().setEphemeral(true).queue();
            if (Launcher.DEBUG) System.out.println("Query: " + query);
            playTrack(query, true, event);
        } else if (event.getName().equalsIgnoreCase("eq")) {
            event.reply("Equalizer Controls")
                    .addActionRow(
                            Button.primary("eq-bassboost", "Bass Boost"),
                            Button.primary("eq-normal", "Normal"),
                            Button.primary("eq-piano", "Piano"),
                            Button.primary("eq-soft", "Soft"),
                            Button.primary("eq-trebleboost", "Treble Boost")
                    ).setEphemeral(true).queue();
        } else if (event.getName().equalsIgnoreCase("test")) {
            System.out.println(gpm.getPlayerForGuild(event.getGuild().getId()).getPlayingTrack());
            event.reply(gpm.getPlayerForGuild(event.getGuild().getId()).getPlayingTrack().toString()).setEphemeral(true).queue();
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        var player = gpm.getPlayerForGuild(event.getGuild().getId());
        System.out.println("DEBUG: Button interaction detected: " + event.getComponentId());
        if (event.getComponentId().equalsIgnoreCase("pause")) {
            if (player.isPaused()) {
                event.reply("No track is currently playing!").setEphemeral(true).queue();
                return;
            }
            player.setPaused(true);
            event.editButton(Button.primary("resume", "Resume")).queue();
            state = PlayerState.PAUSED;
        } else if (event.getComponentId().equalsIgnoreCase("stop")) {
            player.stopTrack();
            event.getHook().sendMessage("Stopped!").queue();
            event.getGuild().getAudioManager().closeAudioConnection();
            event.reply("Stopped!").setEphemeral(true).queue();
            state = PlayerState.STOPPED;
        } else if (event.getComponentId().equalsIgnoreCase("resume")) {
            if (player.getPlayingTrack() == null) {
                event.reply("No track is currently paused!").setEphemeral(true).queue();
                return;
            }
            player.setPaused(false);
            state = PlayerState.PLAYING;
            event.editButton(Button.primary("pause", "Pause")).queue();
        }
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        if (event.getComponentId().equalsIgnoreCase("select-track-search-result")) {
            event.deferReply().queue();
            var query = event.getSelectedOptions().get(0).getValue();
            if (Launcher.DEBUG) System.out.println("Query: " + query);
            playTrack(query, true, event);
        }
    }

    private void playTrack(String query, boolean now, GenericInteractionCreateEvent event) {
        var player = gpm.getPlayerForGuild(event.getGuild().getId());
        if (!query.startsWith("http")) {
            query = "ytsearch:" + query;
        }

        var vcs = event.getGuild().getVoiceChannels();
        if (vcs.isEmpty()) {
            event.getMessageChannel().sendMessage("There are no voice channels on this server!").queue();
            //event.getHook().sendMessage("There are no voice channels on this server!").queue();
            return;
        }
        var vc = vcs.get(0);

        Launcher.getApm().loadItem(query, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                event.getGuild().getAudioManager().openAudioConnection(vc);
                event.getGuild().getAudioManager().setSendingHandler(new AudioPlayerSendHandler(player));
                player.playTrack(track);


                try (MessageCreateData message = new MessageCreateBuilder().setContent("Now playing: " + track.getInfo().title + "(" + track.getInfo().uri + ")")
                        .addActionRow(
                                Button.primary("pause", "Pause"),
                                Button.danger("stop", "Stop")
                        ).build()) {
                    if (event instanceof SlashCommandInteractionEvent) {
                        System.out.println("DEBUG: Sending ephemeral message SlashCommandInteractionEvent");
                        ((SlashCommandInteractionEvent) event).getHook().sendMessage(message).setEphemeral(false).queue();
                    } else if (event instanceof StringSelectInteractionEvent) {
                        System.out.println("DEBUG: Sending ephemeral message StringSelectInteractionEvent");
                        ((StringSelectInteractionEvent) event).getHook().sendMessage(message).setEphemeral(false).queue();
                    } else {
                        System.out.println("DEBUG: Sending ephemeral message GenericInteractionCreateEvent");
                        event.getMessageChannel().sendMessage(message).queue();
                    }
                }



                state = PlayerState.PLAYING;


            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                if (playlist.isSearchResult()) {
                    List<SelectOption> options = new ArrayList<>();
                    for (var track : playlist.getTracks()) {
                        options.add(SelectOption.of(track.getInfo().title, track.getInfo().uri));
                    }

                    MessageCreateBuilder builder = new MessageCreateBuilder();
                    builder.setContent("The following tracks have been found:");
                    builder.addActionRow(
                            StringSelectMenu.create("select-track-search-result")
                                    .setPlaceholder("Select a track")
                                    .addOptions(options)
                                    .build()

                    );

                    if (event instanceof SlashCommandInteractionEvent) {
                        System.out.println("DEBUG: Sending ephemeral message SlashCommandInteractionEvent");
                        ((SlashCommandInteractionEvent) event).getHook().sendMessage(builder.build()).setEphemeral(true).queue();
                    } else if (event instanceof StringSelectInteractionEvent) {
                        System.out.println("DEBUG: Sending ephemeral message StringSelectInteractionEvent");
                        ((StringSelectInteractionEvent) event).getHook().sendMessage(builder.build()).setEphemeral(true).queue();
                    } else {
                        System.out.println("DEBUG: Sending message");
                        event.getMessageChannel().sendMessage(builder.build()).queue();
                    }


                }
            }

            @Override
            public void noMatches() {

            }

            @Override
            public void loadFailed(FriendlyException exception) {

            }
        });
    }
}
