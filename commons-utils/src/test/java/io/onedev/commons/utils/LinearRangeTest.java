package io.onedev.commons.utils;

import org.junit.Assert;
import org.junit.Test;

import io.onedev.commons.utils.LinearRange;

public class LinearRangeTest {

	@Test
	public void test() {
		LinearRange range = LinearRange.match("hello world", "hello wo");
		Assert.assertEquals(0, range.getFrom());
		Assert.assertEquals(8, range.getTo());

		range = LinearRange.match(" hello world", "hello wo");
		Assert.assertEquals(1, range.getFrom());
		Assert.assertEquals(9, range.getTo());
		
		range = LinearRange.match(" hello world", "ellowo");
		Assert.assertEquals(null, range);
		
		range = LinearRange.match(" hello world", "ello wo");
		Assert.assertEquals(2, range.getFrom());
		Assert.assertEquals(9, range.getTo());
	}

}
