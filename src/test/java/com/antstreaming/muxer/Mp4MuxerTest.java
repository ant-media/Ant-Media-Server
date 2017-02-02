package com.antstreaming.muxer;

import static org.bytedeco.javacpp.avformat.avformat_close_input;
import static org.bytedeco.javacpp.avformat.avformat_find_stream_info;
import static org.bytedeco.javacpp.avformat.avformat_open_input;
import static org.bytedeco.javacpp.avformat.avio_alloc_context;
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.mina.core.buffer.IoBuffer;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.avformat;
import org.bytedeco.javacpp.avutil;
import org.bytedeco.javacpp.avformat.AVFormatContext;
import org.bytedeco.javacpp.avformat.AVIOContext;
import org.bytedeco.javacpp.avutil.AVDictionary;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.red5.io.utils.IOUtils;
import org.red5.server.api.stream.IStreamListener;
import org.red5.server.api.stream.IStreamPacket;
import org.red5.server.scheduling.QuartzSchedulingService;
import org.red5.server.scope.WebScope;
import org.junit.Test;
import org.red5.io.ITag;
import org.red5.io.flv.impl.FLVReader;
import org.red5.io.flv.impl.FLVWriter;
import org.red5.io.mp4.impl.MP4Reader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.util.ResourceUtils;

import com.antstreaming.muxer.MuxAdaptor.ReadCallback;

@ContextConfiguration(locations = { 
		"test.xml" 
})
//@ContextConfiguration(classes = {AppConfig.class})
public class Mp4MuxerTest extends AbstractJUnit4SpringContextTests{

	protected static Logger logger = LoggerFactory.getLogger(Mp4MuxerTest.class);

	private WebScope appScope;

	static {
		System.setProperty("red5.deployment.type", "junit");
		System.setProperty("red5.root", ".");
	}


	@BeforeClass
	public static void beforeClass() {
		avformat.av_register_all();
		//	avformat.avformat_network_init();

		File webApps = new File("webapps");
		if (!webApps.exists()) {
			webApps.mkdirs();
		}
		File junit = new File(webApps, "junit");
		if (!junit.exists()) {
			junit.mkdirs();
		}

		File streams = new File(junit, "streams");
		if (!streams.exists()) {
			streams.mkdirs();
		}
	}

	@AfterClass
	public static void afterClass() {
		try {
			delete(new File("webapps"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void delete(File file)
			throws IOException{

		if(file.isDirectory()){

			//directory is empty, then delete it
			if(file.list().length==0){

				file.delete();
				//System.out.println("Directory is deleted : " 
				//	+ file.getAbsolutePath());

			}else{

				//list all the directory contents
				String files[] = file.list();

				for (String temp : files) {
					//construct the file structure
					File fileDelete = new File(file, temp);

					//recursive delete
					delete(fileDelete);
				}

				//check the directory again, if empty then delete it
				if(file.list().length==0){
					file.delete();
					//System.out.println("Directory is deleted : " 
					//		+ file.getAbsolutePath());
				}
			}

		}else{
			//if file, then delete it
			file.delete();
			//System.out.println("File is deleted : " + file.getAbsolutePath());
		}
	}

	public class StreamPacket implements IStreamPacket {

		private ITag readTag;
		private IoBuffer data;

		public StreamPacket(ITag tag) {
			readTag = tag;
			data = readTag.getBody();
		}

		@Override
		public int getTimestamp() {
			return readTag.getTimestamp();
		}

		@Override
		public byte getDataType() {
			return readTag.getDataType();
		}

		@Override
		public IoBuffer getData() {
			return data;
		}
		
		public void setData(IoBuffer data) {
			this.data = data;
		}
	};


	//TODO: write production test code for mp4 and hls
	//check that red5.properties autorecording mp4 is set correctly
	//check that red5.properties autorecording hls is set correctly
	//start a live stream and check that mp4 and hls is working correclyt

	//TODO: when prepare fails, there is memorly leak or thread leak?


	
	
	@Test
	public void testMuxingSimultaneously()  {
		
		MuxAdaptor muxAdaptor = new MuxAdaptor();
		muxAdaptor.addMuxer(new Mp4Muxer());
		muxAdaptor.addMuxer(new HLSMuxer());

		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}

		//File file = new File(getResource("test.mp4").getFile());
		File file = null;

		try {

			QuartzSchedulingService scheduler = (QuartzSchedulingService) applicationContext.getBean(QuartzSchedulingService.BEAN_NAME);
			assertNotNull(scheduler);
			assertEquals(scheduler.getScheduledJobNames().size(), 0);

			file = new File("target/test-classes/test.flv"); //ResourceUtils.getFile(this.getClass().getResource("test.flv"));
			final FLVReader flvReader = new FLVReader(file);

			logger.debug("f path:" + file.getAbsolutePath());
			assertTrue(file.exists());

			boolean result = muxAdaptor.init(appScope, "test", false);
			assertTrue(result);
			
			muxAdaptor.start();
			
			int i = 0;
			while (flvReader.hasMoreTags()) 
			{
				ITag readTag = flvReader.readTag();
				StreamPacket streamPacket = new StreamPacket(readTag);
				muxAdaptor.packetReceived(null, streamPacket);
			}
			
			assertTrue(muxAdaptor.isRecording());

			muxAdaptor.stop();

			flvReader.close();

			while (muxAdaptor.isRecording()) {
				Thread.sleep(50);
			}


			assertFalse(muxAdaptor.isRecording());

			testFile(muxAdaptor.getMuxerList().get(0).getFile().getAbsolutePath());
			testFile(muxAdaptor.getMuxerList().get(1).getFile().getAbsolutePath());

		}
		catch (Exception e) {
			fail("exception:" + e );
		}

	}
	
	
	@Test
	public void testMp4Muxing()  {

		MuxAdaptor muxAdaptor = new MuxAdaptor();
		muxAdaptor.addMuxer(new Mp4Muxer());

		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}

		//File file = new File(getResource("test.mp4").getFile());
		File file = null;

		try {

			QuartzSchedulingService scheduler = (QuartzSchedulingService) applicationContext.getBean(QuartzSchedulingService.BEAN_NAME);
			assertNotNull(scheduler);
			assertEquals(scheduler.getScheduledJobNames().size(), 0);

			file = new File("target/test-classes/test.flv"); //ResourceUtils.getFile(this.getClass().getResource("test.flv"));
			final FLVReader flvReader = new FLVReader(file);

			logger.debug("f path:" + file.getAbsolutePath());
			assertTrue(file.exists());

			boolean result = muxAdaptor.init(appScope, "test", false);
			assertTrue(result);


			muxAdaptor.start();

			int i = 0;
			while (flvReader.hasMoreTags()) 
			{
				ITag readTag = flvReader.readTag();
				StreamPacket streamPacket = new StreamPacket(readTag);
				muxAdaptor.packetReceived(null, streamPacket);

			}

			assertEquals(scheduler.getScheduledJobNames().size(), 1);
			assertTrue(muxAdaptor.isRecording());

			muxAdaptor.stop();

			flvReader.close();

			while (muxAdaptor.isRecording()) {
				Thread.sleep(50);
			}

			assertFalse(muxAdaptor.isRecording());
			
			assertEquals(scheduler.getScheduledJobNames().size(), 0);
			
			testFile(muxAdaptor.getMuxerList().get(0).getFile().getAbsolutePath());

		}
		catch (Exception e) {
			fail("exception:" + e );
		}

	}

	




	@Test
	public void testHLSMuxing()  {

		MuxAdaptor muxAdaptor = new MuxAdaptor();
		muxAdaptor.addMuxer(new HLSMuxer());

		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}

		File file = null;
		try {

			QuartzSchedulingService scheduler = (QuartzSchedulingService) applicationContext.getBean(QuartzSchedulingService.BEAN_NAME);
			assertNotNull(scheduler);
			assertEquals(scheduler.getScheduledJobNames().size(), 0);

			file = new File("target/test-classes/test.flv"); //ResourceUtils.getFile(this.getClass().getResource("test.flv"));
			final FLVReader flvReader = new FLVReader(file);

			logger.debug("f path:" + file.getAbsolutePath());
			assertTrue(file.exists());

			boolean result = muxAdaptor.init(appScope, "testhls", false);
			assert(result);

			muxAdaptor.start();

			int i = 0;
			while (flvReader.hasMoreTags()) 
			{
				ITag readTag = flvReader.readTag();
				StreamPacket streamPacket = new StreamPacket(readTag);
				muxAdaptor.packetReceived(null, streamPacket);
			}

			assertTrue(muxAdaptor.isRecording());

			muxAdaptor.stop();

			flvReader.close();

			while (muxAdaptor.isRecording()) {
				Thread.sleep(50);
			}

			assertFalse(muxAdaptor.isRecording());
			assertEquals(scheduler.getScheduledJobNames().size(), 0);
			
			File hlsFile = muxAdaptor.getMuxerList().get(0).getFile();
			
			testFile(hlsFile.getAbsolutePath());
			

		}
		catch (Exception e) {

		}

	}

	private void testFile(String absolutePath) {
		int ret;
		AVFormatContext inputFormatContext = avformat.avformat_alloc_context();
		if (inputFormatContext == null) {
			fail("cannot allocate input context");
		}
		
		if ((ret = avformat_open_input(inputFormatContext, absolutePath, null, (AVDictionary)null)) < 0) {
			fail("cannot open input context");
		}
		
		ret = avformat_find_stream_info(inputFormatContext, (AVDictionary)null);
		if (ret < 0) {
			fail("Could not find stream information\n");
		}
		
		avformat_close_input(inputFormatContext);
		
	}
	


}
