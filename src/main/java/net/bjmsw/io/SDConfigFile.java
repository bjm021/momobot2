package net.bjmsw.io;

import net.bjmsw.model.SDConfig;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class SDConfigFile {

    private static final File CONFIG_FILE = new File("sd_config.json");
    JSONObject config;

    public SDConfigFile() {
        System.out.println("Reading sd config file");
        if (!CONFIG_FILE.exists()) {
            try {
                createConfig();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            readConfig();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createConfig() throws IOException {
        System.out.println("Creating sd config file");
        config = new JSONObject();
        SDConfig template = new SDConfig("guild-id", "defaultNegativePrompt", 1, 1, "sampler");
        JSONObject guild = new JSONObject(template);
        config.put("guild-id", guild);
        FileUtils.writeStringToFile(CONFIG_FILE, config.toString(4), StandardCharsets.UTF_8);
    }

    private void readConfig() throws IOException {
        config = new JSONObject(FileUtils.readFileToString(CONFIG_FILE, StandardCharsets.UTF_8));
    }

    private void writeConfig() throws IOException {
        FileUtils.writeStringToFile(CONFIG_FILE, config.toString(4), StandardCharsets.UTF_8);
    }

    public SDConfig getConfig(String guildId) {
        try {
            readConfig();
        } catch (IOException ignored) {}
        if (!config.has(guildId)) return null;
        JSONObject guild = config.getJSONObject(guildId);


        return new SDConfig(guild.getString("guildId"), guild.getString("defaultNegativePrompt"), guild.getInt("samplerSteps"), guild.getInt("cfgScale"), guild.getString("sampler"));
    }

    public void addConfig(SDConfig sdConfig) throws IOException {
        JSONObject guild = new JSONObject(sdConfig);
        config.put(sdConfig.getGuildId(), guild);
        writeConfig();
    }

}
