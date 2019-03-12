package io.onedev.commons.jsymbol.java.symbols;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.resource.PackageResourceReference;

import io.onedev.commons.jsymbol.TokenPosition;
import io.onedev.commons.jsymbol.java.symbols.ui.icon.IconLocator;
import io.onedev.commons.jsymbol.util.NoAntiCacheImage;
import io.onedev.commons.utils.Range;

/**
 * This symbol represents a Java package
 *  
 * @author robin
 *
 */
public class CompilationUnitSymbol extends JavaSymbol {
	
	private static final long serialVersionUID = 1L;
	
	public CompilationUnitSymbol(String packageName, TokenPosition position, TokenPosition scope) {
		super(null, packageName, position, scope);
	}
	
	@Override
	public Component render(String componentId, Range highlight) {
		return new Label(componentId, getName());
	}

	@Override
	public Image renderIcon(String componentId) {
		Image icon = new NoAntiCacheImage("icon", new PackageResourceReference(IconLocator.class, "package_obj.png"));
		icon.add(AttributeAppender.append("title", "package"));
		return icon;
	}

	@Override
	public boolean isDisplayInOutline() {
		return false;
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

}
