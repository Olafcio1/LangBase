package pl.olafcio.langbase

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class File<RootElement, Element> : Parser<RootElement, Element> {
    private val path: Path

    constructor(path: Path) :
           this(path, Files.readString(path, StandardCharsets.UTF_8))

    constructor(path: Path, content: String) :
          super(content)
    {
        this.path = path
    }

    fun getPath(): Path {
        return path
    }
}
