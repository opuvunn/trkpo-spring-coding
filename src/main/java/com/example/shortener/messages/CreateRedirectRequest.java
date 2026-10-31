package com.example.shortener.messages;

import java.util.concurrent.TimeUnit;

public class CreateRedirectRequest {

    private String longUrl;
    private String vipKey;
    private Long timeToLive;
    private TimeUnit timeToLiveUnit;

    public CreateRedirectRequest() {
    }

    public CreateRedirectRequest(String longUrl) {
        this.longUrl = longUrl;
    }

    public String getLongUrl() {
        return longUrl;
    }

    public void setLongUrl(String longUrl) {
        this.longUrl = longUrl;
    }

    public String getVipKey() {
        return vipKey;
    }

    public void setVipKey(String vipKey) {
        this.vipKey = vipKey;
    }

    public Long getTimeToLive() {
        return timeToLive;
    }

    public void setTimeToLive(Long timeToLive) {
        this.timeToLive = timeToLive;
    }

    public TimeUnit getTimeToLiveUnit() {
        return timeToLiveUnit;
    }

    public void setTimeToLiveUnit(TimeUnit timeToLiveUnit) {
        this.timeToLiveUnit = timeToLiveUnit;
    }
}