package io.onedev.commons.jsymbol.cpp.symbols;

import javax.annotation.Nullable;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.resource.PackageResourceReference;

import io.onedev.commons.jsymbol.TokenPosition;
import io.onedev.commons.jsymbol.cpp.symbols.ui.SpecialFunctionSymbolPanel;
import io.onedev.commons.jsymbol.cpp.symbols.ui.icon.IconLocator;
import io.onedev.commons.jsymbol.util.NoAntiCacheImage;
import io.onedev.commons.utils.Range;

public class SpecialFunctionSymbol extends CppSymbol {

	private static final long serialVersionUID = 1L;
	
	private final boolean definition;
	
	private final String params;

	private Modifier modifier;
	
	public SpecialFunctionSymbol(CppSymbol parent, String name, boolean local, boolean definition, @Nullable String params, 
			 TokenPosition position, TokenPosition scope, Modifier modifier) {
		super(parent, name, local, position, scope);
		
		this.definition = definition;
		this.params = params;
		this.modifier = modifier;
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
	public Modifier getModifier(){
		return modifier;
	}
	@Override
	public Image renderIcon(String componentId) {
		String loadIcon = "function_obj.png";
		switch(modifier){
		case NORMAL:
			loadIcon="function_obj.png";
			break;
		case PRIVATE:
			loadIcon="pri_func_obj.png";
			break;
		case PROTECTED:
			loadIcon="pro_func_obj.png";
			break;
		case PUBLIC:
			loadIcon="function_obj.png";
			break;
		case FRIENDFUNC:
			loadIcon="friend_func_obj.png";
			break;
		case MACRO:
			loadIcon="macro_obj.png";
			break;
		default:
			break;
		}
		String symbolType = "function";
		if(modifier == Modifier.MACRO){
			symbolType = "macro";
		}
		Image icon = new NoAntiCacheImage(componentId, 
				new PackageResourceReference(IconLocator.class, loadIcon));
        icon.add(AttributeAppender.append("title", symbolType));
        return icon;
	}

	@Override
	public Component render(String componentId, Range highlight) {
		return new SpecialFunctionSymbolPanel(componentId, this, highlight);
	}

}
