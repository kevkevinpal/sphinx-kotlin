package chat.sphinx.new_contact.ui

import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import javax.inject.Inject

@HiltViewModel
internal class NewContactViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers
): BaseViewModel<NewContactViewState>(dispatchers, NewContactViewState.Idle)
{
}