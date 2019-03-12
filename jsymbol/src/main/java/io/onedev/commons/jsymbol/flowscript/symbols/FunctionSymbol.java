package io.onedev.commons.jsymbol.flowscript.symbols;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.resource.PackageResourceReference;

import io.onedev.commons.jsymbol.flowscript.symbols.ui.FunctionSymbolPanel;
import io.onedev.commons.jsymbol.flowscript.symbols.ui.icon.IconLocator;
import io.onedev.commons.jsymbol.util.NoAntiCacheImage;
import io.onedev.commons.utils.Range;

/**
 * Represents a function declaration
 * @author robin
 *
 */
public class FunctionSymbol extends FlowScriptSymbol {

	private static final long serialVersionUID = 1L;
	
	private String parameters;
	
	public String getParameters() {
		return parameters;
	}

	public void setParameters(String parameters) {
		this.parameters = parameters;
	}

	@Override
	public Component render(String componentId, Range highlight) {
		return new FunctionSymbolPanel(componentId, this, highlight);
	}

	@Override
	public Image renderIcon(String componentId) {
		Image icon;
		if (getModuleAccess() == ModuleAccess.EXPORT) {
            icon = new NoAntiCacheImage(componentId, new PackageResourceReference(IconLocator.class, "exported_function.png"));
            icon.add(AttributeAppender.append("title", "exported function"));
        } else if (isLocal()) {
            icon = new NoAntiCacheImage(componentId, new PackageResourceReference(IconLocator.class, "local_function.png"));
            icon.add(AttributeAppender.append("title", "local function"));
		} else {
			icon = new NoAntiCacheImage(componentId, new PackageResourceReference(IconLocator.class, "function.png"));
			icon.add(AttributeAppender.append("title", "function"));
		}
		return icon;
	}
	
}
