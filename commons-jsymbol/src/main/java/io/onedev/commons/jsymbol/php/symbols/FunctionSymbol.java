package io.onedev.commons.jsymbol.php.symbols;

import org.jspecify.annotations.Nullable;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.resource.PackageResourceReference;

import io.onedev.commons.utils.PlanarRange;
import io.onedev.commons.jsymbol.php.symbols.icon.IconLocator;
import io.onedev.commons.jsymbol.util.HighlightableLabel;
import io.onedev.commons.jsymbol.util.NoAntiCacheImage;
import io.onedev.commons.jsymbol.util.QualifiedName;
import io.onedev.commons.utils.LinearRange;

public class FunctionSymbol extends PhpSymbol {

	private static final long serialVersionUID = 1L;
	
	private final Visibility visibility;
	
	private final String params;
	
	private final String returnType;

	public FunctionSymbol(PhpSymbol parent, String name, Visibility visibility, String params, 
			@Nullable String returnType, PlanarRange position, PlanarRange scope) {
		super(parent, name, position, scope);
		this.visibility = visibility;
		this.params = params;
		this.returnType = returnType;
	}

	@Override
	public boolean isSearchable() {
		if (getName().equals("__construct") || getName().equals("__destruct"))
			return false;
		else
			return super.isSearchable();
	}

	public String getParams() {
		return params;
	}

	@Nullable
	public String getReturnType() {
		return returnType;
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
					new PackageResourceReference(IconLocator.class, "methpri_obj.png"));
		} else if (visibility == Visibility.PROTECTED) {
			return new NoAntiCacheImage(componentId, 
					new PackageResourceReference(IconLocator.class, "methpro_obj.png"));
		} else {
			return new NoAntiCacheImage(componentId, 
					new PackageResourceReference(IconLocator.class, "methpub_obj.png"));
		}
	}

	@Override
	public Component render(String componentId, LinearRange highlight) {
		QualifiedName qualifiedName;
		if (returnType != null)
			qualifiedName = new QualifiedName(getName(), null, params + returnType);
		else
			qualifiedName = new QualifiedName(getName(), null, params);
		return new HighlightableLabel(componentId, qualifiedName, highlight);
	}
	
}
