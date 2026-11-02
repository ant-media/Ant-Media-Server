package io.antmedia.rest.model;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Streams matching a search across the applications the user can access")
public record StreamSearchResult(
		@Schema(description = "Matching streams") List<Item> results,
		@Schema(description = "True when more streams match than are returned") boolean truncated) {

	public record Item(
			@Schema(description = "The stream id") String streamId,
			@Schema(description = "The stream name, null if it has none") String name,
			@Schema(description = "The application holding the stream") String appName) {
	}
}
