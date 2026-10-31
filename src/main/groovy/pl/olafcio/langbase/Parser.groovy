package pl.olafcio.langbase

import java.util.function.BiConsumer
import java.util.function.BiFunction
import java.util.function.Consumer
import java.util.function.Function
import java.util.regex.Matcher
import java.util.regex.Pattern

class Parser<RootElement, Element> {
    private final String content
    private       String remaining

    Parser(String content) {
        this.content = content.replace("\r\n", "\n") \
                              .replace("\r",   "\n")

        this.remaining = this.content
    }

    String getContent() {
        return content
    }

    String getRemainingContent() {
        return remaining
    }

    Matcher findStatement(String regex) {
        var line = findLine()

        regex = normalizeRegex(regex)

        var matcher = Pattern.compile(regex).matcher(line)
        if (matcher.find()) {
            return matcher
        }

        throw new SyntaxError("Expected '${regex}'")
    }

    protected String findLine(boolean count = true) {
        var line = remaining
        var newline = line.indexOf('\n')

        if (newline != -1)
            line = line.substring(0, newline)

        if (count)
            nextLine()

        return line
    }

    protected void nextLine() {
        var newline = remaining.indexOf('\n')
        if (newline != -1)
            remaining = remaining.substring(newline + 1)
        else if (reachedEOF())
            throw new SyntaxError("Cannot proceed to next line: EOF reached")
        else
            remaining = ""
    }

    boolean reachedEOF() {
        return remaining.isEmpty()
    }

    private static String normalizeRegex(String regex) {
        if (!regex.endsWith('$'))
            regex += '$'

        return regex
    }

    void findStatement(Map parameters, String regex, Consumer<Matcher> consumer) {
        findStatement(regex, consumer, parameters)
    }

    void findStatement(String regex, Consumer<Matcher> consumer, Map parameters = Map.of()) {
        Matcher stmt

        try {
            stmt = findStatement(regex)
        } catch (SyntaxError e) {
            if (!parameters.getOrDefault("optional", false))
                throw e

            return
        }

        consumer.accept(stmt)
    }

    void findStatement(Map parameters, String regex, BiConsumer<Matcher, String> consumer) {
        findStatement(regex, consumer, parameters)
    }

    void findStatement(String regex, BiConsumer<Matcher, String> consumer, Map parameters = Map.of()) {
        var line = findLine()

        regex = normalizeRegex(regex)

        var matcher = Pattern.compile(regex).matcher(line)
        if (matcher.find()) {
            consumer.accept(matcher, line)
            return
        }

        if (!parameters.getOrDefault("optional", false))
            throw new SyntaxError("Expected '${regex}'")
    }

    private final ArrayList<Function<String, RootElement>> statements
            = new ArrayList<>()

    void addGlobalStatement(String regex, Function<Matcher, RootElement> function) {
        addGlobalStatement(regex, (matcher, firstLine) -> function.apply(matcher))
    }

    void addGlobalStatement(String regex, BiFunction<Matcher, String, RootElement> function) {
        regex = normalizeRegex(regex)

        var pattern = Pattern.compile(regex)

        statements.add(line -> {
            var matcher = pattern.matcher(line)
            if (matcher.find()) {
                nextLine()
                return function.apply(matcher, line)
            }

            return null
        })
    }

    void addGlobalSection(String regex, SectionType sectionType, Function<Section<Element, ? super Element>, RootElement> function) {
        addGlobalStatement(
                regex,
                (BiFunction<Matcher, String, RootElement>) (
                    (Matcher matcher, String firstLine) -> {
                        return this.<RootElement>sectionCallback(matcher, firstLine, sectionType, el -> function.apply(el))
                    }
                )
        )
    }

    void findSection(Map parameters, String regex, Consumer<Section<Element, ? super Element>> consumer, SectionType sectionType) {
        findSection(regex, consumer, sectionType, parameters)
    }

    void findSection(String regex, Consumer<Section<Element, ? super Element>> consumer, SectionType sectionType, Map parameters = Map.of()) {
        findStatement(
                regex,
                (BiConsumer<Matcher, String>) (
                    (Matcher matcher, String firstLine) -> {
                        this.<Void>sectionCallback(matcher, firstLine, sectionType, el -> {
                            consumer.accept(el)
                            return null
                        })
                    }
                ),
                parameters
        )
    }

    private <Return> Return sectionCallback(
            Matcher matcher, String firstLine,
            SectionType sectionType, Function<Section<Element, ? super Element>, Return> function
    ) {
        sectionType.setInitialPrefix(firstLine.substring(0, firstLine.length() - firstLine.stripLeading().length()))

        var content = []
        while (true) {
            var line = findLine(false)
            var state = sectionType.accept(line)

            if (state == SectionType.State.BREAK) {
                break
            } else {
                nextLine()

                if (state == SectionType.State.CLOSE)
                    break
            }
        }

        var section = new Section<Element, ? super Element>(String.join("\n", content), matcher)
        return function.apply(section)
    }

    RootElement parseStatement() {
        var line = findLine(false)

        for (Function<String, RootElement> stmt : statements) {
            var ret = stmt.apply(line)
            if (ret != null)
                return ret
        }

        throw new SyntaxError("Expected a statement")
    }

    Collection<RootElement> run() {
        var output = new ArrayList<RootElement>()

        while (!reachedEOF()) {
            var line = findLine(false)
            if (line.trim().isEmpty()) {
                nextLine()
                continue
            }

            output.add(parseStatement())
        }

        return output
    }
}
