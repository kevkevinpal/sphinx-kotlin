package chat.sphinx.di

import chat.sphinx.authentication.SphinxAuthenticationCoreManager
import chat.sphinx.authentication.SphinxAuthenticationCoreStorage
import chat.sphinx.feature_background_login.BackgroundLoginHandlerImpl
import chat.sphinx.authentication.SphinxEncryptionKeyHandler
import chat.sphinx.authentication.SphinxKeyRestore
import chat.sphinx.concept_background_login.BackgroundLoginHandler
import chat.sphinx.key_restore.KeyRestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.components.SingletonComponent
import io.matthewnelson.concept_authentication.coordinator.AuthenticationCoordinator
import io.matthewnelson.concept_authentication.data.AuthenticationStorage
import io.matthewnelson.concept_authentication.state.AuthenticationStateManager
import io.matthewnelson.concept_encryption_key.EncryptionKeyHandler
import io.matthewnelson.feature_authentication_core.AuthenticationCoreManager

@Module
@InstallIn(SingletonComponent::class)
object AuthenticationModule {

    @Provides
    fun provideAuthenticationCoreManager(
        sphinxAuthenticationCoreManager: SphinxAuthenticationCoreManager
    ): AuthenticationCoreManager =
        sphinxAuthenticationCoreManager

    @Provides
    fun provideAuthenticationStateManager(
        sphinxAuthenticationCoreManager: SphinxAuthenticationCoreManager
    ): AuthenticationStateManager =
        sphinxAuthenticationCoreManager

    @Provides
    fun provideAuthenticationStorage(
        sphinxAuthenticationCoreStorage: SphinxAuthenticationCoreStorage
    ): AuthenticationStorage =
        sphinxAuthenticationCoreStorage

    @Provides
    fun provideEncryptionKeyHandler(
        sphinxEncryptionKeyHandler: SphinxEncryptionKeyHandler
    ): EncryptionKeyHandler =
        sphinxEncryptionKeyHandler

    @Provides
    fun provideBackgroundLoginHandler(
        authenticationCoreManager: AuthenticationCoreManager,
        authenticationStorage: AuthenticationStorage
    ): BackgroundLoginHandler =
        BackgroundLoginHandlerImpl(
            authenticationCoreManager,
            authenticationStorage
        )

    @Provides
    fun provideKeyRestore(
        sphinxKeyRestore: SphinxKeyRestore
    ): KeyRestore =
        sphinxKeyRestore
}