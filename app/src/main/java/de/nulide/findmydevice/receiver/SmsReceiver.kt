package de.nulide.findmydevice.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import de.nulide.findmydevice.data.Settings
import de.nulide.findmydevice.data.SettingsRepository
import de.nulide.findmydevice.services.SmsService
import de.nulide.findmydevice.utils.log


class SmsReceiver : BroadcastReceiver() {

    companion object {
        private val TAG = SmsReceiver::class.simpleName
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            // wrong receiver
            return
        }
        val bundle = intent.extras ?: return
        val format = bundle.getString("format") ?: return
        val pdus = bundle.get("pdus") as Array<ByteArray>? ?: return
        val subscriptionId = intent.getIntExtra("subscription", -1)

        for (pdu in pdus) {
            val sms = SmsMessage.createFromPdu(pdu, format)

            val phoneNumber = sms.originatingAddress
            if (phoneNumber.isNullOrBlank()) {
                context.log().i(TAG, "Cannot handle SMS: phoneNumber is empty!")
                return
            }

            val msg = sms.messageBody
            if (msg.isNullOrBlank()) {
                context.log().i(TAG, "Cannot handle SMS: msg is empty!")
                return
            }

            // Early sanity check + abort
            val settings = SettingsRepository.getInstance(context)
            val fmdTriggerWord = (settings.get(Settings.SET_FMD_COMMAND) as String).lowercase()
            if (!msg.lowercase().startsWith(fmdTriggerWord)) {
                return
            }

            SmsService.scheduleJob(context, phoneNumber, subscriptionId, msg)
        }
    }
}
