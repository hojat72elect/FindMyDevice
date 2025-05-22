package de.nulide.findmydevice.commands

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import de.nulide.findmydevice.R
import de.nulide.findmydevice.permissions.DoNotDisturbAccessPermission
import de.nulide.findmydevice.permissions.OverlayPermission
import de.nulide.findmydevice.services.FmdJobService
import de.nulide.findmydevice.transports.Transport
import de.nulide.findmydevice.ui.RingerActivity
import kotlinx.coroutines.CoroutineScope


const val RING_DURATION_DEFAULT_SECS = 30
const val RING_DURATION_LONG_SECS = 3 * 60
const val RING_DURATION_MAX_SECS = 5 * 60

class RingCommand(context: Context) : Command(context) {

    override val keyword = "ring"
    override val usage = "ring [long]"

    @get:DrawableRes
    override val icon = R.drawable.ic_volume_up

    @get:StringRes
    override val shortDescription = R.string.cmd_ring_description_short

    override val longDescription = null

    // DNDAccess is required for changing the alarm mode and volume
    // TODO(#145): Implement this without needing the overlay permission
    override val requiredPermissions = listOf(DoNotDisturbAccessPermission(), OverlayPermission())

    override fun <T> executeInternal(
        args: List<String>,
        transport: Transport<T>,
        coroutineScope: CoroutineScope,
        job: FmdJobService?,
    ) {
        val firstArg = args.getOrElse(0) { "" }

        var duration = RING_DURATION_DEFAULT_SECS
        if (firstArg == "long") {
            duration = RING_DURATION_LONG_SECS
        } else if (firstArg.isNotEmpty()) {
            firstArg.toIntOrNull()?.let {
                duration = it
            }
        }
        RingerActivity.newInstance(context, duration)
        transport.send(context, context.getString(R.string.cmd_ring_response))
        job?.jobFinished()
    }
}
