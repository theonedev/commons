package io.onedev.commons.jsymbol.less.symbols;

import org.jspecify.annotations.Nullable;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.resource.PackageResourceReference;

import io.onedev.commons.utils.PlanarRange;
import io.onedev.commons.jsymbol.less.symbols.icon.IconLocator;
import io.onedev.commons.jsymbol.util.HighlightableLabel;
import io.onedev.commons.jsymbol.util.NoAntiCacheImage;
import io.onedev.commons.jsymbol.util.QualifiedName;
import io.onedev.commons.utils.LinearRange;

public class MixinSymbol extends LessSymbol {

	private static final long serialVersionUID = 1L;

	private final String prefix;
	
	private final String params;
	
	public MixinSymbol(LessSymbol parent, String name, @Nullable String prefix, String params, PlanarRange position, 
			PlanarRange scope) {
		super(parent, name, position, scope);
		this.prefix = prefix;
		this.params = params;
	}

	@Override
	public boolean isPrimary() {
		return false;
	}

	@Override
	public boolean isLocal() {
		return false;
	}

	public String getParams() {
		return params;
	}

	@Nullable
	public String getPrefix() {
		return prefix;
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
		return new HighlightableLabel(componentId, new QualifiedName(getName(), prefix, params), highlight);
	}

}
