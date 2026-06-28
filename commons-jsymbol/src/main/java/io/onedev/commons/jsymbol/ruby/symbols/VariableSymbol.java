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

public class VariableSymbol extends RubySymbol {

	private static final long serialVersionUID = 1L;

	private final String kind;

	private final boolean local;

	public VariableSymbol(@Nullable RubySymbol parent, String name, String kind, PlanarRange position,
			@Nullable PlanarRange scope, boolean local) {
		super(parent, name, position, scope);
		this.kind = kind;
		this.local = local;
	}

	public String getKind() {
		return kind;
	}

	@Override
	public boolean isLocal() {
		return local || getName().startsWith("_");
	}

	@Override
	public boolean isPrimary() {
		return false;
	}

	@Override
	public Image renderIcon(String componentId) {
		Image icon = new NoAntiCacheImage(componentId, new PackageResourceReference(IconLocator.class,
				isLocalInHierarchy()? "field_private_obj.png": "field_public_obj.png"));
		icon.add(AttributeAppender.append("title", kind));
		return icon;
	}

	@Override
	public Component render(String componentId, @Nullable LinearRange highlight) {
		return new HighlightableLabel(componentId, getName(), highlight);
	}

}
