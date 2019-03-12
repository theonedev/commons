package io.onedev.commons.jsymbol.php.symbols;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.resource.PackageResourceReference;

import io.onedev.commons.jsymbol.TokenPosition;
import io.onedev.commons.jsymbol.php.symbols.icon.IconLocator;
import io.onedev.commons.jsymbol.util.HighlightableLabel;
import io.onedev.commons.jsymbol.util.NoAntiCacheImage;
import io.onedev.commons.utils.Range;

public class VariableSymbol extends PhpSymbol {

	private static final long serialVersionUID = 1L;
	
	private final Visibility visibility;

	public VariableSymbol(PhpSymbol parent, String name, Visibility visibility, TokenPosition position) {
		super(parent, name, position, null);
		this.visibility = visibility;
	}

	@Override
	public boolean isLocal() {
		return visibility == Visibility.PRIVATE;
	}

	@Override
	public boolean isPrimary() {
		return true;
	}

	public Visibility getVisibility() {
		return visibility;
	}

	@Override
	public Image renderIcon(String componentId) {
		if (visibility == Visibility.PRIVATE) {
			return new NoAntiCacheImage(componentId, 
					new PackageResourceReference(IconLocator.class, "field_private_obj.png"));
		} else if (visibility == Visibility.PROTECTED) {
			return new NoAntiCacheImage(componentId, 
					new PackageResourceReference(IconLocator.class, "field_protected_obj.png"));
		} else {
			return new NoAntiCacheImage(componentId, 
					new PackageResourceReference(IconLocator.class, "field_public_obj.png"));
		}
	}

	@Override
	public Component render(String componentId, Range highlight) {
		return new HighlightableLabel(componentId, getName(), highlight);
	}
	
}
