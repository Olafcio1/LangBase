package pl.olafcio.langbase

open class SyntaxError : RuntimeException {
    constructor(message: String) : super(message)
}
