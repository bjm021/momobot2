package net.bjmsw.model;

public class SDConfig {

    private final String guildId;
    private final String defaultNegativePrompt;
    private final int samplerSteps;
    private final int cfgScale;
    private final String sampler;

    public SDConfig(String guildId, String defaultNegativePrompt, int samplerSteps, int cfgScale, String sampler) {
        this.guildId = guildId;
        this.defaultNegativePrompt = defaultNegativePrompt;
        this.samplerSteps = samplerSteps;
        this.cfgScale = cfgScale;
        this.sampler = sampler;
    }

    public int getCfgScale() {
        return cfgScale;
    }

    public int getSamplerSteps() {
        return samplerSteps;
    }

    public String getDefaultNegativePrompt() {
        return defaultNegativePrompt;
    }

    public String getGuildId() {
        return guildId;
    }

    public String getSampler() {
        return sampler;
    }
}

