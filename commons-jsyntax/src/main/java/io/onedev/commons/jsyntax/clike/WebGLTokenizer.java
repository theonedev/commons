package io.onedev.commons.jsyntax.clike;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;

public class WebGLTokenizer extends ClikeTokenizer {

	private static final Set<String> KEYWORDS = wordsOf(
			"sampler1D sampler2D sampler3D samplerCube " +
            "sampler1DShadow sampler2DShadow " +
            "const attribute uniform varying " +
            "break continue discard return " +
            "for while do if else struct " +
            "in out inout");
	
	private static final Set<String> TYPES = wordsOf(
			"float int bool void " +
	        "vec2 vec3 vec4 ivec2 ivec3 ivec4 bvec2 bvec3 bvec4 " +
	        "mat2 mat3 mat4");
	
	private static final Set<String> BLOCK_KEYWORDS = wordsOf("for while do if else struct");
	
	private static final Set<String> BUILTIN = wordsOf(
			"radians degrees sin cos tan asin acos atan " +
            "pow exp log exp2 sqrt inversesqrt " +
            "abs sign floor ceil fract mod min max clamp mix step smoothstep " +
            "length distance dot cross normalize ftransform faceforward " +
            "reflect refract matrixCompMult " +
            "lessThan lessThanEqual greaterThan greaterThanEqual " +
            "equal notEqual any all not " +
            "texture1D texture1DProj texture1DLod texture1DProjLod " +
            "texture2D texture2DProj texture2DLod texture2DProjLod " +
            "texture3D texture3DProj texture3DLod texture3DProjLod " +
            "textureCube textureCubeLod " +
            "shadow1D shadow2D shadow1DProj shadow2DProj " +
            "shadow1DLod shadow2DLod shadow1DProjLod shadow2DProjLod " +
            "dFdx dFdy fwidth " +
            "noise1 noise2 noise3 noise4");
	
	private static final Set<String> ATOMS = wordsOf(
			"true false " +
            "gl_FragColor gl_SecondaryColor gl_Normal gl_Vertex " +
            "gl_MultiTexCoord0 gl_MultiTexCoord1 gl_MultiTexCoord2 gl_MultiTexCoord3 " +
            "gl_MultiTexCoord4 gl_MultiTexCoord5 gl_MultiTexCoord6 gl_MultiTexCoord7 " +
            "gl_FogCoord gl_PointCoord " +
            "gl_Position gl_PointSize gl_ClipVertex " +
            "gl_FrontColor gl_BackColor gl_FrontSecondaryColor gl_BackSecondaryColor " +
            "gl_TexCoord gl_FogFragCoord " +
            "gl_FragCoord gl_FrontFacing " +
            "gl_FragData gl_FragDepth " +
            "gl_ModelViewMatrix gl_ProjectionMatrix gl_ModelViewProjectionMatrix " +
            "gl_TextureMatrix gl_NormalMatrix gl_ModelViewMatrixInverse " +
            "gl_ProjectionMatrixInverse gl_ModelViewProjectionMatrixInverse " +
            "gl_TexureMatrixTranspose gl_ModelViewMatrixInverseTranspose " +
            "gl_ProjectionMatrixInverseTranspose " +
            "gl_ModelViewProjectionMatrixInverseTranspose " +
            "gl_TextureMatrixInverseTranspose " +
            "gl_NormalScale gl_DepthRange gl_ClipPlane " +
            "gl_Point gl_FrontMaterial gl_BackMaterial gl_LightSource gl_LightModel " +
            "gl_FrontLightModelProduct gl_BackLightModelProduct " +
            "gl_TextureColor gl_EyePlaneS gl_EyePlaneT gl_EyePlaneR gl_EyePlaneQ " +
            "gl_FogParameters " +
            "gl_MaxLights gl_MaxClipPlanes gl_MaxTextureUnits gl_MaxTextureCoords " +
            "gl_MaxVertexAttribs gl_MaxVertexUniformComponents gl_MaxVaryingFloats " +
            "gl_MaxVertexTextureImageUnits gl_MaxTextureImageUnits " +
            "gl_MaxFragmentUniformComponents gl_MaxCombineTextureImageUnits " +
            "gl_MaxDrawBuffers");
	
	private static Map<String, Processor> HOOKS = Maps.newHashMap();

	static {
		HOOKS.put("#", new CppHook());
	}

	@Override
	protected Set<String> keywords() {
		return KEYWORDS;
	}

	@Override
	protected boolean typesContains(String word) {
		return TYPES.contains(word);
	}
	
	@Override
	protected Set<String> blockKeywords() {
		return BLOCK_KEYWORDS;
	}

	@Override
	protected Set<String> builtin() {
		return BUILTIN;
	}

	@Override
	protected Set<String> atoms() {
		return ATOMS;
	}

	@Override
	protected boolean indentSwitch() {
		return false;
	}

	@Override
	protected Map<String, Processor> hooks() {
		return HOOKS;
	}

	@Override
	public boolean accept(String fileName) {
		// this tokenizer only tokenizes embedded script in html
		return false;
	}

	@Override
	public boolean acceptMime(String mime) {
		return mime != null
				&& (mime.equals("x-shader/x-vertex")
				|| mime.equals("x-shader/x-fragment"));
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("webgl");
	}
}