package io.onedev.commons.jsymbol.r.symbols;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.resource.PackageResourceReference;

import io.onedev.commons.utils.PlanarRange;
import io.onedev.commons.jsymbol.r.symbols.icon.IconLocator;
import io.onedev.commons.jsymbol.util.HighlightableLabel;
import io.onedev.commons.jsymbol.util.NoAntiCacheImage;
import io.onedev.commons.utils.LinearRange;

public class VariableSymbol extends RSymbol {

	private static final long serialVersionUID = 1L;

	private boolean local;
	
	public VariableSymbol(RSymbol parent, String name, PlanarRange position, PlanarRange scope, 
			boolean local) {
		super(parent, name, position, scope);
		this.local = local;
	}

	@Override
	public boolean isLocal() {
		return local;
	}

	@Override
	public boolean isPrimary() {
		return false;
	}

	@Override
	public Image renderIcon(String componentId) {
		Image icon = new NoAntiCacheImage(componentId, 
				new PackageResourceReference(IconLocator.class, "variable.png"));
		icon.add(AttributeAppender.append("title", "variable"));
		return icon;
	}

	@Override
	public Component render(String componentId, LinearRange highlight) {
		return new HighlightableLabel(componentId, getName(), highlight);
	}

}
