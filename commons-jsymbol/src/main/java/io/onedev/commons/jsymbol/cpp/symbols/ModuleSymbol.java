package io.onedev.commons.jsymbol.cpp.symbols;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.resource.PackageResourceReference;

import io.onedev.commons.jsymbol.cpp.symbols.ui.icon.IconLocator;
import io.onedev.commons.jsymbol.util.HighlightableLabel;
import io.onedev.commons.jsymbol.util.NoAntiCacheImage;
import io.onedev.commons.utils.LinearRange;
import io.onedev.commons.utils.PlanarRange;

public class ModuleSymbol extends CppSymbol {

	private static final long serialVersionUID = 1L;

	public ModuleSymbol(CppSymbol parent, String name, boolean local, PlanarRange position) {
		super(parent, name, local, position, null);
	}

	@Override
	public boolean isPrimary() {
		return true;
	}

	@Override
	public Image renderIcon(String componentId) {
		Image icon = new NoAntiCacheImage(componentId,
				new PackageResourceReference(IconLocator.class, "namespace_obj.png"));
		icon.add(AttributeAppender.append("title", "module"));
		return icon;
	}

	@Override
	public Component render(String componentId, LinearRange highlight) {
		return new HighlightableLabel(componentId, getName(), highlight);
	}

}
