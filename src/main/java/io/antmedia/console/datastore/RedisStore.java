package io.antmedia.console.datastore;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedisStore extends MapBasedDataStore {

	RedissonClient redisson;

	protected static Logger logger = LoggerFactory.getLogger(RedisStore.class);

	/**
	 * It can be redis url starts with redis://.. or it can be config file address the in the disk
	 * @param redisConnectionUrl
	 */
	public RedisStore(String redisConnectionUrl) {
		try {
			File file = new File(redisConnectionUrl);

			Config config;
			if (file.exists()) {

				config = Config.fromYAML(file);

			}
			else {
				config  = new Config();
				config.useSingleServer()
					.setAddress(redisConnectionUrl);
			}

			redisson = Redisson.create(config);

			userMap = redisson.getMap("users");
		} catch (IOException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		} 

	}

	@Override
	public void clear() {
		synchronized (this) {
			userMap.clear();
		}
	}

	@Override
	public void close() {
		synchronized (this) {
			available = false;
			redisson.shutdown();
		}
	}

}
