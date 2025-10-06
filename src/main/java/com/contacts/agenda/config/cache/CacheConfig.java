package com.contacts.agenda.config.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Redis cache configuration with type-safe JSON serialization.
 * <p>
 * Configures Redis as a caching layer for external API responses with Jackson-based serialization
 * that preserves type information. Without type metadata, Jackson would deserialize cached objects
 * as {@code LinkedHashMap}, losing type safety.
 * <p>
 * <b>Key Features:</b>
 * <ul>
 *   <li>Type-safe serialization with {@code @class} metadata</li>
 *   <li>Support for Java 8+ time types (e.g., {@code Instant})</li>
 *   <li>Configurable TTL per cache name via {@code application.yml}</li>
 *   <li>Default TTL of 5 minutes for unconfigured caches</li>
 * </ul>
 * <p>
 * <b>Example Cached Object Structure:</b>
 * <p>
 * Each cached entry includes type metadata to ensure proper deserialization:
 * <blockquote><pre>{@code
 * {
 *   "@class": "com.contacts.agenda.domain.Contact",
 *   "id": 123,
 *   "name": "John Doe",
 *   "email": "john.doe@example.com",
 *   "source": "API",
 *   "createdAt": "2024-10-05T12:34:56.789Z",
 *   "updatedAt": "2024-10-05T12:34:56.789Z"
 * }
 * }</pre></blockquote>
 * <p>
 * <b>Configuration Example:</b>
 * <p>
 * Define cache-specific settings in {@code application.yml}:
 * <blockquote><pre>{@code
 * redis-cache:
 *   caches:
 *     - name: contactPages
 *       ttl: 5m
 *       cache-null-values: false
 * }</pre></blockquote>
 *
 * @see RedisCacheProperties
 * @see org.springframework.cache.annotation.Cacheable
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory,
                                     RedisCacheProperties properties) {
        Map<String, RedisCacheConfiguration> cacheConfigs = properties.getCaches().stream()
                .collect(Collectors.toMap(
                        RedisCacheProperties.CacheProperty::getName,
                        cache -> buildCacheConfig(
                                parseDuration(cache.getTtl()),
                                cache.isCacheNullValues()
                        )
                ));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(buildCacheConfig(Duration.ofMinutes(15), false))
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }

    private RedisCacheConfiguration buildCacheConfig(Duration ttl, boolean cacheNulls) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(Object.class)
                .build();

        objectMapper.activateDefaultTyping(
                typeValidator,
                ObjectMapper.DefaultTyping.NON_FINAL
        );

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper)));

        return cacheNulls ? config : config.disableCachingNullValues();
    }

    private Duration parseDuration(String duration) {
        if (duration == null) return Duration.ofMinutes(5);

        long value = Long.parseLong(duration.replaceAll("[^0-9]", ""));
        char unit = duration.charAt(duration.length() - 1);

        return switch (unit) {
            case 's' -> Duration.ofSeconds(value);
            case 'm' -> Duration.ofMinutes(value);
            case 'h' -> Duration.ofHours(value);
            case 'd' -> Duration.ofDays(value);
            default -> Duration.ofMinutes(15);
        };
    }
}
