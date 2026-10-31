package pl.olafcio.langbase

import java.util.regex.Matcher

open class Section<RootElement, Element> : Parser<RootElement, Element> {
    private val pattern: Matcher

    constructor(content: String, pattern: Matcher) : super(content) {
        this.pattern = pattern
    }

    fun getPattern(): Matcher {
        return pattern
    }
}
