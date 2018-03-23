package io.antmedia.datastore.preference;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.springframework.web.context.ServletContextAware;

public class PreferenceStore implements ServletContextAware{

	private String fileName;
	private Properties prop;
	private String fullPath;

	public PreferenceStore(String fileName) {
		this.fileName = fileName;
	}

	public void put(String key, String value) {
		Properties properties = getProperties();
		properties.put(key, value);
	}

	public String get(String key) {
		Properties properties = getProperties();
		return properties.getProperty(key);
	}
	
	public void remove(String key) {
		getProperties().remove(key);
	}

	private Properties getProperties() {
		if (prop == null) {
			prop = new Properties();
			FileInputStream input = null;
			try {
				input = new FileInputStream(fullPath);
				prop.load(input);
			} catch (Exception e) {
				//e.printStackTrace();
				//this exception may appear if file does not exist so not to log
			}
			finally {
				if (input != null) {
					try {
						input.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return prop;
	}

	public boolean save() {
		boolean result = false;
		if (prop != null) {
			FileOutputStream output = null;
			try {
				output = new FileOutputStream(fullPath);
				prop.store(output, null);
				result = true;
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (output != null) {
					try {
						output.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

			}
		}
		return result;
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		fullPath = servletContext.getRealPath(fileName);		
	}
	
	public void setFullPath(String fullpath) {
		this.fullPath = fullpath;
	}

}
