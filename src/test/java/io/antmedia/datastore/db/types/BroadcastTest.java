package io.antmedia.datastore.db.types;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.test.UnitTestBase;

@Tag("fast")
class BroadcastTest extends UnitTestBase<Broadcast> {

	@Test
	void olderRowsWantWhatBootResumeStarted() {
		assertThat(broadcastOf(AntMediaApplicationAdapter.STREAM_SOURCE, false).getFetcherDesiredAction()).isEqualTo(Broadcast.FETCHER_DESIRED_ACTION_START);
		assertThat(broadcastOf(AntMediaApplicationAdapter.IP_CAMERA, false).getFetcherDesiredAction()).isEqualTo(Broadcast.FETCHER_DESIRED_ACTION_START);
		assertThat(broadcastOf(AntMediaApplicationAdapter.STREAM_SOURCE, true).getFetcherDesiredAction()).isEqualTo(Broadcast.FETCHER_DESIRED_ACTION_STOP);
		assertThat(broadcastOf(AntMediaApplicationAdapter.LIVE_STREAM, false).getFetcherDesiredAction()).isEqualTo(Broadcast.FETCHER_DESIRED_ACTION_STOP);
		assertThat(broadcastOf(AntMediaApplicationAdapter.PLAY_LIST, false).getFetcherDesiredAction()).isEqualTo(Broadcast.FETCHER_DESIRED_ACTION_STOP);

		Broadcast stopped = broadcastOf(AntMediaApplicationAdapter.STREAM_SOURCE, false);
		stopped.setFetcherDesiredAction(Broadcast.FETCHER_DESIRED_ACTION_STOP);
		assertThat(stopped.getFetcherDesiredAction()).isEqualTo(Broadcast.FETCHER_DESIRED_ACTION_STOP);
	}

	@Test
	void restReadsTheFetcherFieldsButNeverWritesThem() throws Exception {
		ObjectMapper mapper = new ObjectMapper();

		Broadcast broadcast = mapper.readValue("{\"streamId\":\"s1\",\"type\":\"streamSource\",\"fetcherDesiredAction\":\"stop\","
				+ "\"fetcherNode\":\"10.0.0.1\",\"fetcherFailures\":3}", Broadcast.class);
		assertThat(broadcast.getStreamId()).isEqualTo("s1");
		//stop was dropped, so a stream source falls back to its default
		assertThat(broadcast.getFetcherDesiredAction()).isEqualTo(Broadcast.FETCHER_DESIRED_ACTION_START);
		assertThat(broadcast.getFetcherNode()).isNull();
		assertThat(broadcast.getFetcherFailures()).isZero();

		broadcast.setFetcherNode("10.0.0.2");
		broadcast.setFetcherFailures(2);
		assertThat(mapper.writeValueAsString(broadcast))
				.contains("\"fetcherDesiredAction\":\"start\"", "\"fetcherNode\":\"10.0.0.2\"", "\"fetcherFailures\":2");

		BroadcastUpdate update = mapper.readValue("{\"name\":\"n\",\"fetcherDesiredAction\":\"start\",\"fetcherFailures\":3}", BroadcastUpdate.class);
		assertThat(update.getName()).isEqualTo("n");
		assertThat(update.getFetcherDesiredAction()).isNull();
		assertThat(update.getFetcherFailures()).isNull();

		//a client that sends back the whole broadcast it read, every field like Gson writes it, gets no error and changes none of them
		broadcast.setFetcherDesiredAction(Broadcast.FETCHER_DESIRED_ACTION_STOP);
		BroadcastUpdate whole = mapper.readValue(new Gson().toJson(broadcast), BroadcastUpdate.class);
		assertThat(whole.getStreamId()).isEqualTo("s1");
		assertThat(whole.getFetcherDesiredAction()).isNull();
		assertThat(whole.getFetcherFailures()).isNull();
	}

	@Test
	void restWritesThePreferredFetcherNode() throws Exception {
		ObjectMapper mapper = new ObjectMapper();

		assertThat(mapper.readValue("{\"streamId\":\"s1\",\"preferredFetcherNode\":\"10.0.0.1\"}", Broadcast.class).getPreferredFetcherNode()).isEqualTo("10.0.0.1");
		//a body without it, as every client sent before the field existed
		assertThat(mapper.readValue("{\"streamId\":\"s1\"}", Broadcast.class).getPreferredFetcherNode()).isNull();

		//on an update, missing or null leaves it as it is, empty clears it
		assertThat(mapper.readValue("{\"name\":\"n\"}", BroadcastUpdate.class).getPreferredFetcherNode()).isNull();
		assertThat(mapper.readValue("{\"preferredFetcherNode\":null}", BroadcastUpdate.class).getPreferredFetcherNode()).isNull();
		assertThat(mapper.readValue("{\"preferredFetcherNode\":\"\"}", BroadcastUpdate.class).getPreferredFetcherNode()).isEmpty();

		//a client that sends back the whole broadcast it read keeps it as it was
		Broadcast read = new Broadcast();
		read.setPreferredFetcherNode("10.0.0.1");
		assertThat(mapper.readValue(new Gson().toJson(read), BroadcastUpdate.class).getPreferredFetcherNode()).isEqualTo("10.0.0.1");
	}

	private static Broadcast broadcastOf(String type, boolean autoStartStopEnabled) {
		Broadcast broadcast = new Broadcast();
		broadcast.setType(type);
		broadcast.setAutoStartStopEnabled(autoStartStopEnabled);
		return broadcast;
	}
}
