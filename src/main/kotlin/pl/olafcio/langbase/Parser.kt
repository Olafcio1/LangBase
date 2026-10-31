package pl.olafcio.langbase

import java.util.function.BiConsumer
import java.util.function.BiFunction
import java.util.function.Consumer
import java.util.function.Function
import java.util.regex.Matcher
import java.util.regex.Pattern

open class Parser<RootElement, Element> {
    private val content: String
    private var remaining: String

    constructor(content: String) {
        this.content = content.replace("\r\n", "\n")
                              .replace("\r",   "\n")

        this.remaining = this.content
    }

    fun getContent(): String {
        return content
    }

    fun getRemainingContent(): String {
        return remaining
    }

    open fun findStatement(regex: String): Matcher {
        val line = findLine()
        val regex = normalizeRegex(regex)

        val matcher = Pattern.compile(regex).matcher(line)
        if (matcher.find()) {
            return matcher
        }

        throw SyntaxError("Expected '${regex}'")
    }

    protected open fun findLine(count: Boolean = true): String {
        var line = remaining
        val newline = line.indexOf('\n')

        if (newline != -1)
            line = line.substring(0, newline)

        if (count)
            nextLine()

        return line
    }

    protected fun nextLine() {
        val newline = remaining.indexOf('\n')
        if (newline != -1)
            remaining = remaining.substring(newline + 1)
        else if (reachedEOF())
            throw SyntaxError("Cannot proceed to next line: EOF reached")
        else
            remaining = ""
    }

    open fun reachedEOF(): Boolean {
        return remaining.isEmpty()
    }

    companion object Static {
        @JvmStatic
        private fun normalizeRegex(regex: String): String {
            // Stupid kotlin
            var regex = regex

            if (!regex.startsWith('^'))
                regex = '^' + regex

            if (!regex.endsWith('$'))
                regex += '$'

            return regex
        }
    }

    open fun findStatement(parameters: Map<*, *>, regex: String, consumer: Consumer<Matcher>) {
        findStatement(regex, consumer, parameters)
    }

    open fun findStatement(regex: String, consumer: Consumer<Matcher>, parameters: Map<*, *> = java.util.Map.of<Object, Object>()) {
        var stmt: Matcher

        try {
            stmt = findStatement(regex)
        } catch (e: SyntaxError) {
            if (parameters.getOrDefault("optional", false) == false)
                throw e

            return
        }

        consumer.accept(stmt)
    }

    open fun findStatement(parameters: Map<*, *>, regex: String, consumer: BiConsumer<Matcher, String>) {
        findStatement(regex, consumer, parameters)
    }

    open fun findStatement(regex: String, consumer: BiConsumer<Matcher, String>, parameters: Map<*, *> = java.util.Map.of<Object, Object>()) {
        val line = findLine()
        val regex = normalizeRegex(regex)

        val matcher = Pattern.compile(regex).matcher(line)
        if (matcher.find()) {
            consumer.accept(matcher, line)
            return
        }

        if (parameters.getOrDefault("optional", false) == false)
            throw SyntaxError("Expected '${regex}'")
    }

    private val statements: ArrayList<Function<String, RootElement?>>
                          = ArrayList()

    open fun addGlobalStatement(regex: String, function: Function<Matcher, RootElement?>) {
        addGlobalStatement(regex) { matcher, _ -> function.apply(matcher) }
    }

    open fun addGlobalStatement(regex: String, function: BiFunction<Matcher, String, RootElement?>) {
        val regex = normalizeRegex(regex)
        val pattern = Pattern.compile(regex)

        statements.add { line ->
            val matcher = pattern.matcher(line)
            if (matcher.find()) {
                nextLine()
                return@add function.apply(matcher, line)
            }

            return@add null
        }
    }

    open fun addGlobalSection(regex: String, sectionType: SectionType, function: Function<Section<Element, in Element>, RootElement?>) {
        addGlobalStatement(regex) { matcher, firstLine ->
            return@addGlobalStatement this.sectionCallback(
                matcher,
                firstLine,
                sectionType
            ) { el -> function.apply(el) }
        }
    }

    open fun findSection(parameters: Map<*, *>, regex: String, consumer: Consumer<Section<Element, in Element>>, sectionType: SectionType) {
        findSection(regex, consumer, sectionType, parameters)
    }

    open fun findSection(regex: String, consumer: Consumer<Section<Element, in Element>>, sectionType: SectionType, parameters: Map<*, *> = java.util.Map.of<Object, Object>()) {
        findStatement(
                regex,
                { matcher, firstLine ->
                    this.sectionCallback<Void>(matcher, firstLine, sectionType, { el ->
                        consumer.accept(el)
                        return@sectionCallback null
                    })
                },
                parameters
        )
    }

    private fun <Return> sectionCallback(
            matcher: Matcher, firstLine: String,
            sectionType: SectionType, function: Function<Section<Element, in Element>, Return?>
    ): Return? {
        sectionType.setInitialPrefix(firstLine.substring(0, firstLine.length - firstLine.trimStart().length))

        val content = mutableListOf<String>()
        while (true) {
            val line = findLine(false)
            val state = sectionType.accept(line)

            if (state == SectionType.State.BREAK) {
                break
            } else {
                nextLine()

                if (state == SectionType.State.CLOSE)
                    break
            }
        }

        val section = Section<Element, Element>(content.joinToString("\n"), matcher)
        return function.apply(section)
    }

    open fun parseStatement(): RootElement {
        val line = findLine(false)

        for (stmt in this.statements) {
            val ret = stmt.apply(line)
            if (ret != null)
                return ret
        }

        throw SyntaxError("Expected a statement")
    }

    open fun run(): Collection<RootElement> {
        val output = ArrayList<RootElement>()

        while (!reachedEOF()) {
            val line = findLine(false)
            if (line.trim().isEmpty()) {
                nextLine()
                continue
            }

            output.add(parseStatement())
        }

        return output
    }
}
