package net.bjmsw.io;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Config {

    public static final File CONFIG_FILE = new File("config.json");

    private String token;

    public Config() {
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

    private void readConfig() throws IOException {
        JSONObject config = new JSONObject(FileUtils.readFileToString(CONFIG_FILE, StandardCharsets.UTF_8));
        token = config.getString("token");
    }

    private void writeConfig() throws IOException {
        JSONObject config = new JSONObject();
        config.put("token", token);
        FileUtils.writeStringToFile(CONFIG_FILE, config.toString(4), StandardCharsets.UTF_8);
    }

    private void createConfig() throws IOException {
        JSONObject config = new JSONObject();
        config.put("token", "PUT-YOUR-TOKEN-HERE");
        FileUtils.writeStringToFile(CONFIG_FILE, config.toString(4), StandardCharsets.UTF_8);
    }

    public String getToken() {
        return token;
    }
}
