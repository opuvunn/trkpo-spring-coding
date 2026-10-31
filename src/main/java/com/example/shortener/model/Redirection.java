package com.example.shortener.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Date;

@Entity
@Table(name = "redirections")
public class Redirection {

    @Id
    @GeneratedValue
    private Long id;

    @Column(unique = true)
    private String shortKey;

    @Column
    private String secretKey;

    private String longUrl;

    private Date creationDate;

    private Date expirationDate;

    private long usageCount;

    public Redirection() {
    } // needed by hibernate

    public Redirection(String longUrl, String shortKey, String secretKey) {
        this(longUrl, shortKey, secretKey, null);
    }

    public Redirection(
            String longUrl,
            String shortKey,
            String secretKey,
            Date expirationDate) {

        this.longUrl = longUrl;
        this.shortKey = shortKey;
        this.secretKey = secretKey;
        this.creationDate = new Date();
        this.expirationDate = expirationDate;
    }

    public void setLongUrl(String longUrl) {
        this.longUrl = longUrl;
    }

    public void setShortKey(String shortKey) {
        this.shortKey = shortKey;
    }

    public String getLongUrl() {
        return longUrl;
    }

    public String getShortKey() {
        return shortKey;
    }

    public void incrementUsageCount() {
        usageCount++;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public long getUsageCount() {
        return usageCount;
    }

    public boolean isExpired() {
        return expirationDate != null
                && !new Date().before(expirationDate);
    }
}