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

public class TypeSymbol extends RubySymbol {

	private static final long serialVersionUID = 1L;

	private final String kind;

	public TypeSymbol(@Nullable RubySymbol parent, String name, String kind, PlanarRange position,
			@Nullable PlanarRange scope) {
		super(parent, name, position, scope);
		this.kind = kind;
	}

	public String getKind() {
		return kind;
	}

	@Override
	public boolean isLocal() {
		return getName().startsWith("_");
	}

	@Override
	public boolean isPrimary() {
		return true;
	}

	@Override
	public Image renderIcon(String componentId) {
		String iconName = "module".equals(kind)? "innerclass_private_obj.png": "class_obj.png";
		Image icon = new NoAntiCacheImage(componentId, new PackageResourceReference(IconLocator.class, iconName));
		icon.add(AttributeAppender.append("title", kind));
		return icon;
	}

	@Override
	public Component render(String componentId, @Nullable LinearRange highlight) {
		return new HighlightableLabel(componentId, kind + " " + getName(), highlight);
	}

}
