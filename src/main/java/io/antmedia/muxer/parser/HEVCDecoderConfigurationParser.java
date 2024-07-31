package io.antmedia.muxer.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 
 ISO/IEC 14496-15, 8.3.3.1.2 Syntax


 aligned(8) class HEVCDecoderConfigurationRecord {
	unsigned int(8) configurationVersion = 1;
	unsigned int(2) general_profile_space;
	unsigned int(1) general_tier_flag;
	unsigned int(5) general_profile_idc;
	unsigned int(32) general_profile_compatibility_flags;
	unsigned int(48) general_constraint_indicator_flags;
	unsigned int(8) general_level_idc;
	bit(4) reserved = ‘1111’b;
	unsigned int(12) min_spatial_segmentation_idc;
	bit(6) reserved = ‘111111’b;
	unsigned int(2) parallelismType;
	bit(6) reserved = ‘111111’b;
	unsigned int(2) chromaFormat;
	bit(5) reserved = ‘11111’b;
	unsigned int(3) bitDepthLumaMinus8;
	bit(5) reserved = ‘11111’b;
	unsigned int(3) bitDepthChromaMinus8;
	
	bit(16) avgFrameRate;
	bit(2) constantFrameRate;
	bit(3) numTemporalLayers;
	bit(1) temporalIdNested;
	unsigned int(2) lengthSizeMinusOne; 
	unsigned int(8) numOfArrays;
	for (j=0; j < numOfArrays; j) {
		bit(1) array_completeness;
		unsigned int(1) reserved = 0;
		unsigned int(6) NAL_unit_type;
		unsigned int(16) numNalus;
		for (i=0; i< numNalus; i) {
			unsigned int(16) nalUnitLength;
			bit(8*nalUnitLength) nalUnit;
		}
	}
}
*/
public class HEVCDecoderConfigurationParser  extends Parser {

	
	private static Logger logger = LoggerFactory.getLogger(HEVCDecoderConfigurationParser.class);

	
	public HEVCDecoderConfigurationParser(byte[] data, int offset) {
		super(data, offset, false);
	}
	
	
	public static final int NAL_UNIT_TYPE_HEVC_VPS = 32;
	
	public static final int NAL_UNIT_TYPE_HEVC_SPS = 33;
	
	public static final int NAL_UNIT_TYPE_HEVC_PPS = 34;
	
	
	private byte[] vps;
	private byte[] sps;
	private byte[] pps;

	public static class HEVCSPSParser extends Parser {

		public HEVCSPSParser(byte[] data, int offset) {
			super(data, offset, true);
		}

		/*
		     buffer.ReadBits(&sps.vps_id, 4);
			 //sps_max_sub_layers_minus1 3 bits
			 uint32_t max_sub_layers_minus1;
			 buffer.ReadBits(&max_sub_layers_minus1, 3);
			 //sps_temporal_id_nesting_flag 1 bit
			 buffer.ConsumeBits(1);
		
			 //processProfileTierLevel
			 //general_profile_space 2 bits
		     buffer.ConsumeBits(2);
		     //general_tier_flag 2 bits
		     buffer.ConsumeBits(1);
		     //general_profile_idc 5bits
		     buffer.ConsumeBits(5);
		
		     //for(std::size_t i=0; i<32; i)
		     //   ptl.general_profile_compatibility_flag[i] = bs.getBits(1);
		     //
		     buffer.ConsumeBits(32);
		
		     //ptl.general_progressive_source_flag = bs.getBits(1);
		     buffer.ConsumeBits(1);
		     //ptl.general_interlaced_source_flag = bs.getBits(1);
		     buffer.ConsumeBits(1);
		     //ptl.general_non_packed_constraint_flag = bs.getBits(1);
		     buffer.ConsumeBits(1);
		     //ptl.general_frame_only_constraint_flag = bs.getBits(1);
		     buffer.ConsumeBits(1);
		
		     //bs.getBits(32);
		     buffer.ConsumeBits(32);
		     //bs.getBits(12);
		     buffer.ConsumeBits(12);
		     //ptl.general_level_idc = bs.getBits(8);
		     buffer.ConsumeBits(8);
		     uint32_t sub_layer_profile_present_flag[max_sub_layers_minus1];
		     for(std::size_t i=0; i<max_sub_layers_minus1; i)
		     {
		        //ptl.sub_layer_profile_present_flag[i] = bs.getBits(1);
		    	    buffer.ReadBits(&sub_layer_profile_present_flag[i], 1);
		        //ptl.sub_layer_level_present_flag[i] = bs.getBits(1);
		    	    buffer.ConsumeBits(1);
		     }
		
		     if(max_sub_layers_minus1 > 0)
		     {
		         for(std::size_t i=max_sub_layers_minus1; i<8; i)
		           //bs.getBits(2);
		        	   buffer.ConsumeBits(2);
		     }
		
		
		     for(std::size_t i=0; i<max_sub_layers_minus1; i)
		       {
		         if(sub_layer_profile_present_flag[i])
		         {
		          // ptl.sub_layer_profile_space[i] = bs.getBits(2);
		        	   buffer.ConsumeBits(2);
		           //ptl.sub_layer_tier_flag[i] = bs.getBits(1);
		        	   buffer.ConsumeBits(1);
		        	   //ptl.sub_layer_profile_idc[i] = bs.getBits(5);
		        	   buffer.ConsumeBits(5);
		        	   //ptl.sub_layer_profile_compatibility_flag[i].resize(32);
		
		           for(std::size_t j=0; j<32; j) {
		             //ptl.sub_layer_profile_compatibility_flag[i][j] = bs.getBits(1);
		        	     buffer.ConsumeBits(5);
		           }
		
		           //ptl.sub_layer_progressive_source_flag[i] = bs.getBits(1);
		           buffer.ConsumeBits(1);
		          // ptl.sub_layer_interlaced_source_flag[i] = bs.getBits(1);
		           buffer.ConsumeBits(1);
		           //ptl.sub_layer_non_packed_constraint_flag[i] = bs.getBits(1);
		           buffer.ConsumeBits(1);
		           //ptl.sub_layer_frame_only_constraint_flag[i] = bs.getBits(1);
		           buffer.ConsumeBits(1);
		           //bs.getBits(32);
		           buffer.ConsumeBits(32);
		           //bs.getBits(12);
		           buffer.ConsumeBits(12);
		         }
		
		         if(sub_layer_profile_present_flag[i])
		         {
		           //ptl.sub_layer_level_idc[i] = bs.getBits(8);
		        	 	 buffer.ConsumeBits(8);
		         }
		         else {
		          // ptl.sub_layer_level_idc[i] = 1;
		         }
		
		       }
		
		     buffer.ReadExponentialGolomb(&sps.id);
		
		     uint32_t chroma_format_idc;
		     buffer.ReadExponentialGolomb(&chroma_format_idc);
		
			 if(chroma_format_idc == 3)
			     //psps -> separate_colour_plane_flag = bs.getBits(1);
				 buffer.ConsumeBits(1);
			   else {
			    // psps -> separate_colour_plane_flag = 0;
			   }
		
			 uint32_t width;
			 //psps -> pic_width_in_luma_samples = bs.getGolombU();
		     buffer.ReadExponentialGolomb(&width);
			 //psps -> pic_height_in_luma_samples = bs.getGolombU();
		     uint32_t height;
		     buffer.ReadExponentialGolomb(&height);
		 */
		
		@Override
		protected void parse() {
			readBits(4); //sps_video_parameter_set_id
			int max_sub_layers_minus1 = readBits(3) ; //sps_max_sub_layers_minus1
			readBits(1); //sps_temporal_id_nesting_flag
			
			//processProfileTierLevel - profilePresentFlag exists in SPS by default
			readBits(2); //general_profile_space
			readBits(1); //general_tier_flag
			readBits(5); //general_profile_idc
			
			readBits(32); //general_profile_compatibility_flags
			
			readBits(1); //general_progressive_source_flag
			
			readBits(1); //general_interlaced_source_flag
			readBits(1); //general_non_packed_constraint_flag 
			
			readBits(1); //general_frame_only_constraint_flag
			
			readBits(44); //always skip 44 bits according to configuration
			
			readBits(8); //general_level_idc
			
			int[] sub_layer_profile_present_flag = new int[max_sub_layers_minus1];
			int[] sub_layer_level_present_flag = new int[max_sub_layers_minus1];
			
			for (int i = 0; i < max_sub_layers_minus1; i++) {
				sub_layer_profile_present_flag[i] = readBits(1); // sub_layer_profile_present_flag[i]
				sub_layer_level_present_flag[i] = readBits(1); // sub_layer_level_present_flag[i]
			}
			
			if (max_sub_layers_minus1 > 0) {
				for (int i = max_sub_layers_minus1; i < 8; i++) {
					readBits(2); // skip 2 bits
				}
			}
			
			for (int i = 0; i < max_sub_layers_minus1; i++) 
			{
				if (sub_layer_profile_present_flag[i] == 1) 
				{
					readBits(2); // sub_layer_profile_space[i]
					readBits(1); // sub_layer_tier_flag[i]
					readBits(5); // sub_layer_profile_idc[i]

					for (int j = 0; j < 32; j++) {
						readBits(1); // sub_layer_profile_compatibility_flag[i][j]
					}

					readBits(1); // sub_layer_progressive_source_flag[i]
					readBits(1); // sub_layer_interlaced_source_flag[i]
					readBits(1); // sub_layer_non_packed_constraint_flag[i]
					readBits(1); // sub_layer_frame_only_constraint_flag[i]

					readBits(44); // always skip 44 bits according to configuration
				}
				if (sub_layer_profile_present_flag[i] == 1) {
					readBits(8); // sub_layer_level_idc[i]
				}
			}
			
			readExponentialGolombCode(); //sps.id 
				
			int chroma_format_idc = readExponentialGolombCode(); // chroma_format_idc
			if (chroma_format_idc == 3) { // chroma_format_idc
				readBits(1); // separate_colour_plane_flag
			}
			
			
			
			width = readExponentialGolombCode(); // pic_width_in_luma_samples
			
			height = readExponentialGolombCode(); // pic_height_in_luma_samples	
			
		}
		
	}
	
	@Override
	protected void parse() {
		readBits(8); // configurationVersion
		
		readBits(2); //general_profile_space
		
		readBits(1); // general_tier_flag
		
		readBits(5); // general_profile_idc
		
		readBits(32); // general_profile_compatibility_flags
		
		readBits(48); // general_constraint_indicator_flags
		
		readBits(8); // general_level_idc
		
		readBits(4); // reserved
		
		readBits(12); // min_spatial_segmentation_idc
		
		readBits(6); // reserved
		
		readBits(2); // parallelismType
		
		readBits(6); // reserved
		
		readBits(2); // chromaFormat
		
		readBits(5); // reserved
		
		readBits(3); // bitDepthLumaMinus8
		
		readBits(5); // reserved
		
		readBits(3); // bitDepthChromaMinus8
		
		int averageFrameRate = readBits(16); // avgFrameRate
		
		readBits(2); // constantFrameRate
		
		readBits(3); // numTemporalLayers
		
		readBits(1); // temporalIdNested
		
		int nalUnitLength = readBits(2) + 1; // lengthSizeMinusOne - NAL Unit Length Size
		
		int numOfArrays = readBits(8); // numOfArrays
		
		for (int i = 0; i < numOfArrays; i++) {
			readBits(1); // array_completeness
			readBits(1); // reserved
			int nalUnitType = readBits(6); // NAL_unit_type
			
			
			int numNalus = readBits(16); // numNalus
			
			byte[] nalUnit = null;
			for (int j = 0; j < numNalus; j++) {
                int nalLength = readBits(16); // nalUnitLength
                nalUnit = readByte(nalLength); // nalUnit
			}
			
			if (nalUnitType == NAL_UNIT_TYPE_HEVC_VPS) {
				vps = nalUnit;
			}
			else if (nalUnitType == NAL_UNIT_TYPE_HEVC_SPS) 
			{
				sps = nalUnit;
				logger.info("NAL_UNIT_TYPE_HEVC_SPS: {}", sps);
			} 
			else if (nalUnitType == NAL_UNIT_TYPE_HEVC_PPS) 
			{
				pps = nalUnit;
			}
			
		}
		
		HEVCSPSParser spsParser = new HEVCSPSParser(sps, 2);
		
		width = spsParser.width;
		height = spsParser.height;
		
		logger.info("found width:{} and height:{}", width, height);
		
		
	}

}
