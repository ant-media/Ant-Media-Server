package io.antmedia;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.management.MBeanServer;

import org.apache.commons.lang3.StringUtils;
import org.bytedeco.javacpp.Pointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.management.HotSpotDiagnosticMXBean;
import com.sun.management.UnixOperatingSystemMXBean;
/**
 * This utility is designed for accessing server's
 * system information more easier.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author DZS|All-In-One (coolest2guy@gmail.com)
 * 
 */
/*
 * 
 * 	Server-side Status for Red5
 * 	-------------------------------
 * 	System.getProperty("_____")
 * 	===============================
 * 	os.name						:Operating System Name
 * 	os.arch						: x86/x64/...
 * 	java.specification.version	: Java Version (Required 1.5 or 1.6 and higher to run Red5)
 * 	-------------------------------
 * 	Runtime.getRuntime()._____  (Java Virtual Machine Memory)
 * 	===============================
 * 	maxMemory()					: Maximum limitation
 * 	totalMemory()				: Total can be used
 * 	freeMemory()				: Availability
 * 	totalMemory()-freeMemory()	: In Use
 * 	availableProcessors()		: Total Processors available
 * 	-------------------------------
 *  getOperatingSystemMXBean()	(Actual Operating System RAM)
 *	===============================
 *  osCommittedVirtualMemory()	: Virtual Memory
 *  osTotalPhysicalMemory()		: Total Physical Memory
 *  osFreePhysicalMemory()		: Available Physical Memory
 *  osInUsePhysicalMemory()		: In Use Physical Memory
 *  osTotalSwapSpace()			: Total Swap Space
 *  osFreeSwapSpace()			: Available Swap Space
 *  osInUseSwapSpace()			: In Use Swap Space
 *  -------------------------------
 *  File						(Actual Harddrive Info: Supported for JRE 1.6)
 *	===============================
 *	osHDUsableSpace()			: Usable Space
 *	osHDTotalSpace()			: Total Space
 *	osHDFreeSpace()				: Available Space
 *	osHDInUseSpace()			: In Use Space
 *  -------------------------------
 *  
 */
public class SystemUtils {

	public static final String HEAPDUMP_HPROF = "heapdump.hprof";
	public static final long MAX_CONTAINER_MEMORY_LIMIT_BYTES = 109951162777600L; //100TB
	public static final String MAX_MEMORY_CGROUP_V2 = "max";

	/**
	 * Obtain Operating System's name.
	 * 
	 * @return OS's name
	 */
	public static final String osName = System.getProperty("os.name");

	/**
	 * Obtain Operating System's Architecture.
	 * 
	 * @return x86 (32-bit), x64 (64-bit), ...
	 */
	public static final String osArch = System.getProperty("os.arch");

	/**
	 * Obtain Java JRE version.
	 * 
	 * @return version
	 */
	public static final String jvmVersion = System.getProperty("java.specification.version");

	/**
	 * Obtain Operating System's processor.
	 * 
	 * @return Number of Processor(s)
	 */
	public static final int osProcessorX = Runtime.getRuntime().availableProcessors();


	private static final String HOTSPOT_BEAN_NAME =
			"com.sun.management:type=HotSpotDiagnostic";

	private static HotSpotDiagnosticMXBean hotspotMBean;

	protected static final Logger logger = LoggerFactory.getLogger(SystemUtils.class);
	
	public static final int MAC_OS_X = 0;
	public static final int LINUX = 1;
	public static final int WINDOWS = 2;
	
	public static final int OS_TYPE;

	public static Boolean containerized = null;
	
	static {
		String osName = SystemUtils.osName.toLowerCase();
		if (osName.startsWith("mac os x") || osName.startsWith("darwin")) {
			OS_TYPE = MAC_OS_X;
		} else if (osName.startsWith("windows")) {
			OS_TYPE = WINDOWS;
		} else if (osName.startsWith("linux")) {
			OS_TYPE = LINUX;
		}
		else {
			OS_TYPE = -1;
		}
	}


	/**
	 * These functions below are used for Java Virtual Machine (JVM)
	 * RAM usage base on Runtime.getRuntime().______.
	 * -------------------------------
	 * jvmMaxMemory()
	 * jvmTotalMemory()
	 * jvmFreeMemory()
	 * jvmInUseMemory()
	 * -------------------------------
	 * 
	 */
	/**
	 * Obtain JVM's Maximum Memory.
	 * 
	 * @param size null, AUTO, B, KB, MB, GB, TB, or PB
	 * (PetaByte does not exist yet)
	 * Is not case sensitive.
	 * @param txtByte true if include byte extension, false exclude extension
	 * @return bytes size
	 * 
	 */
	public static long jvmMaxMemory() {
		return Runtime.getRuntime().maxMemory();
	}

	/**
	 * Obtain JVM's Total Memory.
	 *
	 * @return bytes size
	 * 
	 */
	public static long jvmTotalMemory() {
		return Runtime.getRuntime().totalMemory();
	}

	/**
	 * Obtain JVM's Free Memory.
	 * 
	 * @param size null, AUTO, B, KB, MB, GB, TB, or PB
	 * (PetaByte does not exist yet)
	 * Is not case sensitive.
	 * @param txtByte true if include byte extension, false exclude extension
	 * @return bytes size
	 * 
	 */
	public static long jvmFreeMemory() {
		return Runtime.getRuntime().freeMemory();
	}

	/**
	 * Obtain JVM's In Use Memory.
	 * 
	 * @param size null, AUTO, B, KB, MB, GB, TB, or PB
	 * (PetaByte does not exist yet)
	 * Is not case sensitive.
	 * @param txtByte true if include byte extension, false exclude extension
	 * @return bytes size
	 * 
	 */
	public static long jvmInUseMemory() {
		return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
	}

	/**
	 * These functions below are used for Operating System's
	 * RAM usage base on getOperatingSystemMXBean.
	 * -------------------------------
	 * osCommittedVirtualMemory()
	 * osTotalPhysicalMemory()
	 * osFreePhysicalMemory()
	 * osInUsePhysicalMemory()
	 * osTotalSwapSpace()
	 * osFreeSwapSpace()
	 * osInUseSwapSpace()
	 * -------------------------------
	 */
	/**
	 * Obtain Virtual Memory from Operating System's RAM.
	 * 
	 * @param size null, AUTO, B, KB, MB, GB, TB, or PB
	 * (PetaByte does not exist yet)
	 * Is not case sensitive.
	 * @param txtByte true if include byte extension, false exclude extension
	 * @return bytes size
	 * 
	 */
	public static long osCommittedVirtualMemory() {
		final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
		if (osBean instanceof UnixOperatingSystemMXBean) {
			return (((UnixOperatingSystemMXBean) osBean).getCommittedVirtualMemorySize());
		} else {
			try {
				Method m = osBean.getClass().getDeclaredMethod("getCommittedVirtualMemorySize");
				m.setAccessible(true);
				return (long) m.invoke(osBean);
			} catch (Exception e) {
				error(e);
				return -1L;
			}
		}
	}

	/**
	 * Obtain Total Physical Memory from Operating System's RAM.
	 * @return bytes size
	 */
	public static long osTotalPhysicalMemory() {
		if(containerized == null){
			containerized = isContainerized();
		}

		if(containerized) {
			try{
				return getMemoryLimitFromCgroup();

			}catch (IOException e) {
				logger.debug("Could not get memory limit from c group. {}", e.getMessage());
			}
		}

		return getTotalPhysicalMemorySize();

	}

	public static long getTotalPhysicalMemorySize(){

		final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
		if (osBean instanceof UnixOperatingSystemMXBean) {
			return (((UnixOperatingSystemMXBean) osBean).getTotalPhysicalMemorySize());
		} else {
			try {
				Method m = osBean.getClass().getDeclaredMethod("getTotalPhysicalMemorySize");
				m.setAccessible(true);
				return  (long) m.invoke(osBean);
			} catch (Exception e) {
				error(e);
				return -1L;
			}
		}

	}


	/**
	 * Obtain Free Physical Memory from Operating System's RAM.
	 * 
	 * @param size null, AUTO, B, KB, MB, GB, TB, or PB
	 * (PetaByte does not exist yet)
	 * Is not case sensitive.
	 * @param txtByte true if include byte extension, false exclude extension
	 * @return bytes size
	 * 
	 */
	public static long osFreePhysicalMemory() {
		final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
		if (osBean instanceof UnixOperatingSystemMXBean) {
			return ((UnixOperatingSystemMXBean) osBean).getFreePhysicalMemorySize();
		} else {
			try {
				Method m = osBean.getClass().getDeclaredMethod("getFreePhysicalMemorySize");
				m.setAccessible(true);
				return (long) m.invoke(osBean);
			} catch (Exception e) {
				error(e);
				return -1L;
			}
		}
	}

	/**
	 * 
	 * @return the amount of available physical memory
	 */
	public static long osAvailableMemory() {
		if(containerized == null){
			containerized = isContainerized();
		}

		if(containerized) {
			try{
				return getMemAvailableFromCgroup();

			}catch (IOException e) {
				logger.debug("Could not get mem available from cgroup. Will return os free physical memory instead.");
                return osFreePhysicalMemory();
            }
        }

		return availablePhysicalBytes();
	}

	/**
	 * Obtain In Use Physical Memory from Operating System's RAM.
	 * 
	 * @param size null, AUTO, B, KB, MB, GB, TB, or PB
	 * (PetaByte does not exist yet)
	 * Is not case sensitive.
	 * @param txtByte true if include byte extension, false exclude extension
	 * @return bytes size
	 * 
	 */
	public static long osInUsePhysicalMemory() {
		return osTotalPhysicalMemory() - osFreePhysicalMemory();
	}

	/**
	 * Obtain Total Swap Space from Operating System's RAM.
	 * 
	 * @param size null, AUTO, B, KB, MB, GB, TB, or PB
	 * (PetaByte does not exist yet)
	 * Is not case sensitive.
	 * @param txtByte true if include byte extension, false exclude extension
	 * @return bytes size
	 * 
	 */
	public static long osTotalSwapSpace() {
		final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
		if (osBean instanceof UnixOperatingSystemMXBean) {
			return ((UnixOperatingSystemMXBean) osBean).getTotalSwapSpaceSize();
		} else {
			try {
				Method m = osBean.getClass().getDeclaredMethod("getTotalSwapSpaceSize");
				m.setAccessible(true);
				return (long) m.invoke(osBean);
			} catch (Exception e) {
				error(e);
				return -1L;
			}
		}
	}

	/**
	 * Obtain Free Swap Space from Operating System's RAM.
	 * 
	 * @param size null, AUTO, B, KB, MB, GB, TB, or PB
	 * (PetaByte does not exist yet)
	 * Is not case sensitive.
	 * @param txtByte true if include byte extension, false exclude extension
	 * @return bytes size
	 * 
	 */
	public static long osFreeSwapSpace() {
		final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
		if (osBean instanceof UnixOperatingSystemMXBean) {
			return ((UnixOperatingSystemMXBean) osBean).getFreeSwapSpaceSize();
		} else {
			try {
				Method m = osBean.getClass().getDeclaredMethod("getFreeSwapSpaceSize");
				m.setAccessible(true);
				return (long) m.invoke(osBean);
			} catch (Exception e) {
				error(e);
				return -1L;
			}
		}
	}

	/**
	 * Obtain In Use Swap Space from Operating System's RAM.
	 * 
	 * @param size null, AUTO, B, KB, MB, GB, TB, or PB
	 * (PetaByte does not exist yet)
	 * Is not case sensitive.
	 * @param txtByte true if include byte extension, false exclude extension
	 * @return bytes size
	 * 
	 */
	public static long osInUseSwapSpace() {
		return osTotalSwapSpace() - osFreeSwapSpace();
	}

	/**
	 * These functions below are used for Operating System's
	 * Harddrive usage base on File.
	 * -------------------------------
	 * osHDUseableSpace()
	 * osTotalSwapSpace()
	 * osFreeSwapSpace()
	 * osHDInUseSpace()
	 * -------------------------------
	 * 
	 */
	/**
	 * Obtain Harddrive's Usable Space.
	 * 
	 * @param path actual path
	 * @param size null, AUTO, B, KB, MB, GB, TB, or PB
	 * (PetaByte does not exist yet)
	 * Is not case sensitive.
	 * @param txtByte true if include byte extension, false exclude extension
	 * @return bytes size
	 * 
	 */
	public static long osHDUsableSpace(String path) {
		if (path == null)
			path = File.listRoots()[0].getPath();
		File f = new File(path);
		if (f.getTotalSpace() == 0) {
			error(0, f.getPath());
		} else {
			return f.getUsableSpace();
		}
		return -1L;
	}

	/**
	 * Obtain Harddrive's Overall Space.
	 * 
	 * @param path actual path
	 * @param size null, AUTO, B, KB, MB, GB, TB, or PB
	 * (PetaByte does not exist yet)
	 * Is not case sensitive.
	 * @param txtByte true if include byte extension, false exclude extension
	 * @return bytes size
	 * 
	 */
	public static long osHDTotalSpace(String path) {
		if (path == null)
			path = File.listRoots()[0].getPath();
		File f = new File(path);
		if (f.getTotalSpace() == 0) {
			error(0, f.getPath());
		} else {
			return f.getTotalSpace();
		}
		return -1L;
	}

	/**
	 * Obtain Harddrive's Available Space.
	 * 
	 * @param path actual path
	 * @param size null, AUTO, B, KB, MB, GB, TB, or PB
	 * (PetaByte does not exist yet)
	 * Is not case sensitive.
	 * @param txtByte true if include byte extension, false exclude extension
	 * @return bytes size
	 * 
	 */
	public static long osHDFreeSpace(String path) {
		if (path == null)
			path = File.listRoots()[0].getPath();
		File f = new File(path);
		if (f.getTotalSpace() == 0) {
			error(0, f.getPath());
		} else {
			return f.getFreeSpace();
		}
		return -1L;
	}

	/**
	 * Obtain Harddrive's In Use Space.
	 * 
	 * @param path actual path
	 * @param size null, AUTO, B, KB, MB, GB, TB, or PB
	 * (PetaByte does not exist yet)
	 * Is not case sensitive.
	 * @param txtByte true if include byte extension, false exclude extension
	 * @return bytes size
	 * 
	 */
	public static long osHDInUseSpace(String path) {
		if (path == null)
			path = File.listRoots()[0].getPath();
		File f = new File(path);
		if (f.getTotalSpace() == 0) {
			error(0, f.getPath());
			return -1L;
		} else {
			return f.getTotalSpace() - f.getFreeSpace();
		}
	}

	/**
	 * Requirement support for...
	 * -------------------------------
	 * jvmMaxMemory()
	 * jvmTotalMemory()
	 * jvmFreeMemory()
	 * jvmInUseMemory()
	 * osCommittedVirtualMemory()
	 * osFreePhysicalMemory()
	 * osTotalPhysicalMemory()
	 * osInUsePhysicalMemory()
	 * osFreeSwapSpace()
	 * osTotalSwapSpace()
	 * osInUseSwapSpace()
	 * osHDUsableSpace()
	 * osHDTotalSpace()
	 * osHDFreeSpace()
	 * osHDInUseSpace()
	 * -------------------------------
	 */




	public static String convertByteSize(Long bytes, String size, boolean txtByte) {
		return convertByteSize( bytes,  size,  txtByte,  false);
	}

	public static String convertByteSizeToDisk(Long bytes, String size, boolean txtByte) {
		return convertByteSize( bytes,  size,  txtByte,  true);
	}


	public static long convertByteSize(long bytes, String size) {
		Long num = 1024L;
		long convertB;
		size = size.toUpperCase();

		if (size.equals("PB")) {
			convertB = bytes / (num * num * num * num * num);
		} else if (size.equals("TB")) {
			convertB = bytes / (num * num * num * num);
		} else if (size.equals("GB")) {
			convertB = bytes / (num * num * num);
		} else if (size.equals("MB")) {
			convertB = bytes / (num * num);
		} else if (size.equals("KB")) {
			convertB = bytes / num ;
		} else {
			convertB = bytes;
		}

		return convertB;
	}
	/**
	 * Permit to convert bytes to ALMOST any upper bytes with/without extension
	 * (Currently at existing TeraByte but one step ahead, PetaByte)
	 * 
	 * @param bytes length of bytes
	 * @param size null, AUTO, B, KB, MB, GB, TB, or PB
	 * (PetaByte does not exist yet)
	 * Is not case sensitive.
	 * @param txtByte true if include byte extension, false exclude extension
	 * @return bytes size
	 */
	public static String convertByteSize(Long bytes, String size, Boolean txtByte, boolean isDisk) {
		String convertB = null;
		if (bytes != null) {
			if (size != null)
				size = size.toUpperCase();
			if (txtByte == null)
				txtByte = true;
			Long num = 1024L;
			if (isDisk) {
				num = 1000L;//DO NOT CHANGE THIS VALUE!
			}
			if (size == null || size.equals("AUTO")) {
				if (bytes > (num * num * num * num * num)) {
					convertB = bytes / (num * num * num * num * num) + "";
					size = "PB";
				} else if (bytes > (num * num * num * num)) {
					convertB = bytes / (num * num * num * num) + "";
					size = "TB";
				} else if (bytes > (num * num * num)) {
					convertB = bytes / (num * num * num) + "";
					size = "GB";
				} else if (bytes > (num * num)) {
					convertB = bytes / (num * num) + "";
					size = "MB";
				} else if (bytes > num) {
					convertB = bytes / num + "";
					size = "KB";
				} else {
					convertB = bytes + "";
					size = "B";
				}
			} else if (size.equals("PB")) {
				convertB = bytes / (num * num * num * num * num) + "";
			} else if (size.equals("TB")) {
				convertB = bytes / (num * num * num * num) + "";
			} else if (size.equals("GB")) {
				convertB = bytes / (num * num * num) + "";
			} else if (size.equals("MB")) {
				convertB = bytes / (num * num) + "";
			} else if (size.equals("KB")) {
				convertB = bytes / num + "";
			} else {
				convertB = bytes + "";
			}
			if (txtByte) {
				if (size.equals("PB")) {
					convertB += "PB";
				} else if (size.equals("TB")) {
					convertB += "TB";
				} else if (size.equals("GB")) {
					convertB += "GB";
				} else if (size.equals("MB")) {
					convertB += "MB";
				} else if (size.equals("KB")) {
					convertB += "KB";
				} else {
					convertB += "B";
				}
			}
		}
		return convertB;
	}

	/**
	 * Throws error switch support between IDE errors and Red5 errors
	 */
	/**
	 * Error Exception issued switch.
	 * 
	 * @param e Throws exception errors
	 */
	protected static void error(Exception e) {
		String preError = "SystemUtils: ";

		System.out.println(preError + e);
	}

	/**
	 * Custom Error issued switch.
	 * 
	 * @param error Error #
	 * @param info Extra info from error executed
	 */
	protected static void error(int error, String info) {
		String preError = "SystemUtils: ";
		//Red5 logs
		//IDE debug logs
		if (error == 0) {
			System.out.println(preError + "Harddrive: " + info + ", doesn't appears to exist!");
		} else if (error == 1) {
			System.out.println(preError + "Your current JVM Version is " + info + ", this function required 1.6 or above!");
		} else {
			System.out.println(preError + "Unknown error #" + error);
		}
		//*/
	}



	/**
	 * Returns the "% recent cpu usage" for the whole system. 
	 * @return
	 */
	public static Integer getSystemCpuLoad() {
		final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
		if (osBean instanceof UnixOperatingSystemMXBean) {
			return (int) (((UnixOperatingSystemMXBean) osBean).getCpuLoad() * 100.0);
		} else {
			try {
				Method m = osBean.getClass().getDeclaredMethod("getSystemCpuLoad");
				m.setAccessible(true);
				return (int) (((Double) m.invoke(osBean)) * 100);
			} catch (Exception e) {
				error(e);
				return -1;
			}
		}
	}

	
	public static double getSystemLoadAverageLastMinute() {
		return  ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
	}

	/**
	 * Returns the "% recent cpu usage" for the Java Virtual Machine process. 
	 *  the method returns a negative value.
	 * @return
	 */
	public static Integer getProcessCpuLoad() {
		final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
		if (osBean instanceof UnixOperatingSystemMXBean) {
			return (int) (((UnixOperatingSystemMXBean) osBean).getProcessCpuLoad() * 100.0);
		} else {
			try {
				Method m = osBean.getClass().getDeclaredMethod("getProcessCpuLoad");
				m.setAccessible(true);
				return (int) (((Double) m.invoke(osBean)) * 100);
			} catch (Exception e) {
				error(e);
				return -1;
			}
		}
	}

	/**
	 * Returns the CPU time used by the process on which the Java virtual machine 
	 * is running in microseconds.
	 * @return
	 */
	public static Long getProcessCpuTime() {
		final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
		if (osBean instanceof UnixOperatingSystemMXBean) {
			return  (Long) (((UnixOperatingSystemMXBean) osBean).getProcessCpuTime() / 1000);
		} else {
		try {
			Method m = osBean.getClass().getDeclaredMethod("getProcessCpuTime");
			m.setAccessible(true);
			return (Long) m.invoke(osBean)/1000;
		} catch (Exception e) {
			error(e);
			return -1l;
		}
		}
	}	
	
	public static void getHeapDump(String filepath) {
		try {
			getHotspotMBean().dumpHeap(filepath, true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static HotSpotDiagnosticMXBean getHotspotMBean() {
		try {
			synchronized (SystemUtils.class) {
				if (hotspotMBean == null) {
					MBeanServer server = ManagementFactory.getPlatformMBeanServer();
					hotspotMBean = ManagementFactory.newPlatformMXBeanProxy(server,
							HOTSPOT_BEAN_NAME, HotSpotDiagnosticMXBean.class);
				}
			}
		} catch (RuntimeException re) {
			throw re;
		} catch (Exception exp) {
			throw new RuntimeException(exp);
		}
		return hotspotMBean;
	}

	public static long availablePhysicalBytes(){
		return Pointer.availablePhysicalBytes();
	}

	public static boolean isContainerized() {
		try {
			Path dockerEnvPath = Paths.get("/.dockerenv");

			// 1. Check for .dockerenv file
			if (Files.exists(dockerEnvPath)) {
				logger.debug("Container detected via .dockerenv file");
				return true;
			}

			// 2. Check env variable
			String container = System.getenv("container");
			if(StringUtils.isNotBlank(container)){
				logger.debug("Container detected via env variable.");
				return true;
			}

			// 3. Check cgroup info
			Path cgroupPath = Paths.get("/proc/self/cgroup");
			if (Files.exists(cgroupPath)) {
				List<String> cgroupContent = Files.readAllLines(cgroupPath);
				for (String line : cgroupContent) {
					if (line.contains("docker") ||
							line.contains("lxc") ||
							line.contains("kubepods") ||
							line.contains("containerd")) {
						logger.debug("Container detected via cgroup: {}", line);
						return true;
					}
				}
			}

		} catch (Exception e) {
			logger.error("Error during container detection: {}", e.getMessage());

		}

		return false;
	}

	public static Long getMemoryLimitFromCgroup() throws IOException {
		long memoryLimit;

		// Try reading memory limit for cgroups v1
		if (Files.exists(Paths.get("/sys/fs/cgroup/memory/memory.limit_in_bytes"))) {
			String memoryLimitString = readCgroupFile("/sys/fs/cgroup/memory/memory.limit_in_bytes");

			memoryLimit = Long.parseLong(memoryLimitString);

			// In cgroups v1, if the memory limit for a container isn't set, it typically returns 9223372036854771712 (2^63-1).
			// However, this value may vary based on the architecture.
			// Therefore, we consider it acceptable if the returned memory limit exceeds 100TB, indicating that the limit is not configured using cgroups.
			if(memoryLimit > MAX_CONTAINER_MEMORY_LIMIT_BYTES || memoryLimit == 0 || memoryLimit == -1) {
				memoryLimit = getTotalPhysicalMemorySize();
			}

		// Try reading memory limit for cgroups v2
		} else if (Files.exists(Paths.get("/sys/fs/cgroup/memory.max"))) {
			String memoryLimitString = readCgroupFile("/sys/fs/cgroup/memory.max");

			if(MAX_MEMORY_CGROUP_V2.equals(memoryLimitString)){ // memory limit is not set using cgroups v2
				memoryLimit = getTotalPhysicalMemorySize();
			}else{
				memoryLimit = Long.parseLong(memoryLimitString);
			}


		} else {
			logger.debug("Could not find cgroup max memory file. Will return os physical memory instead.");
			return getTotalPhysicalMemorySize();
		}

		return memoryLimit;
	}

	public static Long getMemAvailableFromCgroup() throws IOException {
		Long memoryUsage;
		Long memoryLimit;

		// Try reading memory usage and limit for cgroups v1
		if (Files.exists(Paths.get("/sys/fs/cgroup/memory/memory.usage_in_bytes")) &&
				Files.exists(Paths.get("/sys/fs/cgroup/memory/memory.limit_in_bytes"))) {
			String memoryUsageString = readCgroupFile("/sys/fs/cgroup/memory/memory.usage_in_bytes");
			memoryUsage = Long.parseLong(memoryUsageString);
			memoryLimit = getMemoryLimitFromCgroup();

			// Try reading memory usage and limit for cgroups v2
		} else if (Files.exists(Paths.get("/sys/fs/cgroup/memory.current")) &&
				Files.exists(Paths.get("/sys/fs/cgroup/memory.max"))) {

			String memoryUsageString = readCgroupFile("/sys/fs/cgroup/memory.current");
			memoryUsage = Long.parseLong(memoryUsageString);
			memoryLimit = getMemoryLimitFromCgroup();

		} else {
			logger.debug("Could not find cgroup memory files. Will return os free physical memory instead.");
			return osFreePhysicalMemory();
		}

		// Calculate available memory
		return memoryLimit - memoryUsage;
	}

	public static String readCgroupFile(String filePath) throws IOException {
		try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
			return reader.readLine().trim();
		}
	}
	
}
