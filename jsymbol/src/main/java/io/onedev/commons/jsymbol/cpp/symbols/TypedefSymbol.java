package io.onedev.commons.jsymbol.cpp.symbols;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.resource.PackageResourceReference;

import io.onedev.commons.jsymbol.TokenPosition;
import io.onedev.commons.jsymbol.cpp.symbols.ui.TypedefSymbolPanel;
import io.onedev.commons.jsymbol.cpp.symbols.ui.icon.IconLocator;
import io.onedev.commons.jsymbol.util.NoAntiCacheImage;
import io.onedev.commons.utils.Range;

public class TypedefSymbol extends CppSymbol {

	private static final long serialVersionUID = 1L;
	
	private final String type;
	
	private boolean isTemp;
	
	private Modifier modifier;
	
	public TypedefSymbol(CppSymbol parent, String name, boolean local, String type, TokenPosition position, Modifier modifier, boolean isTemp) {
		super(parent, name, local, position, null);
		this.type = type;
		this.modifier=modifier;
		this.isTemp = isTemp;
	}

	@Override
	public boolean isPrimary() {
		return false;
	}

	public String getType() {
		return type;
	}

	@Override
	public Image renderIcon(String componentId) {
		String loadIcon = "typedef_obj.png";
		switch(modifier){
		case NORMAL:
			loadIcon="typedef_obj.png";
			if(isTemp){
				loadIcon="temp_obj.png";
			}
			break;
		case PRIVATE:
			loadIcon="pri_typedef_obj.png";
			if(isTemp){
				loadIcon="pri_temp_obj.png";
			}
			break;
		case PROTECTED:
			loadIcon="pro_typedef_obj.png";
			if(isTemp){
				loadIcon="pro_temp_obj.png";
			}
			break;
		case PUBLIC:
			loadIcon="typedef_obj.png";
			if(isTemp){
				loadIcon="template_obj.png";
			}
			break;
		case FRIENDTYPEDEF:
			loadIcon="friend_typedef_obj.png";
			if(isTemp){
				loadIcon="friend_temp_obj.png";
			}
		default:
			break;
		}
		Image icon = new NoAntiCacheImage(componentId, 
            		new PackageResourceReference(IconLocator.class, loadIcon));
		icon.add(AttributeAppender.append("title", "typedef"));
        return icon;
	}

	@Override
	public Component render(String componentId, Range highlight) {
		return new TypedefSymbolPanel(componentId, this, highlight);
	}

}
