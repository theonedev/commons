package io.onedev.commons.jsymbol.cpp.symbols;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.resource.PackageResourceReference;

import io.onedev.commons.jsymbol.TokenPosition;
import io.onedev.commons.jsymbol.cpp.symbols.ui.icon.IconLocator;
import io.onedev.commons.jsymbol.util.HighlightableLabel;
import io.onedev.commons.jsymbol.util.NoAntiCacheImage;
import io.onedev.commons.utils.Range;

public class NamespaceSymbol extends CppSymbol {

	private static final long serialVersionUID = 1L;
	
	private boolean isTemp;
	
	private Modifier modifier;
	
	public NamespaceSymbol(CppSymbol parent, String name, boolean local, TokenPosition position, 
			TokenPosition scope, Modifier modifier, boolean isTemp) {
		super(parent, name, local, position, scope);
		this.modifier=modifier;
		this.isTemp = isTemp;
	}

	@Override
	public boolean isPrimary() {
		return true;
	}

	@Override
	public Image renderIcon(String componentId) {
		String loadIcon = "namespace_obj.png";
		switch(modifier){
		case NORMAL:
			loadIcon="namespace_obj.png";
			if(isTemp){
				loadIcon="template_obj.png";
			}
			break;
		case PRIVATE:
			loadIcon="pri_name_obj.png";
			if(isTemp){
				loadIcon="pri_temp_obj.png";
			}
			break;
		case PROTECTED:
			loadIcon="pro_name_obj.png";
			if(isTemp){
				loadIcon="pro_temp_obj.png";
			}
			break;
		case PUBLIC:
			loadIcon="namespace_obj.png";
			if(isTemp){
				loadIcon="template_obj.png";
			}
			break;
		case FRIENDNAMESPACE:
			loadIcon="friend_name_obj.png";
			if(isTemp){
				loadIcon="friend_temp_obj.png";
			}
			break;
		default:
			break;
		}
		Image icon = new NoAntiCacheImage(componentId, 
            		new PackageResourceReference(IconLocator.class, loadIcon));
        icon.add(AttributeAppender.append("title", "namespace"));
        return icon;
	}

	@Override
	public Component render(String componentId, Range highlight) {
		return new HighlightableLabel(componentId, getName(), highlight);
	}

}
