package net.bjmsw.model;

public class ChannelIDConfig {

    private final String textChannelID;
    private final String voiceChannelID;

    public ChannelIDConfig(String textChannelID, String voiceChannelID) {
        this.textChannelID = textChannelID;
        this.voiceChannelID = voiceChannelID;
    }

    public String getTextChannelID() {
        return textChannelID;
    }

    public String getVoiceChannelID() {
        return voiceChannelID;
    }
}
