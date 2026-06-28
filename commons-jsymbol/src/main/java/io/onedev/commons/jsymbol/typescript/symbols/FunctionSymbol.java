package io.onedev.commons.jsymbol.typescript.symbols;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.jspecify.annotations.Nullable;

import io.onedev.commons.jsymbol.util.HighlightableLabel;
import io.onedev.commons.jsymbol.util.NoAntiCacheImage;
import io.onedev.commons.jsymbol.typescript.symbols.ui.icon.IconLocator;
import io.onedev.commons.utils.LinearRange;
import io.onedev.commons.utils.PlanarRange;

public class FunctionSymbol extends TypeScriptSymbol {

	private static final long serialVersionUID = 1L;
	
	private final String parameters;
	
	public FunctionSymbol(@Nullable TypeScriptSymbol parent, String name, String parameters, 
			PlanarRange position, @Nullable PlanarRange scope, boolean local) {
		super(parent, name, position, scope, local);
		this.parameters = parameters;
	}
	
	public String getParameters() {
		return parameters;
	}

	@Override
	public boolean isPrimary() {
		return getParent() == null;
	}

	@Override
	public Image renderIcon(String componentId) {
		Image icon = new NoAntiCacheImage(componentId, new PackageResourceReference(IconLocator.class, 
				isLocalInHierarchy()? "methpri_obj.png": "methpub_obj.png"));
		icon.add(AttributeAppender.append("title", getParent()!=null? "method": "function"));
		return icon;
	}

	@Override
	public Component render(String componentId, @Nullable LinearRange highlight) {
		return new HighlightableLabel(componentId, getName() + parameters, highlight);
	}
	
}
