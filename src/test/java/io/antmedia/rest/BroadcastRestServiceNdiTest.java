package io.antmedia.rest;

import static org.assertj.core.api.Assertions.assertThat;

import io.antmedia.ndi.NdiSourceProvider;
import io.antmedia.test.UnitTestBase;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.red5.server.api.scope.IScope;
import org.springframework.context.support.GenericApplicationContext;

@Tag("fast")
class BroadcastRestServiceNdiTest extends UnitTestBase<BroadcastRestService> {

	private GenericApplicationContext applicationContext;

	@BeforeEach
	void setUp() {
		classUnderTest = new BroadcastRestService();
		applicationContext = new GenericApplicationContext();
	}

	@AfterEach
	void tearDown() {
		applicationContext.close();
	}

	@Test
	void returnsDiscoveredNdiSources() {
		applicationContext.registerBean(NdiSourceProvider.class,
				() -> new TestNdiSourceProvider(List.of("Camera A", "Camera B")));
		applicationContext.refresh();
		classUnderTest.setAppCtx(applicationContext);

		assertThat(classUnderTest.getNdiSources()).containsExactly("Camera A", "Camera B");
	}

	@Test
	void returnsEmptyListWithoutApplicationContext() {
		assertThat(classUnderTest.getNdiSources()).isEmpty();
	}

	@Test
	void returnsEmptyListWithoutNdiProvider() {
		applicationContext.refresh();
		classUnderTest.setAppCtx(applicationContext);

		assertThat(classUnderTest.getNdiSources()).isEmpty();
	}

	private record TestNdiSourceProvider(List<String> sources) implements NdiSourceProvider {

		@Override
		public List<String> getNdiSources() {
			return sources;
		}

		@Override
		public boolean startNdiSource(String sourceName, String streamId, IScope scope) {
			return false;
		}
	}
}
