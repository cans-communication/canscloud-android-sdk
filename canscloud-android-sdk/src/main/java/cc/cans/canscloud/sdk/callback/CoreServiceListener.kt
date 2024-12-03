package cc.cans.canscloud.sdk.callback

interface CoreServiceListener {
    fun onCreate()
    fun onStartCommand()
    fun showForegroundServiceNotification()
    fun hideForegroundServiceNotification()
    fun onTaskRemoved()
    fun onDestroy()
}