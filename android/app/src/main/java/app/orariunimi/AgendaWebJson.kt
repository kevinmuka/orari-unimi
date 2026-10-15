package app.orariunimi

/** Extracts a JSON literal assigned to a JavaScript variable by AgendaWeb combo.php. */
internal fun extractAgendaVariable(body: String, name: String): String {
    val match = Regex("\\bvar\\s+${Regex.escape(name)}\\s*=").find(body)
        ?: throw IllegalStateException("La risposta UNIMI non contiene $name.")
    val start = (match.range.last + 1 until body.length).firstOrNull { !body[it].isWhitespace() }
        ?: throw IllegalStateException("Il formato di $name non è valido.")
    if (body[start] != '[' && body[start] != '{') {
        throw IllegalStateException("Il formato di $name non è valido.")
    }
    val stack = ArrayDeque<Char>()
    var quoted = false
    var escaped = false
    for (index in start until body.length) {
        val char = body[index]
        if (quoted) {
            if (escaped) escaped = false
            else if (char == '\\') escaped = true
            else if (char == '"') quoted = false
            continue
        }
        when (char) {
            '"' -> quoted = true
            '{' -> stack.addLast('}')
            '[' -> stack.addLast(']')
            '}', ']' -> {
                if (stack.removeLastOrNull() != char) {
                    throw IllegalStateException("Il formato di $name non è valido.")
                }
                if (stack.isEmpty()) return body.substring(start, index + 1)
            }
        }
    }
    throw IllegalStateException("La risposta UNIMI per $name è incompleta.")
}
