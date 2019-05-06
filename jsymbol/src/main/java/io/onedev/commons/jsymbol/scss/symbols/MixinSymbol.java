package io.onedev.commons.jsymbol.scss.symbols;

import javax.annotation.Nullable;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.resource.PackageResourceReference;

import io.onedev.commons.utils.PlanarRange;
import io.onedev.commons.jsymbol.scss.symbols.icon.IconLocator;
import io.onedev.commons.jsymbol.util.HighlightableLabel;
import io.onedev.commons.jsymbol.util.NoAntiCacheImage;
import io.onedev.commons.jsymbol.util.QualifiedName;
import io.onedev.commons.utils.LinearRange;

public class MixinSymbol extends ScssSymbol {

	private static final long serialVersionUID = 1L;
	
	private final String params;
	
	private final boolean local;

	public MixinSymbol(ScssSymbol parent, String name, @Nullable String params, PlanarRange position, 
			PlanarRange scope, boolean local) {
		super(parent, name, position, scope);
		this.params = params;
		this.local = local;
	}

	@Override
	public boolean isLocal() {
		return local;
	}

	@Override
	public boolean isPrimary() {
		return false;
	}

	public String getParams() {
		return params;
	}

	@Override
	public Image renderIcon(String componentId) {
		Image icon = new NoAntiCacheImage(componentId, 
				new PackageResourceReference(IconLocator.class, "rule.png"));
		icon.add(AttributeAppender.append("title", "SCSS mixin"));
		return icon;
	}

	@Override
	public Component render(String componentId, LinearRange highlight) {
		QualifiedName name;
		if (params != null)
			name = new QualifiedName(getName(), "@mixin ", params);
		else
			name = new QualifiedName(getName(), "@mixin ", params);
		return new HighlightableLabel(componentId, name, highlight);
	}

}
