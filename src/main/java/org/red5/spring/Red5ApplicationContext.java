package org.red5.spring;

import org.red5.logging.Red5LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.FileSystemXmlApplicationContext;

public class Red5ApplicationContext extends FileSystemXmlApplicationContext implements ApplicationContextAware {

    private static final Logger log = Red5LoggerFactory.getLogger(Red5ApplicationContext.class);

    // parent context
    private ApplicationContext parentContext;

    // creating context
    private ApplicationContext applicationContext;

    // to refresh or not
    private boolean refresh = true;

    @Override
    public void afterPropertiesSet() {
        log.info("Red5ApplicationContext init");
        if (parentContext == null) {
            log.debug("Setting application context as parent");
            super.setParent(applicationContext);
        }
        super.afterPropertiesSet();
    }

    @Override
    public void refresh() throws BeansException, IllegalStateException {
        if (refresh) {
            super.refresh();
        }
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        log.info("setApplicationContext: {}", applicationContext);
        this.applicationContext = applicationContext;
    }

    @Override
    public void setParent(ApplicationContext parent) {
        super.setParent(parent);
        parentContext = parent;
    }

    public void setRefresh(boolean refresh) {
        this.refresh = refresh;
    }

}
