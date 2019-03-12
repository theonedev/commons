package io.onedev.commons.jsyntax.protobuf;

import java.util.List;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;

public class ProtobufTokenizer extends AbstractTokenizer<ProtobufTokenizer.State> {
	static Pattern identifiers = Pattern.compile("^[_A-Za-z\\xa1-\\uffff][_A-Za-z0-9\\xa1-\\uffff]*");
	static final Pattern pattern[] = new Pattern[6];
	static Pattern keywords = wordRegexp(
			Lists.newArrayList("package", "message", "import", "syntax", "required", "optional", "repeated", "reserved",
					"default", "extensions", "packed", "bool", "bytes", "double", "enum", "float", "string", "int32",
					"int64", "uint32", "uint64", "sint32", "sint64", "fixed32", "fixed64", "sfixed32", "sfixed64"));
	static {
		pattern[0] = Pattern.compile("^[0-9\\.+-]", Pattern.CASE_INSENSITIVE);
		pattern[1] = Pattern.compile("^[+-]?0x[0-9a-fA-F]+", Pattern.CASE_INSENSITIVE);
		pattern[2] = Pattern.compile("^[+-]?\\d*\\.\\d+([EeDd][+-]?\\d+)?", Pattern.CASE_INSENSITIVE);
		pattern[3] = Pattern.compile("^[+-]?\\d+([EeDd][+-]?\\d+)?");
		pattern[4] = Pattern.compile("^\"([^\"]|(\"\"))*\"", Pattern.CASE_INSENSITIVE);
		pattern[5] = Pattern.compile("^'([^']|(''))*'", Pattern.CASE_INSENSITIVE);
	}

	static class State {

	}

	static Pattern wordRegexp(List<String> words) {
		return Pattern.compile("^((" + StringUtils.join(words, ")|(") + "))\\b", Pattern.CASE_INSENSITIVE);
	}

	static String tokenBase(StringStream stream) {
		if (stream.eatSpace())
			return "";
		else if (stream.match("//")) {
			stream.skipToEnd();
			return "comment";
		} else if (!stream.match(pattern[0], false).isEmpty()) {
			if (stream.equals(pattern[1]))
				return "number";

			else if (!stream.match(pattern[2]).isEmpty())
				return "number";

			else if (!stream.match(pattern[3]).isEmpty())
				return "number";
		}

		else if (!stream.match(pattern[4]).isEmpty()) {
			return "string";
		}

		else if (!stream.match(pattern[5]).isEmpty()) {
			return "string";
		}

		else if (!stream.match(keywords).isEmpty()) {
			return "keyword";
		}

		else if (!stream.match(identifiers).isEmpty()) {
			return "variable";
		}
		stream.next();
		return "";
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "proto");
	}

	@Override
	public State startState() {
		// TODO Auto-generated method stub
		return new State();
	}

	@Override
	public String token(StringStream stream, State state) {
		// TODO Auto-generated method stub
		return tokenBase(stream);
	}

	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-protobuf");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("protobuf");
	}

}
