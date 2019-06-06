package io.onedev.commons.jsymbol.cpp.symbols;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.resource.PackageResourceReference;

import io.onedev.commons.utils.PlanarRange;
import io.onedev.commons.jsymbol.cpp.symbols.ui.VariableSymbolPanel;
import io.onedev.commons.jsymbol.cpp.symbols.ui.icon.IconLocator;
import io.onedev.commons.jsymbol.util.NoAntiCacheImage;
import io.onedev.commons.utils.LinearRange;

public class VariableSymbol extends CppSymbol {

	private static final long serialVersionUID = 1L;
	
	private final String type;
	
	private boolean isTemp;
	
	private List<Modifier> modifiers;
	
	public VariableSymbol(CppSymbol parent, String name, boolean local, String type, PlanarRange position, List<Modifier> modifiers, boolean isTemp) {
		super(parent, name, local, position, null);
		this.type = type;
		this.modifiers=modifiers;
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
		String loadIcon = "variable_obj.png";
		if(isTemp){
			loadIcon="template_obj.png";
		}
		if(modifiers.contains(Modifier.NORMAL)){
			loadIcon="variable_obj.png";
			if(isTemp){
				loadIcon="template_obj.png";
			}
			if(modifiers.contains(Modifier.CONSTANT)){
				loadIcon="const_var_obj.png";
			}
		}else if(modifiers.contains(Modifier.PRIVATE)){
			loadIcon="pri_var_obj.png";
			if(isTemp){
				loadIcon="pri_temp_obj.png";
			}
			if(modifiers.contains(Modifier.CONSTANT)){
				loadIcon="pri_const_var_obj.png";
			}else if(modifiers.contains(Modifier.FRIENDVAR)){
				loadIcon = "friend_var_obj.png";
				if(isTemp){
					loadIcon="friend_temp_obj.png";
				}
			}
		}else if(modifiers.contains(Modifier.PROTECTED)){
			loadIcon="pro_var_obj.png";
			if(isTemp){
				loadIcon="pro_temp_obj.png";
			}
			if(modifiers.contains(Modifier.CONSTANT)){
				loadIcon="pro_const_var_obj.png";
			}else if(modifiers.contains(Modifier.FRIENDVAR)){
				loadIcon = "friend_var_obj.png";
				if(isTemp){
					loadIcon="friend_temp_obj.png";
				}
			}
		}else if(modifiers.contains(Modifier.PUBLIC)){
			loadIcon="variable_obj.png";
			if(isTemp){
				loadIcon="template_obj.png";
			}
			if(modifiers.contains(Modifier.CONSTANT)){
				loadIcon="const_var_obj.png";
			}else if(modifiers.contains(Modifier.FRIENDVAR)){
				loadIcon = "friend_var_obj.png";
				if(isTemp){
					loadIcon="friend_temp_obj.png";
				}
			}
		}
		Image icon = new NoAntiCacheImage(componentId, 
            		new PackageResourceReference(IconLocator.class, loadIcon));
		icon.add(AttributeAppender.append("title", "variable"));
        return icon;
	}

	@Override
	public Component render(String componentId, LinearRange highlight) {
		return new VariableSymbolPanel(componentId, this, highlight);
	}

}
