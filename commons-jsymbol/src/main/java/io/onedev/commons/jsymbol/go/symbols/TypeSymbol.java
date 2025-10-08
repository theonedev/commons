package io.onedev.commons.jsymbol.go.symbols;

import org.jspecify.annotations.Nullable;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.resource.PackageResourceReference;

import io.onedev.commons.utils.PlanarRange;
import io.onedev.commons.jsymbol.go.symbols.ui.icon.IconLocator;
import io.onedev.commons.jsymbol.util.HighlightableLabel;
import io.onedev.commons.jsymbol.util.NoAntiCacheImage;
import io.onedev.commons.utils.LinearRange;

public class TypeSymbol extends GolangSymbol {

	private static final long serialVersionUID = 1L;
	
	private final boolean intf;
	
	private final boolean searchable;

	public TypeSymbol(@Nullable GolangSymbol parent, String name, PlanarRange position, 
			PlanarRange scope, boolean intf, boolean searchable) {
		super(parent, name, position, scope);
		this.intf = intf;
		this.searchable = searchable;
	}
	
	@Override
	public Component render(String componentId, LinearRange highlight) {
		return new HighlightableLabel(componentId, getName(), highlight);
	}

	@Override
	public Image renderIcon(String componentId) {
		String icon;
		if (intf) {
			icon = Character.isUpperCase(getName().charAt(0))? "Interface_blue_16x.png": "InterfacePrivate_16x.png";
		} else {
			icon = Character.isUpperCase(getName().charAt(0))? "Structure_16x.png": "StructurePrivate_16x.png";
		}
		return new NoAntiCacheImage(componentId, new PackageResourceReference(IconLocator.class, icon));
	}

	@Override
	public boolean isLocal() {
		return false;
	}

	@Override
	public boolean isSearchable() {
		return searchable;
	}

}
