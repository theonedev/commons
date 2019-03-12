package io.onedev.commons.jsymbol.cpp.symbols;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.resource.PackageResourceReference;

import io.onedev.commons.jsymbol.cpp.symbols.ui.icon.IconLocator;
import io.onedev.commons.jsymbol.util.NoAntiCacheImage;
import io.onedev.commons.utils.Range;

/**
 * This symbol represents C source file in order to show in which source file a symbol is defined
 * 
 * @author robin
 *
 */
public class SourceFileSymbol extends CppSymbol {

	private static final long serialVersionUID = 1L;
	
	public SourceFileSymbol(String name) {
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
		Image icon;
		if (getName().endsWith(".cpp") || getName().endsWith(".cc") || getName().endsWith(".cxx")) {
			icon = new NoAntiCacheImage(componentId, 
					new PackageResourceReference(IconLocator.class, "CPPSourceFile_16x.png"));
	        icon.add(AttributeAppender.append("title", "CPP source file"));
		} else {
			icon = new NoAntiCacheImage(componentId, 
					new PackageResourceReference(IconLocator.class, "CPPSourceFile_16x.png"));
	        icon.add(AttributeAppender.append("title", "C source file"));
		}
        return icon;
	}

	@Override
	public Component render(String componentId, Range highlight) {
		return new Label(componentId, getName());
	}

	@Override
	public String getFQNSeparator() {
		return ": ";
	}

}
