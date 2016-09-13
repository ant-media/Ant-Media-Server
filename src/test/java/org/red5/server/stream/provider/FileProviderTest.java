package org.red5.server.stream.provider;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.red5.server.messaging.IMessage;
import org.red5.server.messaging.IPipe;
import org.red5.server.messaging.InMemoryPullPullPipe;
import org.red5.server.scope.WebScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ContextConfiguration(locations = { "FileProviderTest.xml" })
public class FileProviderTest extends AbstractJUnit4SpringContextTests {

    private Logger log = LoggerFactory.getLogger(FileProviderTest.class);

    static {
        System.setProperty("red5.deployment.type", "junit");
    }

    @Test
    public void test() throws IOException {
        // get dummy scope
        WebScope scope = (WebScope) applicationContext.getBean("web.scope");
        // test file fixture
        File file = new File("target/test-classes/fixtures/test.flv");
        // new file provider instance
        FileProvider provider = new FileProvider(scope, file);
        // data pipe
        IPipe pipe = new InMemoryPullPullPipe();
        // subscribe the provider to the pipe
        pipe.subscribe(provider, null);
        // grab a message from the pipe (can do this until no messages are remaining)
        IMessage msg = pipe.pullMessage();
        log.info("Message: {}", msg);
        assertNotNull(msg);
    }

}
