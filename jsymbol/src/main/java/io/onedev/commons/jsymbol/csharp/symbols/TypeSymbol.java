package io.onedev.commons.jsymbol.csharp.symbols;

import java.util.EnumSet;

import javax.annotation.Nullable;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.resource.PackageResourceReference;

import io.onedev.commons.utils.PlanarRange;
import io.onedev.commons.jsymbol.csharp.symbols.ui.TypeSymbolPanel;
import io.onedev.commons.jsymbol.csharp.symbols.ui.icon.IconLocator;
import io.onedev.commons.jsymbol.util.NoAntiCacheImage;
import io.onedev.commons.utils.LinearRange;

public class TypeSymbol extends CSharpSymbol {

	private static final long serialVersionUID = 1L;

	public enum Kind {CLASS, INTERFACE, ENUM, STRUCT};
	
	private final Kind kind;
	
	private final String typeParams;
	
	private final EnumSet<CSharpSymbol.Modifier> modifiers;

	public TypeSymbol(@Nullable CSharpSymbol parent, String typeName, PlanarRange position, PlanarRange scope,
			Kind kind, @Nullable String typeParams, EnumSet<CSharpSymbol.Modifier> modifiers) {
		super(parent, typeName, position, scope);

		this.kind = kind;
		this.typeParams = typeParams;
		this.modifiers = modifiers;
	}
	
	public Kind getKind() {
		return kind;
	}

	public String getTypeParams() {
		return typeParams;
	}

	public EnumSet<CSharpSymbol.Modifier> getModifiers() {
		return modifiers;
	}

	@Override
	public Component render(String componentId, LinearRange highlight) {
		return new TypeSymbolPanel(componentId, this, highlight);
	}

	@Override
	public Image renderIcon(String componentId) {
		String icon; 
		if (kind == Kind.ENUM) {
			if (modifiers.contains(CSharpSymbol.Modifier.PUBLIC))
				icon = "Enumerator_orange_16x.png";
			else if (modifiers.contains(CSharpSymbol.Modifier.PROTECTED))
				icon = "EnumProtect_16x.png";
			else if (modifiers.contains(CSharpSymbol.Modifier.PRIVATE))
				icon = "EnumPrivate_16x.png";
			else if (modifiers.contains(CSharpSymbol.Modifier.SEALED))
				icon = "EnumSealed_16x.png";
			else if (modifiers.contains(CSharpSymbol.Modifier.INTERNAL))
				icon = "EnumFriend_16x.png";
			else if (getParent() == null || getParent() instanceof NamespaceSymbol) 
				icon = "EnumFriend_16x.png";
			else
				icon = "EnumPrivate_16x.png";
		} else if (kind == Kind.INTERFACE) {
			if (modifiers.contains(CSharpSymbol.Modifier.PUBLIC))
				icon = "Interface_blue_16x.png";
			else if (modifiers.contains(CSharpSymbol.Modifier.PROTECTED))
				icon = "InterfaceProtect_16x.png";
			else if (modifiers.contains(CSharpSymbol.Modifier.PRIVATE))
				icon = "InterfacePrivate_16x.png";
			else if (modifiers.contains(CSharpSymbol.Modifier.SEALED))
				icon = "InterfaceSealed_16x.png";
			else if (modifiers.contains(CSharpSymbol.Modifier.INTERNAL))
				icon = "InterfaceFriend_16x.png";
			else if (getParent() == null || getParent() instanceof NamespaceSymbol) 
				icon = "InterfaceFriend_16x.png";
			else
				icon = "InterfacePrivate_16x.png";
		} else if (kind == Kind.STRUCT) {
			if (modifiers.contains(CSharpSymbol.Modifier.PUBLIC))
				icon = "Structure_16x.png";
			else if (modifiers.contains(CSharpSymbol.Modifier.PROTECTED))
				icon = "StructureProtect_16x.png";
			else if (modifiers.contains(CSharpSymbol.Modifier.PRIVATE))
				icon = "StructurePrivate_16x.png";
			else if (modifiers.contains(CSharpSymbol.Modifier.SEALED))
				icon = "StructureSealed_16x.png";
			else if (modifiers.contains(CSharpSymbol.Modifier.INTERNAL))
				icon = "StructureFriend_16x.png";
			else if (getParent() == null || getParent() instanceof NamespaceSymbol) 
				icon = "StructureFriend_16x.png";
			else
				icon = "StructurePrivate_16x.png";
		} else {
			if (modifiers.contains(CSharpSymbol.Modifier.PUBLIC))
				icon = "Class_yellow_16x.png";
			else if (modifiers.contains(CSharpSymbol.Modifier.PROTECTED))
				icon = "ClassProtect_16x.png";
			else if (modifiers.contains(CSharpSymbol.Modifier.PRIVATE))
				icon = "ClassPrivate_16x.png";
			else if (modifiers.contains(CSharpSymbol.Modifier.SEALED))
				icon = "ClassSealed_16x.png";
			else if (modifiers.contains(CSharpSymbol.Modifier.INTERNAL))
				icon = "ClassFriend_16x.png";
			else if (getParent() == null || getParent() instanceof NamespaceSymbol) 
				icon = "ClassFriend_16x.png";
			else
				icon = "ClassPrivate_16x.png";
		} 
		return new NoAntiCacheImage(componentId, new PackageResourceReference(IconLocator.class, icon));
	}

	@Override
	public boolean isLocal() {
		return modifiers.contains(CSharpSymbol.Modifier.PRIVATE);
	}

	@Override
	public boolean isPrimary() {
		return true;
	}

}
