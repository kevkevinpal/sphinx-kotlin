package chat.sphinx.activitymain

import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import androidx.navigation.findNavController
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.activitymain.databinding.ActivityMainBinding
import chat.sphinx.activitymain.navigation.MainNavigationDriver
import chat.sphinx.resources.R as R_common
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_activity.NavigationActivity
import io.matthewnelson.concept_navigation.NavigationRequest

@AndroidEntryPoint
class MainActivity: NavigationActivity<
        MainViewModel,
        MainNavigationDriver,
        MainViewModel,
        ActivityMainBinding,
        >(R.layout.activity_main)
{
    override val binding: ActivityMainBinding by viewBinding(ActivityMainBinding::bind)
    override val navController: NavController by lazy(LazyThreadSafetyMode.NONE) {
        binding.navHostFragment.findNavController()
    }
    override val viewModel: MainViewModel by viewModels()
    override val navigationViewModel: MainViewModel
        get() = viewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R_common.style.AppPostLaunchTheme)
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    override suspend fun onPostNavigationRequestExecution(request: NavigationRequest<NavController>) {
        super.onPostNavigationRequestExecution(request)
    }
}