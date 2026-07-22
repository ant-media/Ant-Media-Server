package io.antmedia.test.console;


import org.junit.jupiter.api.Tag;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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

	@Test
	public void testClusterNode()
	{
		ClusterNode node = new ClusterNode("192.168.1.1", "node-1");
		assertEquals("192.168.1.1", node.getIp());
		assertEquals("node-1", node.getId());
		assertNull(node.getNote());

		node.setId("node-2");
		node.setIp("10.0.0.5");
		node.setMemory("2048");
		node.setCpu("50");
		node.setDbQueryAveargeTimeMs(7);
		node.setLastUpdateTime(12345L);
		node.setNote("origin node in eu-west-1");

		assertEquals("node-2", node.getId());
		assertEquals("10.0.0.5", node.getIp());
		assertEquals("2048", node.getMemory());
		assertEquals("50", node.getCpu());
		assertEquals(7, node.getDbQueryAveargeTimeMs());
		assertEquals(12345L, node.getLastUpdateTime());
		assertEquals("origin node in eu-west-1", node.getNote());

		// status is derived from how stale the last heartbeat is
		node.setLastUpdateTime(System.currentTimeMillis());
		assertEquals(ClusterNode.ALIVE, node.getStatus());

		node.setLastUpdateTime(System.currentTimeMillis() - ClusterNode.NODE_UPDATE_PERIOD * 5);
		assertEquals(ClusterNode.DEAD, node.getStatus());
	}

}
