package org.red5.server.stream.provider;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.red5.server.messaging.IMessage;
import org.red5.server.messaging.IPipe;
import org.red5.server.messaging.InMemoryPullPullPipe;
import org.red5.server.scope.WebScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@TestMethodOrder(MethodName.class)
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "FileProviderTest.xml" })
public class FileProviderTest {

    @Autowired
    private ApplicationContext applicationContext;

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
