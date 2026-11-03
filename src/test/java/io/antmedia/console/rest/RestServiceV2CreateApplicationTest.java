package io.antmedia.console.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.List;

import io.antmedia.rest.model.Result;
import io.antmedia.test.UnitTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("fast")
class RestServiceV2CreateApplicationTest extends UnitTestBase<RestServiceV2> {

	@BeforeEach
	void setUp() {
		classUnderTest = spy(new RestServiceV2());
	}

	@Test
	void rejectsTheClassicPanelFolderName() {
		Result result = classUnderTest.createApplication("classic-panel", null);

		assertThat(result.isSuccess()).isFalse();
		assertThat(result.getMessage()).isEqualTo("Application name classic-panel is reserved");
		verify(classUnderTest, never()).isApplicationExists(anyString());
	}

	@Test
	void rejectsTheOtherRootWebappPaths() {
		assertThat(List.of("root", "rest", "metrics", "assets", "images"))
				.allSatisfy(name -> assertThat(classUnderTest.createApplication(name, null).getMessage())
						.isEqualTo("Application name " + name + " is reserved"));
		verify(classUnderTest, never()).isApplicationExists(anyString());
	}

	@Test
	void reservationMatchesTheExactNameOnly() {
		doReturn(true).when(classUnderTest).isApplicationExists("Classic-Panel");

		Result result = classUnderTest.createApplication("Classic-Panel", null);

		assertThat(result.getMessage()).isEqualTo("Application with the same name already exists");
	}
}
