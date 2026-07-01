package com.example.demo.course.service.interactive;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LessonHtmlServiceTests {

    private final LessonHtmlService lessonHtmlService = new LessonHtmlService();

    @Test
    void convertsPlainTextToParagraphHtml() {
        String html = lessonHtmlService.sanitizeForStorage("First paragraph\n\nSecond line\nstill second");

        assertThat(html).isEqualTo("<p>First paragraph</p><p>Second line<br>still second</p>");
    }

    @Test
    void removesDangerousTagsAttributesAndExternalImages() {
        String html = lessonHtmlService.sanitizeForStorage("""
                <h2 onclick="alert(1)">Intro</h2>
                <script>alert(1)</script>
                <p>Use <strong>vectors</strong> and <code>x</code>.</p>
                <a href="javascript:alert(1)">bad</a>
                <img src="https://example.com/x.png" onerror="alert(1)">
                <figure><img data-asset-id="42" src="https://signed.example/x.png" alt="diagram"><figcaption>Diagram</figcaption></figure>
                """);

        assertThat(html).contains("<h2>Intro</h2>");
        assertThat(html).contains("<strong>vectors</strong>");
        assertThat(html).doesNotContain("script", "onclick", "onerror", "javascript:", "https://signed.example");
        assertThat(html).doesNotContain("<img src");
        assertThat(html).contains("<img data-asset-id=\"42\" alt=\"diagram\">");
    }
}
