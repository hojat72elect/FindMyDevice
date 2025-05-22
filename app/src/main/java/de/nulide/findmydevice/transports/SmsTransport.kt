package de.nulide.findmydevice.transports

import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsManager
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import de.nulide.findmydevice.R
import de.nulide.findmydevice.commands.ParserResult
import de.nulide.findmydevice.data.AllowlistRepository
import de.nulide.findmydevice.data.Settings
import de.nulide.findmydevice.data.SettingsRepository
import de.nulide.findmydevice.data.TEMP_USAGE_VALIDITY_MILLIS
import de.nulide.findmydevice.data.TemporaryAllowlistRepository
import de.nulide.findmydevice.permissions.SmsPermission
import de.nulide.findmydevice.services.TempContactExpiredService
import de.nulide.findmydevice.ui.settings.AllowlistActivity
import de.nulide.findmydevice.utils.Notifications
import de.nulide.findmydevice.utils.log


class SmsTransport(
    private val context: Context,
    private val phoneNumber: String,
    private val subscriptionId: Int
) : Transport<String>(phoneNumber) {

    companion object {
        private val TAG = SmsTransport::class.simpleName
    }

    @get:DrawableRes
    override val icon = R.drawable.ic_sms

    @get:StringRes
    override val title = R.string.transport_sms_title

    @get:StringRes
    override val description = R.string.transport_sms_description

    @get:StringRes
    override val descriptionAuth = R.string.transport_sms_description_auth

    @get:StringRes
    override val descriptionNote = R.string.transport_sms_description_note

    override val requiredPermissions = listOf(SmsPermission())

    override val actions = listOf(TransportAction(R.string.Settings_WhiteList) { activity ->
        activity.startActivity(Intent(context, AllowlistActivity::class.java))
    })

    private val settings = SettingsRepository.getInstance(context)
    private val allowlistRepo = AllowlistRepository.getInstance(context)
    private val tempAllowlistRepo = TemporaryAllowlistRepository.getInstance(context)

    override fun getDestinationString() = phoneNumber

    override fun isAllowed(parsed: ParserResult.Success): Boolean {
        // Case 1: phone number in Allowed Contacts
        if (allowlistRepo.containsNumber(phoneNumber)) {
            context.log().i(TAG, "$phoneNumber used FMD via allowlist")
            return true
        }

        // Case 2: phone number in temporary allowlist (i.e., it send the correct PIN earlier)
        val pinAccessEnabled = settings.get(Settings.SET_ACCESS_VIA_PIN) as Boolean
        if (pinAccessEnabled) {
            if (tempAllowlistRepo.containsValidNumber(phoneNumber)) {
                context.log().i(TAG, "$phoneNumber used FMD via temporary allowlist")
                return true
            }

            // Case 3: the message contains the correct PIN
            if (parsed.pin != null) {
                context.log().i(TAG, "$phoneNumber used FMD via PIN")
                send(context, context.getString(R.string.MH_Pin_Accepted))
                Notifications.notify(
                    context,
                    context.getString(R.string.usage_notification_pin_title),
                    context.getString(R.string.usage_notification_pin_text, phoneNumber),
                    Notifications.CHANNEL_PIN
                )

                tempAllowlistRepo.add(phoneNumber, subscriptionId)
                TempContactExpiredService.scheduleJob(context, TEMP_USAGE_VALIDITY_MILLIS + 1000)

                return true
            }
        }

        // Not allowed
        return false
    }

    override fun send(context: Context, msg: String) {
        super.send(context, msg)

        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val defaultSmsManager = context.getSystemService(SmsManager::class.java)
            if (subscriptionId == -1) {
                defaultSmsManager
            } else {
                defaultSmsManager.createForSubscriptionId(subscriptionId)
            }
        } else {
            if (subscriptionId == -1) {
                SmsManager.getDefault()
            } else {
                SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
            }
        }

        if (msg.length <= 160) {
            smsManager.sendTextMessage(phoneNumber, null, msg, null, null)
        } else {
            val parts = smsManager.divideMessage(msg)
            smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
        }
    }
}