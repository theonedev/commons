package io.onedev.commons.jsymbol.scss.symbols;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.resource.PackageResourceReference;

import io.onedev.commons.utils.PlanarRange;
import io.onedev.commons.jsymbol.scss.symbols.icon.IconLocator;
import io.onedev.commons.jsymbol.util.HighlightableLabel;
import io.onedev.commons.jsymbol.util.NoAntiCacheImage;
import io.onedev.commons.jsymbol.util.QualifiedName;
import io.onedev.commons.utils.LinearRange;

public class ClassSymbol extends ScssSymbol {

	private static final long serialVersionUID = 1L;

	private final QualifiedName name;
	
	public ClassSymbol(ScssSymbol parent, QualifiedName name, PlanarRange position) {
		super(parent, name.getUnqualified(), position, null);
		this.name = name;
	}

	@Override
	public boolean isLocal() {
		return false;
	}

	@Override
	public boolean isDisplayInOutline() {
		return false;
	}

	@Override
	public boolean isPrimary() {
		return false;
	}

	@Override
	public Image renderIcon(String componentId) {
		Image icon = new NoAntiCacheImage(componentId, 
				new PackageResourceReference(IconLocator.class, "css.png"));
		icon.add(AttributeAppender.append("title", "CSS element class"));
		return icon;
	}

	@Override
	public Component render(String componentId, LinearRange highlight) {
		return new HighlightableLabel(componentId, name, highlight);
	}

}
