package io.antmedia.muxer.parser;

public abstract class Parser {
	
	protected int currentBit;
	protected byte[] data;
	
	protected boolean errorOccured = false;
	
	public Parser(byte[] data, int offset) {
		this.data = data;
		currentBit = offset * 8;
		parse();
	}
	
	protected abstract void parse();
	
	protected int readBit()
	{
	    int nIndex = currentBit / 8;
	    int nOffset = currentBit % 8 + 1;

	    currentBit ++;
	    return (data[nIndex] >> (8-nOffset)) & 0x01;
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
	
	public boolean isErrorOccured() {
		return errorOccured;
	}

}
