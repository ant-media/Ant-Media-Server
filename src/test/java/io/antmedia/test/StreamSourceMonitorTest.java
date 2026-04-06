package io.antmedia.test;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.cluster.ClusterNode;
import io.antmedia.cluster.IClusterNotifier;
import io.antmedia.cluster.IClusterStore;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.rest.model.Result;
import io.antmedia.settings.ServerSettings;
import io.antmedia.streamsource.StreamSourceMonitor;
import io.vertx.core.Vertx;

public class StreamSourceMonitorTest {

	@Test
	public void testConstructorSchedulesPeriodicAndStopCancelsTimer() {
		Vertx vertx = mock(Vertx.class);
		AntMediaApplicationAdapter app = mock(AntMediaApplicationAdapter.class);
		IClusterNotifier clusterNotifier = mock(IClusterNotifier.class);

		when(app.getVertx()).thenReturn(vertx);
		when(vertx.setPeriodic(eq(10000L), any())).thenReturn(77L);

		StreamSourceMonitor monitor = new StreamSourceMonitor(app, clusterNotifier);
		monitor.stop();

		verify(vertx).setPeriodic(eq(10000L), any());
		verify(vertx).cancelTimer(77L);
	}

	@Test
	public void testMonitorSystemStatusPromotesToMasterWhenNoMaster() throws Exception {
		String thisNodeIp = "10.0.0.1";

		Vertx vertx = mock(Vertx.class);
		ServerSettings serverSettings = mock(ServerSettings.class);
		AntMediaApplicationAdapter app = mock(AntMediaApplicationAdapter.class);
		IClusterNotifier clusterNotifier = mock(IClusterNotifier.class);
		IClusterStore clusterStore = mock(IClusterStore.class);
		when(clusterNotifier.getClusterStore()).thenReturn(clusterStore);
		DataStore dataStore = mock(DataStore.class);
		ClusterNode thisNode = mock(ClusterNode.class);
		ClusterNode liveNode = mock(ClusterNode.class);

		when(app.getVertx()).thenReturn(vertx);
		when(app.getServerSettings()).thenReturn(serverSettings);
		when(app.getDataStore()).thenReturn(dataStore);
		when(serverSettings.getHostAddress()).thenReturn(thisNodeIp);
		when(vertx.setPeriodic(eq(10000L), any())).thenReturn(1L);

		when(clusterStore.getMasterNode()).thenReturn(null);
		when(clusterStore.getClusterNodeFromIP(thisNodeIp)).thenReturn(thisNode);
		when(clusterStore.getClusterNodes(0, 1000)).thenReturn(Collections.singletonList(liveNode));
		when(liveNode.getStatus()).thenReturn("alive");
		when(liveNode.getIp()).thenReturn("10.0.0.2");

		StreamSourceMonitor monitor = new StreamSourceMonitor(app, clusterNotifier);
		monitor.monitorStreamSources();

		verify(clusterStore).promoteToMaster(thisNode, null);
		verify(clusterStore).getClusterNodes(0, 1000);
	}

	@Test
	public void testMonitorSystemStatusDoesNothingWhenAnotherLiveMasterExists() throws Exception {
		String thisNodeIp = "10.0.0.1";

		Vertx vertx = mock(Vertx.class);
		ServerSettings serverSettings = mock(ServerSettings.class);
		AntMediaApplicationAdapter app = mock(AntMediaApplicationAdapter.class);
		
		IClusterNotifier clusterNotifier = mock(IClusterNotifier.class);
		IClusterStore clusterStore = mock(IClusterStore.class);
		when(clusterNotifier.getClusterStore()).thenReturn(clusterStore);		
		DataStore dataStore = mock(DataStore.class);
		ClusterNode masterNode = mock(ClusterNode.class);

		when(app.getVertx()).thenReturn(vertx);
		when(app.getServerSettings()).thenReturn(serverSettings);
		when(app.getDataStore()).thenReturn(dataStore);
		when(serverSettings.getHostAddress()).thenReturn(thisNodeIp);
		when(vertx.setPeriodic(eq(10000L), any())).thenReturn(1L);

		when(clusterStore.getMasterNode()).thenReturn(masterNode);
		when(masterNode.getIp()).thenReturn("10.0.0.99");
		when(masterNode.getStatus()).thenReturn("alive");

		StreamSourceMonitor monitor = new StreamSourceMonitor(app, clusterNotifier);
		monitor.monitorStreamSources();

		verify(clusterStore, never()).promoteToMaster(any(), any());
		verify(clusterStore, never()).getClusterNodes(any(Integer.class), any(Integer.class));
		verify(app, never()).startStreaming(any(Broadcast.class));
	}

	@Test
	public void testMonitorSystemStatusTransferToLiveNodesRoundRobin() throws Exception {
		String thisNodeIp = "10.0.0.1";
		String deadNodeIp = "10.0.0.50";
		String liveNodeIp1 = "10.0.0.11";
		String liveNodeIp2 = "10.0.0.12";

		Vertx vertx = mock(Vertx.class);
		ServerSettings serverSettings = mock(ServerSettings.class);
		AntMediaApplicationAdapter app = mock(AntMediaApplicationAdapter.class);
		IClusterNotifier clusterNotifier = mock(IClusterNotifier.class);
		IClusterStore clusterStore = mock(IClusterStore.class);
		when(clusterNotifier.getClusterStore()).thenReturn(clusterStore);	
		DataStore dataStore = new InMemoryDataStore("test");
		ClusterNode masterNode = mock(ClusterNode.class);
		ClusterNode deadNode = mock(ClusterNode.class);
		ClusterNode liveNode1 = mock(ClusterNode.class);
		ClusterNode liveNode2 = mock(ClusterNode.class);

		when(app.getVertx()).thenReturn(vertx);
		when(app.getServerSettings()).thenReturn(serverSettings);
		when(app.startStreaming(any())).thenReturn(new Result(true));
		when(app.getDataStore()).thenReturn(dataStore);
		when(serverSettings.getHostAddress()).thenReturn(thisNodeIp);
		when(vertx.setPeriodic(eq(10000L), any())).thenReturn(1L);

		when(clusterStore.getMasterNode()).thenReturn(masterNode);
		when(masterNode.getIp()).thenReturn(thisNodeIp);
		when(clusterStore.getClusterNodes(0, 1000)).thenReturn(Arrays.asList(deadNode, liveNode1, liveNode2));
		when(deadNode.getStatus()).thenReturn(ClusterNode.DEAD);
		when(deadNode.getIp()).thenReturn(deadNodeIp);
		when(liveNode1.getStatus()).thenReturn("alive");
		when(liveNode1.getIp()).thenReturn(liveNodeIp1);
		when(liveNode2.getStatus()).thenReturn("alive");
		when(liveNode2.getIp()).thenReturn(liveNodeIp2);

		Broadcast unExpectedlyTerminated1 = new Broadcast();
		unExpectedlyTerminated1.setStreamId("s1");
		unExpectedlyTerminated1.setType(AntMediaApplicationAdapter.STREAM_SOURCE);
		dataStore.save(unExpectedlyTerminated1);
		dataStore.updateStatus(unExpectedlyTerminated1.getStreamId(), AntMediaApplicationAdapter.BROADCAST_STATUS_TERMINATED_UNEXPECTEDLY);

		Broadcast unExpectedlyTerminated2 = new Broadcast();
		unExpectedlyTerminated2.setStreamId("s2");
		unExpectedlyTerminated2.setType(AntMediaApplicationAdapter.STREAM_SOURCE);
		dataStore.save(unExpectedlyTerminated2);
		dataStore.updateStatus(unExpectedlyTerminated2.getStreamId(), AntMediaApplicationAdapter.BROADCAST_STATUS_TERMINATED_UNEXPECTEDLY);


		Broadcast nonSource = new Broadcast();
		nonSource.setStreamId("s3");
		nonSource.setType(AntMediaApplicationAdapter.LIVE_STREAM);
		dataStore.save(nonSource);
		dataStore.updateStatus(nonSource.getStreamId(), AntMediaApplicationAdapter.BROADCAST_STATUS_TERMINATED_UNEXPECTEDLY);


		Broadcast unExpectedlyTerminated3 = new Broadcast();
		unExpectedlyTerminated3.setStreamId("s4");
		unExpectedlyTerminated3.setType(AntMediaApplicationAdapter.STREAM_SOURCE);
		dataStore.save(unExpectedlyTerminated3);
		dataStore.updateStatus(unExpectedlyTerminated3.getStreamId(), AntMediaApplicationAdapter.BROADCAST_STATUS_TERMINATED_UNEXPECTEDLY);


		List<Broadcast> externalStreams = dataStore.getExternalStreamsList();


		StreamSourceMonitor monitor = new StreamSourceMonitor(app, clusterNotifier);
		monitor.monitorStreamSources();

		assertEquals(liveNodeIp1, unExpectedlyTerminated1.getOriginAdress());
		assertEquals(liveNodeIp2, unExpectedlyTerminated2.getOriginAdress());
		assertEquals(liveNodeIp1, unExpectedlyTerminated3.getOriginAdress());

		verify(app, times(3)).startStreaming(any(Broadcast.class));
		verify(app).startStreaming(unExpectedlyTerminated1);
		verify(app).startStreaming(unExpectedlyTerminated2);
		verify(app).startStreaming(unExpectedlyTerminated3);
	}
}
