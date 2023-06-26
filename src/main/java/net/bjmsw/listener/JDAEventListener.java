package net.bjmsw.listener;

import com.sedmelluq.discord.lavaplayer.filter.equalizer.EqualizerFactory;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.bjmsw.Launcher;
import net.bjmsw.io.SDConfigFile;
import net.bjmsw.manager.GuildPlayerManager;
import net.bjmsw.model.SDConfig;
import net.bjmsw.util.AudioPlayerSendHandler;
import net.bjmsw.util.MusicScheduler;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.apache.commons.codec.binary.StringUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JDAEventListener extends ListenerAdapter {

    private enum PlayerState {
        PLAYING,
        PAUSED,
        STOPPED
    }

    private AudioPlayerManager apm;
    private GuildPlayerManager gpm;
    private MusicScheduler scheduler;

    private static final float[] BASS_BOOST = {0.2f, 0.15f, 0.1f, 0.05f, 0.0f, -0.05f, -0.1f, -0.1f, -0.1f, -0.1f, -0.1f,
            -0.1f, -0.1f, -0.1f, -0.1f};


    JDAEventListener.PlayerState state = JDAEventListener.PlayerState.STOPPED;

    public JDAEventListener(AudioPlayerManager apm, GuildPlayerManager gpm, MusicScheduler scheduler) {
        this.apm = apm;
        this.gpm = gpm;
        this.scheduler = scheduler;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equalsIgnoreCase("play")) {
            //event.deferReply().queue();
            var query = event.getOption("query").getAsString();
            event.deferReply().setEphemeral(true).queue();
            if (Launcher.DEBUG) System.out.println("Query: " + query);
            if (event.getOption("skip-queue") != null)
                playTrack(query, event.getOption("skip-queue").getAsBoolean(), event);
            else
                playTrack(query, false, event);
        } else if (event.getName().equalsIgnoreCase("eq")) {
            event.reply("Equalizer Controls")
                    .addActionRow(
                            Button.primary("eq-bassboost", "Bass Boost"),
                            Button.primary("eq-normal", "Normal")
                    ).setEphemeral(true).queue();
        } else if (event.getName().equalsIgnoreCase("txt2img")) {
            SDConfig config = Launcher.getSdConfigFile().getConfig(event.getGuild().getId());
            var samplerSteps = "20";
            if (config != null) samplerSteps = String.valueOf(config.getSamplerSteps());
            event.replyModal(
                    Modal.create("sd-text2img", "SD txt2img generator")
                            .addActionRow(
                                    TextInput.create("sd-prompt", "Image prompt", TextInputStyle.PARAGRAPH)
                                            .setPlaceholder("Enter the prompt for the image (or leave blank to use the last prompt)")
                                            .setRequired(false)
                                            .build()
                            )
                            .addActionRow(
                                    TextInput.create("sd-negative-prompt", "Negative Image prompt", TextInputStyle.PARAGRAPH)
                                            .setPlaceholder("Enter the prompt for the negative image (or leave blank to use the last prompt)")
                                            .setRequired(false)
                                            .build()
                            )
                            .addActionRow(
                                    TextInput.create("sd-batch-size", "Batch Size (num of images)", TextInputStyle.SHORT)
                                            .setValue("1")
                                            .setPlaceholder("Enter the number of images you want to be created")
                                            .setRequired(true)
                                            .build()
                            )
                            .addActionRow(
                                    TextInput.create("sd-steps", "Sampler Steps", TextInputStyle.SHORT)
                                            .setValue(samplerSteps)
                                            .setPlaceholder("Enter the number of steps to use for the sampler")
                                            .setRequired(true)
                                            .build()
                            )
                            .build()
            ).queue();


        } else if (event.getName().equalsIgnoreCase("sd-config")) {
            boolean hasConfig = Launcher.getSdConfigFile().getConfig(event.getGuild().getId()) != null;
            SDConfig sdConfig = null;

            if (hasConfig) {
                sdConfig = Launcher.getSdConfigFile().getConfig(event.getGuild().getId());

                event.replyModal(
                        Modal.create("sd-configure", "StableDiffusion Configuration")
                                .addActionRow(
                                        TextInput.create("sd-default-negative-prompt", "Default Negative Prompt", TextInputStyle.PARAGRAPH)
                                                .setPlaceholder("Enter the default negative prompt")
                                                .setValue(sdConfig.getDefaultNegativePrompt())
                                                .setRequired(true)
                                                .build()
                                )
                                .addActionRow(
                                        TextInput.create("sd-default-steps", "Default Sampler Steps", TextInputStyle.SHORT)
                                                .setPlaceholder("Enter the default steps")
                                                .setValue(String.valueOf(sdConfig.getSamplerSteps()))
                                                .setRequired(true)
                                                .build()
                                )
                                .addActionRow(
                                        TextInput.create("sd-default-cfg-scale", "Default CFG-Scale", TextInputStyle.SHORT)
                                                .setPlaceholder("Enter the default scale")
                                                .setValue(String.valueOf(sdConfig.getCfgScale()))
                                                .setRequired(true)
                                                .build()
                                )
                                .addActionRow(
                                        TextInput.create("sd-default-sampler", "Default Sampler Name", TextInputStyle.SHORT)
                                                .setPlaceholder("Enter the default sampler")
                                                .setValue(sdConfig.getSampler())
                                                .setRequired(true)
                                                .build()
                                )
                                .build()
                ).queue();
            } else {
                event.replyModal(
                        Modal.create("sd-configure", "StableDiffusion Configuration")
                                .addActionRow(
                                        TextInput.create("sd-default-negative-prompt", "Default Negative Prompt", TextInputStyle.PARAGRAPH)
                                                .setPlaceholder("Enter the default negative prompt")
                                                .setRequired(true)
                                                .build()
                                )
                                .addActionRow(
                                        TextInput.create("sd-default-steps", "Sampler Steps", TextInputStyle.SHORT)
                                                .setPlaceholder("Enter the default steps")
                                                .setRequired(true)
                                                .build()
                                )
                                .addActionRow(
                                        TextInput.create("sd-default-cfg-scale", "CFG-Scale", TextInputStyle.SHORT)
                                                .setPlaceholder("Enter the default scale")
                                                .setRequired(true)
                                                .build()
                                )
                                .addActionRow(
                                        TextInput.create("sd-default-sampler", "Sampler Name", TextInputStyle.SHORT)
                                                .setPlaceholder("Enter the default sampler")
                                                .setRequired(true)
                                                .build()
                                )
                                .build()
                ).queue();
            }



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
            //event.getHook().sendMessage("Stopped!").queue();
            event.getGuild().getAudioManager().closeAudioConnection();
            event.reply("Stopped!").setEphemeral(false).queue();
            state = PlayerState.STOPPED;
        } else if (event.getComponentId().equalsIgnoreCase("resume")) {
            if (player.getPlayingTrack() == null) {
                event.reply("No track is currently paused!").setEphemeral(true).queue();
                return;
            }
            player.setPaused(false);
            state = PlayerState.PLAYING;
            event.editButton(Button.primary("pause", "Pause")).queue();
        } else if (event.getComponentId().equalsIgnoreCase("skip")) {
           scheduler.playNextTrack(event.getGuild().getId(), event);
        } else if (event.getComponentId().equalsIgnoreCase("eq-bassboost")) {
            event.reply("Enabling bassboost! Be prepared!").setEphemeral(true).queue();
            var bassBoost = gpm.getEqualizerForGuild(event.getGuild().getId());
            player.setFilterFactory(bassBoost);
            for (int i = 0; i < BASS_BOOST.length; i++) {
                bassBoost.setGain(i, BASS_BOOST[i] + 2);
            }
        } else if (event.getComponentId().equalsIgnoreCase("eq-normal")) {
            event.reply("Disabling eq filters!").setEphemeral(true).queue();
            player.setFilterFactory(null);
        } else if (event.getComponentId().equalsIgnoreCase("eq-piano")) {
            var piano = gpm.getEqualizerForGuild(event.getGuild().getId());
            player.setFilterFactory(piano);

        }
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (event.getModalId().equals("sd-text2img")) {
            var prompt = event.getValue("sd-prompt").getAsString();
            var negativePrompt = event.getValue("sd-negative-prompt").getAsString();
            try {
                Integer.parseInt(event.getValue("sd-batch-size").getAsString());
                Integer.parseInt(event.getValue("sd-steps").getAsString());
            } catch (NumberFormatException e) {
                event.reply("Invalid modal inputs in numeric fields (at least one input was not numeric)!").setEphemeral(true).queue();
                return;
            }
            int batchSize = Integer.parseInt(event.getValue("sd-batch-size").getAsString());
            int steps = Integer.parseInt(event.getValue("sd-steps").getAsString());
            Launcher.getStableDiffusion().txt2img(prompt, negativePrompt, batchSize, steps, event);
        } else if (event.getModalId().equalsIgnoreCase("sd-configure")) {

            try {
                Integer.parseInt(event.getValue("sd-default-steps").getAsString());
                Integer.parseInt(event.getValue("sd-default-cfg-scale").getAsString());
            } catch (NumberFormatException e) {
                event.reply("Invalid modal inputs in numeric fields (at least one input was not numeric)!").setEphemeral(true).queue();
                return;
            }

            SDConfig sdConfig = new SDConfig(
                    event.getGuild().getId(),
                    event.getValue("sd-default-negative-prompt").getAsString(),
                    Integer.parseInt(event.getValue("sd-default-steps").getAsString()),
                    Integer.parseInt(event.getValue("sd-default-cfg-scale").getAsString()),
                    event.getValue("sd-default-sampler").getAsString()
            );

            try {
                Launcher.getSdConfigFile().addConfig(sdConfig);
                event.reply("Successfully saved the configuration!").setEphemeral(true).queue();
            } catch (IOException e) {
                event.reply("An error occurred while saving the configuration!").setEphemeral(true).queue();
                throw new RuntimeException(e);
            }

        }
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        if (event.getComponentId().startsWith("select-track-search-result")) {
            boolean skip = Boolean.parseBoolean(event.getComponentId().split(":")[1]);
            event.deferReply().queue();
            var query = event.getSelectedOptions().get(0).getValue();
            if (Launcher.DEBUG) System.out.println("Query: " + query);
            playTrack(query, skip, event);
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

                if (now) {
                    scheduler.insertTrack(track, event.getGuild().getId());
                    try (MessageCreateData message = new MessageCreateBuilder().setContent("Starting track: " + track.getInfo().title + " (" + track.getInfo().uri + ")")
                            .addActionRow(
                                    Button.primary("pause", "Pause"),
                                    Button.danger("stop", "Stop"),
                                    Button.primary("skip", "Skip")
                            ).build()) {
                        dispatchMessageAsEventReply(event, message, false);
                    }
                } else {
                    MessageCreateData message = new MessageCreateBuilder().setContent("Added to queue: " + track.getInfo().title).build();
                    scheduler.addTrack(track, event.getGuild().getId());
                    dispatchMessageAsEventReply(event, message, false);
                }


            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                if (playlist.isSearchResult()) {
                    List<SelectOption> options = new ArrayList<>();
                    for (var track : playlist.getTracks()) {
                        options.add(SelectOption.of(track.getInfo().title, track.getInfo().uri));
                    }

                    var selectionID = "select-track-search-result-skip:" + now;
                    System.out.println("DEBUG: Selection ID: " + selectionID);
                    MessageCreateBuilder builder = new MessageCreateBuilder();
                    builder.setContent("The following tracks have been found:");
                    builder.addActionRow(
                            StringSelectMenu.create(selectionID)
                                    .setPlaceholder("Select a track")
                                    .addOptions(options)
                                    .build()

                    );

                    dispatchMessageAsEventReply(event, builder.build(), true);


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

    private void dispatchMessageAsEventReply(GenericInteractionCreateEvent event, MessageCreateData message, boolean ephemeral) {
        if (event instanceof SlashCommandInteractionEvent) {
            System.out.println("DEBUG: Sending ephemeral message SlashCommandInteractionEvent");
            ((SlashCommandInteractionEvent) event).getHook().sendMessage(message).setEphemeral(ephemeral).queue();
        } else if (event instanceof StringSelectInteractionEvent) {
            System.out.println("DEBUG: Sending ephemeral message StringSelectInteractionEvent");
            ((StringSelectInteractionEvent) event).getHook().sendMessage(message).setEphemeral(ephemeral).queue();
        } else {
            System.out.println("DEBUG: Sending ephemeral message GenericInteractionCreateEvent");
            event.getMessageChannel().sendMessage(message).queue();
        }
    }
}
