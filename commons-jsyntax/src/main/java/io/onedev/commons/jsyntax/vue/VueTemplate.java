package io.onedev.commons.jsyntax.vue;

import java.util.regex.Pattern;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;
import io.onedev.commons.jsyntax.addon.Overlay;
import io.onedev.commons.jsyntax.htmlmixed.HtmlMixedTokenizer;

public class VueTemplate extends Overlay {

	@SuppressWarnings("rawtypes" )
	public VueTemplate() {
		super(new HtmlMixedTokenizer(), new AbstractTokenizer() {

			final Pattern pattern = Pattern.compile("^\\{\\{.*?\\}\\}");
			
			@Override
			public boolean accept(String fileName) {
				return false;
			}

			@Override
			public boolean acceptMime(String mime) {
				return false;
			}

			@Override
			public boolean acceptMode(String mode) {
				return false;
			}

			@Override
			public Object startState() {
				return null;
			}

			@Override
			public String token(StringStream stream, Object state) {
				if (!stream.match(pattern).isEmpty())
					return "meta mustache";
				while (!stream.next().isEmpty() && !stream.match("{{", false, false));
				return "";
			}
		});
	}

	@Override
	public boolean accept(String fileName) {
		return false;
	}

	@Override
	public boolean acceptMime(String mime) {
		return false;
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("vue-template");
	}
}
