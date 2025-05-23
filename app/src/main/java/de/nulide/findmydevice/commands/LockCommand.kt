package de.nulide.findmydevice.commands

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import de.nulide.findmydevice.R
import de.nulide.findmydevice.data.Settings
import de.nulide.findmydevice.permissions.DeviceAdminPermission
import de.nulide.findmydevice.permissions.OverlayPermission
import de.nulide.findmydevice.services.FmdJobService
import de.nulide.findmydevice.transports.Transport
import de.nulide.findmydevice.ui.LockScreenMessage
import kotlinx.coroutines.CoroutineScope


class LockCommand(context: Context) : Command(context) {

    override val keyword = "lock"
    override val usage = "lock [msg]"

    @get:DrawableRes
    override val icon = R.drawable.ic_phone_lock

    @get:StringRes
    override val shortDescription = R.string.cmd_lock_description_short

    override val longDescription = R.string.cmd_lock_description_long

    override val requiredPermissions = listOf(DeviceAdminPermission())

    override val optionalPermissions = listOf(OverlayPermission())

    override fun <T> executeInternal(
        args: List<String>,
        transport: Transport<T>,
        coroutineScope: CoroutineScope,
        job: FmdJobService?,
    ) {
        val devicePolicyManager =
            context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        devicePolicyManager.lockNow()

        var customMessage = args.joinToString(" ")

        if (customMessage.isEmpty()) {
            customMessage = settings.get(Settings.SET_LOCKSCREEN_MESSAGE) as String
        }

        // Only show the full-screen activity if there is a message. This allows you to silently
        // lock your device (by not providing a message) without alerting the potential thief.
        if (customMessage.isNotEmpty() && OverlayPermission().isGranted(context)) {
            val lockScreenMessage = Intent(context, LockScreenMessage::class.java)
            lockScreenMessage.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // TODO: bring back passing this data??
            //lockScreenMessage.putExtra(LockScreenMessage.SENDER, transport.getDestinationString())
            //lockScreenMessage.putExtra(LockScreenMessage.SENDER_TYPE, ch.getSender().SENDER_TYPE)
            lockScreenMessage.putExtra(LockScreenMessage.CUSTOM_TEXT, customMessage)
            context.startActivity(lockScreenMessage)
        }

        transport.send(context, context.getString(R.string.cmd_lock_response))
        job?.jobFinished()
    }
}
