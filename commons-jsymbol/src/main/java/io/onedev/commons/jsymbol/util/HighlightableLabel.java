package io.onedev.commons.jsymbol.util;

import org.jspecify.annotations.Nullable;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.LoadableDetachableModel;
import org.unbescape.html.HtmlEscape;

import io.onedev.commons.utils.LinearRange;

public class HighlightableLabel extends Label {

	public HighlightableLabel(String id, @Nullable String label, @Nullable LinearRange highlight) {
		super(id, new LoadableDetachableModel<String>() {

			@Override
			protected String load() {
				if (label != null) {
					if (highlight != null) {
						String prefix = label.substring(0, highlight.getFrom());
						String middle = label.substring(highlight.getFrom(), highlight.getTo());
						String suffix = label.substring(highlight.getTo());
						return HtmlEscape.escapeHtml5(prefix) 
								+ "<b>" 
								+ HtmlEscape.escapeHtml5(middle) 
								+ "</b>" 
								+ HtmlEscape.escapeHtml5(suffix);
					} else {
						return HtmlEscape.escapeHtml5(label);
					}
				} else {
					return "";
				}
			}
			
		});
	}

	public HighlightableLabel(String id, @Nullable QualifiedName qualifiedName, @Nullable LinearRange highlight) {
		super(id, new LoadableDetachableModel<String>() {

			@Override
			protected String load() {
				if (qualifiedName != null) {
					String unqualified = qualifiedName.getUnqualified();
					String highlighted;
					if (highlight != null) {
						String prefix = unqualified.substring(0, highlight.getFrom());
						String middle = unqualified.substring(highlight.getFrom(), highlight.getTo());
						String suffix = unqualified.substring(highlight.getTo());
						highlighted = HtmlEscape.escapeHtml5(prefix) 
								+ "<b>" 
								+ HtmlEscape.escapeHtml5(middle) 
								+ "</b>" 
								+ HtmlEscape.escapeHtml5(suffix);
					} else {
						highlighted = HtmlEscape.escapeHtml5(unqualified);
					}
					String prefix = qualifiedName.getPrefix();
					String suffix = qualifiedName.getSuffix();
					return (prefix!=null?HtmlEscape.escapeHtml5(prefix):"") + highlighted 
							+ (suffix!=null?HtmlEscape.escapeHtml5(suffix):"");
				} else {
					return "";
				}
			}
			
		});
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		setEscapeModelStrings(false);
	}

}
