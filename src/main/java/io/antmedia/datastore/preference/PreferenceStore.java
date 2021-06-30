package io.antmedia.datastore.preference;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PreferenceStore{

	private Properties prop;
	private String path;

	protected static Logger logger = LoggerFactory.getLogger(PreferenceStore.class);

	public PreferenceStore(String path) {
		this.path = path;
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
				input = new FileInputStream(path);
				prop.load(input);
			} catch (Exception e) {
				//this exception may appear if file does not exist so not to log
			}
			finally {
				if (input != null) {
					try {
						input.close();
					} catch (IOException e) {
						logger.error(ExceptionUtils.getStackTrace(e));
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
				output = new FileOutputStream(path);
				prop.store(output, null);
				result = true;
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			} finally {
				if (output != null) {
					try {
						output.close();
					} catch (IOException e) {
						logger.error(ExceptionUtils.getStackTrace(e));
					}
				}

			}
		}
		return result;
	}

}
