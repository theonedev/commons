package io.onedev.commons.jsymbol.python.symbols;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.resource.PackageResourceReference;

import io.onedev.commons.jsymbol.TokenPosition;
import io.onedev.commons.jsymbol.python.symbols.ui.icons.IconLocator;
import io.onedev.commons.jsymbol.util.HighlightableLabel;
import io.onedev.commons.jsymbol.util.NoAntiCacheImage;
import io.onedev.commons.utils.Range;

public class VariableSymbol extends PythonSymbol {

	private static final long serialVersionUID = 1L;

	private boolean attribute;
	
	public VariableSymbol(PythonSymbol parent, String name, TokenPosition position, TokenPosition scope, 
			boolean attribute) {
		super(parent, name, position, scope);
		this.attribute = attribute;
	}

	@Override
	public boolean isLocal() {
		return getName().startsWith("_") || !attribute && getParent() != null;
	}

	@Override
	public boolean isPrimary() {
		return false;
	}

	public boolean isAttribute() {
		return attribute;
	}

	public void setAttribute(boolean attribute) {
		this.attribute = attribute;
	}

	@Override
	public Image renderIcon(String componentId) {
		String iconName;
		if (getName().startsWith("_"))
			iconName = "field_private_obj.png";
		else
			iconName = "field_public_obj.png";
		
		Image icon = new NoAntiCacheImage(componentId, 
				new PackageResourceReference(IconLocator.class, iconName));
		icon.add(AttributeAppender.append("title", "variable"));
		return icon;
	}

	@Override
	public Component render(String componentId, Range highlight) {
		return new HighlightableLabel(componentId, getName(), highlight);
	}

}
