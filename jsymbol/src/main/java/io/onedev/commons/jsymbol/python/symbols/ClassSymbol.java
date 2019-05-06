package io.onedev.commons.jsymbol.python.symbols;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.resource.PackageResourceReference;

import io.onedev.commons.utils.PlanarRange;
import io.onedev.commons.jsymbol.python.symbols.ui.ClassSymbolPanel;
import io.onedev.commons.jsymbol.python.symbols.ui.icons.IconLocator;
import io.onedev.commons.jsymbol.util.NoAntiCacheImage;
import io.onedev.commons.utils.LinearRange;

/**
 * Represents a method declaration
 * @author robin
 *
 */
public class ClassSymbol extends PythonSymbol {

	private static final long serialVersionUID = 1L;
	
	private final String parameters;
	
	public ClassSymbol(PythonSymbol parent, String name, PlanarRange position, PlanarRange scope, 
			String parameters) {
		super(parent, name, position, scope);
		this.parameters = parameters;
	}
	
	public String getParameters() {
		return parameters;
	}

	@Override
	public Component render(String componentId, LinearRange highlight) {
		return new ClassSymbolPanel(componentId, this, highlight);
	}

	@Override
	public Image renderIcon(String componentId) {
		String iconName;
		if (getName().startsWith("_"))
			iconName = "innerclass_private_obj.png";
		else
			iconName = "class_obj.png";
		Image icon = new NoAntiCacheImage(componentId, new PackageResourceReference(IconLocator.class, iconName));
		icon.add(AttributeAppender.append("title", "class"));
		return icon;
	}

	@Override
	public boolean isLocal() {
		return getName().startsWith("_") || !(getParent() instanceof ClassSymbol) && getParent() != null;
	}

	@Override
	public boolean isPrimary() {
		return true;
	}
	
}
