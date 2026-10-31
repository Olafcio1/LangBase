package pl.olafcio.langbase

abstract class SectionType {
    protected String prefix

    final void setInitialPrefix(String prefix) {
        this.prefix = prefix
        reset()
    }

    static enum State {
        /**
         * Means the line is still part of the section.
         */
        CONTINUE,

        /**
         * Means the line closes the section <i>(e.g. a right curly bracket)</i>.
         */
        CLOSE,

        /**
         * Means the line isn't part of the section, and should be processed separately.
         */
        BREAK
    }

    abstract State accept(String line)

    void reset() {}


    public static final SectionType SPACED = new SectionType() {
        private var prefixReady = false

        @Override
        State accept(String line) {
            if (!line.startsWith(prefix))
                return State.BREAK

            var after = line.substring(prefix.length())
            var trim = after.stripLeading()

            int spaces = after.length() - trim.length()

            if (!prefixReady) {
                var space = after.substring(0, spaces)
                if (space.isEmpty())
                    return State.BREAK

                prefix += space
                prefixReady = true
            } else {
                if (!trim.isEmpty())
                    throw new SyntaxError("Incorrect amount of spaces provided: ${spaces}")
            }

            return State.CONTINUE
        }

        @Override
        void reset() {
            prefixReady = false
        }
    }
}
