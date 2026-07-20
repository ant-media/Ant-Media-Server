package io.antmedia.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.rest.model.Result;
import io.antmedia.test.UnitTestBase;

@Tag("fast")
class VoDRestServiceRescanTest extends UnitTestBase<VoDRestService> {

	@Test
	void testRescanEndpointDelegatesToApplication() {
		AntMediaApplicationAdapter application = mock(AntMediaApplicationAdapter.class);
		Result expected = new Result(true, "scan complete");
		when(application.rescanVodAssets()).thenReturn(expected);

		VoDRestService service = new VoDRestService();
		service.setApplication(application);

		assertThat(service.rescanVodAssets()).isSameAs(expected);
		verify(application).rescanVodAssets();
	}
}
