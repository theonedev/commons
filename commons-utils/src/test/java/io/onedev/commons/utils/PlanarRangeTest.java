package io.onedev.commons.utils;

import static org.junit.Assert.*;

import org.junit.Test;

public class PlanarRangeTest {

	@Test
	public void test() {
		PlanarRange range = new PlanarRange("1.2-3.4-8");
		assertEquals(0, range.getFromRow());
		assertEquals(4, range.getToColumn());
		assertEquals(8, range.getTabWidth());
		
		range = new PlanarRange("1.2-3.4");
		assertEquals(0, range.getFromRow());
		assertEquals(4, range.getToColumn());
		assertEquals(1, range.getTabWidth());
	}

}
