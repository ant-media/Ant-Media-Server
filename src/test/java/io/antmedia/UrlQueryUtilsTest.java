package io.antmedia;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.antmedia.test.UnitTestBase;

@Tag("fast")
class UrlQueryUtilsTest extends UnitTestBase<UrlQueryUtils> {

	@Test
	void returnsEveryRawValueOfTheKeyInOrder() {
		assertThat(UrlQueryUtils.getParamValues("", "a")).isEmpty();
		assertThat(UrlQueryUtils.getParamValues("b=1&ab=2", "a")).isEmpty();
		assertThat(UrlQueryUtils.getParamValues("a=1&b=2", "b")).containsExactly("2");
		assertThat(UrlQueryUtils.getParamValues("a=1&b=2&a=3", "a")).containsExactly("1", "3");
		assertThat(UrlQueryUtils.getParamValues("nodelay&a=", "nodelay")).containsExactly("");
		assertThat(UrlQueryUtils.getParamValues("nodelay&a=", "a")).containsExactly("");
		assertThat(UrlQueryUtils.getParamValues("a=x=y", "a")).containsExactly("x=y");
		assertThat(UrlQueryUtils.getParamValues("a=video%2Baudio", "a")).containsExactly("video%2Baudio");
	}

	@Test
	void removesEveryParamOfTheKeyAndKeepsTheRestAsWritten() {
		assertThat(UrlQueryUtils.removeParam("a=1", "a")).isEmpty();
		assertThat(UrlQueryUtils.removeParam("ab=1", "a")).isEqualTo("ab=1");
		assertThat(UrlQueryUtils.removeParam("c=3&a=1&b=2&a", "a")).isEqualTo("c=3&b=2");
		assertThat(UrlQueryUtils.removeParam("b=%2B&a=1&c=u@w", "a")).isEqualTo("b=%2B&c=u@w");
		//same as the loops it replaced: an empty param in front stays, a trailing '&' is dropped by split
		assertThat(UrlQueryUtils.removeParam("&b=2&", "a")).isEqualTo("&b=2");
	}
}
