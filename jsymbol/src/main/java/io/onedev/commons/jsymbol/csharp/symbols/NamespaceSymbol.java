package io.onedev.commons.jsymbol.csharp.symbols;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.resource.PackageResourceReference;

import io.onedev.commons.jsymbol.TokenPosition;
import io.onedev.commons.jsymbol.csharp.symbols.ui.icon.IconLocator;
import io.onedev.commons.jsymbol.util.HighlightableLabel;
import io.onedev.commons.jsymbol.util.NoAntiCacheImage;
import io.onedev.commons.utils.Range;

public class NamespaceSymbol extends CSharpSymbol {

	private static final long serialVersionUID = 1L;

	public NamespaceSymbol(CSharpSymbol parent, String name, TokenPosition position, TokenPosition scope) {
		super(parent, name, position, scope);
	}

	@Override
	public boolean isPrimary() {
		return false;
	}

	@Override
	public boolean isDisplayInOutline() {
		return false;
	}

	@Override
	public Image renderIcon(String componentId) {
		Image icon = new NoAntiCacheImage("icon", new PackageResourceReference(IconLocator.class, "Namespace_16x.png"));
		icon.add(AttributeAppender.append("title", "namespace"));
		return icon;
	}

	@Override
	public Component render(String componentId, Range highlight) {
		return new HighlightableLabel(componentId, getName(), highlight);
	}

	@Override
	public boolean isLocal() {
		return false;
	}

}
