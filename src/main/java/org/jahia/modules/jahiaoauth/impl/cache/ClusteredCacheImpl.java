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

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.HazelcastInstance;
import org.apache.commons.lang.StringUtils;
import org.jahia.modules.jahiaoauth.service.JahiaOAuthCacheService;
import org.jahia.modules.jahiaoauth.service.JahiaOAuthConstants;

import java.util.HashMap;

/**
 * @author dgaillard
 */
public class ClusteredCacheImpl implements JahiaOAuthCacheService {
    private HazelcastInstance hazelcastInstance;

    public ClusteredCacheImpl(Object hazelcast) {
        this.hazelcastInstance = (HazelcastInstance) hazelcast;
        Config config = hazelcastInstance.getConfig();
        MapConfig mapConfig = new MapConfig(JahiaOAuthConstants.JAHIA_OAUTH_USER_CACHE).setTimeToLiveSeconds(180);
        config.addMapConfig(mapConfig);
    }

    @Override
    public void cacheMapperResults(String cacheKey, HashMap<String, Object> mapperResult) {
        if (hazelcastInstance.getMap(JahiaOAuthConstants.JAHIA_OAUTH_USER_CACHE).containsKey(cacheKey)) {
            hazelcastInstance.getMap(JahiaOAuthConstants.JAHIA_OAUTH_USER_CACHE).replace(cacheKey, mapperResult);
        } else {
            hazelcastInstance.getMap(JahiaOAuthConstants.JAHIA_OAUTH_USER_CACHE).set(cacheKey, mapperResult);
        }
    }

    @Override
    public HashMap<String, Object> getMapperResultsCacheEntry(String cacheKey) {
        HashMap<String, Object> mapperResult = null;
        if (hazelcastInstance.getMap(JahiaOAuthConstants.JAHIA_OAUTH_USER_CACHE).containsKey(cacheKey)) {
            mapperResult = (HashMap<String, Object>) hazelcastInstance.getMap(JahiaOAuthConstants.JAHIA_OAUTH_USER_CACHE).get(cacheKey);
        }
        return mapperResult;
    }

    @Override
    public void updateCacheEntry(String originalSessionId, String newSessionId) {
        for (Object key : hazelcastInstance.getMap(JahiaOAuthConstants.JAHIA_OAUTH_USER_CACHE).keySet()) {
            String keyAsString = (String) key;
            if (StringUtils.endsWith(keyAsString, originalSessionId)) {
                String newKey = StringUtils.substringBefore(keyAsString, originalSessionId) + newSessionId;
                HashMap<String, Object> mapperResult = (HashMap<String, Object>) hazelcastInstance.getMap(JahiaOAuthConstants.JAHIA_OAUTH_USER_CACHE).get(key);
                hazelcastInstance.getMap(JahiaOAuthConstants.JAHIA_OAUTH_USER_CACHE).remove(key);
                hazelcastInstance.getMap(JahiaOAuthConstants.JAHIA_OAUTH_USER_CACHE).set(newKey, mapperResult);
            }
        }
    }
}
