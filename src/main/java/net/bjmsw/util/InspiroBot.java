package net.bjmsw.util;

import net.bjmsw.Launcher;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.io.IOExceptionList;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class InspiroBot {

    public static void inspiro(SlashCommandInteractionEvent event) {
        try {
            CloseableHttpClient client = HttpClientBuilder.create().build();
            HttpGet get = new HttpGet("https://inspirobot.me/api?generate=true");
            CloseableHttpResponse response = client.execute(get);
            String url = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            System.out.println(url);
            var baos = new ByteArrayOutputStream();
            var image = ImageIO.write(ImageIO.read(new URL(url)), "png", baos);
            FileUpload file = FileUpload.fromData(baos.toByteArray(), "inspiro.png");
            event.getHook().sendFiles(file).queue();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
