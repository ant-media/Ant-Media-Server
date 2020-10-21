package io.antmedia.statistic;

import static org.bytedeco.cuda.global.nvml.NVML_SUCCESS;
import static org.bytedeco.cuda.global.nvml.nvmlDeviceGetCount_v2;
import static org.bytedeco.cuda.global.nvml.nvmlDeviceGetHandleByIndex_v2;
import static org.bytedeco.cuda.global.nvml.nvmlDeviceGetMemoryInfo;
import static org.bytedeco.cuda.global.nvml.nvmlDeviceGetName;
import static org.bytedeco.cuda.global.nvml.nvmlDeviceGetUtilizationRates;
import static org.bytedeco.cuda.global.nvml.nvmlInit_v2;

import org.bytedeco.cuda.global.nvml;
import org.bytedeco.cuda.nvml.nvmlDevice_st;
import org.bytedeco.cuda.nvml.nvmlMemory_t;
import org.bytedeco.cuda.nvml.nvmlUtilization_t;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.Loader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GPUUtils {

	private static Logger logger = LoggerFactory.getLogger(GPUUtils.class);

	private static GPUUtils instance;

	private static boolean noGPU = true;

	private Integer deviceCount = null;

	public static class MemoryStatus {
		private long memoryTotal;
		private long memoryUsed;
		private long memoryFree;

		public MemoryStatus(long memoryTotal, long memoryUsed, long memoryFree) {
			this.memoryTotal = memoryTotal;
			this.memoryUsed = memoryUsed;
			this.memoryFree = memoryFree;
		}

		public long getMemoryTotal() {
			return memoryTotal;
		}

		public long getMemoryUsed() {
			return memoryUsed;
		}

		public long getMemoryFree() {
			return memoryFree;
		}
	}

	private GPUUtils() {}

	public static GPUUtils getInstance() {
		if(instance == null) {
			instance = new GPUUtils();

			try {
				Class.forName("org.bytedeco.cuda.global.nvml");

				Loader.load(nvml.class);
				int result = nvmlInit_v2();
				if (result == NVML_SUCCESS) {
					logger.info("cuda initialized {}", "");
					noGPU = false;
				}
				else {
					logger.warn("Nvml cannot be initialized {}", GPUUtils.class.getSimpleName());
				}
			}
			catch (UnsatisfiedLinkError e) {
				logger.warn("UnsatisfiedLinkError no cuda installed {}", e.getMessage());
				
			} 
			catch (ClassNotFoundException e) {
				logger.warn("ClassNotFoundException nvml class not found {}", e.getMessage());
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

	public nvmlDevice_st getDevice(int deviceIndex) 
	{
		if (!noGPU) {
			nvmlDevice_st device = new nvmlDevice_st();
			if (nvmlDeviceGetHandleByIndex_v2(deviceIndex, device) == NVML_SUCCESS) {
				return device;
			}
		}
		return null;
	}

	private nvmlUtilization_t getUtilization(int deviceNo) {
		nvmlDevice_st device = null;
		if ((device = getDevice(deviceNo)) != null) {
			nvmlUtilization_t deviceUtilization = new nvmlUtilization_t();
			if (nvmlDeviceGetUtilizationRates(device, deviceUtilization) == NVML_SUCCESS) {
				return deviceUtilization;
			}
		}
		return null;
	} 


	public MemoryStatus getMemoryStatus(int deviceNo) {
		nvmlDevice_st device = null;
		if ((device = getDevice(deviceNo)) != null) {
			nvmlMemory_t nvmlMemory = new nvmlMemory_t();
			if (nvmlDeviceGetMemoryInfo(device, nvmlMemory) == NVML_SUCCESS) {
				return new MemoryStatus(nvmlMemory.total(), nvmlMemory.used(), nvmlMemory._free());
			}
		}
		return null;
	}
	
	public String getDeviceName(int deviceIndex) {
		nvmlDevice_st device = null;
		if ((device = getDevice(deviceIndex)) != null) {
			byte[] nameByte = new byte[64];
			if (nvmlDeviceGetName(device, nameByte, nameByte.length) == NVML_SUCCESS) {
				String name = new String(nameByte, 0, nameByte.length);
				int indexOf = name.indexOf("\u0000");	
				return name.substring(0, indexOf > 0 ? indexOf : name.length());
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
}
