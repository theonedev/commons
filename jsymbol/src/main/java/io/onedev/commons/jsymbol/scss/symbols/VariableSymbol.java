package io.onedev.commons.jsymbol.scss.symbols;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.resource.PackageResourceReference;

import io.onedev.commons.jsymbol.TokenPosition;
import io.onedev.commons.jsymbol.scss.symbols.icon.IconLocator;
import io.onedev.commons.jsymbol.util.HighlightableLabel;
import io.onedev.commons.jsymbol.util.NoAntiCacheImage;
import io.onedev.commons.jsymbol.util.QualifiedName;
import io.onedev.commons.utils.Range;

public class VariableSymbol extends ScssSymbol {

	private static final long serialVersionUID = 1L;
	
	private final boolean local;

	public VariableSymbol(ScssSymbol parent, String name, TokenPosition position, TokenPosition scope, boolean local) {
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
				new PackageResourceReference(IconLocator.class, "rule.png"));
		icon.add(AttributeAppender.append("title", "SCSS variable"));
		return icon;
	}

	@Override
	public Component render(String componentId, Range highlight) {
		return new HighlightableLabel(componentId, new QualifiedName(getName(), "@", null), highlight);
	}

}
