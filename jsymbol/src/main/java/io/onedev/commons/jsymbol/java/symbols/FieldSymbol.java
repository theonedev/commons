package io.onedev.commons.jsymbol.java.symbols;

import java.util.EnumSet;

import javax.annotation.Nullable;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.resource.PackageResourceReference;

import com.github.javaparser.ast.Modifier;

import io.onedev.commons.jsymbol.TokenPosition;
import io.onedev.commons.jsymbol.java.symbols.ui.FieldSymbolPanel;
import io.onedev.commons.jsymbol.java.symbols.ui.icon.IconLocator;
import io.onedev.commons.jsymbol.util.NoAntiCacheImage;
import io.onedev.commons.utils.Range;

public class FieldSymbol extends JavaSymbol {

	private static final long serialVersionUID = 1L;

	private final String type;
	
	private final EnumSet<Modifier> modifiers;
	
	public FieldSymbol(TypeSymbol parent, String fieldName, TokenPosition position, TokenPosition scope, 
			@Nullable String type, EnumSet<Modifier> modifiers) {
		super(parent, fieldName, position, scope);
		
		this.type = type;
		this.modifiers = modifiers;
	}
	
	/**
	 * Get type of this field.
	 * 
	 * @return 
	 * 			type of this field, or <tt>null</tt> for enum constant
	 */
	@Nullable
	public String getType() {
		return type;
	}

	public EnumSet<Modifier> getModifiers() {
		return modifiers;
	}

	@Override
	public boolean isPrimary() {
		return false;
	}

	@Override
	public Component render(String componentId, Range highlight) {
		return new FieldSymbolPanel(componentId, this, highlight);
	}

	@Override
	public Image renderIcon(String componentId) {
		Image icon;
		if (modifiers.contains(Modifier.PRIVATE)) {
			icon = new NoAntiCacheImage(componentId, new PackageResourceReference(IconLocator.class, "field_private_obj.png"));
			icon.add(AttributeAppender.append("title", "private field"));
		}  else if (modifiers.contains(Modifier.PROTECTED)) {
			icon = new NoAntiCacheImage(componentId, new PackageResourceReference(IconLocator.class, "field_protected_obj.png"));
			icon.add(AttributeAppender.append("title", "protected field"));
		} else if (modifiers.contains(Modifier.PUBLIC)) {
			icon = new NoAntiCacheImage(componentId, new PackageResourceReference(IconLocator.class, "field_public_obj.png"));
			icon.add(AttributeAppender.append("title", "public field"));
		} else {
			icon = new NoAntiCacheImage(componentId, new PackageResourceReference(IconLocator.class, "field_default_obj.png"));
			icon.add(AttributeAppender.append("title", "field"));
		}
		return icon;
	}

	@Override
	public boolean isLocal() {
		return modifiers.contains(Modifier.PRIVATE);
	}

}
