package org.red5.server.stream.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;
import org.red5.spring.ExtendedPropertyPlaceholderConfigurer.ResourceFilenameComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

public class MiscellaniousTests  {

    private Logger log = LoggerFactory.getLogger(MiscellaniousTests.class);

    static {
        System.setProperty("red5.deployment.type", "junit");
    }

    @Test
    public void testResourceFilenameComparator() throws IOException {
    	List<Resource> resourceGroup = new ArrayList<Resource>();
    	resourceGroup.add(new FileSystemResource("test3"));
    	resourceGroup.add(new FileSystemResource("test2"));
    	resourceGroup.add(new FileSystemResource("test1"));
    	resourceGroup.add(new FileSystemResource("test4"));
    	resourceGroup.add(new FileSystemResource("atest4"));
    
    	resourceGroup.add(null);
    	
    	Resource resource = Mockito.mock(Resource.class);
    	Mockito.when(resource.getFilename()).thenReturn(null);
    	resourceGroup.add(resource);
    	resourceGroup.add(null);
    	
    	Collections.sort(resourceGroup, new ResourceFilenameComparator());
    	
    	
    	assertNull(resourceGroup.get(0));
    	
    }

}
