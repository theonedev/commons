package io.onedev.commons.jsymbol.cpp.symbols;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.resource.PackageResourceReference;

import io.onedev.commons.utils.PlanarRange;
import io.onedev.commons.jsymbol.cpp.symbols.ui.SpecialVariableSymbolPanel;
import io.onedev.commons.jsymbol.cpp.symbols.ui.icon.IconLocator;
import io.onedev.commons.jsymbol.util.NoAntiCacheImage;
import io.onedev.commons.utils.LinearRange;

public class SpecialVariableSymbol extends CppSymbol {

	private static final long serialVersionUID = 1L;
	
	private Modifier modifier;
	
	public SpecialVariableSymbol(CppSymbol parent, String name, boolean local, PlanarRange position, Modifier modifier) {
		super(parent, name, local, position, null);
		this.modifier=modifier;
	}

	@Override
	public boolean isPrimary() {
		return false;
	}

	@Override
	public Image renderIcon(String componentId) {
		String loadIcon = "variable_obj.png";
		switch(modifier){
		case NORMAL:
			loadIcon="variable_obj.png";
			break;
		case PRIVATE:
			loadIcon="pri_var_obj.png";
			break;
		case PROTECTED:
			loadIcon="pro_var_obj.png";
			break;
		case PUBLIC:
			loadIcon="variable_obj.png";
			break;
		case FRIENDCLASS:
			loadIcon="friend_class_obj.png";
			break;
		case FRIENDSTRUCT:
			loadIcon="friend_struct_obj.png";
			break;
		case FRIENDUNION:
			loadIcon="friend_union_obj.png";
			break;
		case FRIENDENUM:
			loadIcon="friend_enum_obj.png";
			break;
		case FRIENDTYPEDEF:
			loadIcon="friend_typedef_obj.png";
			break;
		case FRIENDVAR:
			loadIcon="friend_var_obj.png";
			break;
		default:
			break;
		}
		Image icon = new NoAntiCacheImage(componentId, 
            		new PackageResourceReference(IconLocator.class, loadIcon));
		icon.add(AttributeAppender.append("title", "variable"));
        return icon;
	}

	@Override
	public Component render(String componentId, LinearRange highlight) {
		return new SpecialVariableSymbolPanel(componentId, this, highlight);
	}

}