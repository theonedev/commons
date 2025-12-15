package io.onedev.commons.utils;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class PlanarRangeTest {

	@Test
	public void testFromString() {
		PlanarRange range = new PlanarRange("1.2-3.4-8");
		assertEquals(0, range.getFromRow());
		assertEquals(3, range.getToColumn());
		assertEquals(8, range.getTabWidth());
		
		range = new PlanarRange("1.2-3.4");
		assertEquals(0, range.getFromRow());
		assertEquals(3, range.getToColumn());
		assertEquals(1, range.getTabWidth());
	}

	private List<String> lines(String... lines) {
		return Arrays.asList(lines);
	}

	@Test
	public void testNormalizeEmptyLines() {
		PlanarRange range = new PlanarRange(0, 0, 0, 0);
		PlanarRange normalized = range.normalize(Arrays.asList());
		assertEquals(0, normalized.getFromRow());
		assertEquals(0, normalized.getFromColumn());
		assertEquals(0, normalized.getToRow());
		assertEquals(0, normalized.getToColumn());
	}

	@Test
	public void testNormalizeNegativeRows() {
		List<String> content = lines("line1", "line2", "line3");
		PlanarRange range = new PlanarRange(-1, -1, -1, -1);
		PlanarRange normalized = range.normalize(content);
		assertEquals(0, normalized.getFromRow());
		assertEquals(0, normalized.getFromColumn());
		assertEquals(2, normalized.getToRow());
		assertEquals(5, normalized.getToColumn());
	}

	@Test
	public void testNormalizeNegativeFromRow() {
		List<String> content = lines("line1", "line2", "line3");
		PlanarRange range = new PlanarRange(-1, 0, 1, 5);
		PlanarRange normalized = range.normalize(content);
		assertEquals(0, normalized.getFromRow());
		assertEquals(0, normalized.getFromColumn());
		assertEquals(1, normalized.getToRow());
		assertEquals(5, normalized.getToColumn());
	}

	@Test
	public void testNormalizeNegativeToRow() {
		List<String> content = lines("line1", "line2", "line3");
		PlanarRange range = new PlanarRange(1, 0, -1, -1);
		PlanarRange normalized = range.normalize(content);
		assertEquals(1, normalized.getFromRow());
		assertEquals(0, normalized.getFromColumn());
		assertEquals(2, normalized.getToRow());
		assertEquals(5, normalized.getToColumn());
	}

	@Test
	public void testNormalizeNegativeFromColumn() {
		List<String> content = lines("line1", "line2", "line3");
		PlanarRange range = new PlanarRange(1, -1, 1, 3);
		PlanarRange normalized = range.normalize(content);
		assertEquals(1, normalized.getFromRow());
		assertEquals(0, normalized.getFromColumn());
		assertEquals(1, normalized.getToRow());
		assertEquals(3, normalized.getToColumn());
	}

	@Test
	public void testNormalizeNegativeToColumn() {
		List<String> content = lines("line1", "line2", "line3");
		PlanarRange range = new PlanarRange(1, 2, 1, -1);
		PlanarRange normalized = range.normalize(content);
		assertEquals(1, normalized.getFromRow());
		assertEquals(2, normalized.getFromColumn());
		assertEquals(1, normalized.getToRow());
		assertEquals(5, normalized.getToColumn());
	}

	@Test
	public void testNormalizeWithTabs() {
		List<String> content = lines("line1", "\tline2", "line3");
		PlanarRange range = new PlanarRange(1, 0, 1, 5, 4);
		PlanarRange normalized = range.normalize(content);
		assertEquals(1, normalized.getFromRow());
		assertEquals(0, normalized.getFromColumn());
		assertEquals(1, normalized.getToRow());
		assertEquals(2, normalized.getToColumn());
		assertEquals(1, normalized.getTabWidth());
	}

	@Test
	public void testNormalizeTabColumnMapping() {
		List<String> content = lines("\t\ttext");
		PlanarRange range = new PlanarRange(0, 8, 0, 12, 4);
		PlanarRange normalized = range.normalize(content);
		assertEquals(0, normalized.getFromRow());
		assertEquals(2, normalized.getFromColumn());
		assertEquals(0, normalized.getToRow());
		assertEquals(6, normalized.getToColumn());
	}

	@Test
	public void testNormalizeOutOfBoundsRows() {
		List<String> content = lines("line1", "line2");
		PlanarRange range = new PlanarRange(0, 0, 10, 5);
		PlanarRange normalized = range.normalize(content);
		assertEquals(0, normalized.getFromRow());
		assertEquals(0, normalized.getFromColumn());
		assertEquals(1, normalized.getToRow());
		assertEquals(5, normalized.getToColumn());
	}

	@Test
	public void testNormalizeOutOfBoundsFromRow() {
		List<String> content = lines("line1", "line2");
		PlanarRange range = new PlanarRange(10, 0, 15, 5);
		PlanarRange normalized = range.normalize(content);
		assertEquals(1, normalized.getFromRow());
		assertEquals(0, normalized.getFromColumn());
		assertEquals(1, normalized.getToRow());
		assertEquals(5, normalized.getToColumn());
	}

	@Test
	public void testNormalizeColumnOutOfBounds() {
		List<String> content = lines("short");
		PlanarRange range = new PlanarRange(0, 0, 0, 100);
		PlanarRange normalized = range.normalize(content);
		assertEquals(0, normalized.getFromRow());
		assertEquals(0, normalized.getFromColumn());
		assertEquals(0, normalized.getToRow());
		assertEquals(5, normalized.getToColumn());
	}

	@Test
	public void testNormalizeMultiLine() {
		List<String> content = lines("line1", "line2", "line3", "line4");
		PlanarRange range = new PlanarRange(1, 2, 3, 4);
		PlanarRange normalized = range.normalize(content);
		assertEquals(1, normalized.getFromRow());
		assertEquals(2, normalized.getFromColumn());
		assertEquals(3, normalized.getToRow());
		assertEquals(4, normalized.getToColumn());
		assertEquals(1, normalized.getTabWidth());
	}

	@Test
	public void testGetContentEmptyLines() {
		PlanarRange range = new PlanarRange(0, 0, 0, 0);
		List<String> result = range.getContent(Arrays.asList());
		assertTrue(result.isEmpty());
	}

	@Test
	public void testGetContentSingleLinePartial() {
		List<String> content = lines("hello world");
		PlanarRange range = new PlanarRange(0, 0, 0, 5);
		List<String> result = range.getContent(content);
		assertEquals(lines("hello"), result);
	}

	@Test
	public void testGetContentSingleLineFull() {
		List<String> content = lines("hello world");
		PlanarRange range = new PlanarRange(0, 0, 0, 11);
		List<String> result = range.getContent(content);
		assertEquals(lines("hello world"), result);
	}

	@Test
	public void testGetContentSingleLineMiddle() {
		List<String> content = lines("hello world");
		PlanarRange range = new PlanarRange(0, 6, 0, 11);
		List<String> result = range.getContent(content);
		assertEquals(lines("world"), result);
	}

	@Test
	public void testGetContentMultiLine() {
		List<String> content = lines("line1", "line2", "line3", "line4");
		PlanarRange range = new PlanarRange(1, 0, 2, 5);
		List<String> result = range.getContent(content);
		assertEquals(lines("line2", "line3"), result);
	}

	@Test
	public void testGetContentMultiLinePartial() {
		List<String> content = lines("hello world", "foo bar", "test");
		PlanarRange range = new PlanarRange(0, 6, 1, 3);
		List<String> result = range.getContent(content);
		assertEquals(lines("world", "foo"), result);
	}

	@Test
	public void testGetContentMultiLineFullRange() {
		List<String> content = lines("line1", "line2", "line3");
		PlanarRange range = new PlanarRange(0, 0, 2, 5);
		List<String> result = range.getContent(content);
		assertEquals(lines("line1", "line2", "line3"), result);
	}

	@Test
	public void testGetContentMultiLineMiddle() {
		List<String> content = lines("line1", "line2", "line3", "line4", "line5");
		PlanarRange range = new PlanarRange(1, 0, 3, 5);
		List<String> result = range.getContent(content);
		assertEquals(lines("line2", "line3", "line4"), result);
	}

	@Test
	public void testGetContentOutOfBounds() {
		List<String> content = lines("line1", "line2");
		PlanarRange range = new PlanarRange(0, 0, 10, 5);
		List<String> result = range.getContent(content);
		assertEquals(lines("line1", "line2"), result);
	}

	@Test
	public void testGetContentColumnOutOfBounds() {
		List<String> content = lines("short", "line2");
		PlanarRange range = new PlanarRange(0, 0, 0, 100);
		List<String> result = range.getContent(content);
		assertEquals(lines("short"), result);
	}

	@Test
	public void testGetContentSingleCharacter() {
		List<String> content = lines("hello");
		PlanarRange range = new PlanarRange(0, 2, 0, 3);
		List<String> result = range.getContent(content);
		assertEquals(lines("l"), result);
	}

	@Test
	public void testGetContentEmptyLine() {
		List<String> content = lines("", "line2", "");
		PlanarRange range = new PlanarRange(0, 0, 0, 0);
		List<String> result = range.getContent(content);
		assertEquals(lines(""), result);
	}

	@Test
	public void testGetContentMultiLineWithEmptyLines() {
		List<String> content = lines("line1", "", "line3", "");
		PlanarRange range = new PlanarRange(0, 0, 3, 0);
		List<String> result = range.getContent(content);
		assertEquals(lines("line1", "", "line3", ""), result);
	}

	@Test
	public void testGetContextEmpty() {
		PlanarRange range = new PlanarRange(0, 0, 0, 0);
		List<String> result = range.getContext(Arrays.asList(), "[BEGIN]", "[END]", "...", 2, 2, 2);
		assertTrue(result.isEmpty());
	}

	@Test
	public void testGetContextSingleLineNoTruncation() {
		List<String> content = lines("line1", "line2", "line3");
		PlanarRange range = new PlanarRange(1, 0, 1, 5);
		List<String> result = range.getContext(content, "[BEGIN]", "[END]", "...", 2, 1, 1);
		assertEquals(lines("line1", "[BEGIN]line2[END]", "line3"), result);
	}

	@Test
	public void testGetContextSingleLineWithStartContext() {
		List<String> content = lines("line1", "line2", "line3", "line4", "line5", "line6");
		PlanarRange range = new PlanarRange(4, 0, 4, 5);
		List<String> result = range.getContext(content, "[BEGIN]", "[END]", "...", 2, 2, 1);
		assertEquals(lines("line1", "line2", "line3", "line4", "[BEGIN]line5[END]", "line6"), result);
	}

	@Test
	public void testGetContextSingleLineWithStartContextAndTruncation() {
		List<String> content = lines("line1", "line2", "line3", "line4", "line5", "line6", "line7", "line8");
		PlanarRange range = new PlanarRange(5, 0, 5, 5);
		List<String> result = range.getContext(content, "[BEGIN]", "[END]", "...", 2, 2, 2);
		assertEquals(lines("line1", "line2", "...", "line4", "line5", "[BEGIN]line6[END]", "line7", "line8"), result);
	}

	@Test
	public void testGetContextMultiLine() {
		List<String> content = lines("line1", "line2", "line3", "line4", "line5");
		PlanarRange range = new PlanarRange(1, 2, 3, 4);
		List<String> result = range.getContext(content, "[BEGIN]", "[END]", "...", 1, 1, 1);
		assertEquals(lines("line1", "li[BEGIN]ne2", "line3", "line[END]4", "line5"), result);
	}

	@Test
	public void testGetContextWithEndTruncation() {
		List<String> content = lines("line1", "line2", "line3", "line4", "line5", "line6");
		PlanarRange range = new PlanarRange(1, 0, 1, 5);
		List<String> result = range.getContext(content, "[BEGIN]", "[END]", "...", 1, 1, 2);
		assertEquals(lines("line1", "[BEGIN]line2[END]", "line3", "line4", "..."), result);
	}

	@Test
	public void testGetContextAtBeginning() {
		List<String> content = lines("line1", "line2", "line3", "line4");
		PlanarRange range = new PlanarRange(0, 0, 0, 5);
		List<String> result = range.getContext(content, "[BEGIN]", "[END]", "...", 2, 2, 2);
		assertEquals(lines("[BEGIN]line1[END]", "line2", "line3", "..."), result);
	}

	@Test
	public void testGetContextAtEnd() {
		List<String> content = lines("line1", "line2", "line3", "line4");
		PlanarRange range = new PlanarRange(3, 0, 3, 5);
		List<String> result = range.getContext(content, "[BEGIN]", "[END]", "...", 2, 2, 2);
		assertEquals(lines("line1", "line2", "line3", "[BEGIN]line4[END]"), result);
	}

	@Test
	public void testGetContextPartialLine() {
		List<String> content = lines("hello world", "foo bar");
		PlanarRange range = new PlanarRange(0, 6, 0, 11);
		List<String> result = range.getContext(content, "[BEGIN]", "[END]", "...", 1, 0, 1);
		assertEquals(lines("hello [BEGIN]world[END]", "foo bar"), result);
	}

	@Test
	public void testGetContextZeroContextSize() {
		List<String> content = lines("line1", "line2", "line3");
		PlanarRange range = new PlanarRange(1, 0, 1, 5);
		List<String> result = range.getContext(content, "[BEGIN]", "[END]", "...", 0, 0, 0);
		assertEquals(lines("...", "[BEGIN]line2[END]", "..."), result);
	}

	@Test
	public void testGetContextDifferentSizes() {
		List<String> content = lines("line1", "line2", "line3", "line4", "line5", "line6", "line7", "line8");
		PlanarRange range = new PlanarRange(4, 0, 4, 5);
		List<String> result = range.getContext(content, "[BEGIN]", "[END]", "...", 1, 2, 3);
		assertEquals(lines("line1", "...", "line3", "line4", "[BEGIN]line5[END]", "line6", "line7", "line8"), result);
	}

	@Test
	public void testGetContextLargeBeforeSmallAfter() {
		List<String> content = lines("line1", "line2", "line3", "line4", "line5", "line6");
		PlanarRange range = new PlanarRange(3, 0, 3, 5);
		List<String> result = range.getContext(content, "[BEGIN]", "[END]", "...", 1, 3, 1);
		assertEquals(lines("line1", "line2", "line3", "[BEGIN]line4[END]", "line5", "..."), result);
	}

	@Test
	public void testGetContextSmallBeforeLargeAfter() {
		List<String> content = lines("line1", "line2", "line3", "line4", "line5", "line6");
		PlanarRange range = new PlanarRange(2, 0, 2, 5);
		List<String> result = range.getContext(content, "[BEGIN]", "[END]", "...", 1, 2, 4);
		assertEquals(lines("line1", "line2", "[BEGIN]line3[END]", "line4", "line5", "line6"), result);
	}

}
