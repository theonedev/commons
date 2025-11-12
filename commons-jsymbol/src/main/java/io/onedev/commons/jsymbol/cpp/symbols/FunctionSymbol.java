package io.onedev.commons.jsymbol.cpp.symbols;

import org.jspecify.annotations.Nullable;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.resource.PackageResourceReference;

import io.onedev.commons.utils.PlanarRange;
import io.onedev.commons.jsymbol.cpp.symbols.ui.FunctionSymbolPanel;
import io.onedev.commons.jsymbol.cpp.symbols.ui.icon.IconLocator;
import io.onedev.commons.jsymbol.util.NoAntiCacheImage;
import io.onedev.commons.utils.LinearRange;

public class FunctionSymbol extends CppSymbol {

	private static final long serialVersionUID = 1L;
	
	private final boolean definition;
	
	private final String params;

	private final String returnType;
	
	private Modifier modifier;
	
    private boolean isTemp;
	
	public FunctionSymbol(CppSymbol parent, String name, boolean local, boolean definition, @Nullable String params, 
			@Nullable String type, PlanarRange position, PlanarRange scope, Modifier modifier, boolean isTemp) {
		super(parent, name, local, position, scope);
		
		this.definition = definition;
		this.params = params;
		this.returnType = type;
        this.modifier=modifier;
		this.isTemp = isTemp;
	}

	@Override
	public boolean isPrimary() {
		return true;
	}

	public boolean isDefinition() {
		return definition;
	}

	@Nullable
	public String getParams() {
		return params;
	}

	@Nullable
	public String getReturnType() {
		return returnType;
	}

	@Override
	public Image renderIcon(String componentId) {
		String loadIcon = "function_obj.png";
		switch(modifier){
		case NORMAL:
			loadIcon="function_obj.png";
			if(isTemp){
				loadIcon="template_obj.png";
			}
			break;
		case PRIVATE:
			loadIcon="pri_func_obj.png";
			if(isTemp){
				loadIcon="pri_temp_obj.png";
			}
			break;
		case PROTECTED:
			loadIcon="pro_func_obj.png";
			if(isTemp){
				loadIcon="pro_temp_obj.png";
			}
			break;
		case PUBLIC:
			loadIcon="function_obj.png";
			if(isTemp){
				loadIcon="template_obj.png";
			}
			break;
		case FRIENDFUNC:
			loadIcon="friend_func_obj.png";
			if(isTemp){
				loadIcon="friend_temp_obj.png";
			}
			break;
		default:
			break;
		}
		Image icon = new NoAntiCacheImage(componentId, 
				new PackageResourceReference(IconLocator.class, loadIcon));
        icon.add(AttributeAppender.append("title", "function"));
        return icon;
	}

	@Override
	public Component render(String componentId, LinearRange highlight) {
		return new FunctionSymbolPanel(componentId, this, highlight);
	}

}
