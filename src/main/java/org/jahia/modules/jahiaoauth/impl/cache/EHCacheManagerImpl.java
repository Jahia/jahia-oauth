/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.jahiaoauth.impl.cache;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import org.apache.commons.lang.StringUtils;
import org.jahia.modules.jahiaoauth.service.JahiaOAuthCacheService;
import org.jahia.modules.jahiaoauth.service.JahiaOAuthConstants;
import org.jahia.services.cache.CacheHelper;
import org.jahia.services.cache.CacheProvider;
import org.jahia.services.cache.ModuleClassLoaderAwareCacheEntry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author dgaillard
 */
public class EHCacheManagerImpl implements JahiaOAuthCacheService {
    private CacheProvider ehCacheProvider;
    private CacheManager cacheManager;
    private Ehcache userCache;

    public void init() {
        cacheManager = ehCacheProvider.getCacheManager();
        userCache = cacheManager.getCache(JahiaOAuthConstants.JAHIA_OAUTH_USER_CACHE);

        if (userCache == null) {
            CacheConfiguration cacheConfiguration = new CacheConfiguration();
            cacheConfiguration.setName(JahiaOAuthConstants.JAHIA_OAUTH_USER_CACHE);
            cacheConfiguration.setTimeToLiveSeconds(180);
            // Create a new cache with the configuration
            Ehcache ehcache = new Cache(cacheConfiguration);
            ehcache.setName(JahiaOAuthConstants.JAHIA_OAUTH_USER_CACHE);
            // Cache name has been set now we can initialize it by putting it in the manager.
            // Only Cache manager is initializing caches.
            userCache = cacheManager.addCacheIfAbsent(ehcache);
        }
    }

    public void destroy() {
        // flush
        if (userCache != null) {
            userCache.removeAll();
        }

        cacheManager.removeCache(JahiaOAuthConstants.JAHIA_OAUTH_USER_CACHE);
    }

    @Override
    public void cacheMapperResults(String cacheKey, Map<String, Object> mapperResult) {
        ModuleClassLoaderAwareCacheEntry cacheEntry = new ModuleClassLoaderAwareCacheEntry(mapperResult, "jahia-oauth");
        userCache.put(new Element(cacheKey, cacheEntry));
    }

    @Override
    public Map<String, Object> getMapperResultsCacheEntry(String cacheKey) {
        return (Map<String, Object>) CacheHelper.getObjectValue(userCache, cacheKey);
    }

    @Override
    public Map<String, Map<String, Object>> getMapperResultsForSession(String sessionId) {
        Map<String, Map<String, Object>> res = new HashMap<>();
        for (String key : (List<String>) userCache.getKeys()) {
            if (StringUtils.endsWith(key, sessionId)) {
                String mapper = StringUtils.substringBefore(key, "_" + sessionId);
                Map<String, Object> mapperResults = getMapperResultsCacheEntry(key);
                if (mapperResults != null) {
                    res.put(mapper, mapperResults);
                }
            }
        }
        return res;
    }

    @Override
    public void updateCacheEntry(String originalSessionId, String newSessionId) {
        for (String key : (List<String>) userCache.getKeys()) {
            if (StringUtils.endsWith(key, originalSessionId)) {
                String newKey = StringUtils.substringBefore(key, originalSessionId) + newSessionId;
                Map<String, Object> mapperResults = getMapperResultsCacheEntry(key);
                if (mapperResults != null) {
                    cacheMapperResults(newKey, mapperResults);
                }
            }
        }
    }

    public void setEhCacheProvider(CacheProvider ehCacheProvider) {
        this.ehCacheProvider = ehCacheProvider;
    }
}
