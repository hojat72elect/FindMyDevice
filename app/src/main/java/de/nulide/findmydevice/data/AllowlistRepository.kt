package de.nulide.findmydevice.data

import android.content.Context
import android.telephony.PhoneNumberUtils
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.stream.JsonReader
import de.nulide.findmydevice.R
import de.nulide.findmydevice.utils.Notifications
import de.nulide.findmydevice.utils.Notifications.CHANNEL_FAILED
import de.nulide.findmydevice.utils.SingletonHolder
import de.nulide.findmydevice.utils.log
import java.io.File
import java.io.FileReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.LinkedList


const val ALLOWLIST_FILENAME = "whitelist.json"


class AllowlistModel : LinkedList<Contact>()


class AllowlistRepository private constructor(private val context: Context) {

    companion object : SingletonHolder<AllowlistRepository, Context>(::AllowlistRepository) {
        val TAG = AllowlistRepository::class.simpleName
    }

    private val gson = Gson()

    var list: AllowlistModel
        private set

    init {
        val file = File(context.filesDir, ALLOWLIST_FILENAME)
        if (!file.exists()) {
            file.createNewFile()
        }
        val reader = JsonReader(FileReader(file))
        list = try {
            gson.fromJson(reader, AllowlistModel::class.java) ?: AllowlistModel()
        } catch (e: JsonSyntaxException) {
            context.log().e(TAG, e.stackTraceToString())
            // Reset the list
            notifyAllowlistReset()
            AllowlistModel()
        }
    }

    private fun saveList() {
        val copiedList = list.clone()
        val raw = gson.toJson(copiedList)
        val file = File(context.filesDir, ALLOWLIST_FILENAME)
        file.writeText(raw)
    }

    fun importFromStream(inputStream: InputStream) {
        val reader = JsonReader(InputStreamReader(inputStream))
        list = gson.fromJson(reader, AllowlistModel::class.java) ?: AllowlistModel()
        saveList()
    }

    fun contains(c: Contact): Boolean {
        return containsNumber(c.number)
    }

    fun containsNumber(number: String): Boolean {
        for (ele in list) {
            if (PhoneNumberUtils.compare(ele.number, number)) {
                return true
            }
        }
        return false
    }

    fun add(c: Contact) {
        if (!contains(c)) {
            list.add(c)
            saveList()
        }
    }

    fun remove(phoneNumber: String) {
        val toRemove = mutableListOf<Contact>()
        for (ele in list) {
            if (PhoneNumberUtils.compare(ele.number, phoneNumber)) {
                toRemove.add(ele)
            }
        }
        list.removeAll(toRemove)
        saveList()
    }

    private fun notifyAllowlistReset() {
        val title = context.getString(R.string.allowlist_reset_title)
        val text = context.getString(R.string.allowlist_reset_text)
        Notifications.notify(context, title, text, CHANNEL_FAILED)
    }
}
