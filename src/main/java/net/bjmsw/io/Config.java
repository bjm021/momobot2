package net.bjmsw.io;

import com.cedarsoftware.util.io.JsonWriter;
import net.bjmsw.model.ChannelIDConfig;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class Config {

    public static final File CONFIG_FILE = new File("config.json");

    JSONObject config;

    public Config() {
        if (!CONFIG_FILE.exists()) {
            try {
                createConfig();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        readConfig();
    }

    private void readConfig() {
        try {
            config = new JSONObject(FileUtils.readFileToString(CONFIG_FILE, StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeConfig() throws IOException {
        FileUtils.writeStringToFile(CONFIG_FILE, JsonWriter.formatJson(config.toString()), StandardCharsets.UTF_8);
    }

    private void createConfig() throws IOException {
        JSONObject config = new JSONObject();
        config.put("token", "PUT-YOUR-TOKEN-HERE");
        config.put("sd_url", "http://localhost:7860/");
        config.put("tenor_api_key", "API-KEY-HERE");
        JSONObject channelIDS = new JSONObject();
        JSONObject tmpChanelGuildConfig = new JSONObject();
        tmpChanelGuildConfig.put("vc", "PUT-VOICE-CHANNEL-ID-HERE");
        tmpChanelGuildConfig.put("general-messages-tc", "PUT-TEXT-CHANNEL-ID-HERE");
        channelIDS.put("GUILD-ID-HERE", tmpChanelGuildConfig);
        config.put("channel-ids", channelIDS);
        JSONArray admins = new JSONArray();
        admins.put("ADMIN-ID-HERE");
        config.put("admins", admins);
        FileUtils.writeStringToFile(CONFIG_FILE, config.toString(4), StandardCharsets.UTF_8);
    }

    public String getToken() {
        readConfig();
        return config.getString("token");
    }
    public String getTenorKey() {
        readConfig();
        return config.getString("tenor_api_key");
    }

    public String getSD_url() {
        readConfig();
        return config.getString("sd_url");
    }

    public ChannelIDConfig getChannelIDsForGuild(String guildID) {
        readConfig();
        JSONObject channelIDS = config.getJSONObject("channel-ids");
        if (channelIDS.has(guildID)) {
            JSONObject channelIDConfig = channelIDS.getJSONObject(guildID);
            return new ChannelIDConfig(channelIDConfig.getString("general-messages-tc"), channelIDConfig.getString("vc"));
        } else return null;
    }

    public void saveChannelIDsForGuild(String guildID, ChannelIDConfig channelIDConfig) {
        try {
            readConfig();
            JSONObject channelIDS = config.getJSONObject("channel-ids");
            JSONObject tmpChanelGuildConfig = new JSONObject();
            tmpChanelGuildConfig.put("vc", channelIDConfig.getVoiceChannelID());
            tmpChanelGuildConfig.put("general-messages-tc", channelIDConfig.getTextChannelID());
            channelIDS.put(guildID, tmpChanelGuildConfig);
            config.put("channel-ids", channelIDS);
            writeConfig();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<String> getAdmins() {
        readConfig();
        return config.getJSONArray("admins").toList().stream().map(Object::toString).toList();
    }

    public void addAdmin(String adminID) {
        try {
            readConfig();
            JSONArray admins = config.getJSONArray("admins");
            admins.put(adminID);
            config.put("admins", admins);
            writeConfig();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
