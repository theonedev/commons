package io.onedev.commons.jsymbol.cpp.symbols;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.resource.PackageResourceReference;

import io.onedev.commons.utils.PlanarRange;
import io.onedev.commons.jsymbol.cpp.symbols.ui.icon.IconLocator;
import io.onedev.commons.jsymbol.util.HighlightableLabel;
import io.onedev.commons.jsymbol.util.NoAntiCacheImage;
import io.onedev.commons.utils.LinearRange;

public class MacroSymbol extends CppSymbol {

	private static final long serialVersionUID = 1L;
	
	private Modifier modifier;
	
	public MacroSymbol(CppSymbol parent, String name, boolean local, PlanarRange position ,Modifier modifier) {
		super(parent, name, local, position, null);
		this.modifier=modifier;
	}

	@Override
	public boolean isPrimary() {
		return true;
	}

	@Override
	public Image renderIcon(String componentId) {
		String loadIcon="macro_obj.png";
		switch(modifier){
		case NORMAL:
			loadIcon="macro_obj.png";
			break;
		case PRIVATE:
			loadIcon="pri_macro_obj.png";
			break;
		case PROTECTED:
			loadIcon="pro_macro_obj.png";
			break;
		case PUBLIC:
			loadIcon="macro_obj.png";
			break;
		default:
			break;
		}
		Image icon = new NoAntiCacheImage(componentId, 
				new PackageResourceReference(IconLocator.class, loadIcon));
        icon.add(AttributeAppender.append("title", "macro"));
        return icon;
	}

	@Override
	public Component render(String componentId, LinearRange highlight) {
		return new HighlightableLabel(componentId, getName(), highlight);
	}

}
