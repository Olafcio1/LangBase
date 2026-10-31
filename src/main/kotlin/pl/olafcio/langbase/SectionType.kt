package pl.olafcio.langbase

abstract class SectionType {
    protected lateinit var prefix: String

    fun setInitialPrefix(prefix: String) {
        this.prefix = prefix
        reset()
    }

    enum class State {
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

    abstract fun accept(line: String): State

    open fun reset() {}


    companion object Static {
        @JvmStatic
        val SPACED = object : SectionType() {
            private var prefixReady = false

            override fun accept(line: String): State {
                if (!line.startsWith(prefix))
                    return State.BREAK

                var after = line.substring(prefix.length)
                var trim = after.trimStart()

                var spaces: Int = after.length - trim.length

                if (!prefixReady) {
                    var space = after.substring(0, spaces)
                    if (space.isEmpty())
                        return State.BREAK

                    prefix += space
                    prefixReady = true
                } else {
                    if (!trim.isEmpty())
                        throw SyntaxError("Incorrect amount of spaces provided: ${spaces}")
                }

                return State.CONTINUE
            }

            override fun reset() {
                prefixReady = false
            }
        }
    }
}
