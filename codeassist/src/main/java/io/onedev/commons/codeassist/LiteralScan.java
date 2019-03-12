package io.onedev.commons.codeassist;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;

import io.onedev.commons.utils.StringUtils;

public class LiteralScan implements Serializable {
	
	private static final long serialVersionUID = 1L;

	private final List<String> literals;
	
	private final boolean stop;
	
	/**
	 * Construct literal scan object
	 * 
	 * @param literals
	 * 			list of literals
	 * @param stop
	 * 			whether or not the literal scan should be stopped
	 */
	public LiteralScan(List<String> literals, boolean stop) {
		this.literals = literals;
		this.stop = stop;
	}

	public List<String> getLiterals() {
		return literals;
	}

	public boolean isStop() {
		return stop;
	}

	public boolean isSpaceSeparator() {
		return !stop && literals.size() == 1 && literals.iterator().next().equals(" ");
	}

	public LiteralScan copy() {
		return new LiteralScan(new ArrayList<>(getLiterals()), isStop());
	}
	
	public LiteralScan merge() {
		return new LiteralScan(Lists.newArrayList(StringUtils.join(getLiterals(), "")), isStop());
	}
	
	public static LiteralScan stop() {
		return new LiteralScan(new ArrayList<String>(), true);
	}
	
}
