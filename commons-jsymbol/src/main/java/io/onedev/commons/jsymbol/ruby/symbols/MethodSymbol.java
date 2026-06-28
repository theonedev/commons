package io.onedev.commons.jsymbol.ruby.symbols;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.jspecify.annotations.Nullable;

import io.onedev.commons.jsymbol.ruby.symbols.ui.icons.IconLocator;
import io.onedev.commons.jsymbol.util.HighlightableLabel;
import io.onedev.commons.jsymbol.util.NoAntiCacheImage;
import io.onedev.commons.utils.LinearRange;
import io.onedev.commons.utils.PlanarRange;

public class MethodSymbol extends RubySymbol {

	private static final long serialVersionUID = 1L;

	private final String parameters;

	private final boolean singleton;

	public MethodSymbol(@Nullable RubySymbol parent, String name, String parameters, PlanarRange position,
			@Nullable PlanarRange scope, boolean singleton) {
		super(parent, name, position, scope);
		this.parameters = parameters;
		this.singleton = singleton;
	}

	public String getParameters() {
		return parameters;
	}

	public boolean isSingleton() {
		return singleton;
	}

	@Override
	public boolean isLocal() {
		return getName().startsWith("_") || getParent() != null && !(getParent() instanceof TypeSymbol);
	}

	@Override
	public boolean isPrimary() {
		return getParent() == null;
	}

	@Override
	public Image renderIcon(String componentId) {
		Image icon = new NoAntiCacheImage(componentId, new PackageResourceReference(IconLocator.class,
				isLocalInHierarchy()? "methpri_obj.png": "methpub_obj.png"));
		icon.add(AttributeAppender.append("title", singleton? "singleton method": "method"));
		return icon;
	}

	@Override
	public Component render(String componentId, @Nullable LinearRange highlight) {
		return new HighlightableLabel(componentId, getName() + parameters, highlight);
	}

}
