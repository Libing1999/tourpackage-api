package com.tourpackage.api.config;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Caching for the read paths every page hit touches.
 *
 * <p>Two managers, chosen by {@code app.cache.store}. Application code only ever
 * sees {@code @Cacheable}, so moving from one instance to a fleet with a shared
 * cache is a configuration change.
 *
 * <p>Short TTLs on purpose. These caches sit in front of content an admin edits
 * and then immediately reloads the site to check; a long TTL turns "my change
 * didn't save" into a support question. A minute absorbs the traffic spike of a
 * page being crawled or shared without making the CMS feel broken.
 */
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    /**
     * A cache outage degrades performance, not availability.
     *
     * <p>Spring's default handler rethrows, so with Redis selected an
     * unreachable server turns every cached read into a failed request — the
     * cache becomes a hard dependency of the thing it was added to speed up.
     * Logging and continuing means the call falls through to the database,
     * which is exactly what the cache was avoiding, and nothing more.
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException ex, Cache cache, Object key) {
                log.warn("Cache read failed ({}), falling through to source: {}", cache.getName(), ex.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException ex, Cache cache, Object key, Object value) {
                log.warn("Cache write failed ({}): {}", cache.getName(), ex.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException ex, Cache cache, Object key) {
                // Worth a louder note than the others: a failed eviction means
                // an admin's edit stays invisible until the entry expires.
                log.warn("Cache evict failed ({}) — stale entries may persist until TTL: {}",
                        cache.getName(), ex.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException ex, Cache cache) {
                log.warn("Cache clear failed ({}): {}", cache.getName(), ex.getMessage());
            }
        };
    }

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    /** Named so a call site can't invent a cache by typo and silently get an unbounded one. */
    public static final String SITE_CONTENT = "siteContent";
    public static final String PAGE_SEO = "pageSeo";
    public static final String SETTINGS = "settings";
    public static final String DESTINATIONS = "destinations";
    public static final String POPULAR_SEARCHES = "popularSearches";
    public static final String FAQS = "faqs";

    static final List<String> CACHE_NAMES =
            List.of(SITE_CONTENT, PAGE_SEO, SETTINGS, DESTINATIONS, POPULAR_SEARCHES, FAQS);

    @Bean
    @ConditionalOnProperty(name = "app.cache.store", havingValue = "memory", matchIfMissing = true)
    CacheManager caffeineCacheManager(@Value("${app.cache.ttl-seconds:60}") long ttlSeconds) {
        log.info("Cache: in-memory (per instance), TTL {}s", ttlSeconds);

        // Passing the names to the constructor turns off dynamic mode, so a
        // mistyped cache name fails instead of silently creating an unbounded
        // cache nobody configured.
        CaffeineCacheManager manager = new CaffeineCacheManager(CACHE_NAMES.toArray(String[]::new));
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(ttlSeconds))
                // Bounded: every one of these caches is keyed by something with a
                // small domain (a route path, a settings key), but a bound means
                // an unexpected key pattern can't grow into the heap.
                .maximumSize(1_000));
        // Nulls are cached deliberately. `getSeoForPath` returns null for any
        // route with no page_seo row, and that lookup runs on every render of
        // every such page — caching the absence is precisely what saves the
        // query. Rejecting nulls would also make the @Cacheable there throw.
        manager.setAllowNullValues(true);
        return manager;
    }

    @Bean
    @ConditionalOnProperty(name = "app.cache.store", havingValue = "redis")
    CacheManager redisCacheManager(
            RedisConnectionFactory connectionFactory,
            @Value("${app.cache.ttl-seconds:60}") long ttlSeconds) {
        log.info("Cache: redis (shared across instances), TTL {}s", ttlSeconds);

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(ttlSeconds))
                // JSON rather than JDK serialization: the cached values are DTOs
                // that change shape between releases, and a JDK-serialized cache
                // fails to deserialize across a deploy instead of simply missing.
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .initialCacheNames(Set.copyOf(CACHE_NAMES))
                .build();
    }

}
