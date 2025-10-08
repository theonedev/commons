package io.onedev.commons.jsymbol.go.symbols;

import io.onedev.commons.jsymbol.go.symbols.ui.VariableSymbolPanel;
import io.onedev.commons.jsymbol.go.symbols.ui.icon.IconLocator;
import io.onedev.commons.jsymbol.util.NoAntiCacheImage;
import io.onedev.commons.utils.LinearRange;
import io.onedev.commons.utils.PlanarRange;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.resource.PackageResourceReference;

import org.jspecify.annotations.Nullable;

public class VariableSymbol extends GolangSymbol {

	private static final long serialVersionUID = 1L;

	private String type;
	
	private final boolean searchable;
	
	public VariableSymbol(@Nullable GolangSymbol parent, String name, @Nullable String type, PlanarRange position, 
			boolean searchable) {
		super(parent, name, position, null);

		this.type = type;
		this.searchable = searchable;
	}
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	public boolean isSearchable() {
		return searchable;
	}

	@Override
	public Component render(String componentId, LinearRange highlight) {
		return new VariableSymbolPanel(componentId, this, highlight);
	}

	@Override
	public Image renderIcon(String componentId) {
		String icon = Character.isUpperCase(getName().charAt(0))? "Field_blue_16x.png": "FieldPrivate_16x.png";
		return new NoAntiCacheImage(componentId, new PackageResourceReference(IconLocator.class, icon));
	}

	@Override
	public boolean isLocal() {
		return false;
	}

}
