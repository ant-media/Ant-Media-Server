package io.antmedia.component;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.CustomEditorConfigurer;
import org.springframework.context.annotation.Bean;

import org.springframework.context.annotation.*;

/**
 * This class is used to configure the applications. It's populated by the scan which is added by src/main/resources/WEB-INF/application.xml - subconfig file to provide better backward comppatibility
 * We use CustomEditorConfigurer to convert strings to JSONObjects in AppSettings
 */
@Configuration
public class AppConfig {
	
	
	public static final String INTERNAL_APP_CONFIG_LOCATION = "WEB-INF/application.xml";
	
	public static class JSONObjectEditor extends PropertyEditorSupport {
		
		Logger logger = LoggerFactory.getLogger(JSONObjectEditor.class);
		
	    @Override
	    public void setAsText(String text) {
	        JSONParser parser = new JSONParser();
	        try {
	            JSONObject jsonObject = (JSONObject) parser.parse(text);
	            setValue(jsonObject);
	        } catch (ParseException e) {
	            throw new IllegalArgumentException("Could not parse JSON string", e);
	        }
	    }
	}
	

	@Bean
    public static CustomEditorConfigurer customEditorConfigurer() {
        CustomEditorConfigurer configurer = new CustomEditorConfigurer();
        Map<Class<?>, Class<? extends PropertyEditor>> customEditors = new HashMap<>();
        customEditors.put(JSONObject.class, JSONObjectEditor.class);
        configurer.setCustomEditors(customEditors);
        return configurer;
    }

}
