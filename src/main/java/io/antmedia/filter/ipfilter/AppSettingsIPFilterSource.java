package io.antmedia.filter.ipfilter;

import io.antmedia.AppSettings;

public class AppSettingsIPFilterSource implements IPFilterSource {

    private AppSettings appSettings;

    public AppSettingsIPFilterSource(AppSettings appSettings) {
        this.appSettings = appSettings;
    }

    @Override
    public String getIPFilterRegex() {
        //appSettings.getIPFilterRegex();
        return "";
    }
}
