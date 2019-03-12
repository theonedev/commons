package io.onedev.commons.jsymbol.flowscript.symbols;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.resource.PackageResourceReference;

import io.onedev.commons.jsymbol.flowscript.symbols.ui.icon.IconLocator;
import io.onedev.commons.jsymbol.util.HighlightableLabel;
import io.onedev.commons.jsymbol.util.NoAntiCacheImage;
import io.onedev.commons.utils.Range;

/**
 * This symbol represents a ES6 class definition
 * 
 * @author robin
 *
 */
public class ClassSymbol extends FlowScriptSymbol {

	private static final long serialVersionUID = 1L;
	
	@Override
	public Component render(String componentId, Range highlight) {
		if (getName() != null)
			return new HighlightableLabel(componentId, getName(), highlight);
		else
			return new WebMarkupContainer(componentId).setVisible(false);
	}

	@Override
	public Image renderIcon(String componentId) {
		Image icon;
		if (getModuleAccess() == ModuleAccess.EXPORT) {
            icon = new NoAntiCacheImage(componentId, new PackageResourceReference(IconLocator.class, "exported_class.png"));
            icon.add(AttributeAppender.append("title", "exported class"));
        } else if (isLocal()) {
            icon = new NoAntiCacheImage(componentId, new PackageResourceReference(IconLocator.class, "local_class.png"));
            icon.add(AttributeAppender.append("title", "local class"));
		} else {
			icon = new NoAntiCacheImage(componentId, new PackageResourceReference(IconLocator.class, "class.png"));
			icon.add(AttributeAppender.append("title", "class"));
		}
		return icon;
	}

	@Override
	public boolean isPrimary() {
		return true;
	}

}
