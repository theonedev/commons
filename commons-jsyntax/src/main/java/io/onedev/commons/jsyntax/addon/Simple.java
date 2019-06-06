package io.onedev.commons.jsyntax.addon;

import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Pattern;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;

public abstract class Simple extends AbstractTokenizer<Simple.State> {

	static class Pair {
		String text;
		String token;
		
		public Pair(String text, String token) {
			this.text = text;
			this.token = token;
		}
	}
	
	public static class Rule {
		Pattern regex;
		List<String> token;
		String next;
		String push;
		boolean pop;
		
		public Rule(Pattern regex, List<String> token, String next, String push, boolean pop) {
			this.regex = regex;
			this.token = token;
			this.next = next;
			this.push = push;
			this.pop = pop;
		}
	}
	
	static class State {
		Map<String, List<Rule>> states;
		String state;
		Stack<String> stack;
		Stack<Pair> pending;
		
		public State(Map<String, List<Rule>> states, String state, Stack<String> stack, Stack<Pair> pending) {
			this.states = states;
			this.state = state;
			this.stack = stack;
			this.pending = pending;
		}
	}
	
	public abstract Map<String, List<Rule>> getStates();

	@Override
	public State startState() {
		return new State(getStates(), "start", new Stack<String>(), new Stack<Pair>());
	}

	@Override
	public String token(StringStream stream, State state) {
		if (!state.pending.isEmpty()) {
			Pair pend = state.pending.get(0);
			state.pending.remove(0);
			stream.pos(stream.pos() + pend.text.length());
			return pend.token;
		}
		List<Rule> curState = state.states.get(state.state);
		for (Rule rule : curState) {
			List<String> matches = stream.match(rule.regex);
			if (!matches.isEmpty()) {
				if (!rule.next.isEmpty()) {
					state.state = rule.next;
				} else if (!rule.push.isEmpty()) {
					state.stack.push(state.state);
					state.state = rule.push;
				} else if (rule.pop && !state.stack.isEmpty()) {
					state.state = state.stack.pop();
				}
				if (matches.size() > 2) {
					state.pending.clear();
					for (int j = 2; j < matches.size(); j++)
						if (!matches.get(j).isEmpty())
							state.pending.push(new Pair(matches.get(j), rule.token.get(j - 1)));
					stream.backUp(matches.get(0).length() - (matches.size() > 1 ? matches.get(1).length() : 0));
					return rule.token.get(0);
				} else if (!rule.token.isEmpty()) {
					return rule.token.get(0);
				}
			}
		}
		stream.next();
		return "";
	}
}
