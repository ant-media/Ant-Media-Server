package io.antmedia.console.rest;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import jakarta.servlet.ServletContext;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.antmedia.SystemUtils;
import io.antmedia.rest.RestServiceBase;
import io.antmedia.rest.model.Result;
import io.antmedia.rest.model.Version;
import io.antmedia.statistic.IStatsCollector;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

@Component
@Path("/v2/support")
public class SupportRestService  extends CommonRestService {
	class SupportResponse {
		private boolean result;

		public boolean isResult() {
			return result;
		}

		public void setResult(boolean result) {
			this.result = result;
		}
	}

	protected static Logger logger = LoggerFactory.getLogger(SupportRestService.class);

	@Context
	private ServletContext servletContext;
	private IStatsCollector statsCollector;
	public static final String LOG_FILE = "ant-media-server.log.zip";

	public static final int SEND_SUPPORT_CONNECT_TIMEOUT_SECONDS= 5;

	public static final int SEND_SUPPORT_SOCKET_TIMEOUT_SECONDS = 20;

	@POST
	@Path("/request")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Result sendSupportRequest(SupportRequest request) {
		boolean success = false;
		logger.info("New Support Request");
		try {
			success = sendSupport(request);
		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}
		return new Result(success);
	}	
	

	public boolean sendSupport(SupportRequest supportRequest) throws Exception {
		boolean success = false;
		String cpuInfo = "not allowed to get";

		CloseableHttpClient httpClient = HttpClients.createDefault();

		try {
			Version version = RestServiceBase.getSoftwareVersion();

			HttpPost httpPost = new HttpPost("https://antmedia.io/livedemo/upload/upload.php");

			RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(SEND_SUPPORT_CONNECT_TIMEOUT_SECONDS * 1000).setSocketTimeout(SEND_SUPPORT_SOCKET_TIMEOUT_SECONDS * 1000).build();
			
			httpPost.setConfig(requestConfig);
			
			MultipartEntityBuilder builder = MultipartEntityBuilder.create();
			builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

			if(supportRequest.isSendSystemInfo()) {
				cpuInfo = getCpuInfo();

				zipFile();
				File logFile = new File(LOG_FILE);

				builder.addBinaryBody(LOG_FILE, logFile, ContentType.create("application/zip"), LOG_FILE);
			}

			builder.addTextBody("name", supportRequest.getName());
			builder.addTextBody("email", supportRequest.getEmail());
			builder.addTextBody("title", supportRequest.getTitle());
			builder.addTextBody("description", supportRequest.getDescription());
			builder.addTextBody("isEnterprise", RestServiceBase.isEnterprise()+"");
			builder.addTextBody("licenseKey", getServerSettings().getLicenceKey());
			builder.addTextBody("cpuInfo", cpuInfo);
			builder.addTextBody("cpuUsage", getStatsCollector().getCpuLoad()+"");
			builder.addTextBody("ramUsage", SystemUtils.osFreePhysicalMemory()+"/"+SystemUtils.osTotalPhysicalMemory());
			builder.addTextBody("diskUsage", SystemUtils.osHDFreeSpace(null)+"/"+SystemUtils.osHDTotalSpace(null));
			builder.addTextBody("version", version.getVersionType()+" "+version.getVersionName()+" "+version.getBuildNumber());
			builder.addTextBody("isMarketplace", getServerSettings().isBuildForMarket() + "");
			if (getServerSettings().isBuildForMarket()) {
				builder.addTextBody("marketplace", getServerSettings().getMarketplace());
			}

			HttpEntity httpEntity = builder.build();

			httpPost.setEntity(httpEntity);

			CloseableHttpResponse response = httpClient.execute(httpPost);

			try {
				if (response.getStatusLine().getStatusCode() == 200) {
					String jsonResponse = readResponse(response).toString();
					SupportResponse supportResponse = gson.fromJson(jsonResponse, SupportResponse.class);
					success = supportResponse.isResult();
				}	
			} finally {
				response.close();
			}
		}catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}
		finally {
			httpClient.close();
		}
		if (!success) {
			logger.error("Cannot send e-mail in support form for e-mail: {}", supportRequest.getEmail());
		}
		return success;		
	}

	public static void zipFile() {

		List<String> files = new ArrayList<>();

		File serverLogFile = new File("log/ant-media-server.log");
		File errorLogFile = new File("log/antmedia-error.log");

		if(serverLogFile.exists()) {
			files.add("log/ant-media-server.log");
		}
		if(errorLogFile.exists()) {
			files.add("log/antmedia-error.log");
		}

		// your directory
		File f = new File(".");
		File[] matchingFiles = f.listFiles((dir, name) -> name.startsWith("hs_err"));

		for (File file : matchingFiles) {
			files.add(file.getName());
		}


		try ( FileOutputStream fos = new FileOutputStream(LOG_FILE);
			  ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(fos))) 
		{
			for(String filePath:files){
				File input = new File(filePath);
				addZipEntry(zipOut, input);
			}
		}
		catch (IOException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		} 
	}

	private static void addZipEntry(ZipOutputStream zipOut, File input) {
		try (FileInputStream fis = new FileInputStream(input)) 
		{
			ZipEntry ze = new ZipEntry(input.getName());
			zipOut.putNextEntry(ze);
			byte[] tmp = new byte[4*1024];
			int size = 0;
			while((size = fis.read(tmp)) != -1){
				zipOut.write(tmp, 0, size);
			}
			zipOut.flush();
		}
		catch (IOException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}
	}


	public static String getCpuInfo() {
		StringBuilder cpuInfo = new StringBuilder();
		ProcessBuilder pb = new ProcessBuilder("lscpu");
		try {
			Process process = pb.start();
			BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			while ((line = br.readLine()) != null) {
				cpuInfo.append(line);
			}			
		} catch (IOException e) {
			logger.error(ExceptionUtils.getMessage(e));
		}

		return cpuInfo.toString();
	}

	public static StringBuilder readResponse(HttpResponse response) throws IOException {
		StringBuilder result = new StringBuilder();
		if(response.getEntity() != null) {
			BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

			String line = "";
			while ((line = rd.readLine()) != null) {
				result.append(line+"\r\n");
			}
		}
		return result;
	}
}
