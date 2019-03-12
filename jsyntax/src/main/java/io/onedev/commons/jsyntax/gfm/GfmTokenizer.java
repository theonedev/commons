package io.onedev.commons.jsyntax.gfm;

import java.util.regex.Pattern;

import com.google.common.collect.Maps;

import io.onedev.commons.jsyntax.addon.Overlay;
import io.onedev.commons.jsyntax.markdown.MarkdownTokenizer;

public class GfmTokenizer extends Overlay {

	public GfmTokenizer() {
		super(new MarkdownTokenizer(false, 0, false, true, true, Maps.newHashMap(), "```"), new GfmOverlay(), true, false);
	}

	@Override
	public boolean accept(String fileName) {
		return acceptPattern(fileName, Pattern.compile("^(readme|contributing|history).md$", Pattern.CASE_INSENSITIVE));
	}

	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-gfm");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("gfm");
	}
}
