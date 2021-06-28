package io.antmedia.muxer.parser;

/**
 * SPS parser parses the AnnexB SPS and finds some codec parameters
 * @author mekya
 *
 */
public class SpsParser extends Parser {

	private int width;
	private int height;

	
	
	/**
	 * It starts parsing with the 6 byte in the annexb format. 
	 * First bytes are in this order 0x00 0x00 0x00 0x01 0xHEADER_BYTE(0x67, 0x27) 0xSTARTS_HERE
	 * @param data
	 * @param offset
	 */
	public SpsParser(byte[] data, int offset) {
		super(data, offset);
	}
	
	
	@Override
	protected void parse()
	{
		
	    int frameCropLeftOffset=0;
	    int frameCropRightOffset=0;
	    int frameCropTopOffset=0;
	    int frameCropBottomOffset=0;

	    int profileIdc =  readBits(8);  
	    
	    /*
	    int constraint_set0_flag = readBit();   
	    int constraint_set1_flag = readBit();   
	    int constraint_set2_flag = readBit();   
	    int constraint_set3_flag = readBit();   
	    int constraint_set4_flag = readBit();   
	    int constraint_set5_flag = readBit();   
	    int reserved_zero_2bits  = readBits(2); 
	    */
	    readBits(8);
	    
	  //  int level_idc =  readBits(8);   
	    readBits(8);
	   // int seq_parameter_set_id = readExponentialGolombCode();

	    readExponentialGolombCode();
	    
	    if( profileIdc == 100 || profileIdc == 110 ||
	        profileIdc == 122 || profileIdc == 244 ||
	        profileIdc == 44 || profileIdc == 83 ||
	        profileIdc == 86 || profileIdc == 118 )
	    {
	        int chromaFormatIdc = readExponentialGolombCode();

	        if( chromaFormatIdc == 3 )
	        {
	            int residual_colour_transform_flag = readBit();         
	        }
	        int bit_depth_luma_minus8 = readExponentialGolombCode();        
	        int bit_depth_chroma_minus8 = readExponentialGolombCode();      
	        int qpprime_y_zero_transform_bypass_flag = readBit();       
	        int seqScalingMatrixPresentFlag = readBit();        

	        if (seqScalingMatrixPresentFlag != 0) 
	        {
	            int i=0;
	            for ( i = 0; i < 8; i++) 
	            {
	                int seqScalingListPresentFlag = readBit();
	                if (seqScalingListPresentFlag != 0) 
	                {
	                    int sizeOfScalingList = (i < 6) ? 16 : 64;
	                    int lastScale = 8;
	                    int nextScale = 8;
	                    int j=0;
	                    for ( j = 0; j < sizeOfScalingList; j++) 
	                    {
	                        if (nextScale != 0) 
	                        {
	                            int deltaScale = readSE();
	                            nextScale = (lastScale + deltaScale + 256) % 256;
	                        }
	                        lastScale = (nextScale == 0) ? lastScale : nextScale;
	                    }
	                }
	            }
	        }
	    }

	    int log2_max_frame_num_minus4 = readExponentialGolombCode();
	    int picOrderCntType = readExponentialGolombCode();
	    if( picOrderCntType == 0 )
	    {
	        int log2_max_pic_order_cnt_lsb_minus4 = readExponentialGolombCode();
	    }
	    else if( picOrderCntType == 1 )
	    {
	        int delta_pic_order_always_zero_flag = readBit();
	        int offset_for_non_ref_pic = readSE();
	        int offset_for_top_to_bottom_field = readSE();
	        int num_ref_frames_in_pic_order_cnt_cycle = readExponentialGolombCode();
	        int i;
	        for( i = 0; i < num_ref_frames_in_pic_order_cnt_cycle; i++ )
	        {
	            readSE();
	        }
	    }
	    int max_num_ref_frames = readExponentialGolombCode();
	    int gaps_in_frame_num_value_allowed_flag = readBit();
	    int picWidthInMbsMinus1 = readExponentialGolombCode();
	    int picHeightInMapUnitsMinus1 = readExponentialGolombCode();
	    int frameMbsOnlyFlag = readBit();
	    if (frameMbsOnlyFlag == 0 )
	    {
	        int mb_adaptive_frame_field_flag = readBit();
	    }
	    int direct_8x8_inference_flag = readBit();
	    int frameCroppingFlag = readBit();
	    if( frameCroppingFlag != 0)
	    {
	        frameCropLeftOffset = readExponentialGolombCode();
	        frameCropRightOffset = readExponentialGolombCode();
	        frameCropTopOffset = readExponentialGolombCode();
	        frameCropBottomOffset = readExponentialGolombCode();
	    }
	    int vui_parameters_present_flag = readBit();

	    width = ((picWidthInMbsMinus1 +1)*16) - frameCropRightOffset *2 - frameCropLeftOffset *2;
	    height = ((2 - frameMbsOnlyFlag)* (picHeightInMapUnitsMinus1 +1) * 16) - (frameCropBottomOffset* 2) - (frameCropTopOffset* 2);
	    

	}
	
	
	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}
	


}
