package io.antmedia.test.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import io.antmedia.servlet.cmafutils.AtomParser;
import io.antmedia.servlet.cmafutils.ICMAFChunkListener;

public class ChunkedTransferServletTest {
	
	
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
