package io.antmedia.test.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.catalina.connector.ClientAbortException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import com.google.common.io.Files;

import io.antmedia.servlet.ChunkedTransferServlet;
import io.antmedia.servlet.ChunkedTransferServlet.ChunkListener;
import io.antmedia.servlet.ChunkedTransferServlet.StatusListener;
import io.antmedia.servlet.IChunkedCacheManager;
import io.antmedia.servlet.MockChunkedCacheManager;
import io.antmedia.servlet.cmafutils.AtomParser;
import io.antmedia.servlet.cmafutils.ICMAFChunkListener;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ChunkedTransferServletTest {


	@Before
	public void before() {
		File streamsDir = new File(ChunkedTransferServlet.WEBAPPS + "/junit" + ChunkedTransferServlet.STREAMS);
		streamsDir.mkdirs();
	}

	@After
	public void after() {

	}

	@Test
	public void testStatusListener() {

		try {
			StatusListener statusListener = new StatusListener("file");
			assertFalse(statusListener.isTimeoutOrErrorExist());

			statusListener.onComplete(null);
			assertFalse(statusListener.isTimeoutOrErrorExist());

			statusListener.onStartAsync(null);
			assertFalse(statusListener.isTimeoutOrErrorExist());

			statusListener.onTimeout(null);
			assertTrue(statusListener.isTimeoutOrErrorExist());

			statusListener = new StatusListener("file");
			assertFalse(statusListener.isTimeoutOrErrorExist());

			statusListener.onError(null);
			assertTrue(statusListener.isTimeoutOrErrorExist());

		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testHandleStream() {

		try {
			ChunkedTransferServlet servlet = new ChunkedTransferServlet();


			HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
			HttpServletResponse resp = Mockito.mock(HttpServletResponse.class);

			ServletContext servletContext = Mockito.mock(ServletContext.class);
			Mockito.when(req.getServletContext()).thenReturn(servletContext);
			Mockito.when(req.getPathInfo()).thenReturn("/stream" + (int)(Math.random()*10000));

			ConfigurableWebApplicationContext appContext = Mockito.mock(ConfigurableWebApplicationContext.class);
			Mockito.when(servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE)).thenReturn(appContext);
			Mockito.when(appContext.getApplicationName()).thenReturn("/junit");

			IChunkedCacheManager cacheManager = Mockito.mock(IChunkedCacheManager.class);
			Mockito.when(appContext.getBean(IChunkedCacheManager.BEAN_NAME)).thenReturn(cacheManager);

			AsyncContext asyncContext = Mockito.mock(AsyncContext.class);
			Mockito.when(req.startAsync()).thenReturn(asyncContext);


			Mockito.when(appContext.isRunning()).thenReturn(false);
			Mockito.when(resp.getWriter()).thenReturn(Mockito.mock(PrintWriter.class));
			servlet.handleIncomingStream(req, resp);
			Mockito.verify(resp).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);


			Mockito.when(asyncContext.getRequest()).thenReturn(req);
			Mockito.when(req.getInputStream()).thenReturn(Mockito.mock(ServletInputStream.class));

			Mockito.when(appContext.isRunning()).thenReturn(true);
			servlet.handleIncomingStream(req, resp);
			Mockito.verify(asyncContext).start(Mockito.any());

			Mockito.when(req.getPathInfo()).thenReturn("/stream" + (int)(Math.random()*10000) + ".mpd");
			servlet.handleIncomingStream(req, resp);
			Mockito.verify(asyncContext, Mockito.times(2)).start(Mockito.any());


			//no slash
			Mockito.when(req.getPathInfo()).thenReturn("stream" + (int)(Math.random()*10000));
			servlet.handleIncomingStream(req, resp);
			//it should 2 again
			Mockito.verify(asyncContext, Mockito.times(2)).start(Mockito.any());


			//
			String streamId = "stream" + (int)(Math.random()*10000);
			Mockito.when(req.getPathInfo()).thenReturn("/" + streamId +"/" + streamId + ".mpd");
			servlet.handleIncomingStream(req, resp);
			//it should 3 
			Mockito.verify(asyncContext, Mockito.times(3)).start(Mockito.any());

			servlet.handleIncomingStream(req, resp);
			//it should 4 
			Mockito.verify(asyncContext, Mockito.times(4)).start(Mockito.any());



		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}


	@Test
	public void testReadInputStream() {
		ChunkedTransferServlet servlet = new ChunkedTransferServlet();

		IChunkedCacheManager cacheManager = Mockito.mock(IChunkedCacheManager.class);

		String streamId = "streamId" + (int)(Math.random()*10000);

		File finalFile = new File(ChunkedTransferServlet.WEBAPPS + "/junit" + ChunkedTransferServlet.STREAMS + "/" + streamId);
		assertFalse(finalFile.exists());

		File tmpFile = new File(ChunkedTransferServlet.WEBAPPS + "/junit" + ChunkedTransferServlet.STREAMS + "/" + streamId + ".tmp");

		AsyncContext asyncContext = Mockito.mock(AsyncContext.class);
		ServletRequest req = Mockito.mock(ServletRequest.class);

		Mockito.when(asyncContext.getRequest()).thenReturn(req);
		StatusListener statusListener = new StatusListener(tmpFile.getName());


		try (FileInputStream istream = new FileInputStream("src/test/resources/chunked-samples/chunk-stream0-00001.m4s")) 
		{
			servlet.readInputStream(finalFile, tmpFile, cacheManager, Mockito.mock(AtomParser.class), asyncContext, istream, statusListener);


			Mockito.verify(cacheManager).removeCache(finalFile.getAbsolutePath());
			assertTrue(finalFile.exists());

		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}


		finalFile.delete();

		try (FileInputStream istream = new FileInputStream("src/test/resources/chunked-samples/chunk-stream0-00001.m4s")) 
		{
			statusListener.onTimeout(null);
			servlet.readInputStream(finalFile, tmpFile, cacheManager, Mockito.mock(AtomParser.class), asyncContext, istream, statusListener);


			Mockito.verify(cacheManager, Mockito.times(2)).removeCache(finalFile.getAbsolutePath());
			assertTrue(finalFile.exists());

			//it should be just 2048 byte because it breaks the loop
			assertEquals(2048, finalFile.length());

		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}



	}


	@Test
	public void testDeleteRequest() 
	{
		try {
			ChunkedTransferServlet servlet = new ChunkedTransferServlet();

			HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
			HttpServletResponse resp = Mockito.mock(HttpServletResponse.class);

			ServletContext servletContext = Mockito.mock(ServletContext.class);
			Mockito.when(req.getServletContext()).thenReturn(servletContext);
			Mockito.when(req.getPathInfo()).thenReturn("/stream" + (int)(Math.random()*10000));

			ConfigurableWebApplicationContext appContext = Mockito.mock(ConfigurableWebApplicationContext.class);
			Mockito.when(servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE)).thenReturn(appContext);
			Mockito.when(appContext.getApplicationName()).thenReturn("/junit");

			IChunkedCacheManager cacheManager = Mockito.mock(IChunkedCacheManager.class);
			Mockito.when(appContext.getBean(IChunkedCacheManager.BEAN_NAME)).thenReturn(cacheManager);

			AsyncContext asyncContext = Mockito.mock(AsyncContext.class);
			Mockito.when(req.startAsync()).thenReturn(asyncContext);


			Mockito.when(appContext.isRunning()).thenReturn(false);
			Mockito.when(resp.getWriter()).thenReturn(Mockito.mock(PrintWriter.class));
			servlet.deleteRequest(req, resp);
			Mockito.verify(resp).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);


			Mockito.when(appContext.isRunning()).thenReturn(true);
			servlet.deleteRequest(req, resp);
			Mockito.verify(resp, Mockito.times(1)).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

			File f = new File(ChunkedTransferServlet.WEBAPPS + "/junit" + ChunkedTransferServlet.STREAMS + req.getPathInfo());
			assertFalse(f.exists());

			assertTrue(f.createNewFile());
			assertTrue(f.exists());
			servlet.deleteRequest(req, resp);
			assertFalse(f.exists());


			String streamId = "stream" + (int)(Math.random()*10000);
			Mockito.when(req.getPathInfo()).thenReturn("/" + streamId + "/ "+ streamId);
			f = new File(ChunkedTransferServlet.WEBAPPS + "/junit" + ChunkedTransferServlet.STREAMS + req.getPathInfo());
			assertFalse(f.exists());
			f.getParentFile().mkdirs();
			assertTrue(f.createNewFile());
			assertTrue(f.exists());
			servlet.deleteRequest(req, resp);
			assertFalse(f.exists());


		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testMockChunckedCacheManger() {
		MockChunkedCacheManager cacheManager = new MockChunkedCacheManager();


		assertFalse(cacheManager.hasCache("key"));

		cacheManager.addCache("key");

		assertFalse(cacheManager.hasCache("key"));

		cacheManager.removeCache("key");

		assertFalse(cacheManager.hasCache("key"));

		cacheManager.registerChunkListener("key", null);

		cacheManager.append("key", null);

		cacheManager.removeChunkListener("key", null);

		assertFalse(cacheManager.hasCache("key"));

	}


	@Test
	public void testHandleGetRequest() 
	{
		try {
			ChunkedTransferServlet servlet = new ChunkedTransferServlet();

			HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
			HttpServletResponse resp = Mockito.mock(HttpServletResponse.class);

			ServletContext servletContext = Mockito.mock(ServletContext.class);
			Mockito.when(req.getServletContext()).thenReturn(servletContext);
			String streamId = "stream" + (int)(Math.random()*10000);
			Mockito.when(req.getRequestURI()).thenReturn("/junit/streams/" + streamId);

			ConfigurableWebApplicationContext appContext = Mockito.mock(ConfigurableWebApplicationContext.class);
			Mockito.when(servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE)).thenReturn(appContext);
			Mockito.when(appContext.getApplicationName()).thenReturn("/junit");

			IChunkedCacheManager cacheManager = Mockito.mock(IChunkedCacheManager.class);
			Mockito.when(appContext.getBean(IChunkedCacheManager.BEAN_NAME)).thenReturn(cacheManager);

			AsyncContext asyncContext = Mockito.mock(AsyncContext.class);
			Mockito.when(req.startAsync()).thenReturn(asyncContext);
			Mockito.when(asyncContext.getResponse()).thenReturn(resp);

			Mockito.when(appContext.isRunning()).thenReturn(false);
			Mockito.when(resp.getWriter()).thenReturn(Mockito.mock(PrintWriter.class));
			servlet.handleGetRequest(req, resp);
			Mockito.verify(resp).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);


			Mockito.when(appContext.isRunning()).thenReturn(true);
			servlet.handleGetRequest(req, resp);
			Mockito.verify(resp).setStatus(HttpServletResponse.SC_NOT_FOUND);
			Mockito.verify(resp, Mockito.never()).setContentType(null);

			File file = new File("/junit/streams/" + streamId);
			Mockito.verify(servletContext).getMimeType(file.getName());


			File f = new File(ChunkedTransferServlet.WEBAPPS + "/junit" + ChunkedTransferServlet.STREAMS + "/" + streamId);

			Mockito.when(cacheManager.hasCache(f.getAbsolutePath())).thenReturn(true);
			servlet.handleGetRequest(req, resp);
			Mockito.verify(cacheManager).registerChunkListener(Mockito.anyString(), Mockito.any());
			Mockito.verify(asyncContext, Mockito.times(1)).start(Mockito.any());

			assertFalse(f.exists());
			File realFile = new File("src/test/resources/chunked-samples/chunk-stream0-00001.m4s");
			f.getParentFile().mkdirs();
			Files.copy(realFile, f);


			Mockito.when(resp.getOutputStream()).thenReturn(Mockito.mock(ServletOutputStream.class));

			servlet.handleGetRequest(req, resp);
			Mockito.verify(asyncContext, Mockito.times(2)).start(Mockito.any());


		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testChunkListener() {

		try {
			ChunkedTransferServlet chunkedTransferServlet = new ChunkedTransferServlet();
			AsyncContext asynContext = Mockito.mock(AsyncContext.class);
			IChunkedCacheManager cacheManager = Mockito.mock(IChunkedCacheManager.class);
			File file = new File("src/test/resources/chunked-samples/chunk-stream0-00001.m4s");


			ServletResponse response = Mockito.mock(ServletResponse.class);

			Mockito.when(asynContext.getResponse()).thenReturn(response);
			ServletOutputStream outputStream = Mockito.mock(ServletOutputStream.class);
			Mockito.when(response.getOutputStream()).thenReturn(outputStream);

			ChunkListener listener = new ChunkListener();

			byte[] data = new byte[1024];
			listener.chunkCompleted(data);
			listener.chunkCompleted(new byte[0]);

			File f = new File("webapps/junit/streams");
			f.getParentFile().mkdirs();
			chunkedTransferServlet.writeChunks(f, cacheManager, asynContext, listener, "text");

			Mockito.verify(outputStream).write(data, 0, 1024);
			Mockito.verify(asynContext).complete();

			Mockito.doThrow(ClientAbortException.class).when(outputStream).flush();
			listener.chunkCompleted(data);
			chunkedTransferServlet.writeChunks(f, cacheManager, asynContext, listener, "text");

			Mockito.verify(cacheManager, Mockito.times(2)).removeChunkListener(f.getAbsolutePath(), listener);


			listener.chunkCompleted(null);
			Mockito.verify(asynContext, Mockito.times(1)).complete();

		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testWriteOutputStream() 
	{
		try {
			ChunkedTransferServlet servlet = new ChunkedTransferServlet();

			File istream = new File("src/test/resources/chunked-samples/chunk-stream0-00001.m4s");

			ServletOutputStream ostream = Mockito.mock(ServletOutputStream.class);
			AsyncContext asyncContext = Mockito.mock(AsyncContext.class);
			HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

			Mockito.when(response.getOutputStream()).thenReturn(ostream);


			Mockito.when(asyncContext.getResponse()).thenReturn(response);

			servlet.writeOutputStream(istream, asyncContext, "text");

			Mockito.verify(asyncContext).complete();

		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}
	
	@Test
	public void testLogHeaders() {
		HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
		
		ChunkedTransferServlet.logHeaders(response);
		
		Mockito.when(response.getHeaderNames()).thenReturn(Arrays.asList("header1","header2", "header3"));
		Mockito.when(response.getHeader(Mockito.anyString())).thenReturn("header value");
		
		ChunkedTransferServlet.logHeaders(response);
		
		Mockito.when(response.getHeader(Mockito.anyString())).thenThrow(new NullPointerException("exception"));
		
		ChunkedTransferServlet.logHeaders(response);
	}


	@Test
	public void testParseChunks_stream0_00001() {

		try {
			FileInputStream istream = new FileInputStream("src/test/resources/chunked-samples/chunk-stream0-00001.m4s");

			byte[] data = new byte[2048];

			int length = 0;

			List<byte[]> chunkedList = new ArrayList<>();
			AtomParser chunkParser = new AtomParser(new ICMAFChunkListener() {

				@Override
				public void chunkCompleted(byte[] completeChunk) {
					chunkedList.add(completeChunk);
				}
			});

			while ((length = istream.read(data, 0, data.length)) > 0) {
				chunkParser.parse(data, 0, length);
			}

			istream.close();

			assertEquals(17, chunkedList.size());

			assertEquals(24, chunkedList.get(0).length);
			assertEquals(160 + 28874, chunkedList.get(1).length);
			assertEquals(208 + 34043, chunkedList.get(2).length);
			assertEquals(0x9c + 0x56f0, chunkedList.get(3).length);
			assertEquals(0xd0 + 0x85d4, chunkedList.get(4).length);

		} 
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) 
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}


	@Test
	public void testParseChunks_stream0_00002() {

		try {
			FileInputStream istream = new FileInputStream("src/test/resources/chunked-samples/chunk-stream0-00002.m4s");

			byte[] data = new byte[2048];

			int length = 0;

			List<byte[]> chunkedList = new ArrayList<>();
			AtomParser chunkParser = new AtomParser(new ICMAFChunkListener() {

				@Override
				public void chunkCompleted(byte[] completeChunk) {
					chunkedList.add(completeChunk);
				}
			});

			while ((length = istream.read(data, 0, data.length)) > 0) {
				chunkParser.parse(data, 0, length);
			}


			istream.close();

			assertEquals(17, chunkedList.size());

			assertEquals(24, chunkedList.get(0).length);
			assertEquals(0xa0 + 0x8888, chunkedList.get(1).length);
			assertEquals(0xd0 + 0x7a1d, chunkedList.get(2).length);
			assertEquals(0x9c + 0x3bbc, chunkedList.get(3).length);
			assertEquals(0xd0 + 0x808b, chunkedList.get(4).length);

		} 
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) 
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testParseChunk_stream1_00001() {

		try {
			FileInputStream istream = new FileInputStream("src/test/resources/chunked-samples/chunk-stream1-00001.m4s");

			byte[] data = new byte[2048];

			int length = 0;

			List<byte[]> chunkedList = new ArrayList<>();
			AtomParser chunkParser = new AtomParser(new ICMAFChunkListener() {

				@Override
				public void chunkCompleted(byte[] completeChunk) {
					chunkedList.add(completeChunk);
				}
			});

			while ((length = istream.read(data, 0, data.length)) > 0) {
				chunkParser.parse(data, 0, length);
			}


			istream.close();

			assertEquals(17, chunkedList.size());

			assertEquals(24, chunkedList.get(0).length);
			assertEquals(0x118 + 0x204f, chunkedList.get(1).length);
			assertEquals(0x118 + 0x1ff1, chunkedList.get(2).length);
			assertEquals(0x118 + 0x20ec, chunkedList.get(3).length);
			assertEquals(0x118 + 0x1fde, chunkedList.get(4).length);

		} 
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) 
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testParseChunk_init_stream0() {

		try {
			FileInputStream istream = new FileInputStream("src/test/resources/chunked-samples/init-stream0.m4s");

			byte[] data = new byte[2048];

			int length = 0;

			List<byte[]> chunkedList = new ArrayList<>();
			AtomParser chunkParser = new AtomParser(new ICMAFChunkListener() {

				@Override
				public void chunkCompleted(byte[] completeChunk) {
					chunkedList.add(completeChunk);
				}
			});

			while ((length = istream.read(data, 0, data.length)) > 0) {
				chunkParser.parse(data, 0, length);
			}


			istream.close();

			assertEquals(1, chunkedList.size());

			assertEquals(0x1c+0x2f0, chunkedList.get(0).length);

		} 
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) 
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testParseChunk_init_stream1() {

		try {
			FileInputStream istream = new FileInputStream("src/test/resources/chunked-samples/init-stream1.m4s");

			byte[] data = new byte[2048];

			int length = 0;

			List<byte[]> chunkedList = new ArrayList<>();
			AtomParser chunkParser = new AtomParser(new ICMAFChunkListener() {

				@Override
				public void chunkCompleted(byte[] completeChunk) {
					chunkedList.add(completeChunk);
				}
			});

			while ((length = istream.read(data, 0, data.length)) > 0) {
				chunkParser.parse(data, 0, length);
			}

			istream.close();

			assertEquals(1, chunkedList.size());

			assertEquals(0x1c+0x2a9, chunkedList.get(0).length);

		} 
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) 
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}
