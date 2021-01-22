package io.onedev.commons.utils;

import java.io.Serializable;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class PlanarRange implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private final int fromRow, fromColumn, toRow, toColumn;
	
	public PlanarRange(int fromRow, int fromColumn, int toRow, int toColumn) {
		this.fromRow = fromRow;
		this.fromColumn = fromColumn;
		this.toRow = toRow;
		this.toColumn = toColumn;
	}
	
	public PlanarRange(PlanarRange range) {
		fromRow = range.fromRow;
		fromColumn = range.fromColumn;
		toRow = range.toRow;
		toColumn = range.toColumn;
	}
	
	public PlanarRange(String string) {
		String from = StringUtils.substringBefore(string, "-");
		String to = StringUtils.substringAfter(string, "-");
		fromRow = Integer.parseInt(StringUtils.substringBefore(from, "."))-1;
		fromColumn = Integer.parseInt(StringUtils.substringAfter(from, "."));
		toRow = Integer.parseInt(StringUtils.substringBefore(to, "."))-1;
		toColumn = Integer.parseInt(StringUtils.substringAfter(to, "."));
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
	
	@Override
	public String toString() {
		return (fromRow+1) + "." + fromColumn + "-" + (toRow+1) + "." + toColumn;
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
				.isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37)
				.append(fromRow)
				.append(fromColumn)
				.append(toRow)
				.append(toColumn)
				.toHashCode();
	}
	
	public static @Nullable PlanarRange of(@Nullable String string) {
		if (string != null)
			return new PlanarRange(string);
		else
			return null;
	}
	
}
