package io.onedev.commons.utils;

import static org.junit.Assert.*;

import org.junit.Test;

public class PlanarRangeTest {

	@Test
	public void test() {
		PlanarRange range = new PlanarRange("1.2-3.4-8");
		assertEquals(0, range.getFromRow());
		assertEquals(3, range.getToColumn());
		assertEquals(8, range.getTabWidth());
		
		range = new PlanarRange("1.2-3.4");
		assertEquals(0, range.getFromRow());
		assertEquals(3, range.getToColumn());
		assertEquals(1, range.getTabWidth());
	}

	@Test
	public void testHighlight() {
		// Test single line highlighting
		java.util.List<String> lines = new java.util.ArrayList<>();
		lines.add("Hello world");
		PlanarRange range = new PlanarRange(0, 0, 0, 5);
		range.highlight(lines);
		assertEquals("<!HIGHLIGHT_BEGIN!>Hello<!HIGHLIGHT_END!> world", lines.get(0));
		
		// Test highlighting entire line
		lines = new java.util.ArrayList<>();
		lines.add("Hello world");
		range = new PlanarRange(0, 0, 0, 11);
		range.highlight(lines);
		assertEquals("<!HIGHLIGHT_BEGIN!>Hello world<!HIGHLIGHT_END!>", lines.get(0));
		
		// Test highlighting at end of line
		lines = new java.util.ArrayList<>();
		lines.add("Hello world");
		range = new PlanarRange(0, 6, 0, 11);
		range.highlight(lines);
		assertEquals("Hello <!HIGHLIGHT_BEGIN!>world<!HIGHLIGHT_END!>", lines.get(0));
		
		// Test multi-line highlighting
		lines = new java.util.ArrayList<>();
		lines.add("First line");
		lines.add("Second line");
		lines.add("Third line");
		range = new PlanarRange(0, 6, 2, 5);
		range.highlight(lines);
		assertEquals("First <!HIGHLIGHT_BEGIN!>line", lines.get(0));
		assertEquals("Second line", lines.get(1));
		assertEquals("Third<!HIGHLIGHT_END!> line", lines.get(2));
		
		// Test highlighting with column beyond line length
		lines = new java.util.ArrayList<>();
		lines.add("Short");
		range = new PlanarRange(0, 3, 0, 100);
		range.highlight(lines);
		assertEquals("Sho<!HIGHLIGHT_BEGIN!>rt<!HIGHLIGHT_END!>", lines.get(0));
		
		// Test highlighting at exact line boundaries
		lines = new java.util.ArrayList<>();
		lines.add("Test");
		range = new PlanarRange(0, 0, 0, 4);
		range.highlight(lines);
		assertEquals("<!HIGHLIGHT_BEGIN!>Test<!HIGHLIGHT_END!>", lines.get(0));
		
		// Test highlighting with negative fromRow (should be skipped)
		lines = new java.util.ArrayList<>();
		lines.add("Line 1");
		lines.add("Line 2");
		range = new PlanarRange(-1, 0, 1, 4);
		range.highlight(lines);
		assertEquals("Line 1", lines.get(0));
		assertEquals("Line<!HIGHLIGHT_END!> 2", lines.get(1));
		
		// Test highlighting with negative toRow (should be skipped)
		lines = new java.util.ArrayList<>();
		lines.add("Line 1");
		lines.add("Line 2");
		range = new PlanarRange(0, 0, -1, 4);
		range.highlight(lines);
		assertEquals("<!HIGHLIGHT_BEGIN!>Line 1", lines.get(0));
		assertEquals("Line 2", lines.get(1));
		
		// Test highlighting with row beyond list size
		lines = new java.util.ArrayList<>();
		lines.add("Only line");
		range = new PlanarRange(0, 0, 5, 4);
		range.highlight(lines);
		assertEquals("<!HIGHLIGHT_BEGIN!>Only line", lines.get(0));
		
		// Test highlighting empty line
		lines = new java.util.ArrayList<>();
		lines.add("");
		range = new PlanarRange(0, 0, 0, 0);
		range.highlight(lines);
		assertEquals("<!HIGHLIGHT_BEGIN!><!HIGHLIGHT_END!>", lines.get(0));
		
		// Test highlighting with same start and end position
		lines = new java.util.ArrayList<>();
		lines.add("Test line");
		range = new PlanarRange(0, 5, 0, 5);
		range.highlight(lines);
		assertEquals("Test <!HIGHLIGHT_BEGIN!><!HIGHLIGHT_END!>line", lines.get(0));
	}

}
