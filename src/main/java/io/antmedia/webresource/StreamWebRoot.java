package io.antmedia.webresource;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.WebResource;
import org.apache.catalina.WebResourceSet;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import io.antmedia.AppSettings;

public class StreamWebRoot extends StandardRoot {

	private static final Logger logger = LoggerFactory.getLogger(StreamWebRoot.class);

	private static final String STREAMS_PATH = "/streams";
	private static final String DEFAULT_VOD_FOLDER = "streams";

	private final AtomicReference<AppSettings> appSettings = new AtomicReference<>();
	private final AtomicReference<WebResourceSet> vodResources = new AtomicReference<>();
	private volatile String configuredVodFolder = DEFAULT_VOD_FOLDER;
	private volatile String observedVodFolder = DEFAULT_VOD_FOLDER;

	boolean streamingResource = false;

	@Override
	public WebResource getResource(String path) {
		streamingResource = false;

		WebResource vodResource = getVodResource(path);
		if (vodResource != null && vodResource.exists()) {
			return vodResource;
		}

		if (path.endsWith(".m3u8") || path.endsWith(".ts") || path.endsWith(".mpd") || path.endsWith(".m4s") || (path.endsWith(".png") && path.contains("/previews/"))) {
			streamingResource = true;
			return getResourceInternal(path, true);
		}
		else {
			return getResourceDefault(path);
		}
	}

	private WebResource getVodResource(String path) {
		if (!isStreamsPath(path)) {
			return null;
		}

		AppSettings settings = getAppSettings();
		if (settings != null) {
			configureVodFolder(settings.getVodFolder());
		}

		WebResourceSet resources = vodResources.get();
		return resources != null ? resources.getResource(path) : null;
	}

	private boolean isStreamsPath(String path) {
		return STREAMS_PATH.equals(path) || path.startsWith(STREAMS_PATH + "/");
	}

	private AppSettings getAppSettings() {
		AppSettings settings = appSettings.get();
		if (settings == null && getContext() != null && getContext().getServletContext() != null) {
			Object contextAttribute = getContext().getServletContext()
					.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
			if (contextAttribute instanceof ApplicationContext applicationContext
					&& applicationContext.containsBean(AppSettings.BEAN_NAME)) {
				settings = applicationContext.getBean(AppSettings.BEAN_NAME, AppSettings.class);
				appSettings.set(settings);
			}
		}
		return settings;
	}

	void setAppSettings(AppSettings appSettings) {
		this.appSettings.set(appSettings);
	}

	public void configureVodFolder(String vodFolder) {
		if (Objects.equals(vodFolder, observedVodFolder)) {
			return;
		}
		synchronized (this) {
			if (Objects.equals(vodFolder, observedVodFolder)) {
				return;
			}
			configureVodFolderInternal(vodFolder);
			observedVodFolder = vodFolder;
		}
	}

	private void configureVodFolderInternal(String vodFolder) {
		String normalizedVodFolder = normalizeVodFolder(vodFolder);
		if (normalizedVodFolder.equals(configuredVodFolder)) {
			return;
		}

		WebResourceSet previousResources = vodResources.getAndSet(null);
		configuredVodFolder = normalizedVodFolder;

		if (!DEFAULT_VOD_FOLDER.equals(normalizedVodFolder)) {
			File folder = new File(normalizedVodFolder);
			if (folder.isAbsolute() && folder.isDirectory() && folder.canRead()) {
				DirResourceSet replacement = new DirResourceSet(this, STREAMS_PATH,
						folder.getAbsolutePath(), "/");
				replacement.setReadOnly(true);
				vodResources.set(replacement);
				logger.info("Mounting VoD folder {} under {} with priority over application resources",
						folder.getAbsolutePath(), STREAMS_PATH);
			}
			else {
				logger.warn("VoD folder {} is not an absolute, readable directory. Application resources will be used",
						normalizedVodFolder);
			}
		}

		stopResourceSet(previousResources);
	}

	private String normalizeVodFolder(String vodFolder) {
		if (vodFolder == null || vodFolder.isBlank() || DEFAULT_VOD_FOLDER.equals(vodFolder)
				|| STREAMS_PATH.equals(vodFolder)) {
			return DEFAULT_VOD_FOLDER;
		}
		return new File(vodFolder).toPath().normalize().toString();
	}

	private void stopResourceSet(WebResourceSet resources) {
		if (resources == null) {
			return;
		}
		try {
			if (resources.getState().isAvailable()) {
				resources.stop();
			}
			if (resources.getState() != LifecycleState.DESTROYED) {
				resources.destroy();
			}
		}
		catch (LifecycleException e) {
			logger.warn("Cannot stop the previous VoD resource set", e);
		}
	}

	public WebResource getResourceDefault(String path) {
		return super.getResource(path);
	}
		 
	public boolean isStreamingResource() {
		return streamingResource;
	}
}
