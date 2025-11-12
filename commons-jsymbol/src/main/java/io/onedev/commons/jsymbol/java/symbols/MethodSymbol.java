package io.onedev.commons.jsymbol.java.symbols;

import java.util.EnumSet;

import org.jspecify.annotations.Nullable;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.resource.PackageResourceReference;

import com.github.javaparser.ast.Modifier;

import io.onedev.commons.utils.PlanarRange;
import io.onedev.commons.jsymbol.java.symbols.ui.MethodSymbolPanel;
import io.onedev.commons.jsymbol.java.symbols.ui.icon.IconLocator;
import io.onedev.commons.jsymbol.util.NoAntiCacheImage;
import io.onedev.commons.utils.LinearRange;

public class MethodSymbol extends JavaSymbol {

	private static final long serialVersionUID = 1L;

	private final String returnType; 
	
	private final String methodParams;
	
	private final String returnTypeParams;

	private final EnumSet<Modifier> modifiers;
	
	public MethodSymbol(TypeSymbol parent, String methodName, PlanarRange position, PlanarRange scope,
			@Nullable String returnType, @Nullable String methodParams, @Nullable String returnTypeParams, 
			EnumSet<Modifier> modifiers) {
		super(parent, methodName, position, scope);
		
		this.returnType = returnType;
		this.methodParams = methodParams;
		this.returnTypeParams = returnTypeParams;
		this.modifiers = modifiers;
	}

	/**
	 * Get type of this method. 
	 * 
	 * @return
	 * 			type of this method, or <tt>null</tt> for constructor
	 */
	@Nullable
	public String getReturnType() {
		return returnType;
	}

	/**
	 * Get method params
	 * 
	 * @return
	 * 			method params, or <tt>null</tt> if no method params
	 */
	@Nullable
	public String getMethodParams() {
		return methodParams;
	}

	/**
	 * Get type params 
	 * 
	 * @return
	 * 			type params, or <tt>null</tt> if no type params
	 */
	@Nullable
	public String getReturnTypeParams() {
		return returnTypeParams;
	}

	public EnumSet<Modifier> getModifiers() {
		return modifiers;
	}

	@Override
	public boolean isPrimary() {
		return false;
	}

	@Override
	public Component render(String componentId, LinearRange highlight) {
		return new MethodSymbolPanel(componentId, this, highlight);
	}

	@Override
	public Image renderIcon(String componentId) {
		Image icon;
		if (modifiers.contains(Modifier.PRIVATE)) {
			icon = new NoAntiCacheImage(componentId, new PackageResourceReference(IconLocator.class, "methpri_obj.png"));
			icon.add(AttributeAppender.append("title", "private method"));
		}  else if (modifiers.contains(Modifier.PROTECTED)) {
			icon = new NoAntiCacheImage(componentId, new PackageResourceReference(IconLocator.class, "methpro_obj.png"));
			icon.add(AttributeAppender.append("title", "protected method"));
		} else if (modifiers.contains(Modifier.PUBLIC)) {
			icon = new NoAntiCacheImage(componentId, new PackageResourceReference(IconLocator.class, "methpub_obj.png"));
			icon.add(AttributeAppender.append("title", "public method"));
		} else {
			icon = new NoAntiCacheImage(componentId, new PackageResourceReference(IconLocator.class, "methdef_obj.png"));
			icon.add(AttributeAppender.append("title", "method"));
		}
		return icon;
	}

	@Override
	public boolean isLocal() {
		return modifiers.contains(Modifier.PRIVATE);
	}
	
}
