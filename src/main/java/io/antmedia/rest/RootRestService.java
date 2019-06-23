package io.antmedia.rest;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Date;
import java.util.jar.Manifest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.rest.model.Result;
import io.antmedia.rest.model.Version;
import io.antmedia.streamsource.StreamFetcher;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Contact;
import io.swagger.annotations.ExternalDocs;
import io.swagger.annotations.Info;
import io.swagger.annotations.License;
import io.swagger.annotations.SwaggerDefinition;

@Api(value = "VoD Rest Service")
@SwaggerDefinition(
        info = @Info(
                description = "Ant Media Server REST API Reference",
                version = "V2.0",
                title = "Ant Media Server REST API Reference",
                contact = @Contact(name = "Ant Media Info", email = "contact@antmedia.io", url = "https://antmedia.io"),
                license = @License(name = "Apache 2.0", url = "http://www.apache.org")),
        consumes = {"application/json"},
        produces = {"application/json"},
        schemes = {SwaggerDefinition.Scheme.HTTP, SwaggerDefinition.Scheme.HTTPS},
        externalDocs = @ExternalDocs(value = "External Docs", url = "https://antmedia.io"),
        basePath = "/v2"
)
@Component
@Path("/v2")
public class RootRestService {
	
	
	protected static Logger logger = LoggerFactory.getLogger(RootRestService.class);
	
	@ApiOperation(value = "Returns the Ant Media Server Version", notes = "", response = Version.class)
	@GET
	@Path("/version")
	@Produces(MediaType.APPLICATION_JSON)
	public Version getVersion() {
		return getSoftwareVersion();
	}

	public static Version getSoftwareVersion() {
		Version version = new Version();
		version.setVersionName(AntMediaApplicationAdapter.class.getPackage().getImplementationVersion());

		URLClassLoader cl = (URLClassLoader) AntMediaApplicationAdapter.class.getClassLoader();
		URL url = cl.findResource("META-INF/MANIFEST.MF");
		Manifest manifest;
		try {
			manifest = new Manifest(url.openStream());
			version.setBuildNumber(manifest.getMainAttributes().getValue(RestServiceBase.BUILD_NUMBER));
		} catch (IOException e) {
			//No need to implement
		}

		version.setVersionType(isEnterprise() ? RestServiceBase.ENTERPRISE_EDITION : RestServiceBase.COMMUNITY_EDITION);

		logger.debug("Version Name {} Version Type {}", version.getVersionName(), version.getVersionType());
		return version;
	}
	
	
	public static boolean isEnterprise() {
		try {
			Class.forName("io.antmedia.enterprise.adaptive.EncoderAdaptor");
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}
	

	
}