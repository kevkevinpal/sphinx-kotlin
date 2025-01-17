package chat.sphinx.onboard_ready.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.insetter_activity.addStatusBarPadding
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.onboard_ready.R
import chat.sphinx.onboard_ready.databinding.FragmentOnBoardReadyBinding
import chat.sphinx.resources.SphinxToastUtils
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.navigation.CloseAppOnBackPress
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.android_feature_viewmodel.updateViewState
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive

@AndroidEntryPoint
internal class OnBoardReadyFragment: SideEffectFragment<
        Context,
        OnBoardReadySideEffect,
        OnBoardReadyViewState,
        OnBoardReadyViewModel,
        FragmentOnBoardReadyBinding
        >(R.layout.fragment_on_board_ready)
{
    override val viewModel: OnBoardReadyViewModel by viewModels()
    override val binding: FragmentOnBoardReadyBinding by viewBinding(FragmentOnBoardReadyBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupHeaderAndFooter()

        CloseAppOnBackPress(view.context)
            .enableDoubleTapToClose(viewLifecycleOwner, SphinxToastUtils())
            .addCallback(viewLifecycleOwner, requireActivity())

        binding.balanceTextView.text = getString(R.string.sphinx_ready_loading_balance)

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.getBalances().collect { loadResponse ->
                @Exhaustive
                when (loadResponse) {
                    LoadResponse.Loading -> {}
                    is Response.Error -> {
                        viewModel.updateViewState(OnBoardReadyViewState.Error)
                        viewModel.submitSideEffect(OnBoardReadySideEffect.CreateInviterFailed)
                    }
                    is Response.Success -> {
                        val balance = loadResponse.value
                        binding.balanceTextView.text = String.format(getString(R.string.sphinx_ready_balance_label), balance.localBalance.value, balance.remoteBalance.value)
                    }
                }
            }
        }

        binding.buttonContinue.setOnClickListener {
            viewModel.updateViewState(OnBoardReadyViewState.Saving)

            context?.getSharedPreferences("sphinx_temp_prefs", Context.MODE_PRIVATE)?.let { sharedPrefs ->
                val nickname = sharedPrefs.getString("sphinx_temp_inviter_nickname", null)
                val pubkey = sharedPrefs.getString("sphinx_temp_inviter_pubkey", null)
                val routeHint = sharedPrefs.getString("sphinx_temp_inviter_route_hint", null)
                val inviteString = sharedPrefs.getString("sphinx_temp_invite_string", null)

                if (nickname != null && pubkey != null) {
                    viewModel.saveInviterAndFinish(nickname, pubkey, routeHint, inviteString)
                } else if (inviteString != null){
                    viewModel.finishInvite(inviteString)
                } else {
                    viewModel.loadAndJoinDefaultTribeData()
                }
            }
        }
    }

    private fun setupHeaderAndFooter() {
        (requireActivity() as InsetterActivity)
            .addStatusBarPadding(binding.layoutConstraintOnBoardReady)
            .addNavigationBarPadding(binding.layoutConstraintOnBoardReady)
    }

    override suspend fun onSideEffectCollect(sideEffect: OnBoardReadySideEffect) {
        // TODO("Not yet implemented")
    }

    override suspend fun onViewStateFlowCollect(viewState: OnBoardReadyViewState) {
        @Exhaustive
        when (viewState) {
            is OnBoardReadyViewState.Idle -> {}
            is OnBoardReadyViewState.Saving -> {
                binding.onboardFinishProgress.visible
            }
            is OnBoardReadyViewState.Error -> {
                binding.onboardFinishProgress.gone
            }
        }
    }
}
