package io.antmedia.filter.ipfilter;

import io.antmedia.AppSettings;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;


public class IPFilter implements Filter {

    private FilterConfig config;
    private IPFilterSource ipFilterSource;

    public IPFilter(IPFilterSource ipFilterSource) {
        this.ipFilterSource = ipFilterSource;
    }

    public IPFilter() {
    }

    // the regex must define whole string to match - for example a substring without .* will not match
    // note the double backslashes that need to be present in Java code but not in web.xml
    private String ip_regex = "172\\.20\\.\\d+\\.\\d+.*";

    public void init(FilterConfig filterConfig) throws ServletException {
        this.config = filterConfig;
        if(this.ipFilterSource != null) {
            this.ipFilterSource = new AppSettingsIPFilterSource(getAppSettings());
        }
    }
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        String ip = request.getRemoteAddr();
        HttpServletResponse httpResp = null;
        if (response instanceof HttpServletResponse) httpResp = (HttpServletResponse) response;
        if (ip.matches(ipFilterSource.getIPFilterRegex())) {

            httpResp.sendError(HttpServletResponse.SC_FORBIDDEN,"Your own message 403 Forbidden");
        } else {
            chain.doFilter(request, response);
        }
    }

    public AppSettings getAppSettings() {
        ApplicationContext context = (ApplicationContext) config.getServletContext().getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
        return (AppSettings) context.getBean(AppSettings.BEAN_NAME);
    }
    public void destroy() {}
}
