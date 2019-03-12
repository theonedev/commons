package io.onedev.commons.jsymbol.r.symbols;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.resource.PackageResourceReference;

import io.onedev.commons.jsymbol.TokenPosition;
import io.onedev.commons.jsymbol.r.symbols.icon.IconLocator;
import io.onedev.commons.jsymbol.util.HighlightableLabel;
import io.onedev.commons.jsymbol.util.NoAntiCacheImage;
import io.onedev.commons.jsymbol.util.QualifiedName;
import io.onedev.commons.utils.Range;

/**
 * Represents a method declaration
 * @author robin
 *
 */
public class FunctionSymbol extends RSymbol {

	private static final long serialVersionUID = 1L;
	
	private final String parameters;
	
	private final boolean local;
	
	public FunctionSymbol(RSymbol parent, String name, TokenPosition position, TokenPosition scope, 
			String parameters, boolean local) {
		super(parent, name, position, scope);
		this.parameters = parameters;
		this.local = local;
	}
	
	public String getParameters() {
		return parameters;
	}

	@Override
	public Component render(String componentId, Range highlight) {
		return new HighlightableLabel(componentId, new QualifiedName(getName(), null, parameters), highlight);
	}

	@Override
	public Image renderIcon(String componentId) {
		Image icon = new NoAntiCacheImage(componentId, 
				new PackageResourceReference(IconLocator.class, "function.png"));
		icon.add(AttributeAppender.append("title", "function"));
		return icon;
	}

	@Override
	public boolean isPrimary() {
		return true;
	}

	@Override
	public boolean isLocal() {
		return local;
	}
	
}
