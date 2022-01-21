package io.antmedia.test.filter;

import com.auth0.jwk.JwkException;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import io.antmedia.AppSettings;
import io.antmedia.console.rest.JWTServerFilter;
import io.antmedia.settings.ServerSettings;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.ServletException;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class JWTServerFilterTest {

    protected static Logger logger = LoggerFactory.getLogger(JWTServerFilterTest.class);

    @Test
    public void testDoFilterPass() throws IOException, ServletException, JwkException {

        JWTServerFilter jwtServerFilter = Mockito.spy(new JWTServerFilter());
        ServerSettings serverSettings;
        MockHttpServletResponse httpServletResponse;
        MockHttpServletRequest httpServletRequest;
        MockFilterChain filterChain;

        serverSettings = new ServerSettings();
        serverSettings.setJwtServerSecretKey("testtesttesttesttesttesttesttest");
        serverSettings.setJwtServerControlEnabled(true);
        
        AppSettings appSettings = new AppSettings();
        appSettings.setJwtSecretKey("2222222222222222222222222222222222222222222222");       
       

        Mockito.doReturn(appSettings).when(jwtServerFilter).getAppSettings();
        Mockito.doReturn(serverSettings).when(jwtServerFilter).getServerSetting();

        String token = JWT.create().sign(Algorithm.HMAC256(serverSettings.getJwtServerSecretKey()));
        String invalidToken = JWT.create().sign(Algorithm.HMAC256("invalid-key-invalid-key-invalid-key"));
        String appJwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e30._dEfIoSDLBJo49hmyivqYziMIGeWOD19Ex0kGGK_6ww";
        String jwkstoken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6InFWMWhqT0o2RFlFOWIzRDFSU0lHSSJ9.eyJpc3MiOiJodHRwczovL2FudG1lZGlhLnVzLmF1dGgwLmNvbS8iLCJzdWIiOiI3UVEzWTlLSzJPY1dOSzRwMXpydGU1UzQxSWR4amxLc0BjbGllbnRzIiwiYXVkIjoiaHR0cHM6Ly9hbnRtZWRpYS51cy5hdXRoMC5jb20vYXBpL3YyLyIsImlhdCI6MTYyODE1MTQ4NSwiZXhwIjoxNjI4MjM3ODg1LCJhenAiOiI3UVEzWTlLSzJPY1dOSzRwMXpydGU1UzQxSWR4amxLcyIsImd0eSI6ImNsaWVudC1jcmVkZW50aWFscyJ9.dgtT0dqL_JiA1AJRIWYyJMU7KG_EpJzucqlmIdEt36rL35G9QLWcxJVWCM-OFDAje9UaNDqFVHMNfXzDhvXrs5LvPlEFSZVAZUtMgjP0X94hlCjKIrvhnfN2lcDZFsSMIqXJeoPMjlRvGItrphRQaMr5ow3eCcvRYVK1MXvptGisdh1rTVBWPRRvaFH8x5yISw98DwNauVWW949o8JDIkortDQEXHmpipC7NoACCzZmGlmBv6ubaZxyTwh7QAp68kx6_tIcmj7nm6cLdoheFjH-io-ee6oTkLRr2krRqSjJrY9A_hJYH4Gixpe-F7mMeE8dMuRGhWue3pfMJmy8ulQ";


        System.out.println("Valid Token: " + token);

        // JWT Token enable and invalid token scenario
        {
            //reset filterchain
            filterChain = new MockFilterChain();

            //reset httpServletResponse
            httpServletResponse = new MockHttpServletResponse();

            //reset httpServletRequest
            httpServletRequest = new MockHttpServletRequest();

            serverSettings.setJwtServerControlEnabled(true);

            Mockito.doReturn(serverSettings).when(jwtServerFilter).getServerSetting();

            httpServletRequest.addHeader("Authorization", invalidToken);

            jwtServerFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
            assertEquals(HttpStatus.FORBIDDEN.value(),httpServletResponse.getStatus());
        }

        // JWT Token disable and passed token scenario
        {
            //reset filterchains
            filterChain = new MockFilterChain();

            //reset httpServletResponses
            httpServletResponse = new MockHttpServletResponse();

            //reset httpServletRequest
            httpServletRequest = new MockHttpServletRequest();

            serverSettings.setJwtServerControlEnabled(false);

            Mockito.doReturn(serverSettings).when(jwtServerFilter).getServerSetting();

            httpServletRequest.addHeader("Authorization", token);

            jwtServerFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
            assertEquals(HttpStatus.OK.value(),httpServletResponse.getStatus());
        }

        // JWT Token enable and valid token scenario
        {
            //reset filterchains
            filterChain = new MockFilterChain();

            //reset httpServletResponses
            httpServletResponse = new MockHttpServletResponse();

            //reset httpServletRequest
            httpServletRequest = new MockHttpServletRequest();

            serverSettings.setJwtServerControlEnabled(true);

            Mockito.doReturn(serverSettings).when(jwtServerFilter).getServerSetting();

            httpServletRequest.addHeader("Authorization", token);

            jwtServerFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
            assertEquals(HttpStatus.OK.value(),httpServletResponse.getStatus());
        }

        // JWT Token enable and null header token scenario
        // Now it will continue and Authentication filter will check permission again
        {
            //reset filterchains
            filterChain = new MockFilterChain();

            //reset httpServletResponses
            httpServletResponse = new MockHttpServletResponse();

            //reset httpServletRequest
            httpServletRequest = new MockHttpServletRequest();

            serverSettings.setJwtServerControlEnabled(true);

            Mockito.doReturn(serverSettings).when(jwtServerFilter).getServerSetting();

            jwtServerFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
            assertEquals(HttpStatus.OK.value(),httpServletResponse.getStatus());            
        }
        ///// Jwks Tests
        /// correct token
        {
            //reset filterchains
            filterChain = new MockFilterChain();

            //reset httpServletResponses
            httpServletResponse = new MockHttpServletResponse();

            //reset httpServletRequest
            httpServletRequest = new MockHttpServletRequest();

            serverSettings.setJwtServerControlEnabled(true);
            serverSettings.setJwksURL("https://antmedia.us.auth0.com");
            serverSettings.setJwtServerSecretKey("4Hr7PWwrTf6YFynkO5QeNQrlxe5r7HtfUdLhis2i_vbXdtF1VI0SwnP0ZSlhf0Yh");


            Mockito.doReturn(serverSettings).when(jwtServerFilter).getServerSetting();

            httpServletRequest.addHeader("Authorization", jwkstoken);

            jwtServerFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
            assertEquals(HttpStatus.OK.value(),httpServletResponse.getStatus());
        }
        /// Jwks Url is not given

        {
            //reset filterchains
            filterChain = new MockFilterChain();

            //reset httpServletResponses
            httpServletResponse = new MockHttpServletResponse();

            //reset httpServletRequest
            httpServletRequest = new MockHttpServletRequest();

            serverSettings.setJwtServerControlEnabled(true);
            serverSettings.setJwksURL("");

            Mockito.doReturn(serverSettings).when(jwtServerFilter).getServerSetting();
            serverSettings.setJwtServerSecretKey("random");

            httpServletRequest.addHeader("Authorization", jwkstoken);

            jwtServerFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
            assertEquals(HttpStatus.FORBIDDEN.value(),httpServletResponse.getStatus());
        }
        // This case covering when JWT Server and JWT App filters are enabled
        // If REST request pass to JWTFilter, it should pass JWTServerFilter
        // Internal request scenario
        {
            //reset filterchains
            filterChain = new MockFilterChain();

            //reset httpServletResponses
            httpServletResponse = new MockHttpServletResponse();

            //reset httpServletRequest
            httpServletRequest = new MockHttpServletRequest();

            appSettings.setJwtControlEnabled(true);
            serverSettings.setJwtServerControlEnabled(true);
            // It means app JWT Token key
            httpServletRequest.addHeader("Authorization", appJwtToken);
            
            httpServletRequest.setRequestURI("/rest/v2/request");

            Mockito.doReturn(appSettings).when(jwtServerFilter).getAppSettings();
            Mockito.doReturn(serverSettings).when(jwtServerFilter).getServerSetting();

            jwtServerFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
            assertEquals(HttpStatus.OK.value(),httpServletResponse.getStatus());            
        }
        
    }

}
