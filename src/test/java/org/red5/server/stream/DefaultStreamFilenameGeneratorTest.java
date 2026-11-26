package org.red5.server.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.scope.ScopeType;
import org.red5.server.api.stream.IStreamFilenameGenerator.GenerationType;

import io.antmedia.test.UnitTestBase;

@Tag("fast")
class DefaultStreamFilenameGeneratorTest extends UnitTestBase<DefaultStreamFilenameGenerator> {

	@TempDir
	Path tempDirectory;

	@ParameterizedTest
	@EnumSource(value = GenerationType.class, names = {"RECORD", "APPEND"})
	void shouldPreserveApplicationAndRecordingSubfolders(GenerationType type) {
		IScope application = mock(IScope.class);
		IScope room = childScope(application, "room");
		IScope cameraGroup = childScope(room, "cameras");
		DefaultStreamFilenameGenerator generator = new DefaultStreamFilenameGenerator();

		String filename = generator.generateFilename(cameraGroup, "camera-1.v2", ".mp4", type, "archive/2026");

		assertThat(filename).isEqualTo("streams/room/cameras///archive/2026/camera-1.v2.mp4");
	}

	@ParameterizedTest
	@MethodSource("unsafeRecordingNames")
	void shouldRejectTraversalBeforeAnyResourceLookup(String name, GenerationType type) {
		DefaultStreamFilenameGenerator generator = new DefaultStreamFilenameGenerator();

		assertThatThrownBy(() -> generator.generateFilename(mock(IScope.class), name, ".flv", type, null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	static Stream<Arguments> unsafeRecordingNames() {
		return Stream.of("../outside", "..//..//OtherApp/streams/victim",
				"..//..//..//conf/red5.properties|x", "../streams/victim", "room/../victim")
				.flatMap(name -> Stream.of(GenerationType.RECORD, GenerationType.APPEND)
						.map(type -> Arguments.of(name, type)));
	}

	@ParameterizedTest
	@EnumSource(value = GenerationType.class, names = {"RECORD", "APPEND"})
	void shouldRejectTraversalThroughAConnectionScope(GenerationType type) {
		DefaultStreamFilenameGenerator generator = new DefaultStreamFilenameGenerator();
		IScope scope = childScope(mock(IScope.class), "..");

		assertThatThrownBy(() -> generator.generateFilename(scope, "camera", ".m3u8", type, null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void shouldPreservePlaybackFilenameGeneration() {
		DefaultStreamFilenameGenerator generator = new DefaultStreamFilenameGenerator();

		assertThat(generator.generateFilename(mock(IScope.class), "../streams/movie.mp4", GenerationType.PLAYBACK))
				.isEqualTo("streams//../streams/movie.mp4");
	}

	@Test
	void shouldPreserveSymlinkedRecordingStorage() throws Exception {
		Path application = Files.createDirectory(tempDirectory.resolve("application"));
		Path storage = Files.createDirectory(tempDirectory.resolve("recording-storage"));
		Files.createSymbolicLink(application.resolve("streams"), storage);
		DefaultStreamFilenameGenerator generator = new DefaultStreamFilenameGenerator();

		String filename = generator.generateFilename(mock(IScope.class), "camera.v1", ".mp4",
				GenerationType.RECORD, "archive/2026");
		Path recording = application.resolve(filename);
		Files.createDirectories(recording.getParent());
		Files.writeString(recording, "recording");

		assertThat(Files.readString(storage.resolve("archive/2026/camera.v1.mp4"))).isEqualTo("recording");
	}

	private IScope childScope(IScope parent, String name) {
		IScope scope = mock(IScope.class);
		when(scope.hasParent()).thenReturn(true);
		when(scope.getParent()).thenReturn(parent);
		when(scope.getType()).thenReturn(ScopeType.ROOM);
		when(scope.getName()).thenReturn(name);
		return scope;
	}
}
