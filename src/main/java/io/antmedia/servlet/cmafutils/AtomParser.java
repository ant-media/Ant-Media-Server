package io.antmedia.servlet.cmafutils;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.servlet.ChunkedTransferServlet;

public class AtomParser implements IParser {
	
	protected static Logger logger = LoggerFactory.getLogger(AtomParser.class);
	
	private int atomLength = 0;
	private byte[] atomData;
	private int currentPosition = 0;
	private byte[] remainerData;
	
	
	private static final int STYP_ATOM = 0;
	private static final int PRFT_ATOM = 1;
	private static final int EMSG_ATOM = 2;
	private static final int MOOF_ATOM = 3;
	private static final int MDAT_ATOM = 4;
	private static final int FTYP_ATOM = 5;
	private static final int MOOV_ATOM = 6;
	
	private int currentAtomType = 0;
	private int currentChunkedSize = 0;
	
	private LinkedList<byte[]> populatingChunked = new LinkedList<>();

	private ICMAFChunkListener chunkListener;

	public static class MockAtomParser implements IParser
	{
		public void parse(byte[] data, int offset, int length) {
			//mock class don't do anything to simplify the code base
		}
	}
	
	public AtomParser(ICMAFChunkListener chunkListener) {
		this.chunkListener = chunkListener;
		
	}
	
	/**
	 * 
	 * @param data
	 * @param offset 
	 * @param length is the total number of meaningfull data in the array
	 */
	public void parse(byte[] data, int offset, int length) 
	{
		
		if (atomLength == 0) 
		{
			if (length >= (offset + 3)) 
			{
				if (remainerData != null) 
				{
					byte[] tmpData = new byte[remainerData.length + length]; 
					System.arraycopy(remainerData, 0, tmpData, 0, remainerData.length);
					System.arraycopy(data, 0, tmpData, remainerData.length, length);
					data = tmpData;
					
				}
				atomLength = (0xFF & data[offset + 0]) << 24 | (0xFF & data[offset + 1]) << 16 | (0xFF & data[offset + 2]) << 8 | (0xFF & data[offset + 3]); 
				atomData = new byte[atomLength];
				logger.trace("atom length:{} ", atomLength);
				
				/*
				 chunks contain prft | emsg | moof | mdat 
				 prft and emsg are optional. 
				 
				 Fragment containts  styp | 
				   
				 fragments consists of one or more chunks
				 segments  consists of one or more fragments
				 
				 
				 init stream contains ftyp + moov atoms
				    
				*/
				
				setCurrentAtomType(data, offset);
				
			}
			else {
				
				remainerData = new byte[length - offset];
				System.arraycopy(data, offset, remainerData, 0, remainerData.length);
				return;
			}
		}
		
		//length is the total available data in the coming array
		//currentPosition is the stored data for that atom previously
		//if new incoming data(length -offset) + current stored data  is bigger than total atom length,
		//it mean atom is complete
		
		int remainingBytesInArray = (length + currentPosition) - atomLength - offset;
		if (remainingBytesInArray >= 0) 
		{
			//this means that atom is completed
			int remaining = atomLength - currentPosition;
			logger.trace("remaining offset: {} currentPosition:{} length:{} remaining:{}", offset, currentPosition, length, remaining);
			System.arraycopy(data, offset, atomData, currentPosition, remaining);
			
			if (currentAtomType == STYP_ATOM || currentAtomType == MDAT_ATOM || currentAtomType == MOOV_ATOM) 
			{
				finalizeChunked();
			}
			else 
			{
				populatingChunked.add(atomData);
				currentChunkedSize += atomData.length;
			}
			
			//reset 
			atomLength = 0;
			currentPosition = 0;
			
			//handle the rest
			if (remainingBytesInArray > 0) 
			{
				parse(data, offset + remaining, length);
			}
			
		}
		else 
		{
			//this means that atom is populating
			logger.trace("offset: {} currentPosition: {} length: {}", offset, currentPosition, length);
			System.arraycopy(data, offset, atomData, currentPosition, length-offset);
			
			currentPosition += (length-offset);
		}
		
		
		
	}


	private void setCurrentAtomType(byte[] data, int offset) {
		if (data[offset + 4] == 's') 
		{
			//styp atom
			currentAtomType = STYP_ATOM;
		}
		else if (data[offset + 4] == 'p') 
		{
			//prft atom
			currentAtomType = PRFT_ATOM;
		}
		else if (data[offset + 4] == 'e') 
		{
			//emsg atom
			currentAtomType = EMSG_ATOM;
		}
		else if (data[offset + 4] == 'm' && data[offset + 5] == 'o' && data[offset + 6] == 'o' && data[offset + 7] == 'f') 
		{
			//moof atom
			currentAtomType = MOOF_ATOM;
		}
		else if (data[offset + 4] == 'm' && data[offset + 5] == 'd' && data[offset + 6] == 'a' && data[offset + 7] == 't') 
		{
			//mdat atom
			currentAtomType = MDAT_ATOM;
		}
		else if (data[offset + 4] == 'f' && data[offset + 5] == 't' && data[offset + 6] == 'y' && data[offset + 7] == 'p') 
		{
			//ftyp atom
			currentAtomType = FTYP_ATOM;
		}
		else if (data[offset + 4] == 'm' && data[offset + 5] == 'o' && data[offset + 6] == 'o' && data[offset + 7] == 'v') 
		{
			//moov atom
			currentAtomType = MOOV_ATOM;
		}
		else {
			logger.error("atom type is not detected {} {} {} {}", data[offset + 4], data[offset + 5], data[offset + 6], data[offset + 7]);
		}
	}


	private void finalizeChunked() {
		populatingChunked.add(atomData);
		currentChunkedSize += atomData.length;
		byte[] completeChunk = new byte[currentChunkedSize];
		int populatingChunkPosition = 0;
		for (byte[] chunkAtom : populatingChunked) 
		{
			System.arraycopy(chunkAtom, 0, completeChunk, populatingChunkPosition, chunkAtom.length);
			populatingChunkPosition += chunkAtom.length;
		}
		populatingChunked.clear();
		currentChunkedSize = 0;
		
		this.chunkListener.chunkCompleted(completeChunk);
		
		
	}
	
}
