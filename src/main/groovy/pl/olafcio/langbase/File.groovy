package pl.olafcio.langbase

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

final class File<RootElement, Element> extends Parser<RootElement, Element> {
    private final Path path

    File(Path path) {
        this(path, Files.readString(path, StandardCharsets.UTF_8))
    }

    File(Path path, String content) {
        super(content)
        this.path = path
    }

    Path getPath() {
        return path
    }
}
