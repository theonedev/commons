package io.onedev.commons.codeassist.grammar;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import io.onedev.commons.codeassist.LiteralScan;
import io.onedev.commons.codeassist.grammar.ElementSpec.Multiplicity;

public class RuleSpec implements Serializable {

	private static final long serialVersionUID = 1L;

	private final String name;
	
	private final List<AlternativeSpec> alternatives;
	
	private LiteralScan prefixScan;
	
	private Boolean allowEmpty;
	
	private LiteralScan mandatoryScan;
	
	public RuleSpec(String name, List<AlternativeSpec> alternatives) {
		this.name = name;
		this.alternatives = alternatives;
	}
	
	public String getName() {
		return name;
	}

	public List<AlternativeSpec> getAlternatives() {
		return alternatives;
	}

	private boolean doIsAllowEmpty() {
		for (AlternativeSpec alternative: getAlternatives()) {
			boolean allowEmpty = true;
			for (ElementSpec elementSpec: alternative.getElements()) {
				if (!elementSpec.isAllowEmpty()) {
					allowEmpty = false;
					break;
				}
			}
			if (allowEmpty)
				return true;
		}
		return false;
	}
	
	public boolean isAllowEmpty() {
		if (allowEmpty == null) {
			// initialize this to return a meaningful value in case this method is 
			// invoked recursively
			allowEmpty = false; 

			allowEmpty = doIsAllowEmpty();
		}
		return allowEmpty;
	}
	
	private LiteralScan doScanMandatories(AlternativeSpec alternative, int fromElementIndex) {
		List<String> mandatories = new ArrayList<>();
		for (int i=fromElementIndex; i<alternative.getElements().size(); i++) {
			ElementSpec element = alternative.getElements().get(i);
			if (element.isOptional()) {
				return new LiteralScan(mandatories, true);
			} else if (element.getMultiplicity() == Multiplicity.ONE_OR_MORE) {
				LiteralScan scan = element.scanMandatories();
				mandatories.addAll(scan.getLiterals());
				if (!scan.isSpaceSeparator())
					return new LiteralScan(mandatories, true);
			} else {
				LiteralScan scan = element.scanMandatories();
				mandatories.addAll(scan.getLiterals());
				if (scan.isStop())
					return new LiteralScan(mandatories, true);
			}
		}
		return new LiteralScan(mandatories, false);
	}
	
	public LiteralScan scanMandatories() {
		if (mandatoryScan == null) {
			// initialize this to return a meaningful value in case this method is 
			// invoked recursively
			mandatoryScan = LiteralScan.stop(); 
			
			List<AlternativeSpec> alternatives = getAlternatives();
			if (alternatives.size() == 1) 
				mandatoryScan = doScanMandatories(alternatives.get(0), 0);
			else 
				mandatoryScan = LiteralScan.stop();
		}
		return mandatoryScan;
	}
	
	private LiteralScan doScanPrefixes() {
		List<String> prefixes = new ArrayList<>();
		boolean stop = false;
		for (AlternativeSpec alternative: getAlternatives()) {
			LiteralScan scan = doScanPrefixes(alternative);
			prefixes.addAll(scan.getLiterals());
			if (scan.isStop())
				stop = true; 
		}
			
		return new LiteralScan(prefixes, stop || prefixes.size()>1);
	}
	
	private LiteralScan doScanPrefixes(AlternativeSpec alternative) {
		List<String> prefixes = new ArrayList<>();
		boolean stop = false;
		for (int i=0; i<alternative.getElements().size(); i++) {
			ElementSpec element = alternative.getElements().get(i);
			LiteralScan prefixScan = element.scanPrefixes();
			if (!prefixScan.isStop() && (!element.isMultiple() || prefixScan.isSpaceSeparator())) {
				LiteralScan mandatoryScan = doScanMandatories(alternative, i+1);
				for (String prefix: prefixScan.getLiterals())
					prefixes.add(prefix + StringUtils.join(mandatoryScan.getLiterals(), ""));
				if (mandatoryScan.isStop())
					stop = true;
			}
			if (!element.isAllowEmpty())
				break;
		}
		return new LiteralScan(prefixes, stop);
	}
	
	public LiteralScan scanPrefixes() {
		if (prefixScan == null) {
			// initialize this to return a meaningful value in case this method is 
			// invoked recursively
			prefixScan = LiteralScan.stop(); 
			
			prefixScan = doScanPrefixes();
		}
		return prefixScan;
	}
	
	@Override
	public String toString() {
		List<String> alternativeStrings = new ArrayList<>();
		for (AlternativeSpec alternative: alternatives)
			alternativeStrings.add(alternative.toString());
		return StringUtils.join(alternativeStrings, " | ");
	}

}
