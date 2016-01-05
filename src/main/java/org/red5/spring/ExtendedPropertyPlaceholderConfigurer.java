/*
 * RED5 Open Source Flash Server - https://github.com/Red5/
 * 
 * Copyright 2006-2016 by respective authors (see below). All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.red5.spring;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * An extension of {@link PropertyPlaceholderConfigurer}. Provides runtime additions of properties and wildcard location lookups.
 * 
 * Properties can be added at runtime by using the static {@link #addGlobalProperty} before* the bean definition is instantiated in the ApplicationContext. A property added by {@link #addGlobalProperty} will get merged into properties specified by the bean definition, overriding keys that overlap.
 * 
 * wildcard locations can be used instead of locations, if both are declared the last will override. Wildcard locations are handled by {@link #setWildcardLocations(String[])}, using {@link PathMatchingResourcePatternResolver} for matching locations. For wildcard locations that matches multiple Properties files, they are merged in by alphabetical filename order.
 * 
 * @author Michael Guymon (michael.guymon@gmail.com)
 * 
 */
public class ExtendedPropertyPlaceholderConfigurer extends PropertyPlaceholderConfigurer {

    private static Logger logger = LoggerFactory.getLogger(ExtendedPropertyPlaceholderConfigurer.class);

    private static Properties globalPlaceholderProperties = new Properties();

    private Properties mergedProperties;

    @Override
    protected void processProperties(ConfigurableListableBeanFactory beanFactoryToProcess, Properties props) throws BeansException {

        props.putAll(copyOfGlobalProperties());
        logger.debug("Placeholder props: {}", props.toString());

        this.mergedProperties = props;

        super.processProperties(beanFactoryToProcess, props);
    }

    /**
     * Merged {@link Properties} created by {@link #processProperties}
     * 
     * @return {@link Properties}
     */
    public Properties getMergedProperties() {
        return mergedProperties;
    }

    /**
     * String[] of wildcard locations of properties that are converted to Resource[] using using {@link PathMatchingResourcePatternResolver}
     * 
     * @param locations
     *            String[]
     * @throws IOException
     *             on IO exception
     */
    public void setWildcardLocations(String[] locations) throws IOException {

        List<Resource> resources = new ArrayList<Resource>();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(this.getClass().getClassLoader());

        for (String location : locations) {
            logger.debug("Loading location {}", location);
            try {
                // Get all Resources for a wildcard location
                Resource[] configs = resolver.getResources(location);
                if (configs != null && configs.length > 0) {
                    List<Resource> resourceGroup = new ArrayList<Resource>();
                    for (Resource resource : configs) {
                        logger.debug("Loading {} for location {}", resource.getFilename(), location);
                        resourceGroup.add(resource);
                    }
                    // Sort all Resources for a wildcard location by filename
                    Collections.sort(resourceGroup, new ResourceFilenameComparator());

                    // Add to master List
                    resources.addAll(resourceGroup);

                } else {
                    logger.info("Wildcard location does not exist: {}", location);
                }
            } catch (IOException ioException) {
                logger.error("Failed to resolve location: {} - {}", location, ioException);
            }
        }
        this.setLocations(resources.toArray(new Resource[resources.size()]));
    }

    /**
     * Add a global property to be merged
     * 
     * @param key
     *            String
     * @param val
     *            String
     */
    public static synchronized void addGlobalProperty(String key, String val) {
        globalPlaceholderProperties.setProperty(key, val);
    }

    /**
     * Copy of the manual properties
     * 
     * @return {@link Properties}
     */
    private static synchronized Properties copyOfGlobalProperties() {
        // return new Properties( runtimeProperties ); returns an empty prop ??

        Properties prop = new Properties();
        prop.putAll(globalPlaceholderProperties);

        return prop;
    }

    public static class ResourceFilenameComparator implements Comparator<Resource>, Serializable {

        private static final long serialVersionUID = -6365943736917478749L;

        public int compare(Resource resource1, Resource resource2) {
            if (resource1 != null) {
                if (resource2 != null) {
                    return resource1.getFilename().compareTo(resource2.getFilename());
                } else {
                    return 1;
                }
            } else if (resource2 == null) {
                return 0;
            } else {
                return -1;
            }
        }

    }

}
