package io.antmedia.test.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.nio.file.Paths;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.rest.model.SslConfigurationType;
import io.antmedia.security.SslConfigurator;

public class SslConfiguratorTest {
    protected static Logger logger = LoggerFactory.getLogger(SslConfiguratorTest.class);


    @Before
    public void before() {
    }

    @After
    public void after() {

    }

    @Rule
    public TestRule watcher = new TestWatcher() {
        protected void starting(Description description) {
            System.out.println("Starting test: " + description.getMethodName());
        }

        protected void failed(Throwable e, Description description) {
            System.out.println("Failed test: " + description.getMethodName());
        }

        ;

        protected void finished(Description description) {
            System.out.println("Finishing test: " + description.getMethodName());
        }

        ;
    };

    @Test
    public void testSslConfigurator() {
    	
    	 SslConfigurator sslConfigurator = new SslConfigurator();
    	 sslConfigurator.setType(SslConfigurationType.CUSTOM_DOMAIN);
    	 sslConfigurator.setDomain("test.antmedia.io");
    	 
    	 String command = sslConfigurator.getCommand();
    	 
    	 String installDirectory = Paths.get("").toAbsolutePath().toString();
    	
    	 
    	 sslConfigurator.setType(SslConfigurationType.ANTMEDIA_SUBDOMAIN);
    	 
    	 command = sslConfigurator.getCommand();
    	 assertEquals("sudo /bin/bash enable_ssl.sh -i " + installDirectory, command);
    	 
    	 sslConfigurator.setType(SslConfigurationType.CUSTOM_CERTIFICATE);
    	 sslConfigurator.setFullChainFile(new File("fullchain.pem"));
    	 sslConfigurator.setChainFile(new File("chain.pem"));
    	 sslConfigurator.setPrivateKeyFile(new File("privatekey.pem"));
    	 command = sslConfigurator.getCommand();
    	 
    	 assertEquals("sudo /bin/bash enable_ssl.sh -f "+ installDirectory +"/fullchain.pem -p "+ installDirectory + "/privatekey.pem -c " + installDirectory + "/chain.pem -d test.antmedia.io -i " +installDirectory, command); 


    	 sslConfigurator.setType(SslConfigurationType.NO_SSL);
    	 assertNull(sslConfigurator.getCommand());
    }

  


}
