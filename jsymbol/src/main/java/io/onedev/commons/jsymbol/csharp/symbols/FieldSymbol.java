package io.onedev.commons.jsymbol.csharp.symbols;

import java.util.EnumSet;

import javax.annotation.Nullable;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.resource.PackageResourceReference;

import io.onedev.commons.jsymbol.TokenPosition;
import io.onedev.commons.jsymbol.csharp.symbols.ui.FieldSymbolPanel;
import io.onedev.commons.jsymbol.csharp.symbols.ui.icon.IconLocator;
import io.onedev.commons.jsymbol.util.NoAntiCacheImage;
import io.onedev.commons.utils.Range;

public class FieldSymbol extends CSharpSymbol {

	private static final long serialVersionUID = 1L;

	public enum Kind {ENUM_ITEM, NORMAL_FIELD, PROPERTY, EVENT};
	
	private final Kind kind;
	
	private final String type;
	
	private final String indexParams;
	
	private final EnumSet<Modifier> modifiers;
	
	public FieldSymbol(CSharpSymbol parent, Kind kind, String fieldName, TokenPosition position, TokenPosition scope, 
			@Nullable String type, @Nullable String indexParams, EnumSet<Modifier> modifiers) {
		super(parent, fieldName, position, scope);
		
		this.kind = kind;
		this.type = type;
		this.indexParams = indexParams;
		this.modifiers = modifiers;
	}
	
	public Kind getKind() {
		return kind;
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

	public String getIndexParams() {
		return indexParams;
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
		String icon; 
		if (kind == Kind.ENUM_ITEM) {
			icon = "EnumItem_16x.png";
		} else if (kind == Kind.NORMAL_FIELD) {
			if (modifiers.contains(CSharpSymbol.Modifier.PUBLIC))
				icon = "Field_blue_16x.png";
			else if (modifiers.contains(CSharpSymbol.Modifier.PROTECTED))
				icon = "FieldProtect_16x.png";
			else if (modifiers.contains(CSharpSymbol.Modifier.INTERNAL))
				icon = "FieldFriend_16x.png";
			else if (modifiers.contains(CSharpSymbol.Modifier.SEALED))
				icon = "FieldSealed_16x.png";
			else 
				icon = "FieldPrivate_16x.png";
		} else if (kind == Kind.PROPERTY) {
			if (modifiers.contains(CSharpSymbol.Modifier.PUBLIC))
				icon = "Property_16xMD.png";
			else if (modifiers.contains(CSharpSymbol.Modifier.PROTECTED))
				icon = "PropertyProtect_16x.png";
			else if (modifiers.contains(CSharpSymbol.Modifier.INTERNAL))
				icon = "PropertyFriend_16x.png";
			else if (modifiers.contains(CSharpSymbol.Modifier.SEALED))
				icon = "PropertySealed_16x.png";
			else 
				icon = "PropertyPrivate_16x.png";
		} else {
			if (modifiers.contains(CSharpSymbol.Modifier.PUBLIC))
				icon = "Event_orange_16x.png";
			else if (modifiers.contains(CSharpSymbol.Modifier.PROTECTED))
				icon = "EventProtect_16x.png";
			else if (modifiers.contains(CSharpSymbol.Modifier.INTERNAL))
				icon = "EventFriend_16x.png";
			else if (modifiers.contains(CSharpSymbol.Modifier.SEALED))
				icon = "EventSealed_16x.png";
			else 
				icon = "EventPrivate_16x.png";
		}
		return new NoAntiCacheImage(componentId, new PackageResourceReference(IconLocator.class, icon));
	}

	@Override
	public boolean isLocal() {
		return modifiers.contains(Modifier.PRIVATE);
	}

	@Override
	public boolean isSearchable() {
		return indexParams == null;
	}

}
