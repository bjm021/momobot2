package net.bjmsw.listener;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.bjmsw.Launcher;
import net.bjmsw.manager.GuildPlayerManager;
import net.bjmsw.model.ChannelIDConfig;
import net.bjmsw.model.SDConfig;
import net.bjmsw.util.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
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
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class JDAEventListener extends ListenerAdapter {

    private BidiMap<String, String> guildVCIDs = new DualHashBidiMap<>();
    private BidiMap<String, String> guildTCIDs = new DualHashBidiMap<>();
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
        checkChanelIDConfig(event.getGuild().getId());
        if (event.getName().equalsIgnoreCase("play")) {
            //event.deferReply().queue();
            var query = event.getOption("query").getAsString();
            event.deferReply().setEphemeral(true).queue();
            if (Launcher.DEBUG) System.out.println("Query (play): " + query);
            playTrack(query, true, event);

        } else if (event.getName().equals("add")) {
            var query = event.getOption("query").getAsString();
            event.deferReply().setEphemeral(true).queue();
            if (Launcher.DEBUG) System.out.println("Query (add): " + query);
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



        } else if(event.getName().equals("test")) {
            EmbedBuilder eb = new EmbedBuilder();
            eb.addField("Test", "Test", false);
            eb.setColor(Color.red);
            event.replyEmbeds(eb.build()).setEphemeral(true).queue();
        } else if (event.getName().equals("queue")) {
            EmbedBuilder eb = new EmbedBuilder();
            AtomicInteger counter = new AtomicInteger(1);
            Launcher.getScheduler().getQueue(event.getGuild().getId()).forEach(track -> {
                eb.addField(counter + ".: " + track.getInfo().title, track.getInfo().author, false);
                counter.getAndIncrement();
            });
            eb.setColor(Color.CYAN);
            var empheral = true;
            if ((guildTCIDs.containsKey(event.getGuild().getId()) && guildTCIDs.get(event.getGuild().getId()).equals(event.getChannel().getId()))
                    || (!guildTCIDs.containsKey(event.getGuild().getId()) && event.getChannel().getId().equals(event.getGuild().getSystemChannel().getId())))
                empheral = false;
            event.replyEmbeds(eb.build()).setEphemeral(empheral).queue();
        } else if (event.getName().equals("inspiro")) {
            event.deferReply().queue();
            InspiroBot.inspiro(event);
        } else if (event.getName().equals("help")) {
            StaticMessages.help(event);
        } else if (event.getName().equals("configure-channels")) {
            if (Launcher.getConfig().getChannelIDsForGuild(event.getGuild().getId()) != null) {
                var channelIDs = Launcher.getConfig().getChannelIDsForGuild(event.getGuild().getId());
                event.replyModal(
                        Modal.create("configure-channels", "Configure Bot Channels")
                                .addActionRow(
                                        TextInput.create("bot-vc-id", "Bot Voice Channel ID", TextInputStyle.SHORT)
                                                .setPlaceholder("Enter the ID of the voice channel you want the bot to join")
                                                .setValue(channelIDs.getVoiceChannelID())
                                                .setRequired(true)
                                                .build()
                                )
                                .addActionRow(
                                        TextInput.create("bot-tc-id", "Bot Text Channel ID", TextInputStyle.SHORT)
                                                .setPlaceholder("Enter the ID of the text channel you want the bot to use for status messaged")
                                                .setRequired(true)
                                                .setValue(channelIDs.getTextChannelID())
                                                .build()
                                )
                                .build()
                ).queue();
            } else {
                event.replyModal(
                        Modal.create("configure-channels", "Configure Bot Channels")
                                .addActionRow(
                                        TextInput.create("bot-vc-id", "Bot Voice Channel ID", TextInputStyle.SHORT)
                                                .setPlaceholder("Enter the ID of the voice channel you want the bot to join")
                                                .setRequired(true)
                                                .build()
                                )
                                .addActionRow(
                                        TextInput.create("bot-tc-id", "Bot Text Channel ID", TextInputStyle.SHORT)
                                                .setPlaceholder("Enter the ID of the text channel you want the bot to use for status messaged")
                                                .setRequired(true)
                                                .build()
                                )
                                .build()
                ).queue();
            }
        } else if(event.getName().equals("pause")) {
            if (gpm.getPlayerForGuild(event.getGuild().getId()).getPlayingTrack() != null) {
                gpm.getPlayerForGuild(event.getGuild().getId()).setPaused(true);
                event.reply("Paused the playback!").setEphemeral(true).queue();
            } else {
                event.reply("No track is currently playing!").setEphemeral(true).queue();
            }
        } else if (event.getName().equals("resume")) {
            if (gpm.getPlayerForGuild(event.getGuild().getId()).isPaused()) {
                gpm.getPlayerForGuild(event.getGuild().getId()).setPaused(false);
                event.reply("Resumed the playback!").setEphemeral(true).queue();
            } else {
                event.reply("No track is currently paused!").setEphemeral(true).queue();
            }
        } else if (event.getName().equals("skip")) {
            event.reply("Trying to skip the current track!").setEphemeral(true).queue();
            scheduler.playNextTrack(event.getGuild().getId());
        } else if (event.getName().equals("cat")) {
            event.deferReply().queue();
            CatAsAService.sendCat(event);
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
            if (gpm.getSeekbarMessageForGuild(event.getGuild().getId()) != null) {
                gpm.getSeekbarMessageForGuild(event.getGuild().getId()).interrupt();
            }
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
        } else if (event.getComponentId().equals("10sback")) {
            player.getPlayingTrack().setPosition(player.getPlayingTrack().getPosition() - 10000);
            event.editButton(Button.primary("10sback", "<< 10s")).queue();
        } else if (event.getComponentId().equals("10sforward")) {
            player.getPlayingTrack().setPosition(player.getPlayingTrack().getPosition() + 10000);
            event.editButton(Button.primary("10sforward", "10s >>")).queue();
        } else if (event.getComponentId().equals("showeq")) {
            event.reply("Equalizer Controls")
                    .addActionRow(
                            Button.primary("eq-bassboost", "Bass Boost"),
                            Button.primary("eq-normal", "Normal")
                    ).setEphemeral(true).queue();
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

        } else if (event.getModalId().equals("configure-channels")) {

            try {
                if (Launcher.getJda().getChannelById(TextChannel.class, event.getValue("bot-tc-id").getAsString()) == null) {
                    event.reply("Invalid text channel ID!").setEphemeral(true).queue();
                    return;
                }
            } catch (IllegalArgumentException e) {
                event.reply("Invalid text channel ID!").setEphemeral(true).queue();
                return;
            }
            try {
                if (Launcher.getJda().getChannelById(VoiceChannel.class, event.getValue("bot-vc-id").getAsString()) == null) {
                    event.reply("Invalid voice channel ID!").setEphemeral(true).queue();
                    return;
                }
            } catch (IllegalArgumentException e) {
                event.reply("Invalid voice channel ID!").setEphemeral(true).queue();
                return;
            }


            ChannelIDConfig config = new ChannelIDConfig(
                    event.getValue("bot-vc-id").getAsString(),
                    event.getValue("bot-tc-id").getAsString()
            );
            Launcher.getConfig().saveChannelIDsForGuild(event.getGuild().getId(), config);
            event.reply("Successfully saved the configuration!").setEphemeral(true).queue();
        }
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        if (event.getComponentId().startsWith("select-track-search-result")) {
            boolean skip = Boolean.parseBoolean(event.getComponentId().split(":")[1]);
            event.deferReply().setEphemeral(true).queue();
            var query = event.getSelectedOptions().get(0).getValue();
            if (Launcher.DEBUG) System.out.println("Query: (select) " + query);
            playTrack(query, skip, event);
            if (Launcher.DEBUG) System.out.println("Query (select) done");
        }
    }

    private void playTrack(String query, boolean now, GenericInteractionCreateEvent event) {
        var player = gpm.getPlayerForGuild(event.getGuild().getId());
        if (!query.startsWith("http")) {
            query = "ytsearch:" + query;
        }

        TextChannel tc;
        if (guildTCIDs.containsKey(event.getGuild().getId())) {
            tc = event.getGuild().getTextChannelById(guildTCIDs.get(event.getGuild().getId()));
        } else {
            tc = event.getGuild().getDefaultChannel().asTextChannel();
        }

        var vcs = event.getGuild().getVoiceChannels();
        if (vcs.isEmpty()) {
            event.getMessageChannel().sendMessage("There are no voice channels on this server!").queue();
            //event.getHook().sendMessage("There are no voice channels on this server!").queue();
            return;
        }

        VoiceChannel vc;
        if (guildVCIDs.containsKey(event.getGuild().getId())) {
            vc = event.getGuild().getVoiceChannelById(guildVCIDs.get(event.getGuild().getId()));
        } else {
            vc = vcs.get(0);
        }

        String finalQuery = query;
        Launcher.getApm().loadItem(query, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                event.getGuild().getAudioManager().openAudioConnection(vc);
                event.getGuild().getAudioManager().setSendingHandler(new AudioPlayerSendHandler(player));

                if (now) {
                    scheduler.insertTrack(track, event.getGuild().getId(), tc);
                    try (MessageCreateData message = new MessageCreateBuilder().setContent("Loaded Track: " + track.getInfo().title).build()) {
                        dispatchMessageAsEventReply(event, message, true);
                    }
                    tc.sendMessage("Starting Track: " + track.getInfo().title + " (" + track.getInfo().uri + ") [" + event.getUser().getEffectiveName() + "]")
                            .addActionRow(
                                    Button.primary("pause", "Pause"),
                                    Button.danger("stop", "Stop"),
                                    Button.primary("skip", "Skip")
                            )
                            .addActionRow(
                                    Button.primary("10sback", "<< 10s"),
                                    Button.success("showeq", "EQ"),
                                    Button.primary("10sforward", "10s >>")
                            )
                            .queue();
                } else {
                    MessageCreateData message = new MessageCreateBuilder().setContent("Added to queue: " + track.getInfo().title).build();
                    scheduler.addTrack(track, event.getGuild().getId());
                    dispatchMessageAsEventReply(event, message, true);
                    dispatchMessageInCorrectChannel(event, message, false);
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


                } else {
                    MessageCreateBuilder builder = new MessageCreateBuilder();
                    builder.setContent("Playlist " + playlist.getName() + " successfully loaded");
                    dispatchMessageAsEventReply(event, builder.build(), true);
                    tc.sendMessage("Playlist loaded: " + playlist.getName()).queue();

                    event.getGuild().getAudioManager().openAudioConnection(vc);
                    event.getGuild().getAudioManager().setSendingHandler(new AudioPlayerSendHandler(player));
                    playlist.getTracks().forEach(track -> {
                        scheduler.addTrack(track, event.getGuild().getId());
                    });
                }
            }

            @Override
            public void noMatches() {
                MessageCreateBuilder builder = new MessageCreateBuilder();
                builder.setContent("No matches found for: " + finalQuery);
                dispatchMessageInCorrectChannel(event, builder.build(), true);
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                EmbedBuilder eb = new EmbedBuilder();
                eb.addField("Test", "Test", false);
                eb.setColor(Color.red);
                if (event instanceof SlashCommandInteractionEvent) {
                    ((SlashCommandInteractionEvent) event).getHook().sendMessageEmbeds(eb.build()).setEphemeral(true).queue();
                } else if (event instanceof StringSelectInteractionEvent) {
                    ((StringSelectInteractionEvent) event).getHook().sendMessageEmbeds(eb.build()).setEphemeral(true).queue();
                } else {
                    event.getMessageChannel().sendMessageEmbeds(eb.build()).queue();
                }
            }
        });
    }

    private void dispatchMessageInCorrectChannel(GenericInteractionCreateEvent event, MessageCreateData message, boolean ephemeral) {
        TextChannel tc;
        if (guildTCIDs.containsKey(event.getGuild().getId())) {
            tc = event.getGuild().getTextChannelById(guildTCIDs.get(event.getGuild().getId()));
        } else {
            tc = event.getGuild().getDefaultChannel().asTextChannel();
        }
        tc.sendMessage(message).queue();
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

    private void checkChanelIDConfig(String guildID) {
        var tmp = Launcher.getConfig().getChannelIDsForGuild(guildID);
        if (tmp != null) {
            guildVCIDs.put(guildID, tmp.getVoiceChannelID());
            guildTCIDs.put(guildID, tmp.getTextChannelID());
        }
    }

    public BidiMap<String, String> getGuildTCIDs() {
        return guildTCIDs;
    }

    public BidiMap<String, String> getGuildVCIDs() {
        return guildVCIDs;
    }
}
