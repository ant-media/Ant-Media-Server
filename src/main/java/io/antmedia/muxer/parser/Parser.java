package io.antmedia.muxer.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Parser {
	
	protected int currentBit;
	protected byte[] data;
	
	protected boolean errorOccured = false;
	protected int width;
	protected int height;
	private boolean nalUnitParsing;
	
	private static Logger logger = LoggerFactory.getLogger(Parser.class);
	
	
	public Parser(byte[] data, int offset, boolean nalUnitParsing) {
		this.nalUnitParsing = nalUnitParsing;
		this.data = data;
		currentBit = offset * 8;
		parse();
	}
	
	protected abstract void parse();
	
	

	
	protected int readBit()
	{		
	    int nIndex = currentBit / 8;
	    int nOffset = currentBit % 8 + 1;

	    currentBit++;
	    
	    int result = (data[nIndex] >> (8-nOffset)) & 0x01;
	    
	    if (nalUnitParsing) 
	    {
		    if (currentBit % 8 == 0) 
		    {
		    	nIndex = currentBit / 8;
		    	if (nIndex >= 2) 
		    	{
					if (data[nIndex - 2] == 0 && data[nIndex - 1] == 0 && data[nIndex] == 3) 
					{
						currentBit += 8;
					}
		    	}		
		    }
	    }
	    
	    return result;
	}

	
	protected int readBits(int n)
	{
	    int r = 0;
	    int i;
	    for (i = 0; i < n; i++)
	    {
	        r |= ( readBit() << ( n - i - 1 ) );
	    }
	    return r;
	}
	

	protected int readExponentialGolombCode()
	{
	    int r = 0;
	    int i = 0;

	    while( (readBit() == 0) && (i < 32) )
	    {
	        i++;
	    }

	    r = readBits(i);
	    r += (1 << i) - 1;
	    return r;
	}

	
	protected int readSE() 
	{
	    int r = readExponentialGolombCode();
	    if ((r & 0x01) != 0x0)
	    {
	        r = (r+1)/2;
	    }
	    else
	    {
	        r = -(r/2);
	    }
	    return r;
	}
	
	protected byte[] readByte(int numBytes) {
        byte[] result = new byte[numBytes];
        for (int i = 0; i < numBytes; i++) {
            result[i] = (byte) readBits(8);
        }
        return result;
    }
	
	public boolean isErrorOccured() {
		return errorOccured;
	}
	
	
	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}

}
