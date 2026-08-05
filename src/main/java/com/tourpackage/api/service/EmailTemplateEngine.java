package com.tourpackage.api.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Renders the HTML email templates under {@code resources/email/}.
 *
 * <p>Deliberately not Thymeleaf. This is a REST API with no view layer, and
 * {@code spring-boot-starter-thymeleaf} would auto-configure an MVC view
 * resolver that nothing here wants, to solve a problem that is placeholder
 * substitution over eight files. The trade-off is no loops or conditionals in
 * templates — anything repeating (a table of booking rows) is built in Java and
 * passed in as a raw fragment.
 *
 * <p>Two placeholder forms, and the difference is the whole security story:
 * <ul>
 *   <li>{@code {{name}}} — HTML-escaped. The default, so a value that reaches a
 *       template without anyone thinking about it cannot inject markup.</li>
 *   <li>{@code {{{rows}}}} — raw. Only for fragments this class's callers built
 *       themselves; never for anything a visitor typed.</li>
 * </ul>
 */
@Component
public class EmailTemplateEngine {

    /** Triple braces first — otherwise the double-brace pattern matches inside them. */
    private static final Pattern RAW = Pattern.compile("\\{\\{\\{(\\w+)}}}");
    private static final Pattern ESCAPED = Pattern.compile("\\{\\{(\\w+)}}");

    private static final Pattern TAG = Pattern.compile("<[^>]+>");
    private static final Pattern BLANK_LINES = Pattern.compile("\n{3,}");

    /**
     * Templates are immutable once packaged, so they are read once. In dev this
     * means an edited template needs a restart, which is the same as every other
     * classpath resource here.
     */
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    /**
     * Renders {@code email/<name>.html} and wraps it in the shared layout.
     *
     * @param name    template file name without the extension
     * @param model   placeholder values; keys map to {@code {{key}}}
     * @param layout  values for the layout itself (brand, footer, preheader)
     */
    public String render(String name, Map<String, ?> model, Map<String, ?> layout) {
        String body = substitute(load("email/" + name + ".html"), model);

        Map<String, Object> layoutModel = new java.util.HashMap<>(layout);
        // The body is already-rendered HTML, so it goes in raw — and it is the
        // one value in the layout that legitimately does.
        layoutModel.put("body", new Raw(body));
        return substitute(load("email/layout.html"), layoutModel);
    }

    /**
     * A text alternative derived from the rendered HTML.
     *
     * <p>Sending {@code multipart/alternative} rather than HTML alone matters for
     * two reasons: text-only clients otherwise show raw markup, and a missing
     * text part is a well-known spam-filter signal. Deriving it from the HTML
     * keeps one source of truth — a hand-written second copy of every template
     * would drift from the first the moment anyone edited one.
     */
    public String toPlainText(String html) {
        String text = html
                // Comments go first. They are not tags, and the tag pattern below
                // stops at the first '>' inside one — which in a comment that
                // mentions a tag name leaves the rest of the prose as body text.
                .replaceAll("(?s)<!--.*?-->", "")
                // The preheader is hidden in HTML, so repeating it here would open
                // the text version with a line no HTML reader ever sees.
                .replaceAll("(?is)<div[^>]*id=\"preheader\".*?</div>", "")
                .replaceAll("(?is)<(head|style|title)\\b.*?</\\1>", "")
                // Keep the destination: a bare "View your booking" is useless to
                // someone whose client is showing them this part.
                .replaceAll("(?is)<a\\b[^>]*href=\"(https?://[^\"]+)\"[^>]*>(.*?)</a>", "$2 ($1)")
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</(p|div|tr|h[1-6]|table)>", "\n")
                .replaceAll("(?i)</td>", "  ");
        text = TAG.matcher(text).replaceAll("");
        text = unescape(text);
        text = text.lines().map(String::strip).reduce("", (a, b) -> a.isEmpty() ? b : a + "\n" + b);
        return BLANK_LINES.matcher(text).replaceAll("\n\n").strip();
    }

    /** Marks a value as pre-built HTML that must not be escaped. */
    public record Raw(String html) {
    }

    private String substitute(String template, Map<String, ?> model) {
        String result = replace(RAW, template, model, false);
        return replace(ESCAPED, result, model, true);
    }

    private String replace(Pattern pattern, String template, Map<String, ?> model, boolean escape) {
        Matcher matcher = pattern.matcher(template);
        StringBuilder out = new StringBuilder();

        while (matcher.find()) {
            Object value = model.get(matcher.group(1));
            String text;
            if (value instanceof Raw raw) {
                text = raw.html();
            } else {
                // A missing key renders empty rather than leaving "{{whatever}}"
                // visible in someone's inbox.
                text = value == null ? "" : String.valueOf(value);
                if (escape) {
                    text = escapeHtml(text);
                }
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(text));
        }

        matcher.appendTail(out);
        return out.toString();
    }

    private String load(String path) {
        return cache.computeIfAbsent(path, key -> {
            try (var stream = new ClassPathResource(key).getInputStream()) {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException ex) {
                // A missing template is a packaging error, not a runtime
                // condition — failing loudly at first use beats sending a blank
                // email that nobody notices.
                throw new UncheckedIOException("Email template not found: " + key, ex);
            }
        });
    }

    static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String unescape(String text) {
        return text.replace("&nbsp;", " ")
                .replace("&mdash;", "—")
                .replace("&middot;", "·")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&amp;", "&");
    }

}
