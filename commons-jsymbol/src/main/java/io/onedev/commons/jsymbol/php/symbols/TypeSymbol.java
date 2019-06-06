package io.onedev.commons.jsymbol.php.symbols;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.resource.PackageResourceReference;

import io.onedev.commons.utils.PlanarRange;
import io.onedev.commons.jsymbol.php.symbols.icon.IconLocator;
import io.onedev.commons.jsymbol.util.HighlightableLabel;
import io.onedev.commons.jsymbol.util.NoAntiCacheImage;
import io.onedev.commons.utils.LinearRange;

public class TypeSymbol extends PhpSymbol {

	private static final long serialVersionUID = 1L;

	public enum Kind {CLASS, TRAIT, INTERFACE};
	
	private final Kind kind;
	
	public TypeSymbol(PhpSymbol parent, String name, Kind kind, PlanarRange position, PlanarRange scope) {
		super(parent, name, position, scope);
		this.kind = kind;
	}

	@Override
	public boolean isLocal() {
		return false;
	}

	@Override
	public boolean isPrimary() {
		return true;
	}

	public Kind getKind() {
		return kind;
	}

	@Override
	public Image renderIcon(String componentId) {
		if (kind == Kind.TRAIT) {
			return new NoAntiCacheImage(componentId, 
					new PackageResourceReference(IconLocator.class, "trait_obj.png"));
		} else if (kind == Kind.CLASS) {
			return new NoAntiCacheImage(componentId, 
					new PackageResourceReference(IconLocator.class, "classes_icon.png"));
		} else {
			return new NoAntiCacheImage(componentId, 
					new PackageResourceReference(IconLocator.class, "interfaces_icon.png"));
		}
	}

	@Override
	public Component render(String componentId, LinearRange highlight) {
		return new HighlightableLabel(componentId, getName(), highlight);
	}
	
}
