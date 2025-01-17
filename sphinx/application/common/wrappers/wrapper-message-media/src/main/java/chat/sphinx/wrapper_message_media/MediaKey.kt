package chat.sphinx.wrapper_message_media

@Suppress("NOTHING_TO_INLINE")
inline fun String.toMediaKey(): MediaKey? =
    try {
        MediaKey(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@JvmInline
value class MediaKey(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "MediaKey cannot be empty"
        }
    }
}
