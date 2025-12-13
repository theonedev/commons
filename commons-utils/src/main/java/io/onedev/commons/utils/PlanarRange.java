package io.onedev.commons.utils;

import java.io.Serializable;
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

	public static final String HIGHLIGHT_BEGIN = "[HIGHLIGHT_BEGIN]";

	public static final String HIGHLIGHT_END = "[HIGHLIGHT_END]";
	
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
				normalizedToRow = toRow;
			} else if (toRow < 0) {
				normalizedFromRow = fromRow;
				normalizedToRow = lines.size() - 1;
			} else {
				normalizedFromRow = fromRow;
				normalizedToRow = toRow;
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
	
	public void highlight(List<String> lines) {		
		if (toRow >= 0 && toRow < lines.size()) {
			String endLine = lines.get(toRow);
			int endPos = Math.min(toColumn, endLine.length());
			lines.set(toRow, endLine.substring(0, endPos) + HIGHLIGHT_END + endLine.substring(endPos));
		}
		
		if (fromRow >= 0 && fromRow < lines.size()) {
			String startLine = lines.get(fromRow);
			int startPos = Math.min(fromColumn, startLine.length());
			lines.set(fromRow, startLine.substring(0, startPos) + HIGHLIGHT_BEGIN + startLine.substring(startPos));
		}		
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
