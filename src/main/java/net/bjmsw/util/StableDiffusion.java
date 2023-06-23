package net.bjmsw.util;

import net.bjmsw.Launcher;
import net.bjmsw.model.SDConfig;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;

public class StableDiffusion {

    private final String url;
    private Map<String, String> lastPrompt;
    private Map<String, String> lastNegativPrompt;

    public StableDiffusion(String url) {
        if (url.endsWith("/")) {
            this.url = url.substring(0, url.length() - 1);
        } else {
            this.url = url;
        }
        lastPrompt = new java.util.HashMap<>();
        lastNegativPrompt = new java.util.HashMap<>();
    }

    public void txt2img(String propmt, String negativPrompt, int batchSize, int steps, ModalInteractionEvent event) {
        try {

            if (propmt.isEmpty()) {
                if (!lastPrompt.containsKey(event.getUser().getId())) {
                    event.reply("You need to provide a prompt (you have not used this once in this session)").setEphemeral(true).queue();
                    return;
                }
                propmt = lastPrompt.get(event.getUser().getId());
            }

            if (negativPrompt.isEmpty()) {
                if (!lastNegativPrompt.containsKey(event.getUser().getId())) {
                    negativPrompt = Launcher.getSdConfigFile().getConfig(event.getGuild().getId()).getDefaultNegativePrompt();
                } else {
                    negativPrompt = lastNegativPrompt.get(event.getUser().getId());
                }
            }


            //event.deferReply().queue();

            if (!checkServerOnline(event)) return;

            SDConfig sdConfig = Launcher.getSdConfigFile().getConfig(event.getGuild().getId());
            var sampler = "DDIM";
            var cfg_scale = "7";
            if (sdConfig != null) {
                sampler = sdConfig.getSampler();
                cfg_scale = String.valueOf(sdConfig.getCfgScale());
            }

            CloseableHttpClient client = HttpClients.createDefault();
            JSONObject body = new JSONObject();
            body.put("prompt", propmt);
            body.put("negative_prompt", negativPrompt);
            body.put("seed", "-1");
            body.put("subseed", "-1");
            body.put("batch_size", batchSize);
            body.put("steps", steps);
            body.put("cfg_scale", cfg_scale);
            body.put("width", "664");
            body.put("height", "800");
            body.put("sampler_index", sampler);
            body.put("restore_faces", "true");

            lastPrompt.put(event.getUser().getId(), propmt);
            lastNegativPrompt.put(event.getUser().getId(), negativPrompt);

            var endpoint = url + "/sdapi/v1/txt2img";
            System.out.println("SD ENDPOINT: " + endpoint);
            HttpPost post = new HttpPost(endpoint);
            post.setHeader("Content-Type", "application/json");
            post.setHeader("Accept", "application/json");

            System.out.println("SD-DEBUG: " + body.toString());
            post.setEntity(new org.apache.http.entity.StringEntity(body.toString()));

            new Thread(() -> {
                try {
                    var response = client.execute(post);
                    JSONObject responseJSON = new JSONObject(IOUtils.toString(response.getEntity().getContent()));
                    //System.out.println(responseJSON.toString(4));

                    var images = responseJSON.getJSONArray("images");
                    client.close();
                    images.forEach(o -> {
                        try {
                            var base64 = (String) o;
                            byte[] imagebytes = java.util.Base64.getDecoder().decode(base64);
                            BufferedImage image = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(imagebytes));
                            FileUpload file = FileUpload.fromData(imagebytes, "image.png");
                            event.getHook().sendFiles(file).queue();
                        } catch (IOException e) {
                            event.getHook().sendMessage("Error while decoding image").queue();
                            e.printStackTrace();
                        }
                    });
                } catch (IOException e) {
                    event.getHook().sendMessage("Error while sending request").queue();
                    e.printStackTrace();
                }
            }).start();


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean checkServerOnline(ModalInteractionEvent event) {
        try {
            CloseableHttpClient client = HttpClients.createDefault();
            HttpGet get = new HttpGet(url);
            var response = client.execute(get);
            if (response.getStatusLine().getStatusCode() != 200) {
                event.reply("The BJM Stable-Diffusion provider server is not online.").setEphemeral(true).queue();
                return false;
            }
            event.deferReply().queue();
            return true;
        } catch (IOException e) {
            event.reply("The BJM Stable-Diffusion provider server is not online.").setEphemeral(true).queue();
            return false;
        }
    }

}
