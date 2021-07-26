package chat.sphinx.splash.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.viewModelScope
import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_background_login.BackgroundLoginHandler
import chat.sphinx.concept_network_query_contact.NetworkQueryContact
import chat.sphinx.concept_network_query_invite.NetworkQueryInvite
import chat.sphinx.concept_network_query_invite.model.RedeemInviteDto
import chat.sphinx.concept_network_tor.TorManager
import chat.sphinx.concept_relay.RelayDataHandler
import chat.sphinx.concept_repository_lightning.LightningRepository
import chat.sphinx.concept_view_model_coordinator.ViewModelCoordinator
import chat.sphinx.key_restore.KeyRestore
import chat.sphinx.key_restore.KeyRestoreResponse
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.onboard_common.model.RedemptionCode
import chat.sphinx.scanner_view_model_coordinator.request.ScannerFilter
import chat.sphinx.scanner_view_model_coordinator.request.ScannerRequest
import chat.sphinx.scanner_view_model_coordinator.response.ScannerResponse
import chat.sphinx.splash.navigation.SplashNavigator
import chat.sphinx.wrapper_invite.InviteString
import chat.sphinx.wrapper_invite.toValidInviteStringOrNull
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import chat.sphinx.wrapper_relay.isOnionAddress
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.MotionLayoutViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_authentication.coordinator.AuthenticationCoordinator
import io.matthewnelson.concept_authentication.coordinator.AuthenticationRequest
import io.matthewnelson.concept_authentication.coordinator.AuthenticationResponse
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.crypto_common.annotations.RawPasswordAccess
import io.matthewnelson.crypto_common.clazzes.PasswordGenerator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
internal class SplashViewModel @Inject constructor(
    private val app: Application,
    private val authenticationCoordinator: AuthenticationCoordinator,
    private val backgroundLoginHandler: BackgroundLoginHandler,
    dispatchers: CoroutineDispatchers,
    private val keyRestore: KeyRestore,
    private val lightningRepository: LightningRepository,
    private val navigator: SplashNavigator,
    private val networkQueryInvite: NetworkQueryInvite,
    private val networkQueryContact: NetworkQueryContact,
    private val relayDataHandler: RelayDataHandler,
    private val scannerCoordinator: ViewModelCoordinator<ScannerRequest, ScannerResponse>,
    private val torManager: TorManager,
): MotionLayoutViewModel<
        Any,
        Context,
        SplashSideEffect,
        SplashViewState
        >(dispatchers, SplashViewState.Start_ShowIcon)
{

    private var screenInit: Boolean = false
    fun screenInit() {
        if (screenInit) {
            return
        } else {
            screenInit = true
        }

        // prime the account balance retrieval from SharePrefs
        viewModelScope.launch(mainImmediate) {
            lightningRepository.getAccountBalance().firstOrNull()
        }

        viewModelScope.launch(mainImmediate) {
            backgroundLoginHandler.attemptBackgroundLogin(
                updateLastLoginTimeOnSuccess = true
            )?.let {
                navigator.toDashboardScreen(
                    // No need as it was already updated
                    updateBackgroundLoginTime = false
                )
            } ?: let {
                if (authenticationCoordinator.isAnEncryptionKeySet()) {
                    authenticationCoordinator.submitAuthenticationRequest(
                        AuthenticationRequest.LogIn()
                    ).firstOrNull().let { response ->
                        @Exhaustive
                        when (response) {
                            null,
                            is AuthenticationResponse.Failure -> {
                                // will not be returned as back press for handling
                                // a LogIn request minimizes the application until
                                // User has authenticated
                            }
                            is AuthenticationResponse.Success.Authenticated -> {
                                navigator.toDashboardScreen(updateBackgroundLoginTime = true)
                            }
                            is AuthenticationResponse.Success.Key -> {
                                // will never be returned
                            }
                        }
                    }
                } else {
                    // Display OnBoard
                    delay(100L) // need a slight delay for window to fully hand over to splash
                    updateViewState(SplashViewState.Transition_Set2_ShowWelcome)
                }
            }
        }
    }

    // TODO: Use coordinator pattern and limit
    fun navigateToScanner() {
        viewModelScope.launch(mainImmediate) {
            val response = scannerCoordinator.submitRequest(
                ScannerRequest(
                    filter = object : ScannerFilter() {
                        override suspend fun checkData(data: String): Response<Any, String> {
                            if (data.toValidInviteStringOrNull() != null) {
                                return Response.Success(Any())
                            }

                            if (RedemptionCode.decode(data) != null) {
                                return Response.Success(Any())
                            }

                            return Response.Error("QR code is not an account restore code")
                        }
                    }
                )
            )
            if (response is Response.Success) {
                submitSideEffect(SplashSideEffect.FromScanner(response.value))
            }
        }
    }

    private var processConnectionCodeJob: Job? = null
    fun processConnectionCode(input: String?) {
        if (processConnectionCodeJob?.isActive == true) {
            return
        }

        processConnectionCodeJob = viewModelScope.launch(mainImmediate) {

            if (input == null || input.isEmpty()) {
                updateViewState(SplashViewState.HideLoadingWheel)
                submitSideEffect(SplashSideEffect.InputNullOrEmpty)
                return@launch
            }

            // Maybe we can have a SignupStyle class to reflect this? Since there's a lot of decoding
            // going on in different classes
            // Invite Code
            input.toValidInviteStringOrNull()?.let { inviteString ->
                redeemInvite(inviteString)
                return@launch
            }

            RedemptionCode.decode(input)?.let { code ->
                @Exhaustive
                when (code) {
                    is RedemptionCode.AccountRestoration -> {
                        updateViewState(
                            SplashViewState.Transition_Set3_DecryptKeys(code)
                        )
                    }
                    is RedemptionCode.NodeInvite -> {
                        withContext(io) {
                            storeTemporaryInvite()
                        }

                        generateToken(
                            ip = code.ip,
                            nodePubKey = null,
                            password = code.password
                        )
                    }
                }

                return@launch
            }

            updateViewState(SplashViewState.HideLoadingWheel)
            submitSideEffect(SplashSideEffect.InvalidCode)
        }
    }

    private suspend fun redeemInvite(input: InviteString) {
        networkQueryInvite.redeemInvite(input).collect { loadResponse ->
            @Exhaustive
            when (loadResponse) {
                is LoadResponse.Loading -> {}
                is Response.Error -> {
                    updateViewState(SplashViewState.HideLoadingWheel)
                    submitSideEffect(SplashSideEffect.InvalidCode)
                }
                is Response.Success -> {
                    val inviteResponse = loadResponse.value.response

                    inviteResponse?.invite?.let { invite ->
                        storeTemporaryInvite(invite)

                        generateToken(
                            ip = RelayUrl(inviteResponse.ip),
                            nodePubKey = inviteResponse.pubkey,
                            password = null,
                        )
                    }
                }
            }
        }
    }

    private suspend fun generateToken(ip: RelayUrl, nodePubKey: String?, password: String?) {
        @OptIn(RawPasswordAccess::class)
        val authToken = AuthorizationToken(
            PasswordGenerator(passwordLength = 20).password.value.joinToString("")
        )

        val relayUrl = relayDataHandler.formatRelayUrl(ip)
        torManager.setTorRequired(relayUrl.isOnionAddress)

        storeToken(authToken.value, relayUrl.value)

        networkQueryContact.generateToken(relayUrl, authToken, password, nodePubKey).collect { loadResponse ->
            @Exhaustive
            when (loadResponse) {
                is LoadResponse.Loading -> {}
                is Response.Error -> {
                    updateViewState(SplashViewState.HideLoadingWheel)
                    submitSideEffect(SplashSideEffect.GenerateTokenFailed)
                }
                is Response.Success -> {
                    navigator.toOnBoardScreen()
                }
            }
        }
    }

    private fun storeTemporaryInvite(invite: RedeemInviteDto? = null) {
        // I needed a way to store this to transition between fragments
        // but I wasn't sure what to use besides this
        app.getSharedPreferences("sphinx_temp_prefs", Context.MODE_PRIVATE).let {
                sharedPrefs ->
            sharedPrefs?.edit()?.let { editor ->
                invite?.let { invite ->
                    // Signing up with invite code. Inviter is coming from relay
                    editor
                        .putString("sphinx_temp_inviter_nickname", invite.nickname)
                        .putString("sphinx_temp_inviter_pubkey", invite.pubkey)
                        .putString("sphinx_temp_inviter_route_hint", invite.route_hint)
                        .putString("sphinx_temp_invite_message", invite.message)
                        .putString("sphinx_temp_invite_action", invite.action)
                        .putString("sphinx_temp_invite_string", invite.pin)
                        .let { editor ->
                            if (!editor.commit()) {
                                editor.apply()
                            }
                        }
                } ?: run {
                    // Signing up relay connection string. Using default inviter
                    editor
                        .putString("sphinx_temp_inviter_nickname", "Sphinx Support")
                        .putString("sphinx_temp_inviter_pubkey", "023d70f2f76d283c6c4e58109ee3a2816eb9d8feb40b23d62469060a2b2867b77f")
                        .putString("sphinx_temp_invite_message", "Welcome to Sphinx")
                        .let { editor ->
                            if (!editor.commit()) {
                                editor.apply()
                            }
                        }
                }
            }
        }
    }

    private fun storeToken(token: String, ip: String) {
        // I needed a way to store this to transition between fragments
        // but I wasn't sure what to use besides this
        app.getSharedPreferences("sphinx_temp_prefs", Context.MODE_PRIVATE).let { sharedPrefs ->

            sharedPrefs?.edit()?.let { editor ->
                editor
                    .putString("sphinx_temp_ip", ip)
                    .putString("sphinx_temp_auth_token", token)
                    .let { editor ->
                        if (!editor.commit()) {
                            editor.apply()
                        }
                    }
            }
        }
    }

    private var decryptionJob: Job? = null
    fun decryptInput(viewState: SplashViewState.Set3_DecryptKeys) {
        // TODO: Replace with automatic launching upon entering the 6th PIN character
        //  when Authentication View's Layout gets incorporated
        if (viewState.pinWriter.size() != 6 /*TODO: https://github.com/stakwork/sphinx-kotlin/issues/9*/) {
            viewModelScope.launch(mainImmediate) {
                submitSideEffect(SplashSideEffect.InvalidPinLength)
            }
        }

        if (decryptionJob?.isActive == true) {
            return
        }

        var decryptionJobException: Exception? = null
        decryptionJob = viewModelScope.launch(default) {
            try {
                val pin = viewState.pinWriter.toCharArray()

                val decryptedCode: RedemptionCode.AccountRestoration.DecryptedRestorationCode =
                    viewState.restoreCode.decrypt(pin, dispatchers)

                // TODO: Ask to use Tor before any network calls go out.
                // TODO: Hit relayUrl to verify creds work

                var success: KeyRestoreResponse.Success? = null
                keyRestore.restoreKeys(
                    privateKey = decryptedCode.privateKey,
                    publicKey = decryptedCode.publicKey,
                    userPin = pin,
                    relayUrl = decryptedCode.relayUrl,
                    authorizationToken = decryptedCode.authorizationToken,
                ).collect { flowResponse ->
                    // TODO: Implement in Authentication View when it get's built/refactored
                    if (flowResponse is KeyRestoreResponse.Success) {
                        success = flowResponse
                    }
                }

                success?.let { _ ->
                    // Overwrite PIN
                    viewState.pinWriter.reset()
                    repeat(6) {
                        viewState.pinWriter.append('0')
                    }

                    navigator.toDashboardScreen(updateBackgroundLoginTime = true)

                } ?: updateViewState(
                    SplashViewState.Set3_DecryptKeys(viewState.restoreCode)
                ).also {
                    submitSideEffect(SplashSideEffect.InvalidPin)
                }

            } catch (e: Exception) {
                decryptionJobException = e
            }
        }

        viewModelScope.launch(mainImmediate) {
            decryptionJob?.join()
            decryptionJobException?.let { exception ->
                updateViewState(
                    // reset view state
                    SplashViewState.Set3_DecryptKeys(viewState.restoreCode)
                )
                exception.printStackTrace()
                submitSideEffect(SplashSideEffect.DecryptionFailure)
            }
        }
    }

    // Unused
    override suspend fun onMotionSceneCompletion(value: Any) {
        return
    }
}
