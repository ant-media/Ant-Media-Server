package io.antmedia;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScriptRunUtil {
	
	protected static Logger logger = LoggerFactory.getLogger(ScriptRunUtil.class);
	
	public boolean runCommand(String command) {

		boolean result = false;
		try {
			Process process = getProcess(command);
			result = process.waitFor() == 0;
		}
		catch (IOException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}
		catch (InterruptedException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
			Thread.currentThread().interrupt();
		}
		return result;
	}

	public Process getProcess(String command) throws IOException {
		ProcessBuilder pb = new ProcessBuilder(command.split(" "));
		pb.inheritIO().redirectOutput(ProcessBuilder.Redirect.INHERIT);
		pb.inheritIO().redirectError(ProcessBuilder.Redirect.INHERIT);
		return pb.start();

	}
	
	public boolean runCreateAppScript(String appName, boolean isCluster, 
			String mongoHost, String mongoUser, String mongoPass, String warFileName) {
		Path currentRelativePath = Paths.get("");
		String webappsPath = currentRelativePath.toAbsolutePath().toString();

		String command;

		if(warFileName != null && !warFileName.isEmpty())
		{
			command = "/bin/bash create_app.sh"
					+ " -n " + appName
					+ " -w true"
					+ " -p " + webappsPath
					+ " -c " + isCluster
					+ " -f " + warFileName;

		}
		else
		{
			command = "/bin/bash create_app.sh"
					+ " -n " + appName
					+ " -w true"
					+ " -p " + webappsPath
					+ " -c " + isCluster;
		} 

		if(isCluster) 
		{
			command += " -m " + mongoHost
					+ " -u "  + mongoUser
					+ " -s "  + mongoPass;
		}

		logger.info("Creating application with command: {}", command);
		return runCommand(command);
	}
	
	public boolean runDeleteAppScript(String appName) {
		Path currentRelativePath = Paths.get("");
		String webappsPath = currentRelativePath.toAbsolutePath().toString();

		String command = "/bin/bash delete_app.sh -n "+appName+" -p "+webappsPath;

		return runCommand(command);
	}

}
