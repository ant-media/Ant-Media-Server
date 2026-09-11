package io.antmedia.rest;

import static org.assertj.core.api.Assertions.assertThat;
import io.antmedia.AppSettings;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.ndi.NdiSourceProvider;
import io.antmedia.rest.model.Result;
import io.antmedia.settings.ServerSettings;
import io.antmedia.statistic.IStatsCollector;
import io.antmedia.test.UnitTestBase;
import java.lang.reflect.Proxy;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.red5.server.scope.Scope;
import org.springframework.context.support.GenericApplicationContext;

@Tag("fast")
class RestServiceBaseNdiTest extends UnitTestBase<RestServiceBase> {

	private InMemoryDataStore dataStore;
	private TestNdiSourceProvider ndiSourceProvider;
	private Scope scope;
	private GenericApplicationContext applicationContext;

	@BeforeEach
	void setUp() {
		classUnderTest = new RestServiceBase() { };
		dataStore = new InMemoryDataStore("ndi-create-test");
		ndiSourceProvider = new TestNdiSourceProvider();
		scope = new Scope();
		scope.setName("live");
		applicationContext = new GenericApplicationContext();
		applicationContext.registerBean(NdiSourceProvider.class, () -> ndiSourceProvider);
		registerStatsCollector(applicationContext);
		applicationContext.refresh();

		classUnderTest.setAppCtx(applicationContext);
		classUnderTest.setAppSettings(new AppSettings());
		classUnderTest.setServerSettings(new ServerSettings());
		classUnderTest.setDataStore(dataStore);
		classUnderTest.setScope(scope);
	}

	@AfterEach
	void tearDown() {
		applicationContext.close();
	}

	@Test
	void startsExplicitNdiSourceAndKeepsBroadcast() throws Exception {
		Broadcast broadcast = ndiBroadcast("ndi-stream");
		ndiSourceProvider.startResult = true;

		Result result = classUnderTest.addStreamSource(broadcast);

		assertThat(result.isSuccess()).isTrue();
		assertThat(result.getDataId()).isEqualTo("ndi-stream");
		assertThat(dataStore.get("ndi-stream"))
				.isNotNull()
				.extracting(Broadcast::getName, Broadcast::getMetaData)
				.containsExactly("JANTHINK (Test Pattern)", IAntMediaStreamHandler.PUBLISH_TYPE_NDI);
		assertThat(ndiSourceProvider.startedSourceName).isEqualTo("JANTHINK (Test Pattern)");
		assertThat(ndiSourceProvider.startedStreamId).isEqualTo("ndi-stream");
		assertThat(ndiSourceProvider.startedScope).isSameAs(scope);
	}

	@Test
	void keepsBroadcastWhenNdiSourceCannotStart() throws Exception {
		Broadcast broadcast = ndiBroadcast("missing-ndi-stream");

		Result result = classUnderTest.addStreamSource(broadcast);

		assertThat(result.isSuccess()).isTrue();
		assertThat(result.getDataId()).isEqualTo("missing-ndi-stream");
		assertThat(result.getMessage()).contains("saved").contains("not available or is already running");
		assertThat(dataStore.get("missing-ndi-stream")).isNotNull();
	}

	@Test
	void rejectsNdiBroadcastWithoutSourceName() throws Exception {
		Broadcast broadcast = ndiBroadcast("unnamed-ndi-stream");
		broadcast.setStreamUrl(null);

		Result result = classUnderTest.addStreamSource(broadcast);

		assertThat(result.isSuccess()).isFalse();
		assertThat(result.getMessage()).isEqualTo("NDI source name is not defined.");
	}

	@Test
	void rejectsNdiBroadcastWhenProviderIsUnavailable() throws Exception {
		applicationContext.close();
		applicationContext = new GenericApplicationContext();
		registerStatsCollector(applicationContext);
		applicationContext.refresh();
		classUnderTest.setAppCtx(applicationContext);

		Result result = classUnderTest.addStreamSource(ndiBroadcast("unsupported-ndi-stream"));

		assertThat(result.isSuccess()).isFalse();
		assertThat(result.getMessage()).isEqualTo("NDI support is not available.");
	}

	@Test
	void startsSavedNdiBroadcastFromStartEndpoint() throws Exception {
		Broadcast broadcast = ndiBroadcast("saved-ndi-stream");
		dataStore.save(broadcast);
		ndiSourceProvider.startResult = true;

		Result result = classUnderTest.startStreamSource("saved-ndi-stream");

		assertThat(result.isSuccess()).isTrue();
		assertThat(result.getDataId()).isEqualTo("saved-ndi-stream");
		assertThat(ndiSourceProvider.startedSourceName).isEqualTo("JANTHINK (Test Pattern)");
		assertThat(ndiSourceProvider.startedStreamId).isEqualTo("saved-ndi-stream");
		assertThat(ndiSourceProvider.startedScope).isSameAs(scope);
	}

	private static Broadcast ndiBroadcast(String streamId) throws Exception {
		Broadcast broadcast = new Broadcast();
		broadcast.setStreamId(streamId);
		broadcast.setType(IAntMediaStreamHandler.PUBLISH_TYPE_NDI);
		broadcast.setStreamUrl("JANTHINK (Test Pattern)");
		return broadcast;
	}

	private static void registerStatsCollector(GenericApplicationContext context) {
		IStatsCollector statsCollector = (IStatsCollector) Proxy.newProxyInstance(
				IStatsCollector.class.getClassLoader(), new Class<?>[] {IStatsCollector.class},
				(proxy, method, args) -> "enoughResource".equals(method.getName()));
		context.getBeanFactory().registerSingleton(IStatsCollector.BEAN_NAME, statsCollector);
	}

	private static class TestNdiSourceProvider implements NdiSourceProvider {

		private boolean startResult;
		private String startedSourceName;
		private String startedStreamId;
		private Scope startedScope;

		@Override
		public List<String> getNdiSources() {
			return List.of();
		}

		@Override
		public boolean startNdiSource(String sourceName, String streamId, org.red5.server.api.scope.IScope scope) {
			startedSourceName = sourceName;
			startedStreamId = streamId;
			startedScope = (Scope) scope;
			return startResult;
		}
	}
}
