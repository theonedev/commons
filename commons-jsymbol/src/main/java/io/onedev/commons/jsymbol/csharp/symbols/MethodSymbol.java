package io.onedev.commons.jsymbol.csharp.symbols;

import java.util.EnumSet;

import org.jspecify.annotations.Nullable;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.resource.PackageResourceReference;

import io.onedev.commons.utils.PlanarRange;
import io.onedev.commons.jsymbol.csharp.symbols.ui.MethodSymbolPanel;
import io.onedev.commons.jsymbol.csharp.symbols.ui.icon.IconLocator;
import io.onedev.commons.jsymbol.util.NoAntiCacheImage;
import io.onedev.commons.jsymbol.util.QualifiedName;
import io.onedev.commons.utils.LinearRange;

public class MethodSymbol extends CSharpSymbol {

	private static final long serialVersionUID = 1L;

	public enum Kind {DELEGATE, NORMAL_METHOD, OPERATOR};
	
	private final Kind kind;
	
	private final String returnTypeParams;
	
	private final String returnType;
	
	private final String methodPrefix;
	
	private final String methodParams;
	
	private final EnumSet<CSharpSymbol.Modifier> modifiers;

	public MethodSymbol(@Nullable CSharpSymbol parent, Kind kind, String methodName, PlanarRange position, 
			PlanarRange scope, @Nullable String returnTypeParams, @Nullable String returnType, 
			@Nullable String methodParams, @Nullable String methodPrefix, EnumSet<CSharpSymbol.Modifier> modifiers) {
		super(parent, methodName, position, scope);

		this.kind = kind;
		this.returnType = returnType;
		this.returnTypeParams = returnTypeParams;
		this.methodParams = methodParams;
		this.methodPrefix = methodPrefix;
		this.modifiers = modifiers;
	}
	
	public MethodSymbol(@Nullable CSharpSymbol parent, Kind kind, QualifiedName qualifiedName, PlanarRange position, 
			PlanarRange scope, @Nullable String returnTypeParams, @Nullable String returnType, 
			@Nullable String methodParams, @Nullable String methodPrefix, EnumSet<CSharpSymbol.Modifier> modifiers) {
		super(parent, qualifiedName, position, scope);

		this.kind = kind;
		this.returnType = returnType;
		this.returnTypeParams = returnTypeParams;
		this.methodParams = methodParams;
		this.methodPrefix = methodPrefix;
		this.modifiers = modifiers;
	}
	
	public String getReturnType() {
		return returnType;
	}
	
	public String getMethodPrefix() {
		return methodPrefix;
	}

	public String getMethodParams() {
		return methodParams;
	}
	
	public String getReturnTypeParams() {
		return returnTypeParams;
	}

	public EnumSet<CSharpSymbol.Modifier> getModifiers() {
		return modifiers;
	}

	public Kind getKind() {
		return kind;
	}

	@Override
	public Component render(String componentId, LinearRange highlight) {
		return new MethodSymbolPanel(componentId, this, highlight);
	}

	@Override
	public Image renderIcon(String componentId) {
		String icon; 
		if (kind == Kind.DELEGATE) {
			if (modifiers.contains(CSharpSymbol.Modifier.PUBLIC))
				icon = "Delegate_purple_16x.png";
			else if (modifiers.contains(CSharpSymbol.Modifier.PROTECTED))
				icon = "DelegateProtect_16x.png";
			else if (modifiers.contains(CSharpSymbol.Modifier.PRIVATE))
				icon = "DelegatePrivate_16x.png";
			else if (modifiers.contains(CSharpSymbol.Modifier.SEALED))
				icon = "DelegateSealed_16x.png";
			else if (modifiers.contains(CSharpSymbol.Modifier.INTERNAL))
				icon = "DelegateFriend_16x.png";
			else if (getParent() == null || getParent() instanceof NamespaceSymbol) 
				icon = "DelegateFriend_16x.png";
			else
				icon = "DelegatePrivate_16x.png";
		} else if (kind == Kind.NORMAL_METHOD) {
			if (modifiers.contains(CSharpSymbol.Modifier.PUBLIC))
				icon = "Method_purple_16x.png";
			else if (modifiers.contains(CSharpSymbol.Modifier.PROTECTED))
				icon = "MethodProtect_16x.png";
			else if (modifiers.contains(CSharpSymbol.Modifier.INTERNAL))
				icon = "MethodFriend_16x.png";
			else if (modifiers.contains(CSharpSymbol.Modifier.SEALED))
				icon = "MethodSealed_16x.png";
			else
				icon = "MethodPrivate_16x.png";
		} else {
			if (modifiers.contains(CSharpSymbol.Modifier.PUBLIC))
				icon = "Operator_16x.png";
			else if (modifiers.contains(CSharpSymbol.Modifier.PROTECTED))
				icon = "OperatorProtect_16x.png";
			else if (modifiers.contains(CSharpSymbol.Modifier.INTERNAL))
				icon = "OperatorFriend_16x.png";
			else if (modifiers.contains(CSharpSymbol.Modifier.SEALED))
				icon = "OperatorSealed_16x.png";
			else
				icon = "OperatorPrivate_16x.png";
		}
		return new NoAntiCacheImage(componentId, new PackageResourceReference(IconLocator.class, icon));
	}

	@Override
	public boolean isLocal() {
		return modifiers.contains(CSharpSymbol.Modifier.PRIVATE);
	}

	@Override
	public boolean isPrimary() {
		return kind == Kind.DELEGATE;
	}

}
