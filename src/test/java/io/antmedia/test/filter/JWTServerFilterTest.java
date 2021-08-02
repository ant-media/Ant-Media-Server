package io.antmedia.test.filter;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.antmedia.AppSettings;
import io.antmedia.console.rest.JWTServerFilter;
import io.antmedia.filter.JWTFilter;
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
import java.security.interfaces.RSAPublicKey;

import static org.junit.Assert.assertEquals;

public class JWTServerFilterTest {

    protected static Logger logger = LoggerFactory.getLogger(JWTServerFilterTest.class);
    private ServerSettings serverSettings;

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

        Mockito.doReturn(serverSettings).when(jwtServerFilter).getServerSetting();

        String token = JWT.create().sign(Algorithm.HMAC256(serverSettings.getJwtServerSecretKey()));
        String invalidToken = JWT.create().sign(Algorithm.HMAC256("invalid-key-invalid-key-invalid-key"));



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
            assertEquals(HttpStatus.FORBIDDEN.value(),httpServletResponse.getStatus());
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

            /*
            String jwkstoken = JWT.create().sign(Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null));

            Mockito.doReturn(serverSettings).when(jwtServerFilter).getServerSetting();

            httpServletRequest.addHeader("Authorization", jwkstoken);

            jwtServerFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
            assertEquals(HttpStatus.FORBIDDEN.value(),httpServletResponse.getStatus());*/
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

            /*
            String jwkstoken = JWT.create().sign(Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null));

            Mockito.doReturn(serverSettings).when(jwtServerFilter).getServerSetting();

            httpServletRequest.addHeader("Authorization", jwkstoken);

            jwtServerFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
            assertEquals(HttpStatus.FORBIDDEN.value(),httpServletResponse.getStatus());*/
        }
    }

}
