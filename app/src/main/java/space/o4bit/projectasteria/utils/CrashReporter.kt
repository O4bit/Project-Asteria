package space.o4bit.projectasteria.utils

object CrashReporter {
	fun log(message: String) {  }

	fun setUserId(userId: String) {  }

	fun setCustomKey(key: String, value: String) {  }
	fun setCustomKey(key: String, value: Number) {  }
	fun setCustomKey(key: String, value: Boolean) {  }

	fun recordException(throwable: Throwable) {  }

	fun simulateCrash(): Nothing = throw RuntimeException("Simulated crash from CrashReporter")
}
