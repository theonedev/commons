package io.onedev.commons.jsymbol.cpp.symbols;

import javax.annotation.Nullable;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.resource.PackageResourceReference;

import io.onedev.commons.jsymbol.TokenPosition;
import io.onedev.commons.jsymbol.cpp.symbols.ui.MemberSymbolPanel;
import io.onedev.commons.jsymbol.cpp.symbols.ui.icon.IconLocator;
import io.onedev.commons.jsymbol.util.NoAntiCacheImage;
import io.onedev.commons.utils.Range;

public class MemberSymbol extends CppSymbol {

	private static final long serialVersionUID = 1L;
	
	private final String type;
	
	private Modifier modifier;
	
	public MemberSymbol(CppSymbol parent, String name, @Nullable String type, TokenPosition position ,Modifier modifier) {
		super(parent, name, false, position, null);
		this.type = type;
		this.modifier=modifier;
	}

	@Override
	public boolean isPrimary() {
		return false;
	}

	@Nullable
	public String getType() {
		return type;
	}

	@Override
	public Image renderIcon(String componentId) {
		String loadIcon = "enum_var_obj.png";
		switch(modifier){
		case NORMAL:
			loadIcon="enum_var_obj.png";
			break;
		case PRIVATE:
			loadIcon="pri_enum_var_obj.png";
			break;
		case PROTECTED:
			loadIcon="pro_enum_var_obj.png";
			break;
		case PUBLIC:
			loadIcon="enum_var_obj.png";
			break;
		default:
			break;
		}
		Image icon = new NoAntiCacheImage(componentId, 
				new PackageResourceReference(IconLocator.class, loadIcon));
        icon.add(AttributeAppender.append("title", "member"));
        return icon;
	}

	@Override
	public Component render(String componentId, Range highlight) {
		return new MemberSymbolPanel(componentId, this, highlight);
	}

}
