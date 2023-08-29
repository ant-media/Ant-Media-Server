package io.antmedia.test.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.antmedia.rest.RequestWrapper;
import org.junit.Test;

import io.antmedia.rest.model.BasicStreamInfo;
import io.antmedia.webrtc.VideoCodec;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class RequestWrapperTest {

    @Test
    public void testRequestWrapper() throws IOException {
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        String requestBody = "test request body";
        mockHttpServletRequest.setCharacterEncoding("UTF-8");
        mockHttpServletRequest.setContent(requestBody.getBytes());
        RequestWrapper wrappedRequest = new RequestWrapper(mockHttpServletRequest);
        ServletInputStream inputStream = wrappedRequest.getInputStream();
        wrappedRequest.getReader();
        StringBuilder sb = new StringBuilder();
        for (int ch; (ch = inputStream.read()) != -1;) {
            sb.append((char) ch);
        }
        String requestBodyStr = sb.toString();
        assertEquals(requestBody, requestBodyStr);
        assertTrue(inputStream.isFinished() && inputStream.isReady());

    }
}
