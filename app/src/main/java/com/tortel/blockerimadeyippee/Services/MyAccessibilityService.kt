package Services

import android.accessibilityservice.AccessibilityService

import android.app.admin.DevicePolicyManager
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.tortel.blockerimadeyippee.MainActivity
import com.tortel.blockerimadeyippee.LockDownTime
import com.tortel.blockerimadeyippee.MainActivity.Companion.isLocked
import com.tortel.blockerimadeyippee.Reciever.MyDeviceAdminReceiver


class MyAccessibilityService : AccessibilityService() {

    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var componentName: ComponentName
    //TODO: ADD A CHECK TO REMOVE USB DEBUGGING SO I WONT BE ABLE TO EDIT ANYTHING ANYMORE
    private val badwords = mapOf(
        "vending" to listOf("tiktok", "pinterest", "tumblr", "reddit", "instagram", "social", "picsart", " x ", "twitter",
            "facebook", "meta", "media", "entertainment", "messaging"),
    )

    private val ignoredApps = listOf("whatsapp", "globallauncher", "inputmethod","systemui")


    companion object {
        var currentApp : String = ""
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            //Log.d("ACCESSIBILITY", "app control : ${MainActivity.appControl}")
            if (MainActivity.appControl == false) return
            val app = event.packageName?.toString()?: return
            val text = extractAllTextFromScreen().toString().lowercase()

            if (ignoredApps.any { app.contains(it) }) {
                return
            }

//            // Check if Instagram is open
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                if (getForegroundApp(this)!!.contains("vending")) {
                    Toast.makeText(this, "playstore is running!", Toast.LENGTH_SHORT).show()
                    Log.d("INSTAGRAM", "playstore is running!")
                }
            }

            //snapchat
            if (app.contains("snapchat")){
                val allwords = extractAllTextFromScreen().split(", ")
                val topSnapchatWord = if (allwords.size >= 2) allwords[allwords.size - 2] else null
                val topSnapchatWord2 = if (allwords.size >= 3) allwords[allwords.size - 3] else null
                Log.d("SNAPCHAT", "$topSnapchatWord $topSnapchatWord2")
                if (topSnapchatWord != null && !text.contains("send to")){
                    if (topSnapchatWord.contains("stories", ignoreCase = true) || topSnapchatWord.contains("spotlight", ignoreCase = true)){
                        startLockDown(LockDownTime, "snapchat scrolling")
                        return
                    }
                }
                if (topSnapchatWord2 != null && !text.contains("send to")){
                    if (topSnapchatWord2.contains("stories", ignoreCase = true) || topSnapchatWord2.contains("spotlight", ignoreCase = true)){
                        startLockDown(LockDownTime, "snapchat scrolling")
                        return
                    }
                }
                return // so that it doesnt block all these general keywords
            }

            //yt
            if (app.contains("youtube")){
                currentApp = "youtube"
                if (isOnShortsPage()){
                   startLockDown(LockDownTime, "shorts")
                }
                return
            }

            //play store
            if (app.contains("vending")){
                currentApp = "vending"
                badwords["vending"]?.forEach { word ->
                    if (text.contains(word)){
                        startLockDown(LockDownTime, word)
                        return
                    }
                }
            }
        }
    }

    private fun startLockDown(millisInFuture: Long, wordfound : String) {
        if (isLocked) return
//        isLocked = true //it is edited in MainActivity LockDown function
//        timeLeft = ""
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.putExtra("millisInFuture", millisInFuture)
        intent.putExtra("startlockdown", true)
        intent.putExtra("appnword", "$currentApp $wordfound")
        startActivity(intent)

//        resetAndStartTimer()

        Log.d("LOCKDOWN","sent to main to start LockDown, $wordfound")
    }


    override fun onInterrupt() {
        Log.d("MyAccessibilityService", "Service interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        componentName = ComponentName(this, MyDeviceAdminReceiver::class.java)

        val appIntent = Intent(this, MainActivity::class.java)
        appIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(appIntent)//open the app
    }

    private fun isOnShortsPage(): Boolean {
        val shortsButtonText = listOf("shorts")
        val allwords = extractAllTextFromScreen().split(", ")

        // Check for "subscribe" or "join" and if the word behind it contains "@"
        for (i in 1 until allwords.size) {
            if ((allwords[i].contains("Subscribe") || allwords[i].contains("Join"))) {
                if (allwords[i - 1].contains("@") || allwords[i - 2].contains("@")){
                    return true
                }
            }
        }

        return false
    }

    fun getForegroundApp(context: Context): String? {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            time - 1000 * 1000,
            time
        )

        stats?.let {
            if (it.isNotEmpty()) {
                it.sortByDescending { it.lastTimeUsed }
                return it[0].packageName
            }
        }
        return null
    }

    private fun isOnShortsPage(event: AccessibilityEvent?): Boolean {
        val shortsPageText = listOf("shorts", "youtube shorts")

        // Check event text for Shorts page keywords
        val eventText = event?.text?.joinToString(" ")?.lowercase()
        Log.d("SHORTSBUTTON", "$eventText")
        return eventText?.let { text ->
            shortsPageText.any { keyword -> text.contains(keyword) }
        } ?: false
    }

    // Extract all text from the screen
    private fun extractAllTextFromScreen(): StringBuilder {
        val rootNode: AccessibilityNodeInfo? = rootInActiveWindow
        if (rootNode != null) {
            val allText = StringBuilder()
            traverseNodeHierarchy(rootNode, allText)
            return allText
        } else {
            return StringBuilder()
        }
    }

    // Recursively traverse the node hierarchy and extract text
    private fun traverseNodeHierarchy(node: AccessibilityNodeInfo, allText: StringBuilder) {
        val text = node.text
        if (text != null && text.isNotEmpty()) {
            allText.append(text).append(", ")
        }

        for (i in 0 until node.childCount) {
            val childNode = node.getChild(i)
            if (childNode != null) {
                traverseNodeHierarchy(childNode, allText)
            }
        }
    }

}