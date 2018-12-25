package io.antmedia.statistic;

import static org.bytedeco.javacpp.nvml.NVML_SUCCESS;
import static org.bytedeco.javacpp.nvml.nvmlDeviceGetCount_v2;
import static org.bytedeco.javacpp.nvml.nvmlDeviceGetHandleByIndex_v2;
import static org.bytedeco.javacpp.nvml.nvmlDeviceGetUtilizationRates;
import static org.bytedeco.javacpp.nvml.nvmlInit_v2;
import static org.bytedeco.javacpp.nvml.nvmlShutdown;

import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.nvml;
import org.bytedeco.javacpp.nvml.nvmlDevice_st;
import org.bytedeco.javacpp.nvml.nvmlUtilization_t;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GPUUtils {

	private static Logger logger = LoggerFactory.getLogger(GPUUtils.class);

	private static GPUUtils instance;

	private static boolean noGPU = true;

	private Integer deviceCount = null;

	public static GPUUtils getInstance() {
		if(instance == null) {
			instance = new GPUUtils();

			logger.info("\n\n log1 \n");
			logger.info("\n\n log2:{} \n",nvml.class.getCanonicalName());
			logger.info("\n\n log3  \n");

			try {
				Class.forName(nvml.class.getCanonicalName());
				logger.info("nvml class found:"+nvml.class.getCanonicalName());
			}
			catch (ClassNotFoundException e) {
				logger.info("nvml class not found:"+nvml.class.getCanonicalName());
				return instance;
			}
			
			try {
				Loader.load(nvml.class);
				int result = nvmlInit_v2();
				if (result == NVML_SUCCESS) {
					logger.info("cuda cannot be initialized.");
					noGPU = false;
				}
			}
			catch (UnsatisfiedLinkError e) {
				logger.info("no cuda installed.");
			} 
		}
		return instance;
	}

	public int getDeviceCount() {
		if(noGPU) {
			return 0;
		}

		if(deviceCount == null) {
			IntPointer count = new IntPointer(1);
			int result = nvmlDeviceGetCount_v2(count);
			if (result == NVML_SUCCESS) {
				deviceCount = count.get();
			}
			else {
				deviceCount = 0;
			}
		}
		return deviceCount;
	}

	private nvmlUtilization_t getUtilization(int deviceNo) {
		nvmlDevice_st device = new nvmlDevice_st();
		int result = nvmlDeviceGetHandleByIndex_v2(deviceNo, device );
		if (result == NVML_SUCCESS) {
			nvmlUtilization_t deviceUtilization = new nvmlUtilization_t();
			result = nvmlDeviceGetUtilizationRates(device, deviceUtilization);
			if (result == NVML_SUCCESS) {
				return deviceUtilization;
			}
		}
		return null;
	} 

	public int getMemoryUtilization(int deviceNo) {
		nvmlUtilization_t utilization = getUtilization(deviceNo);
		if(utilization != null) {
			return utilization.memory();
		}
		return -1;
	}

	public int getGPUUtilization(int deviceNo) {
		nvmlUtilization_t utilization = getUtilization(deviceNo);
		if(utilization != null) {
			return utilization.gpu();
		}
		return -1;
	}

	public void close() {
		nvmlShutdown();
	}
}
