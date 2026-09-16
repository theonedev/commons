package io.onedev.commons.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LinearRangeTest {

	@Test
	public void test() {
		LinearRange range = LinearRange.match("hello world", "hello wo");
		Assertions.assertEquals(0, range.getFrom());
		Assertions.assertEquals(8, range.getTo());

		range = LinearRange.match(" hello world", "hello wo");
		Assertions.assertEquals(1, range.getFrom());
		Assertions.assertEquals(9, range.getTo());
		
		range = LinearRange.match(" hello world", "ellowo");
		Assertions.assertEquals(null, range);
		
		range = LinearRange.match(" hello world", "ello wo");
		Assertions.assertEquals(2, range.getFrom());
		Assertions.assertEquals(9, range.getTo());
	}

}
