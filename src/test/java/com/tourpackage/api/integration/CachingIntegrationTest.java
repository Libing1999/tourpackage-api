package com.tourpackage.api.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import com.tourpackage.api.config.CacheConfig;
import com.tourpackage.api.service.CmsService;
import com.tourpackage.api.service.SettingService;

/** That the caches are actually wired, and that they tolerate an absent row. */
@SpringBootTest
@ActiveProfiles("test")
class CachingIntegrationTest {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private CmsService cmsService;

    @Autowired
    private SettingService settingService;

    @Test
    @DisplayName("every declared cache exists")
    void declaredCachesExist() {
        for (String name : new String[] {
                CacheConfig.SITE_CONTENT, CacheConfig.PAGE_SEO, CacheConfig.SETTINGS,
                CacheConfig.DESTINATIONS, CacheConfig.POPULAR_SEARCHES, CacheConfig.FAQS }) {
            assertThat(cacheManager.getCache(name)).as("cache %s", name).isNotNull();
        }
    }

    @Test
    @DisplayName("an undeclared cache name is rejected rather than created on demand")
    void undeclaredCacheIsRejected() {
        // A typo in a @Cacheable name must not silently produce an unbounded
        // cache that nobody configured a TTL or a size for.
        assertThat(cacheManager.getCache("cacheNobodyDeclared")).isNull();
    }

    @Test
    @DisplayName("a settings read populates its cache")
    void settingsAreCached() {
        cacheManager.getCache(CacheConfig.SETTINGS).clear();

        settingService.getPublicSettings();

        assertThat(cacheManager.getCache(CacheConfig.SETTINGS).getNativeCache()).isNotNull();
        assertThat(settingService.getPublicSettings()).isNotEmpty();
    }

    @Test
    @DisplayName("SEO for an unmanaged route caches the absence instead of throwing")
    void nullSeoIsCacheable() {
        // getSeoForPath returns null for a route with no page_seo row. With
        // null values rejected this throws rather than caching, and the lookup
        // repeats on every render of every such page.
        assertThat(cmsService.getSeoForPath("/no-such-route-anywhere")).isNull();
        assertThat(cmsService.getSeoForPath("/no-such-route-anywhere")).isNull();

        assertThat(cacheManager.getCache(CacheConfig.PAGE_SEO).get("/no-such-route-anywhere"))
                .as("the absent row should be cached, not re-queried")
                .isNotNull();
    }

}
