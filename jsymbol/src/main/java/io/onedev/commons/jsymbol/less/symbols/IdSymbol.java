package io.onedev.commons.jsymbol.less.symbols;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.resource.PackageResourceReference;

import io.onedev.commons.jsymbol.TokenPosition;
import io.onedev.commons.jsymbol.less.symbols.icon.IconLocator;
import io.onedev.commons.jsymbol.util.HighlightableLabel;
import io.onedev.commons.jsymbol.util.NoAntiCacheImage;
import io.onedev.commons.jsymbol.util.QualifiedName;
import io.onedev.commons.utils.Range;

public class IdSymbol extends LessSymbol {

	private static final long serialVersionUID = 1L;

	private final QualifiedName name;
	
	public IdSymbol(LessSymbol parent, QualifiedName name, TokenPosition position) {
		super(parent, name.getUnqualified(), position, null);
		this.name = name;
	}

	@Override
	public boolean isLocal() {
		return false;
	}

	@Override
	public boolean isPrimary() {
		return false;
	}

	@Override
	public boolean isDisplayInOutline() {
		return false;
	}

	@Override
	public Image renderIcon(String componentId) {
		Image icon = new NoAntiCacheImage(componentId, 
				new PackageResourceReference(IconLocator.class, "css.png"));
		icon.add(AttributeAppender.append("title", "CSS element id"));
		return icon;
	}

	@Override
	public Component render(String componentId, Range highlight) {
		return new HighlightableLabel(componentId, name, highlight);
	}

}
