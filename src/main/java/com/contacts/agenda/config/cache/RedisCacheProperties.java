package com.contacts.agenda.config.cache;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "redis-cache")
public class RedisCacheProperties {
    private List<CacheProperty> caches = new ArrayList<>();

    @Data
    public static class CacheProperty {
        private String name;
        private String ttl;
        private boolean cacheNullValues = false;
    }
}
