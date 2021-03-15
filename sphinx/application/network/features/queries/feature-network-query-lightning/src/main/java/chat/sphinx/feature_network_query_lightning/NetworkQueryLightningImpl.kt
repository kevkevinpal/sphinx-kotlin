package chat.sphinx.feature_network_query_lightning

import chat.sphinx.concept_network_client.NetworkClient
import chat.sphinx.concept_network_query_lightning.NetworkQueryLightning
import chat.sphinx.concept_relay.RelayDataHandler
import com.squareup.moshi.Moshi
import io.matthewnelson.concept_coroutines.CoroutineDispatchers

class NetworkQueryLightningImpl(
    private val dispatchers: CoroutineDispatchers,
    private val moshi: Moshi,
    private val networkClient: NetworkClient,
    private val relayDataHandler: RelayDataHandler
): NetworkQueryLightning() {

    companion object {
        private const val ENDPOINT_INVOICES = "/invoices"
        private const val ENDPOINT_INVOICES_CANCEL = "$ENDPOINT_INVOICES/cancel"
        private const val ENDPOINT_PAYMENT = "/payment"
        private const val ENDPOINT_PAYMENTS = "${ENDPOINT_PAYMENT}s"
        private const val ENDPOINT_CHANNELS = "/channels"
        private const val ENDPOINT_BALANCE = "/balance"
        private const val ENDPOINT_BALANCE_ALL = "$ENDPOINT_BALANCE/all"
        private const val ENDPOINT_GET_INFO = "/getinfo"
        private const val ENDPOINT_LOGS = "/logs"
        private const val ENDPOINT_INFO = "/info"
        private const val ENDPOINT_ROUTE = "/route"
        private const val ENDPOINT_QUERY_ONCHAIN_ADDRESS = "/query/onchain_address"
        private const val ENDPOINT_UTXOS = "/utxos"
    }

    ///////////
    /// GET ///
    ///////////
//    app.get('/invoices', invoices.listInvoices)
//    app.get('/payments', payments.listPayments)
//    app.get('/channels', details.getChannels)
//    app.get('/balance', details.getBalance)
//    app.get('/balance/all', details.getLocalRemoteBalance)
//    app.get('/getinfo', details.getInfo)
//    app.get('/logs', details.getLogsSince)
//    app.get('/info', details.getNodeInfo)
//    app.get('/route', details.checkRoute)
//    app.get('/query/onchain_address/:app', queries.queryOnchainAddress)
//    app.get('/utxos', queries.listUTXOs)

    ///////////
    /// PUT ///
    ///////////
//    app.put('/invoices', invoices.payInvoice)

    ////////////
    /// POST ///
    ////////////
//    app.post('/invoices', invoices.createInvoice)
//    app.post('/invoices/cancel', invoices.cancelInvoice)
//    app.post('/payment', payments.sendPayment)

    //////////////
    /// DELETE ///
    //////////////
}