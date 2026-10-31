package pl.olafcio.langbase

import java.util.regex.Matcher

class Section<RootElement, Element> extends Parser<RootElement, Element> {
    private final Matcher pattern

    Section(String content, Matcher pattern) {
        super(content)
        this.pattern = pattern
    }

    Matcher getPattern() {
        return pattern
    }
}
