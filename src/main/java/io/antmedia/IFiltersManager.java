package io.antmedia;

import org.springframework.context.ApplicationContext;

public interface IFiltersManager {
	public static final String BEAN_NAME = "filters.manager";
	public void conference(String roomId, ApplicationContext appContext);
}