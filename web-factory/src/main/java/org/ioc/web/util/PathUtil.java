package org.ioc.web.util;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * Analog Spring AntPathMatcher
 *
 * @author GenCloud
 * @date: 10/2018
 **/
public class PathUtil {
	private static final String DEFAULT_PATH_SEPARATOR = "/";
	private static final char[] WILDCARD_CHARS = {'*', '?', '{'};
	private static final int CACHE_TURNOFF_THRESHOLD = 65536;
	private static boolean caseSensitive = true;
	private static String pathSeparator = DEFAULT_PATH_SEPARATOR;
	private static PathSeparatorPatternCache pathSeparatorPatternCache = new PathSeparatorPatternCache(DEFAULT_PATH_SEPARATOR);
	private final Map<String, String[]> tokenizedPatternCache = new ConcurrentHashMap<>(256);
	private final Map<String, AntPathStringMatcher> stringMatcherCache = new ConcurrentHashMap<>(256);
	private boolean trimTokens = false;
	private volatile Boolean cachePatterns;

	private static String[] tokenizeToStringArray(String str, String delimiters, boolean trimTokens,
												  boolean ignoreEmptyTokens) {
		if (str == null) {
			return null;
		}

		final StringTokenizer st = new StringTokenizer(str, delimiters);
		final List<String> tokens = new ArrayList<>();
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			if (trimTokens) {
				token = token.trim();
			}

			if (!ignoreEmptyTokens || token.length() > 0) {
				tokens.add(token);
			}
		}
		return toStringArray(tokens);
	}

	private static String[] toStringArray(Collection<String> collection) {
		if (collection == null) {
			return null;
		}
		return collection.toArray(new String[0]);
	}

	public boolean match(String pattern, String path) {
		return doMatch(pattern, path, true, null);
	}

	private boolean doMatch(String pattern, String path, boolean fullMatch, Map<String, String> uriTemplateVariables) {
		final String[] pattDirs = tokenizePattern(pattern);
		if (fullMatch && caseSensitive && !isPotentialMatch(path, pattDirs)) {
			return false;
		}

		final String[] pathDirs = tokenizePath(path);

		int pattIdxStart = 0;
		int pattIdxEnd = pattDirs.length - 1;
		int pathIdxStart = 0;
		int pathIdxEnd = pathDirs.length - 1;

		// Match all elements up to the first **
		while (pattIdxStart <= pattIdxEnd && pathIdxStart <= pathIdxEnd) {
			final String pattDir = pattDirs[pattIdxStart];
			if ("**".equals(pattDir)) {
				break;
			}

			if (!matchStrings(pattDir, pathDirs[pathIdxStart], uriTemplateVariables)) {
				return false;
			}

			pattIdxStart++;
			pathIdxStart++;
		}

		if (pathIdxStart > pathIdxEnd) {
			// Path is exhausted, only match if rest of pattern is * or **'s
			if (pattIdxStart > pattIdxEnd) {
				return (pattern.endsWith(pathSeparator) == path.endsWith(pathSeparator));
			}

			if (!fullMatch) {
				return true;
			}

			if (pattIdxStart == pattIdxEnd && pattDirs[pattIdxStart].equals("*") && path.endsWith(pathSeparator)) {
				return true;
			}

			return IntStream.rangeClosed(pattIdxStart, pattIdxEnd).allMatch(i -> pattDirs[i].equals("**"));
		} else if (pattIdxStart > pattIdxEnd) {
			// String not exhausted, but pattern is. Failure.
			return false;
		} else if (!fullMatch && "**".equals(pattDirs[pattIdxStart])) {
			// Path start definitely matches due to "**" part in pattern.
			return true;
		}

		// up to last '**'
		while (pattIdxStart <= pattIdxEnd && pathIdxStart <= pathIdxEnd) {
			final String pattDir = pattDirs[pattIdxEnd];
			if (pattDir.equals("**")) {
				break;
			}

			if (!matchStrings(pattDir, pathDirs[pathIdxEnd], uriTemplateVariables)) {
				return false;
			}

			pattIdxEnd--;
			pathIdxEnd--;
		}

		if (pathIdxStart > pathIdxEnd) {
			// String is exhausted
			return IntStream.rangeClosed(pattIdxStart, pattIdxEnd)
					.allMatch(i -> pattDirs[i].equals("**"));
		}

		while (pattIdxStart != pattIdxEnd && pathIdxStart <= pathIdxEnd) {
			final int patIdxTmp = IntStream.rangeClosed(pattIdxStart + 1, pattIdxEnd)
					.filter(i -> pattDirs[i].equals("**"))
					.findFirst()
					.orElse(-1);

			if (patIdxTmp == pattIdxStart + 1) {
				// '**/**' situation, so skip one
				pattIdxStart++;
				continue;
			}
			// Find the pattern between padIdxStart & padIdxTmp in str between
			// strIdxStart & strIdxEnd
			final int patLength = (patIdxTmp - pattIdxStart - 1);
			final int strLength = (pathIdxEnd - pathIdxStart + 1);
			int foundIdx = -1;

			strLoop:
			for (int i = 0; i <= strLength - patLength; i++) {
				for (int j = 0; j < patLength; j++) {
					final String subPat = pattDirs[pattIdxStart + j + 1];
					final String subStr = pathDirs[pathIdxStart + i + j];
					if (!matchStrings(subPat, subStr, uriTemplateVariables)) {
						continue strLoop;
					}
				}
				foundIdx = pathIdxStart + i;
				break;
			}

			if (foundIdx == -1) {
				return false;
			}

			pattIdxStart = patIdxTmp;
			pathIdxStart = foundIdx + patLength;
		}

		return IntStream.rangeClosed(pattIdxStart, pattIdxEnd).allMatch(i -> pattDirs[i].equals("**"));
	}

	private String[] tokenizePattern(String pattern) {
		String[] tokenized = null;
		final Boolean cachePatterns = this.cachePatterns;
		if (cachePatterns == null || cachePatterns) {
			tokenized = tokenizedPatternCache.get(pattern);
		}

		if (tokenized == null) {
			tokenized = tokenizePath(pattern);
			if (cachePatterns == null && tokenizedPatternCache.size() >= CACHE_TURNOFF_THRESHOLD) {
				// Try to adapt to the runtime situation that we're encountering:
				// There are obviously too many different patterns coming in here...
				// So let's turn off the cache since the patterns are unlikely to be reoccurring.
				deactivatePatternCache();
				return tokenized;
			}

			if (cachePatterns == null || cachePatterns) {
				tokenizedPatternCache.put(pattern, tokenized);
			}
		}
		return tokenized;
	}

	/**
	 * Tokenize the given path String into parts, based on this matcher's settings.
	 *
	 * @param path the path to tokenize
	 * @return the tokenized path parts
	 */
	private String[] tokenizePath(String path) {
		return tokenizeToStringArray(path, pathSeparator, this.trimTokens, true);
	}

	private boolean matchStrings(String pattern, String str, Map<String, String> uriTemplateVariables) {
		return getStringMatcher(pattern).matchStrings(str, uriTemplateVariables);
	}

	private AntPathStringMatcher getStringMatcher(String pattern) {
		AntPathStringMatcher matcher = null;
		final Boolean cachePatterns = this.cachePatterns;
		if (cachePatterns == null || cachePatterns) {
			matcher = stringMatcherCache.get(pattern);
		}

		if (matcher == null) {
			matcher = new AntPathStringMatcher(pattern, caseSensitive);
			if (cachePatterns == null && stringMatcherCache.size() >= CACHE_TURNOFF_THRESHOLD) {
				// Try to adapt to the runtime situation that we're encountering:
				// There are obviously too many different patterns coming in here...
				// So let's turn off the cache since the patterns are unlikely to be reoccurring.
				deactivatePatternCache();
				return matcher;
			}

			if (cachePatterns == null || cachePatterns) {
				stringMatcherCache.put(pattern, matcher);
			}
		}
		return matcher;
	}

	private boolean isPotentialMatch(String path, String[] pattDirs) {
		if (!trimTokens) {
			int pos = 0;
			for (String pattDir : pattDirs) {
				int skipped = skipSeparator(path, pos, pathSeparator);
				pos += skipped;
				skipped = skipSegment(path, pos, pattDir);
				if (skipped < pattDir.length()) {
					return (skipped > 0 || (pattDir.length() > 0 && isWildcardChar(pattDir.charAt(0))));
				}
				pos += skipped;
			}
		}
		return true;
	}

	private int skipSegment(String path, int pos, String prefix) {
		int skipped = 0;
		for (int i = 0; i < prefix.length(); i++) {
			final char c = prefix.charAt(i);
			if (isWildcardChar(c)) {
				return skipped;
			}

			final int currPos = pos + skipped;
			if (currPos >= path.length()) {
				return 0;
			}

			if (c == path.charAt(currPos)) {
				skipped++;
			}
		}
		return skipped;
	}

	private boolean isWildcardChar(char c) {
		for (char candidate : WILDCARD_CHARS) {
			if (c == candidate) {
				return true;
			}
		}
		return false;
	}

	private int skipSeparator(String path, int pos, String separator) {
		int skipped = 0;
		while (path.startsWith(separator, pos + skipped)) {
			skipped += separator.length();
		}
		return skipped;
	}

	private void deactivatePatternCache() {
		cachePatterns = false;
		tokenizedPatternCache.clear();
		stringMatcherCache.clear();
	}

	protected static class AntPathStringMatcher {

		private static final Pattern GLOB_PATTERN = Pattern.compile("\\?|\\*|\\{((?:\\{[^/]+?\\}|[^/{}]|\\\\[{}])+?)\\}");

		private static final String DEFAULT_VARIABLE_PATTERN = "(.*)";

		private final Pattern pattern;

		private final List<String> variableNames = new LinkedList<>();

		public AntPathStringMatcher(String pattern) {
			this(pattern, true);
		}

		AntPathStringMatcher(String pattern, boolean caseSensitive) {
			final StringBuilder patternBuilder = new StringBuilder();
			final Matcher matcher = GLOB_PATTERN.matcher(pattern);
			int end = 0;
			while (matcher.find()) {
				patternBuilder.append(quote(pattern, end, matcher.start()));
				String match = matcher.group();
				if ("?".equals(match)) {
					patternBuilder.append('.');
				} else if ("*".equals(match)) {
					patternBuilder.append(".*");
				} else if (match.startsWith("{") && match.endsWith("}")) {
					int colonIdx = match.indexOf(':');
					if (colonIdx == -1) {
						patternBuilder.append(DEFAULT_VARIABLE_PATTERN);
						variableNames.add(matcher.group(1));
					} else {
						final String variablePattern = match.substring(colonIdx + 1, match.length() - 1);
						patternBuilder.append('(');
						patternBuilder.append(variablePattern);
						patternBuilder.append(')');

						final String variableName = match.substring(1, colonIdx);
						variableNames.add(variableName);
					}
				}
				end = matcher.end();
			}

			patternBuilder.append(quote(pattern, end, pattern.length()));
			this.pattern = (caseSensitive ? Pattern.compile(patternBuilder.toString()) :
					Pattern.compile(patternBuilder.toString(), Pattern.CASE_INSENSITIVE));
		}

		private String quote(String s, int start, int end) {
			if (start == end) {
				return "";
			}
			return Pattern.quote(s.substring(start, end));
		}

		/**
		 * Main entry point.
		 *
		 * @return {@code true} if the string matches against the pattern, or {@code false} otherwise.
		 */
		boolean matchStrings(String str, Map<String, String> uriTemplateVariables) {
			final Matcher matcher = pattern.matcher(str);
			if (matcher.matches()) {
				if (uriTemplateVariables != null) {
					// SPR-8455
					if (variableNames.size() != matcher.groupCount()) {
						throw new IllegalArgumentException("The number of capturing groups in the pattern segment " +
								pattern + " does not match the number of URI template variables it defines, " +
								"which can occur if capturing groups are used in a URI template regex. " +
								"Use non-capturing groups instead.");
					}
					IntStream.rangeClosed(1, matcher.groupCount()).forEach(i -> {
						final String name = variableNames.get(i - 1);
						final String value = matcher.group(i);
						uriTemplateVariables.put(name, value);
					});
				}
				return true;
			} else {
				return false;
			}
		}
	}


	/**
	 * A simple cache for patterns that depend on the configured path separator.
	 */
	private static class PathSeparatorPatternCache {
		private final String endsOnWildCard;
		private final String endsOnDoubleWildCard;

		PathSeparatorPatternCache(String pathSeparator) {
			this.endsOnWildCard = pathSeparator + "*";
			this.endsOnDoubleWildCard = pathSeparator + "**";
		}

		public String getEndsOnWildCard() {
			return this.endsOnWildCard;
		}

		public String getEndsOnDoubleWildCard() {
			return this.endsOnDoubleWildCard;
		}
	}
}
