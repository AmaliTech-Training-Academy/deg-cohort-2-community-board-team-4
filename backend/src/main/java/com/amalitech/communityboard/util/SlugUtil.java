package com.amalitech.communityboard.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;


public final class SlugUtil {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");
    private static final Pattern EDGE_DASHES = Pattern.compile("(^-+|-+$)");

    private SlugUtil() {
    }


    public static String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "post";
        }
        String noWhitespace = WHITESPACE.matcher(input.trim()).replaceAll("-");
        String normalized = Normalizer.normalize(noWhitespace, Normalizer.Form.NFD);
        String slug = NON_LATIN.matcher(normalized).replaceAll("");
        slug = EDGE_DASHES.matcher(slug).replaceAll("");
        slug = slug.toLowerCase(Locale.ENGLISH);
        return slug.isBlank() ? "post" : slug;
    }
}
