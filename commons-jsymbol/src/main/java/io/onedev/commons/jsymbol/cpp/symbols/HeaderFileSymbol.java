package io.onedev.commons.jsymbol.cpp.symbols;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.resource.PackageResourceReference;

import io.onedev.commons.jsymbol.cpp.symbols.ui.icon.IconLocator;
import io.onedev.commons.jsymbol.util.NoAntiCacheImage;
import io.onedev.commons.utils.LinearRange;

/**
 * This symbol represents C header file in order to show in which header file a symbol is defined
 * 
 * @author robin
 *
 */
public class HeaderFileSymbol extends CppSymbol {

	private static final long serialVersionUID = 1L;
	
	public HeaderFileSymbol(String name) {
		super(null, name, false, null, null);
	}

	@Override
	public boolean isSearchable() {
		return false;
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
		Image icon = new NoAntiCacheImage(componentId, 
				new PackageResourceReference(IconLocator.class, "CPPHeaderFile_16x.png"));
        icon.add(AttributeAppender.append("title", "c/c++ header file"));
        return icon;
	}

	@Override
	public Component render(String componentId, LinearRange highlight) {
		return new Label(componentId, getName());
	}

	@Override
	public String getFQNSeparator() {
		return ": ";
	}

}
