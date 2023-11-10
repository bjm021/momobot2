package net.bjmsw.util;

import com.sun.tools.javac.Main;
import net.bjmsw.Launcher;
import net.bjmsw.io.Config;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

public class CatAsAService {

    public static void sendCat(SlashCommandInteractionEvent event) {

        // choose randomly between theCatApi and tenorCat and cataas
        String url = switch ((int) (Math.random() * 3)) {
            case 0 -> theCatApi();
            case 1 -> tenorCat();
            case 2 -> tenorCat(); // should be cataas() when it is working
            default -> "";
        };


        if (!url.isEmpty())
            event.getHook().sendMessage(url).queue();
    }

    private static String cataas(SlashCommandInteractionEvent e) {
        System.out.println("Sending cataas gif");
        CloseableHttpClient client = HttpClients.createDefault();
        String url = "https://cataas.com/cat/gif";
        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("User-Agent", "Mozilla/5.0");
        try {
            var resp = client.execute(httpGet);
            System.out.println(resp.getStatusLine().getStatusCode());
            InputStream is = resp.getEntity().getContent();
            FileUpload fileUpload = FileUpload.fromData(is, "cat.gif");
            e.getHook().sendFiles(fileUpload).queue();
            resp.close();
            client.close();
            System.out.println("Sent cat gif");
        } catch (IOException e1) {
            e1.printStackTrace();
            System.err.println("Error sending cat gif");
        }
        return "";
    }

    private static String theCatApi() {
        String key = "live_5tAxS6SZBQob4PRHxmDRCFnuQGwueL3N6v4YsKAS6xFTrEYZwXvmJsl22ltEaQUW";
        CloseableHttpClient client = HttpClients.createDefault();
        String url = "https://api.thecatapi.com/v1/images/search?api_key=" + key;
        try {
            String json = client.execute(new org.apache.http.client.methods.HttpGet(url), new org.apache.http.client.ResponseHandler<String>() {
                @Override
                public String handleResponse(org.apache.http.HttpResponse response) throws org.apache.http.client.ClientProtocolException, IOException {
                    System.out.println(response.getStatusLine().getStatusCode());
                    return org.apache.http.util.EntityUtils.toString(response.getEntity());
                }
            });
            System.out.println("parse json");
            JSONArray jsonArray = new JSONArray(json);
            JSONObject jsonObject = jsonArray.getJSONObject(0);
            String url2 = jsonObject.getString("url");
            if (Launcher.DEBUG) System.out.println("Replying: " + url2);
            return url2;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String tenorCat() {
        if (Launcher.DEBUG) System.out.println("Sending cat gif");
        String token = Launcher.getConfig().getTenorKey();
        String url = "https://tenor.googleapis.com/v2/search?q=cat&key=" + token + "&searchfilter=-static&limit=1&random=true";

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            String json = client.execute(new org.apache.http.client.methods.HttpGet(url), new org.apache.http.client.ResponseHandler<String>() {
                @Override
                public String handleResponse(org.apache.http.HttpResponse response) throws org.apache.http.client.ClientProtocolException, IOException {
                    System.out.println(response.getStatusLine().getStatusCode());
                    return org.apache.http.util.EntityUtils.toString(response.getEntity());
                }
            });
            System.out.println("parse json");
            JSONObject jsonObject = new JSONObject(json);
            System.out.println(json);
            JSONArray results = jsonObject.getJSONArray("results");
            JSONObject result = results.getJSONObject(0);
            JSONObject media = result.getJSONObject("media_formats");
            JSONObject gif = media.getJSONObject("gif");
            String gifUrl = gif.getString("url");
            if (Launcher.DEBUG) System.out.println("Replying: " + gifUrl);
            return gifUrl;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
