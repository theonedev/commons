package io.onedev.commons.jsymbol.scss.symbols;

import javax.annotation.Nullable;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.resource.PackageResourceReference;

import io.onedev.commons.jsymbol.TokenPosition;
import io.onedev.commons.jsymbol.scss.symbols.icon.IconLocator;
import io.onedev.commons.jsymbol.util.HighlightableLabel;
import io.onedev.commons.jsymbol.util.NoAntiCacheImage;
import io.onedev.commons.jsymbol.util.QualifiedName;
import io.onedev.commons.utils.Range;

public class FunctionSymbol extends ScssSymbol {

	private static final long serialVersionUID = 1L;
	
	private final String params;
	
	private final boolean local;

	public FunctionSymbol(ScssSymbol parent, String name, @Nullable String params, TokenPosition position, 
			TokenPosition scope, boolean local) {
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

	@Nullable
	public String getParams() {
		return params;
	}

	@Override
	public Image renderIcon(String componentId) {
		Image icon = new NoAntiCacheImage(componentId, 
				new PackageResourceReference(IconLocator.class, "rule.png"));
		icon.add(AttributeAppender.append("title", "SCSS function"));
		return icon;
	}

	@Override
	public Component render(String componentId, Range highlight) {
		QualifiedName name;
		if (params != null)
			name = new QualifiedName(getName(), "@function ", params);
		else
			name = new QualifiedName(getName(), "@function ", params);
		return new HighlightableLabel(componentId, name, highlight);
	}

}
