package io.onedev.commons.jsymbol.rust.symbols;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.jspecify.annotations.Nullable;

import io.onedev.commons.jsymbol.rust.symbols.ui.icon.IconLocator;
import io.onedev.commons.jsymbol.util.HighlightableLabel;
import io.onedev.commons.jsymbol.util.NoAntiCacheImage;
import io.onedev.commons.utils.LinearRange;
import io.onedev.commons.utils.PlanarRange;

public class BlockSymbol extends RustSymbol {

	private static final long serialVersionUID = 1L;
	
	private final String kind;
	
	public BlockSymbol(@Nullable RustSymbol parent, String name, String kind, PlanarRange position,
			@Nullable PlanarRange scope, boolean local) {
		super(parent, name, position, scope, local);
		this.kind = kind;
	}
	
	public String getKind() {
		return kind;
	}

	@Override
	public boolean isLocal() {
		return false;
	}

	@Override
	public boolean isPrimary() {
		return false;
	}

	@Override
	public boolean isSearchable() {
		return false;
	}

	@Override
	public Image renderIcon(String componentId) {
		Image icon = new NoAntiCacheImage(componentId, new PackageResourceReference(IconLocator.class, "packd_obj.png"));
		icon.add(AttributeAppender.append("title", kind));
		return icon;
	}

	@Override
	public Component render(String componentId, @Nullable LinearRange highlight) {
		return new HighlightableLabel(componentId, getName(), highlight);
	}
	
}
