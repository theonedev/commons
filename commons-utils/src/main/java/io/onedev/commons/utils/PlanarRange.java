package io.onedev.commons.utils;

import java.io.Serializable;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

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
		String from = fields.get(0);
		String to = fields.get(1);
		fromRow = Integer.parseInt(StringUtils.substringBefore(from, "."))-1;
		fromColumn = Integer.parseInt(StringUtils.substringAfter(from, "."));
		toRow = Integer.parseInt(StringUtils.substringBefore(to, "."))-1;
		toColumn = Integer.parseInt(StringUtils.substringAfter(to, "."));
		
		if (fields.size() >= 3) 
			tabWidth = Integer.parseInt(fields.get(2));
		else
			tabWidth = 1;
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
		return (fromRow+1) + "." + fromColumn + "-" + (toRow+1) + "." + toColumn + "-" + tabWidth;
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
		int normalizedFromRow, normalizedFromColumn, normalizedToRow, normalizedToColumn;
		if (fromColumn == -1) { // only fromRow is set
			normalizedFromRow = fromRow;
			normalizedFromColumn = 0;
			normalizedToRow = fromRow;
			normalizedToColumn = lines.get(fromRow).length();
		} else if (toRow == -1) { // only fromRow and fromColumn is set
			normalizedFromRow = fromRow;
			normalizedFromColumn = normalizeColumn(lines.get(fromRow), fromColumn, tabWidth);
			normalizedToRow = fromRow;
			normalizedToColumn = normalizedFromColumn+1;
		} else {
			normalizedFromRow = fromRow;
			normalizedFromColumn = normalizeColumn(lines.get(fromRow), fromColumn, tabWidth);
			normalizedToRow = toRow;
			normalizedToColumn = normalizeColumn(lines.get(toRow), toColumn, tabWidth);
		}
		return new PlanarRange(normalizedFromRow, normalizedFromColumn, normalizedToRow, 
				normalizedToColumn, 1);
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
	
}
