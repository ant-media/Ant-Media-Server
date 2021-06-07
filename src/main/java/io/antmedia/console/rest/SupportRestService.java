package io.antmedia.console.rest;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
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
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.google.gson.Gson;

import io.antmedia.SystemUtils;
import io.antmedia.licence.ILicenceService;
import io.antmedia.rest.RestServiceBase;
import io.antmedia.rest.model.Result;
import io.antmedia.rest.model.Version;
import io.antmedia.settings.ServerSettings;
import io.antmedia.statistic.IStatsCollector;

@Component
@Path("/support")
public class SupportRestService {
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
	private ILicenceService licenceService;
	private IStatsCollector statsCollector;
	private ServerSettings serverSettings;
	private Gson gson = new Gson();
	private static final String LOG_FILE = "ant-media-server.log.zip";
	
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
	
	public ILicenceService getLicenceServiceInstance () {
		if(licenceService == null) {

			WebApplicationContext ctxt = WebApplicationContextUtils.getWebApplicationContext(servletContext); 
			licenceService = (ILicenceService)ctxt.getBean(ILicenceService.BeanName.LICENCE_SERVICE.toString());
		}
		return licenceService;
	}
	
	public IStatsCollector getStatsCollector () {
		if(statsCollector == null) {
			WebApplicationContext ctxt = WebApplicationContextUtils.getWebApplicationContext(servletContext); 
			statsCollector = (IStatsCollector)ctxt.getBean(IStatsCollector.BEAN_NAME);
		}
		return statsCollector;
	}
	
	public boolean sendSupport(SupportRequest supportRequest) throws Exception {
		boolean success = false;
		String cpuInfo = "not allowed to get";
		
		CloseableHttpClient httpClient = HttpClients.createDefault();
		
		try {
			Version version = RestServiceBase.getSoftwareVersion();

			HttpPost httpPost = new HttpPost("https://antmedia.io/livedemo/upload/upload.php");

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
			logger.error(e.getMessage());
		}
		finally {
			httpClient.close();
		}
		if (!success) {
			logger.error("Cannot send e-mail in support form for e-mail: {}", supportRequest.getEmail());
		}
		return success;		
	}
	
	private static void zipFile() {
		
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
    	File[] matchingFiles = f.listFiles(new FilenameFilter() {
    	    public boolean accept(File dir, String name) {
    	        return name.startsWith("hs_err") ;
    	    }
    	});
    	
    	for (File file : matchingFiles) {
    		files.add(file.getName());
		}
    	
		
		 FileOutputStream fos = null;
		 ZipOutputStream zipOut = null;
	     FileInputStream fis = null;
	     try {
	    	 fos = new FileOutputStream(LOG_FILE);
	         zipOut = new ZipOutputStream(new BufferedOutputStream(fos));
	         for(String filePath:files){
	        	 File input = new File(filePath);
	             fis = new FileInputStream(input);
	             ZipEntry ze = new ZipEntry(input.getName());
	             System.out.println("Zipping the file: "+input.getName());
	             zipOut.putNextEntry(ze);
	             byte[] tmp = new byte[4*1024];
	             int size = 0;
	             while((size = fis.read(tmp)) != -1){
	            	 zipOut.write(tmp, 0, size);
	             }
	             zipOut.flush();
	             fis.close();
	             }
	            zipOut.close();
	        } catch (FileNotFoundException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	        } catch (IOException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	        } finally{
	            try{
	                if(fos != null) fos.close();
	            } catch(Exception ex){
	                 
	            }
	        }
    }
	
	
	public String getCpuInfo() {
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

	public ServerSettings getServerSettings() {
		if(serverSettings == null) {

			WebApplicationContext ctxt = WebApplicationContextUtils.getWebApplicationContext(servletContext); 
			serverSettings = (ServerSettings)ctxt.getBean(ServerSettings.BEAN_NAME);
		}
		return serverSettings;
	}

	public StringBuilder readResponse(HttpResponse response) throws IOException {
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
