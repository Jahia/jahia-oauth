package org.jahia.modules.jahiaoauth.action;

import org.jahia.bin.filters.AbstractServletFilter;

import javax.servlet.*;
import java.io.IOException;

/**
 * This servlet is only here to support FC test server, which only allows /callback url. This can only be used in a root context.
 */
public class FranceConnectFilter extends AbstractServletFilter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Do nothing
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request.getParameter("url") != null && request.getParameter("url").endsWith(".franceConnectOAuthCallbackAction.do")) {
            request.getRequestDispatcher(request.getParameter("url")).forward(request, response);
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {
        // Do nothing
    }
}
