package io.onedev.commons.jsyntax.vue;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import io.onedev.commons.jsyntax.htmlmixed.HtmlMixedTokenizer;

public class VueTokenizer extends HtmlMixedTokenizer {
	
	static final Map<String, List<Info>> tagLanguages = Maps.newHashMap();
	
	static {
		List<Info> lst = Lists.newArrayList();
	    lst.add(new Info("lang", Pattern.compile("coffee(script)?"), "coffeescript"));
	    lst.add(new Info("type", Pattern.compile("^(?:text|application)\\/(?:x-)?coffee(?:script)?$"), "coffeescript"));
	    tagLanguages.put("script", lst);
	    lst = Lists.newArrayList();
	    lst.add(new Info("lang", Pattern.compile("^stylus$", Pattern.CASE_INSENSITIVE), "stylus"));
	    lst.add(new Info("lang", Pattern.compile("^sass$", Pattern.CASE_INSENSITIVE), "sass"));
	    lst.add(new Info("type", Pattern.compile("^(text\\/)?(x-)?styl(us)?$", Pattern.CASE_INSENSITIVE), "stylus"));
	    lst.add(new Info("type", Pattern.compile("^text\\/sass", Pattern.CASE_INSENSITIVE), "sass"));
	    tagLanguages.put("style", lst);
	    lst = Lists.newArrayList();
	    lst.add(new Info("lang", Pattern.compile("^vue-template$", Pattern.CASE_INSENSITIVE), "vue"));
	    lst.add(new Info("lang", Pattern.compile("^pug$", Pattern.CASE_INSENSITIVE), "pug"));
	    lst.add(new Info("lang", Pattern.compile("^handlebars$", Pattern.CASE_INSENSITIVE), "handlebars"));
	    lst.add(new Info("type", Pattern.compile("^(text\\/)?(x-)?pug$", Pattern.CASE_INSENSITIVE), "pug"));
	    lst.add(new Info("type", Pattern.compile("^text\\/x-handlebars-template$", Pattern.CASE_INSENSITIVE), "handlebars"));
	    lst.add(new Info("", null, "vue-template"));
	    tagLanguages.put("template", lst);
	}
	
	public VueTokenizer() {
		super(tagLanguages);
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "vue");
	}

	@Override
	public boolean acceptMime(String mime) {
		return mime != null	&&
				(mime.equals("text/x-vue") || mime.equals("script/x-vue"));
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("vue");
	}
}
