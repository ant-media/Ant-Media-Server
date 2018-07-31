package io.antmedia.test.stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.tika.utils.ExceptionUtils;
import org.awaitility.Awaitility;
import org.bytedeco.javacpp.avformat;
import org.bytedeco.javacpp.avutil;
import org.junit.BeforeClass;
import org.junit.Test;
import org.red5.codec.AudioCodec;
import org.red5.codec.VideoCodec;
import org.red5.io.ITag;
import org.red5.io.flv.impl.FLVWriter;
import org.red5.io.flv.impl.Tag;
import org.red5.io.utils.IOUtils;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IStreamListener;
import org.red5.server.api.stream.IStreamPacket;
import org.red5.server.scheduling.QuartzSchedulingService;
import org.red5.server.scope.WebScope;
import org.red5.server.stream.RemoteBroadcastStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import io.antmedia.integration.MuxingTest;
import io.antmedia.test.MuxerUnitTest;

@ContextConfiguration(locations = { 
		"../test.xml" 
})
public class RemoteBroadcastStreamTest extends AbstractJUnit4SpringContextTests{

	protected static Logger logger = LoggerFactory.getLogger(RemoteBroadcastStreamTest.class);
	protected WebScope appScope;

	private final static byte[] DEFAULT_STREAM_ID = new byte[] { (byte) (0 & 0xff), (byte) (0 & 0xff), (byte) (0 & 0xff) };


	static {
		System.setProperty("red5.deployment.type", "junit");
		System.setProperty("red5.root", ".");
	}

	@BeforeClass
	public static void beforeClass() {
		avformat.av_register_all();
		avformat.avformat_network_init();
		avutil.av_log_set_level(avutil.AV_LOG_INFO);
	}


	/**
	 * Bug this case occurs in test_video_360p.flv file 
	 */
	@Test
	public void testBugRemoteBroadcastStreamStartStop() 
	{
		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}

		QuartzSchedulingService scheduler = (QuartzSchedulingService) applicationContext.getBean(QuartzSchedulingService.BEAN_NAME);
		assertNotNull(scheduler);

		RemoteBroadcastStream rbs = new RemoteBroadcastStream();

		File streamFile = new File("src/test/resources/test_video_360p.flv");
		assertTrue(streamFile.exists());
		rbs.setRemoteStreamUrl(streamFile.getAbsolutePath());
		//boolean containsScheduler = thisScope.getParent().getContext().getApplicationContext().containsBean("rtmpScheduler");
		//if (containsScheduler) 

		rbs.setScheduler(scheduler);

		rbs.setName(UUID.randomUUID().toString());
		rbs.setConnection(null);
		rbs.setScope(appScope);


		String fileName = "target/test" + (int)(Math.random() * 10000) + ".flv"; 
		File file = new File(fileName);
		FLVWriter flvWriter = new FLVWriter(file, false);
		flvWriter.setVideoCodecId(VideoCodec.AVC.getId());
		flvWriter.setAudioCodecId(AudioCodec.AAC.getId());

		rbs.addStreamListener(new IStreamListener() {

			@Override
			public void packetReceived(IBroadcastStream stream, IStreamPacket packet) {
				ITag tag = new Tag();
				tag.setDataType(packet.getDataType());
				tag.setBodySize(packet.getData().limit());
				tag.setTimestamp(packet.getTimestamp());
				tag.setBody(packet.getData());
				try {
					flvWriter.writeTag(tag);
				} catch (IOException e) {
					e.printStackTrace();
					fail(e.getMessage());
				}
			}
		});
		rbs.start();
		


		while(scheduler.getScheduledJobNames().size() != 1) {
			Awaitility.waitAtMost(10, TimeUnit.SECONDS)
					.pollDelay(5, TimeUnit.SECONDS)
					.until(() -> !RemoteBroadcastStream.isExceptionExist());
		}

		flvWriter.close();
		rbs.stop();

		assertTrue(MuxingTest.testFile(file.getAbsolutePath(), 146380, false));


		assertEquals(0, rbs.getReferenceCountInQueue());

	}


	public byte[] getTag(ITag tag) throws IOException {
		/*
		 * Tag header = 11 bytes
		 * |-|---|----|---|
		 *    0 = type
		 *  1-3 = data size
		 *  4-7 = timestamp
		 * 8-10 = stream id (always 0)
		 * Tag data = variable bytes
		 * Previous tag = 4 bytes (tag header size + tag data size)
		 */
		// skip tags with no data
		int bodySize = tag.getBodySize();
		// ensure that the channel is still open
		// get the data type
		byte dataType = tag.getDataType();
		// if we're writing non-meta tags do seeking and tag size update

		// set a var holding the entire tag size including the previous tag length
		int totalTagSize = RemoteBroadcastStream.TAG_HEADER_LENGTH + bodySize + 4;
		// resize
		// create a buffer for this tag
		ByteBuffer tagBuffer = ByteBuffer.allocate(totalTagSize);
		// get the timestamp
		int timestamp = tag.getTimestamp();
		// allow for empty tag bodies
		byte[] bodyBuf = null;
		if (bodySize > 0) {
			// create an array big enough
			bodyBuf = new byte[bodySize];
			// put the bytes into the array
			tag.getBody().get(bodyBuf);
			// get the audio or video codec identifier

		}
		// Data Type
		IOUtils.writeUnsignedByte(tagBuffer, dataType); //1
		// Body Size - Length of the message. Number of bytes after StreamID to end of tag 
		// (Equal to length of the tag - 11) 
		IOUtils.writeMediumInt(tagBuffer, bodySize); //3
		// Timestamp
		IOUtils.writeExtendedMediumInt(tagBuffer, timestamp); //4
		// Stream id
		tagBuffer.put(DEFAULT_STREAM_ID); //3
		// get the body if we have one
		if (bodyBuf != null) {
			tagBuffer.put(bodyBuf);
		}
		// we add the tag size
		tagBuffer.putInt(RemoteBroadcastStream.TAG_HEADER_LENGTH + bodySize);
		// flip so we can process from the beginning
		tagBuffer.flip();
		// write the tag

		return tagBuffer.array();

	}
}
