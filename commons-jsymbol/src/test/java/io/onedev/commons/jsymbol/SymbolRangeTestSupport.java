package io.onedev.commons.jsymbol;

import static org.junit.Assert.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.onedev.commons.utils.PlanarRange;

/**
 * Fixtures mark exact name positions with [[p:name]]...[[/p:name]] and scopes
 * with [[s:name]]...[[/s:name]]. An absent scope marker explicitly expects null.
 * Markers are removed before extraction; expected ranges come from the fixture,
 * independently of the extractor. Columns are UTF-16 offsets, with tab width 1.
 */
public final class SymbolRangeTestSupport {

	private static final Pattern MARKER = Pattern.compile("\\[\\[(/?)([ps]):([^]\\r\\n]+)]]");

	private SymbolRangeTestSupport() {
	}

	public static void verify(SymbolExtractor<?> extractor, String fileName, String fixture) {
		assertFalse("Use LF in annotated fixtures", fixture.contains("\r"));
		String trimmed = fixture.endsWith("\n")? fixture.substring(0, fixture.length()-1): fixture;
		for (String newline : new String[] { "\n", "\r\n" }) {
			for (String prefix : new String[] { "", "\n\n" }) {
				for (String suffix : new String[] { "", "\n" }) {
					String variant = (prefix + trimmed + suffix).replace("\n", newline);
					String context = fileName + " (" + (newline.length() == 1? "LF": "CRLF")
							+ ", prefix=" + prefix.length() + ", final newline=" + !suffix.isEmpty() + ")";
					verifyVariant(extractor, fileName, variant, context);
				}
			}
		}
	}

	private static void verifyVariant(SymbolExtractor<?> extractor, String fileName, String fixture, String context) {
		StringBuilder source = new StringBuilder();
		Map<String, Integer> starts = new LinkedHashMap<>();
		Map<String, PlanarRange> expected = new LinkedHashMap<>();
		Matcher matcher = MARKER.matcher(fixture);
		int offset = 0;
		while (matcher.find()) {
			source.append(fixture, offset, matcher.start());
			String key = matcher.group(2) + ":" + matcher.group(3);
			if (matcher.group(1).isEmpty()) {
				assertFalse("Duplicate marker " + key, expected.containsKey(key));
				assertNull("Duplicate opening marker " + key, starts.put(key, source.length()));
			} else {
				Integer start = starts.remove(key);
				assertNotNull("Unmatched closing marker " + key, start);
				int[] from = coordinates(source, start);
				int[] to = coordinates(source, source.length());
				expected.put(key, new PlanarRange(from[0], from[1], to[0], to[1]));
			}
			offset = matcher.end();
		}
		source.append(fixture, offset, fixture.length());
		assertTrue("Unclosed markers " + starts.keySet(), starts.isEmpty());
		assertFalse("Fixture must assert some ranges", expected.isEmpty());
		List<? extends Symbol> symbols = extractor.extract(fileName, source.toString());
		for (Map.Entry<String, PlanarRange> entry : expected.entrySet()) {
			String name = entry.getKey().substring(2);
			if (entry.getKey().startsWith("s:")) {
				assertTrue("Missing position marker for " + name, expected.containsKey("p:" + name));
				continue;
			}
			List<? extends Symbol> matches = symbols.stream().filter(it -> name.equals(it.getName()))
					.collect(java.util.stream.Collectors.toList());
			assertEquals(context + ": symbol " + name, 1, matches.size());
			Symbol symbol = matches.get(0);
			assertEquals(context + ": position of " + name, entry.getValue(), symbol.getPosition());
			assertEquals(context + ": scope of " + name, expected.get("s:" + name), symbol.getScope());
		}
	}

	private static int[] coordinates(CharSequence source, int offset) {
		int row = 0;
		int column = 0;
		for (int i=0; i<offset; i++) {
			if (source.charAt(i) == '\n') {
				row++;
				column = 0;
			} else if (source.charAt(i) != '\r') {
				column++;
			}
		}
		return new int[] { row, column };
	}
}
