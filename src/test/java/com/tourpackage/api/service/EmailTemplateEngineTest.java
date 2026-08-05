package com.tourpackage.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tourpackage.api.service.EmailTemplateEngine.Raw;

class EmailTemplateEngineTest {

    private final EmailTemplateEngine engine = new EmailTemplateEngine();

    private String render(Map<String, ?> model) {
        return engine.render("inquiry-acknowledgement", model,
                Map.of("subject", "s", "preheader", "p", "brandName", "TourPackage",
                        "siteUrl", "https://example.com", "supportEmail", "support@example.com",
                        "footerAddress", ""));
    }

    @Test
    @DisplayName("a visitor's markup is escaped, not rendered")
    void escapesByDefault() {
        String html = render(Map.of("name", "<script>alert(1)</script>", "message", new Raw("hi")));

        assertThat(html).doesNotContain("<script>");
        assertThat(html).contains("&lt;script&gt;");
    }

    @Test
    @DisplayName("a Raw value is inserted as markup")
    void rawIsNotEscaped() {
        String html = render(Map.of("name", "Ada", "message", new Raw("line<br />break")));

        assertThat(html).contains("line<br />break");
    }

    @Test
    @DisplayName("a missing key renders empty rather than leaving the placeholder visible")
    void missingKeyRendersEmpty() {
        String html = render(Map.of("message", new Raw("x")));

        assertThat(html).doesNotContain("{{name}}");
    }

    @Test
    @DisplayName("the layout wraps the body")
    void layoutWrapsBody() {
        String html = render(Map.of("name", "Ada", "message", new Raw("x")));

        assertThat(html).contains("<!DOCTYPE html");
        assertThat(html).contains("support@example.com");
        assertThat(html).contains("Ada");
    }

    @Test
    @DisplayName("plain text keeps link destinations")
    void plainTextKeepsUrls() {
        String text = engine.toPlainText(
                "<p>See <a href=\"https://example.com/x\">your booking</a></p>");

        assertThat(text).contains("your booking (https://example.com/x)");
    }

    @Test
    @DisplayName("plain text drops HTML comments, including ones that mention tags")
    void plainTextDropsComments() {
        // Regression: the tag-stripping pattern stops at the first '>' inside a
        // comment, so a comment mentioning <style> leaked its prose into the body.
        String text = engine.toPlainText(
                "<!-- Gmail strips <style> blocks on forwarded mail -->\n<p>Hello</p>");

        assertThat(text).isEqualTo("Hello");
    }

    @Test
    @DisplayName("plain text drops the hidden preheader")
    void plainTextDropsPreheader() {
        String text = engine.toPlainText(
                "<div id=\"preheader\" style=\"display:none\">Hidden summary</div><p>Body</p>");

        assertThat(text).doesNotContain("Hidden summary");
        assertThat(text).contains("Body");
    }

    @Test
    @DisplayName("plain text unescapes entities so readers don't see &amp;")
    void plainTextUnescapes() {
        assertThat(engine.toPlainText("<p>Bed &amp; Breakfast</p>")).isEqualTo("Bed & Breakfast");
    }

    @Test
    @DisplayName("escapeHtml covers every character that can break out of markup")
    void escapeHtmlIsComplete() {
        assertThat(EmailTemplateEngine.escapeHtml("<>&\"'"))
                .isEqualTo("&lt;&gt;&amp;&quot;&#39;");
    }

}
