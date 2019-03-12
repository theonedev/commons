package io.onedev.commons.utils.matchscore;

public interface MatchScoreProvider<T> {
	
	double getMatchScore(T object);
	
}
