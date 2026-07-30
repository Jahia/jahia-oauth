/*
 * Copyright (C) 2002-2021 Jahia Solutions Group SA. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jahia.modules.jahiaoauth.action;

import org.jahia.bin.filters.AbstractServletFilter;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * This servlet is only here to support FC test server, which only allows /callback url. This can only be used in a root context.
 */
@Component(immediate = true, service = AbstractServletFilter.class)
public class FranceConnectFilter extends AbstractServletFilter {

    private static final Pattern DISPATCHABLE_CALLBACK_PATH = Pattern
            .compile("/(?:sites|cms)(?:/[\\w-]+)+\\.franceConnectOAuthCallbackAction\\.do");

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Do nothing
    }

    @Activate
    public void activate() {
        setBeanName("fcfilter");
        setFilterName("fcfilter");
        setUrlPatterns(new String[] { "/callback" });
        setOrder(0);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        String url = request.getParameter("url");
        if (url != null && isGet(request) && DISPATCHABLE_CALLBACK_PATH.matcher(url).matches()) {
            request.getRequestDispatcher(url).forward(request, response);
        } else {
            chain.doFilter(request, response);
        }
    }

    private static boolean isGet(ServletRequest request) {
        return request instanceof HttpServletRequest && "GET".equals(((HttpServletRequest) request).getMethod());
    }

    @Override
    public void destroy() {
        // Do nothing
    }
}
