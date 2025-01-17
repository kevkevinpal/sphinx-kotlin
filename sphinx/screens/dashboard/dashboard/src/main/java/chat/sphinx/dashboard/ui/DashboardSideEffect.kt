package chat.sphinx.dashboard.ui

import android.app.AlertDialog
import android.content.Context
import chat.sphinx.dashboard.R
import chat.sphinx.resources.SphinxToastUtils
import io.matthewnelson.android_feature_toast_utils.show
import io.matthewnelson.concept_views.sideeffect.SideEffect

sealed class DashboardSideEffect: SideEffect<Context>() {

    class Notify(
        private val msg: String,
        private val notificationLengthLong: Boolean = true
    ): DashboardSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils(toastLengthLong = notificationLengthLong).show(value, msg)
        }
    }

    class AlertConfirmPayInvite(
        private val amount: Long,
        private val callback: () -> Unit
    ): DashboardSideEffect() {
        override suspend fun execute(value: Context) {
            val successMessage = String.format(value.getString(R.string.alert_confirm_pay_invite_message), amount)

            val builder = AlertDialog.Builder(value)
            builder.setTitle(value.getString(R.string.alert_confirm_pay_invite_title))
            builder.setMessage(successMessage)
            builder.setNegativeButton(android.R.string.cancel) { _,_ -> }
            builder.setPositiveButton(android.R.string.ok) { _, _ ->
                callback()
            }
            builder.show()
        }
    }

    class AlertConfirmDeleteInvite(
        private val callback: () -> Unit
    ): DashboardSideEffect() {
        override suspend fun execute(value: Context) {
            val builder = AlertDialog.Builder(value)
            builder.setTitle(value.getString(R.string.alert_confirm_delete_invite_title))
            builder.setMessage(value.getString(R.string.alert_confirm_delete_invite_message))
            builder.setNegativeButton(android.R.string.cancel) { _,_ -> }
            builder.setPositiveButton(android.R.string.ok) { _, _ ->
                callback()
            }
            builder.show()
        }
    }

}