package chat.sphinx.concept_repository_lightning

import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_lightning.NodeBalance
import chat.sphinx.wrapper_lightning.NodeBalanceAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface LightningRepository {
    suspend fun getAccountBalance(): StateFlow<NodeBalance?>
    suspend fun getAccountBalanceAll(): Flow<LoadResponse<NodeBalanceAll, ResponseError>>
    val networkRefreshBalance: Flow<LoadResponse<Boolean, ResponseError>>
}
