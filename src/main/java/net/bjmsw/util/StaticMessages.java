package net.bjmsw.util;

import net.bjmsw.Launcher;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;

public class StaticMessages {

    public static void help(SlashCommandInteractionEvent event) {
        var prefix = "/";
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("momobot2 HELP / NOW USING THE NEW BOT API");
        eb.setColor(Color.BLUE);
        eb.setDescription("All Bot Commands, args in <arg> required | args in [arg] optional");
        eb.addField("", "Generic COMMANDS", false);
        eb.addField(prefix + "play <query>", "Plays a track instantly", true);
        eb.addField(prefix + "add <query>", "Adds a track to Queue", true);
        eb.addField(prefix + "pause", "Pauses the player", true);
        eb.addField(prefix + "resume", "Resumes after pause", true);
        eb.addField(prefix + "volume <value>", "Sets the audio volume", true);
        eb.addField(prefix + "clear", "Clears the queue", true);
        eb.addField("", "Complete command refrence:", false);
        eb.addField("https://bjm021.github.io/momobot/", "You can visit https://bjm021.github.io/momobot/ for a complete command refrence!", false);
        eb.setFooter("momobot2 " + Launcher.VERSION + " | by b.jm021", "http://cdn.bjm.hesteig.com/BJM_Logo_white.png");
        eb.setThumbnail("http://cdn.bjm.hesteig.com/BJM_Logo_white.png");
        event.replyEmbeds(eb.build()).queue();
    }

}
