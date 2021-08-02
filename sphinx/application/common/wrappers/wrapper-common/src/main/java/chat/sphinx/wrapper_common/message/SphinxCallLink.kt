package chat.sphinx.wrapper_common.message

@Suppress("NOTHING_TO_INLINE")
inline fun String.toSphinxCallLink(): SphinxCallLink? =
    try {
        SphinxCallLink(this)
    } catch (e: IllegalArgumentException) {
        null
    }

inline val String.isValidSphinxCallLink: Boolean
    get() = isNotEmpty() && matches("^${SphinxCallLink.REGEX}\$".toRegex())

@JvmInline
value class SphinxCallLink(val value: String) {

    companion object {
        const val REGEX = "https:\\/\\/.*\\/sphinx\\.call\\..*"

        private const val CALL_SERVER_URL = "https://jitsi.sphinx.chat"
        private const val CALL_ROOM_NAME = "sphinx.call"

        const val AUDIO_ONLY_PARAM = "config.startAudioOnly"

        fun newCallInvite(startAudioOnly: Boolean): SphinxCallLink? {
            val currentTime = System.currentTimeMillis()
            val audioOnlyParam = if (startAudioOnly) "#${AUDIO_ONLY_PARAM}=true" else ""
            val linkString = "$CALL_SERVER_URL/$CALL_ROOM_NAME.$currentTime$audioOnlyParam"

            return linkString.toSphinxCallLink()
        }
    }

    init {
        require(value.isValidSphinxCallLink) {
            "Invalid Sphinx Call Link"
        }
    }

    inline val startAudioOnly : Boolean
        get() = getParameter(AUDIO_ONLY_PARAM).toBoolean()

    fun getParameter(k: String): String? {
        val parameters = value.substringAfter("#").split("&")
        for (parameter in parameters) {
            val paramComponents = parameter.split("=")
            val key:String? = if (paramComponents.isNotEmpty()) paramComponents.elementAtOrNull(0) else null
            val value:String? = if (paramComponents.size > 1) paramComponents.elementAtOrNull(1) else null

            if (key == k) return value
        }
        return null
    }

}