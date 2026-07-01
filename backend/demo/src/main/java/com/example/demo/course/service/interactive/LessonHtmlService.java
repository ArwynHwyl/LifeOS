package com.example.demo.course.service.interactive;

import com.example.demo.course.service.document.PdfStorageService;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

import com.example.demo.course.entity.SubTopicAsset;

@Service
public class LessonHtmlService {

    private final Safelist safelist = Safelist.none()
            .addTags("p", "h2", "h3", "b", "strong", "i", "em", "u", "code", "pre", "a",
                    "ul", "ol", "li", "blockquote", "figure", "figcaption", "img", "br")
            .addAttributes("a", "href", "title", "target", "rel")
            .addAttributes("img", "alt", "data-asset-id")
            .addProtocols("a", "href", "http", "https", "mailto")
            .preserveRelativeLinks(false);

    public String sanitizeForStorage(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        String html = looksLikeHtml(content) ? content : plainTextToHtml(content);
        Document.OutputSettings settings = new Document.OutputSettings().prettyPrint(false);
        String cleaned = Jsoup.clean(html, "", safelist, settings);
        cleaned = stripExternalImageSources(cleaned);
        return cleaned.isBlank() ? null : cleaned;
    }

    public String contentHtml(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        return sanitizeForStorage(content);
    }

    public String contentHtmlWithAssetUrls(
            String content,
            Iterable<SubTopicAsset> assets,
            Function<SubTopicAsset, PdfStorageService.PresignedRead> urlFactory
    ) {
        String html = contentHtml(content);
        if (html == null || html.isBlank()) {
            return "";
        }
        Map<Long, SubTopicAsset> assetsById = toAssetMap(assets);
        Document document = Jsoup.parseBodyFragment(html);
        document.outputSettings().prettyPrint(false);
        for (Element image : document.select("img[data-asset-id]")) {
            Long assetId = parseLong(image.attr("data-asset-id"));
            SubTopicAsset asset = assetId == null ? null : assetsById.get(assetId);
            if (asset == null) {
                image.remove();
                continue;
            }
            PdfStorageService.PresignedRead read = urlFactory.apply(asset);
            image.attr("src", read.fileUrl());
            if (!image.hasAttr("alt") && asset.getAltText() != null) {
                image.attr("alt", asset.getAltText());
            }
        }
        return document.body().html();
    }

    public String plainTextToHtml(String value) {
        String[] paragraphs = value.strip().split("\\R{2,}");
        StringBuilder html = new StringBuilder();
        for (String paragraph : paragraphs) {
            String normalized = paragraph.strip();
            if (normalized.isBlank()) {
                continue;
            }
            html.append("<p>")
                    .append(org.jsoup.nodes.Entities.escape(normalized).replaceAll("\\R", "<br>"))
                    .append("</p>");
        }
        return html.toString();
    }

    private String stripExternalImageSources(String html) {
        Document document = Jsoup.parseBodyFragment(html);
        document.outputSettings().prettyPrint(false);
        for (Element image : document.select("img")) {
            image.removeAttr("src");
            if (!image.hasAttr("data-asset-id")) {
                image.remove();
            }
        }
        for (Element link : document.select("a")) {
            link.attr("rel", "noopener noreferrer");
            link.attr("target", "_blank");
        }
        return document.body().html();
    }

    private boolean looksLikeHtml(String content) {
        return content.matches("(?is).*<\\s*(p|h2|h3|b|strong|i|em|u|code|pre|a|ul|ol|li|blockquote|figure|img|br)\\b.*");
    }

    private Map<Long, SubTopicAsset> toAssetMap(Iterable<SubTopicAsset> assets) {
        return java.util.stream.StreamSupport.stream(assets.spliterator(), false)
                .filter(asset -> asset.getId() != null)
                .collect(Collectors.toMap(SubTopicAsset::getId, Function.identity(), (first, second) -> first));
    }

    private Long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
