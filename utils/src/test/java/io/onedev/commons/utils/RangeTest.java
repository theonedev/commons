package io.onedev.commons.utils;

import org.junit.Assert;
import org.junit.Test;

import io.onedev.commons.utils.Range;

public class RangeTest {

	@Test
	public void test() {
		Range range = Range.match("hello world", "hellowo", true, true, true);
		Assert.assertEquals(0, range.getFrom());
		Assert.assertEquals(8, range.getTo());

		range = Range.match(" hello world", "hellowo", true, true, true);
		Assert.assertEquals(1, range.getFrom());
		Assert.assertEquals(9, range.getTo());
		
		range = Range.match(" hello world", "ellowo", true, true, true);
		Assert.assertEquals(null, range);
		
		range = Range.match(" hello world", "ellowo", false, true, true);
		Assert.assertEquals(2, range.getFrom());
		Assert.assertEquals(9, range.getTo());
	}

}
