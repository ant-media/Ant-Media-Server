package io.antmedia.test.console;


import org.junit.jupiter.api.Tag;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.antmedia.AppSettings;
import io.antmedia.cluster.ClusterNode;
import io.antmedia.cluster.IClusterStore;
import io.antmedia.console.rest.ClusterRestServiceV2;
import io.antmedia.rest.BroadcastRestService.SimpleStat;
import io.antmedia.rest.model.Result;

@Tag("fast")
public class ClusterRestServiceV2Test {
	
	
	private ClusterRestServiceV2 restService;
	private IClusterStore clusterStore;

	@BeforeEach
	public void before() 
	{
		restService = Mockito.spy(new ClusterRestServiceV2());
		
		
	}
	
	@Test
	public void testClusterRestServicesNoStore() 
	{
		Mockito.doReturn(null).when(restService).getClusterStore();
		
		SimpleStat nodeCount = restService.getNodeCount();
		assertEquals(-1, nodeCount.number);
		
		
		List<ClusterNode> nodeList = restService.getNodeList(0, 10);
		assertEquals(0, nodeList.size());
		
		
		Result deleteNode = restService.deleteNode("any_id");
		assertFalse(deleteNode.isSuccess());
		
	}
	
	@Test
	public void testClusterRestServices() 
	{
		clusterStore = Mockito.mock(IClusterStore.class);
		Mockito.doReturn(clusterStore).when(restService).getClusterStore();
		
		
		Mockito.when(clusterStore.getNodeCount()).thenReturn(999999l);
		SimpleStat nodeCount = restService.getNodeCount();
		assertEquals(999999l, nodeCount.number);
		
		Mockito.when(clusterStore.getClusterNodes(0, 100)).thenReturn(Arrays.asList(new ClusterNode()));
		List<ClusterNode> nodeList = restService.getNodeList(0, 100);
		assertEquals(1, nodeList.size());
		
		Mockito.when(clusterStore.deleteNode("any_id")).thenReturn(true);
		Result deleteNode = restService.deleteNode("any_id");
		assertTrue(deleteNode.isSuccess());
		
	}

}
