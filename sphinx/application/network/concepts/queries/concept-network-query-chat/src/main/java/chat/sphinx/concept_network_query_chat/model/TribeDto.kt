package chat.sphinx.concept_network_query_chat.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TribeDto(
    val name: String,
    val description: String,
    val img: String?,
    val group_key: String,
    val owner_pubkey: String,
    val owner_route_hint: String?,
    val owner_alias: String?,
    val price_to_join: Long = 0,
    val price_per_message: Long = 0,
    val escrow_amount: Long = 0,
    val escrow_millis: Long = 0,
    val unlisted: Any?,
    val private: Any?,
    val deleted: Any?,
    val app_url: String?,
    val feed_url: String?,
) {

    var amount: Long? = null
    var myAlias: String? = null
    var host: String? = null
    var uuid: String? = null

    val hourToStake: Long
        get() = (escrow_millis) / 60 / 60 / 1000

    fun set(host: String?, uuid: String) {
        this.host = host
        this.uuid = uuid
    }
}