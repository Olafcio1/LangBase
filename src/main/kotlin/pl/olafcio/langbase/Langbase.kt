package pl.olafcio.langbase

import java.nio.file.Path
import java.util.function.Function

class Langbase<T, RootElement, Element> {
    private val setup: Function<File<RootElement, Element>, T>

    private constructor(setup: Function<File<RootElement, Element>, T>) {
        this.setup = setup
    }

    companion object Static {
        @JvmStatic
        fun <T, RootElement, Element> builder(setup: Function<File<RootElement, Element>, T>): Langbase<T, RootElement, Element> {
            return Langbase(setup)
        }
    }

    fun parseFile(path: Path): T {
        return this.setup.apply(File(path))
    }
}
