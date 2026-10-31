package pl.olafcio.langbase

import java.nio.file.Path
import java.util.function.Function

final class Langbase<T, RootElement, Element> {
    private final Function<File, T> setup

    private Langbase(Function<File, T> setup) {
        this.setup = setup
    }

    static <T, RootElement, Element> Langbase<T, RootElement, Element> builder(Function<File<RootElement, Element>, T> setup) {
        return new Langbase(setup)
    }

    T parseFile(Path path) {
        return this.setup.apply(new File<RootElement, Element>(path))
    }
}
