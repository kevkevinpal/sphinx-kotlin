package chat.sphinx.feature_network_query_save_profile.model

import chat.sphinx.concept_network_query_save_profile.model.SignBase64Dto
//import chat.sphinx.concept_network_query_save_profile.model.VerifyExternalDto
import chat.sphinx.concept_network_relay_call.RelayResponse
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SignBase64RelayResponse(
    override val success: Boolean,
    override val response: SignBase64Dto?,
    override val error: String?
): RelayResponse<SignBase64Dto>()