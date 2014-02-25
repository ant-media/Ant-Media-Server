package org.red5.spring;

import static org.junit.Assert.assertEquals;

import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

@ContextConfiguration(locations = { "placeholder_context.xml" })
public class ExtendedPropertyPlaceholderConfigurerTest extends AbstractJUnit4SpringContextTests {
	
	protected Properties testProperties;

	protected Properties testAProperties;

	protected Properties testBProperties;

	static {
		String userDir = System.getProperty("user.dir");
		System.out.println("User dir: " + userDir);
		System.setProperty("red5.deployment.type", "junit");
		System.setProperty("red5.root", "file:" + userDir + "/target/classes");
		System.setProperty("red5.config_root", "file:" + userDir + "/src/main/server/conf");
	}	
	
	@Before
	public void setUp() throws Exception {
		testProperties = new Properties();
		testProperties.load(this.getClass().getResourceAsStream("/org/red5/spring/test.properties"));

		testAProperties = new Properties();
		testAProperties.load(this.getClass().getResourceAsStream("/org/red5/spring/test_a.properties"));

		testBProperties = new Properties();
		testBProperties.load(this.getClass().getResourceAsStream("/org/red5/spring/test_b.properties"));

	}

	@Test
	public void testLocationsProperty() {
		ExtendedPropertyPlaceholderConfigurer configurer = (ExtendedPropertyPlaceholderConfigurer) applicationContext.getBean("boringPlaceholderConfig");
		assertEquals(testProperties, configurer.getMergedProperties());
	}

/*
	@Test
	public void testWildcardLocationsProperty() {
		ExtendedPropertyPlaceholderConfigurer configurer = (ExtendedPropertyPlaceholderConfigurer) applicationContext.getBean("wildcard1PlaceholderConfig");

		Properties mergedProp = new Properties();
		mergedProp.putAll(testProperties);
		mergedProp.putAll(testAProperties);
		mergedProp.putAll(testBProperties);

		assertEquals(mergedProp, configurer.getMergedProperties());

		configurer = (ExtendedPropertyPlaceholderConfigurer) applicationContext.getBean("wildcard2PlaceholderConfig");

		mergedProp = new Properties();
		mergedProp.putAll(testAProperties);
		mergedProp.putAll(testBProperties);
		mergedProp.putAll(testProperties);

		assertEquals(mergedProp, configurer.getMergedProperties());
	}
*/
	
	@Test
	public void testLocationsPropertyOverridesWildcardLocationsProperty() {
		ExtendedPropertyPlaceholderConfigurer configurer = (ExtendedPropertyPlaceholderConfigurer) applicationContext.getBean("locationsOverridesWildcardLocationsPlaceholderConfig");

		Properties mergedProp = new Properties();
		mergedProp.putAll(testProperties);

		assertEquals(mergedProp, configurer.getMergedProperties());
	}

/*
	@Test
	public void testRuntimeProperties() {
		ExtendedPropertyPlaceholderConfigurer.addGlobalProperty("runtime_key1", "value1");
		ExtendedPropertyPlaceholderConfigurer.addGlobalProperty("runtime_key2", "value2");
		ExtendedPropertyPlaceholderConfigurer configurer = (ExtendedPropertyPlaceholderConfigurer) applicationContext.getBean("locationsOverridesWildcardLocationsPlaceholderConfig");

		Properties mergedProp = new Properties();
		mergedProp.putAll(testProperties);
		mergedProp.put("runtime_key1", "value1");
		mergedProp.put("runtime_key2", "value2");

		assertEquals(mergedProp, configurer.getMergedProperties());
	}
*/
	
/*
	@Test
	public void testRuntimePropertiesOverrideLocationsProperty() {
		ExtendedPropertyPlaceholderConfigurer.addGlobalProperty("override_key", "runtime config");
		ExtendedPropertyPlaceholderConfigurer configurer = (ExtendedPropertyPlaceholderConfigurer) applicationContext.getBean("wildcard2PlaceholderConfig");

		assertEquals("runtime config", configurer.getMergedProperties().getProperty("override_key"));
	}
*/
	
}
