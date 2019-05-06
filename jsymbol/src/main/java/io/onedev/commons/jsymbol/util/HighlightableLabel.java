package io.onedev.commons.jsymbol.util;

import javax.annotation.Nullable;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.LoadableDetachableModel;

import io.onedev.commons.utils.HtmlUtils;
import io.onedev.commons.utils.LinearRange;

@SuppressWarnings("serial")
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
						return HtmlUtils.escape(prefix) 
								+ "<b>" 
								+ HtmlUtils.escape(middle) 
								+ "</b>" 
								+ HtmlUtils.escape(suffix);
					} else {
						return HtmlUtils.escape(label);
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
						highlighted = HtmlUtils.escape(prefix) 
								+ "<b>" 
								+ HtmlUtils.escape(middle) 
								+ "</b>" 
								+ HtmlUtils.escape(suffix);
					} else {
						highlighted = HtmlUtils.escape(unqualified);
					}
					String prefix = qualifiedName.getPrefix();
					String suffix = qualifiedName.getSuffix();
					return (prefix!=null?HtmlUtils.escape(prefix):"") + highlighted 
							+ (suffix!=null?HtmlUtils.escape(suffix):"");
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
