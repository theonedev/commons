package io.onedev.commons.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Splitter;

public class PlanarRange implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private final int fromRow, fromColumn, toRow, toColumn, tabWidth;
	
	public PlanarRange(int fromRow, int fromColumn, int toRow, int toColumn, int tabWidth) {
		this.fromRow = fromRow;
		this.fromColumn = fromColumn;
		this.toRow = toRow;
		this.toColumn = toColumn;
		this.tabWidth = tabWidth;
	}
	
	public PlanarRange(int fromRow, int fromColumn, int toRow, int toColumn) {
		this(fromRow, fromColumn, toRow, toColumn, 1);
	}
	
	public PlanarRange(PlanarRange range) {
		fromRow = range.fromRow;
		fromColumn = range.fromColumn;
		toRow = range.toRow;
		toColumn = range.toColumn;
		tabWidth = range.tabWidth;
	}

	public PlanarRange(String string) {
		List<String> fields = Splitter.on("-").splitToList(string);
		
		if (fields.size() == 1) {
			Pair<Integer, Integer> coordination = parseCoordination(fields.get(0));
			fromRow = coordination.getLeft();
			fromColumn = coordination.getRight();
			toRow = fromRow;
			toColumn = -1;
			tabWidth = 1;
		} else if (fields.size() == 2) {
			Pair<Integer, Integer> coordination = parseCoordination(fields.get(0));
			fromRow = coordination.getLeft();
			fromColumn = coordination.getRight();
			
			coordination = parseCoordination(fields.get(1));
			toRow = coordination.getLeft();
			toColumn = coordination.getRight();
			
			tabWidth = 1;
		} else if (fields.size() == 3) {
			Pair<Integer, Integer> coordination = parseCoordination(fields.get(0));
			fromRow = coordination.getLeft();
			fromColumn = coordination.getRight();
			
			coordination = parseCoordination(fields.get(1));
			toRow = coordination.getLeft();
			toColumn = coordination.getRight();
			
			tabWidth = Integer.parseInt(fields.get(2));
		} else {
			throw new IllegalArgumentException();
		}
	}
	
	public int getFromRow() {
		return fromRow;
	}

	public int getFromColumn() {
		return fromColumn;
	}

	public int getToRow() {
		return toRow;
	}

	public int getToColumn() {
		return toColumn;
	}
	
	public int getTabWidth() {
		return tabWidth;
	}
	
	@Override
	public String toString() {
		return (fromRow+1) + "." + (fromColumn+1) + "-" + (toRow+1) + "." + (toColumn+1) + "-" + tabWidth;
	}
	
	private int normalizeColumn(String lineContent, int column, int tabWidth) {
		int pos = 0;
		int normalizedColumn = 0;
		for (int i = 0; i < lineContent.length(); i++) {
			if (pos >= column) 
				break;
  			char ch = lineContent.charAt(i);
			if (ch == '\t')
				pos += tabWidth;
			else
				pos++;
			normalizedColumn++;
		}
		return normalizedColumn;
	}
	
	public PlanarRange normalize(List<String> lines) {
		if (!lines.isEmpty()) {
			int normalizedFromRow, normalizedToRow, normalizedFromColumn, normalizedToColumn;

			if (fromRow < 0 && toRow < 0) {
				normalizedFromRow = 0;
				normalizedToRow = lines.size() - 1;
			} else if (fromRow < 0) {
				normalizedFromRow = 0;
				normalizedToRow = Math.min(toRow, lines.size() - 1);
			} else if (toRow < 0) {
				normalizedFromRow = Math.max(0, Math.min(fromRow, lines.size() - 1));
				normalizedToRow = lines.size() - 1;
			} else {
				normalizedFromRow = Math.max(0, Math.min(fromRow, lines.size() - 1));
				normalizedToRow = Math.min(toRow, lines.size() - 1);
			}

			if (fromColumn < 0 && toColumn < 0) {
				normalizedFromColumn = 0;
				normalizedToColumn = lines.get(normalizedToRow).length();
			} else if (fromColumn < 0) {
				normalizedToColumn = normalizeColumn(lines.get(normalizedToRow), toColumn, tabWidth);
				normalizedFromColumn = 0;
			} else if (toColumn < 0) {
				normalizedFromColumn = normalizeColumn(lines.get(normalizedFromRow), fromColumn, tabWidth);
				normalizedToColumn = lines.get(normalizedToRow).length();
			} else {
				normalizedFromColumn = normalizeColumn(lines.get(normalizedFromRow), fromColumn, tabWidth);
				normalizedToColumn = normalizeColumn(lines.get(normalizedToRow), toColumn, tabWidth);
			}

			return new PlanarRange(normalizedFromRow, normalizedFromColumn, normalizedToRow, normalizedToColumn, 1);
		} else {
			return new PlanarRange(0, 0, 0, 0);
		}
	}

	/**
	 * Get the content of the range from the given lines. The range should be normalized before calling this method.
	 */
	public List<String> getContent(List<String> lines) {
		if (lines.isEmpty()) {
			return new ArrayList<>();
		}
				
		List<String> content = new ArrayList<>();
		for (int i = fromRow; i <= toRow; i++) {
			if (i >= lines.size()) {
				break;
			}
			String line = lines.get(i);
			String extractedLine;
			if (i == fromRow && i == toRow) {
				extractedLine = line.substring(fromColumn, Math.min(toColumn, line.length()));
			} else if (i == fromRow) {
				extractedLine = line.substring(fromColumn);
			} else if (i == toRow) {
				extractedLine = line.substring(0, Math.min(toColumn, line.length()));
			} else {
				extractedLine = line;
			}
			content.add(extractedLine);
		}
		
		return content;
	}

	/**
	 * Get context around the range and at start
	 * 
	 * @param lines the lines to get context from
	 * @param rangeBeginMark the mark to indicate the begin of the range
	 * @param rangeEndMark the mark to indicate the end of the range
	 * @param omittedLinesMark the mark to indicate the omitted lines
	 * @param startContextSize number of lines at the start of file to include in the context
	 * @param beforeContextSize number of lines before the range to include in the context
	 * @param afterContextSize number of lines after the range to include in the context
	 * 
	 * @return the context around the range and at start
	 */
	public List<String> getContext(List<String> lines, String rangeBeginMark, String rangeEndMark, 
			String omittedLinesMark, int startContextSize, int beforeContextSize, int afterContextSize) {
		List<String> context = new ArrayList<>();
		
		if (lines.isEmpty()) {
			return context;
		}
		
		PlanarRange normalizedRange = normalize(lines);
		int fromRow = normalizedRange.getFromRow();
		int fromColumn = normalizedRange.getFromColumn();
		int toRow = normalizedRange.getToRow();
		int toColumn = normalizedRange.getToColumn();
		
		int aroundStart;
		if (beforeContextSize == Integer.MAX_VALUE) {
			aroundStart = 0;
		} else {
			aroundStart = Math.max(0, fromRow - beforeContextSize);
		}
		
		int aroundEnd;
		if (afterContextSize == Integer.MAX_VALUE) {
			aroundEnd = lines.size() - 1;
		} else {
			aroundEnd = Math.min(lines.size() - 1, toRow + afterContextSize);
		}
		
		int startLineCount;
		if (startContextSize == Integer.MAX_VALUE) {
			startLineCount = aroundStart;
		} else {
			startLineCount = Math.min(aroundStart, startContextSize);
		}
		for (int i = 0; i < startLineCount; i++) {
			context.add(lines.get(i));
		}
		
		if (startLineCount < aroundStart) {
			context.add(omittedLinesMark);
		}
		
		List<String> markedLines = new ArrayList<>();
		for (int i = aroundStart; i <= aroundEnd; i++) {
			markedLines.add(lines.get(i));
		}
		
		int relativeFromRow = fromRow - aroundStart;
		int relativeToRow = toRow - aroundStart;
		
		if (relativeToRow >= 0 && relativeToRow < markedLines.size()) {
			String endLine = markedLines.get(relativeToRow);
			int endPos = Math.min(toColumn, endLine.length());
			markedLines.set(relativeToRow, endLine.substring(0, endPos) + rangeEndMark + endLine.substring(endPos));
		}
		
		if (relativeFromRow >= 0 && relativeFromRow < markedLines.size()) {
			String startLine = markedLines.get(relativeFromRow);
			int startPos = Math.min(fromColumn, startLine.length());
			markedLines.set(relativeFromRow, startLine.substring(0, startPos) + rangeBeginMark + startLine.substring(startPos));
		}
		
		context.addAll(markedLines);
		
		if (aroundEnd < lines.size() - 1) {
			context.add(omittedLinesMark);
		}
		
		return context;
	}
	
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof PlanarRange))
			return false;
		if (this == other)
			return true;
		PlanarRange otherRange = (PlanarRange) other;
		return new EqualsBuilder()
				.append(fromRow, otherRange.fromRow)
				.append(fromColumn, otherRange.fromColumn)
				.append(toRow, otherRange.toRow)
				.append(toColumn, otherRange.toColumn)
				.append(tabWidth, otherRange.tabWidth)
				.isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37)
				.append(fromRow)
				.append(fromColumn)
				.append(toRow)
				.append(toColumn)
				.append(tabWidth)
				.toHashCode();
	}
	
	public static @Nullable PlanarRange of(@Nullable String string) {
		if (string != null)
			return new PlanarRange(string);
		else
			return null;
	}
	
	private static Pair<Integer, Integer> parseCoordination(String string) {
		int row, column;
		if (string.contains(".")) {
			row = Integer.parseInt(StringUtils.substringBefore(string, "."))-1;
			column = Integer.parseInt(StringUtils.substringAfter(string, "."))-1;
		} else {
			row = Integer.parseInt(string)-1;
			column = -1;
		}
		return new ImmutablePair<>(row, column);
	}
	
}
