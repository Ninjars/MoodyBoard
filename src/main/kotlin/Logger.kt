object Logger {
    var verboseEnabled = false

    fun logv(message: () -> String) {
        if (verboseEnabled) {
            log(message())
        }
    }

    fun log(message: String) {
        println(message)
    }
}
