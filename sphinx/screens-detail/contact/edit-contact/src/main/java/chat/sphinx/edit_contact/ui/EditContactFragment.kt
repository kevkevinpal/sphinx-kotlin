package chat.sphinx.edit_contact.ui

import android.widget.ImageView
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_user_colors_helper.UserColorsHelper
import chat.sphinx.contact.databinding.LayoutContactBinding
import chat.sphinx.contact.ui.ContactFragment
import chat.sphinx.detail_resources.databinding.LayoutDetailScreenHeaderBinding
import chat.sphinx.edit_contact.R
import chat.sphinx.edit_contact.databinding.FragmentEditContactBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
internal class EditContactFragment : ContactFragment<
        FragmentEditContactBinding,
        EditContactFragmentArgs,
        EditContactViewModel
        >(R.layout.fragment_edit_contact)
{

    override val viewModel: EditContactViewModel by viewModels()
    override val binding: FragmentEditContactBinding by viewBinding(FragmentEditContactBinding::bind)

    override val headerBinding: LayoutDetailScreenHeaderBinding by viewBinding(
        LayoutDetailScreenHeaderBinding::bind, R.id.include_edit_contact_header
    )
    override val contactBinding: LayoutContactBinding by viewBinding(
        LayoutContactBinding::bind, R.id.include_edit_contact_layout
    )


    @Inject
    lateinit var imageLoaderInj: ImageLoader<ImageView>

    override val imageLoader: ImageLoader<ImageView>
        get() = imageLoaderInj

    @Inject
    lateinit var userColorsHelperInj: UserColorsHelper

    override val userColorsHelper: UserColorsHelper
        get() = userColorsHelperInj

    override fun getHeaderText(): String = getString(R.string.edit_contact_header_name)

    override fun getSaveButtonText(): String = getString(R.string.save_contact_button)
}
