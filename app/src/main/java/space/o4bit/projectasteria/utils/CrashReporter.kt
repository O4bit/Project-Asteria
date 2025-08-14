package space.o4bit.projectasteria.utils

/**
 * Lightweight no-op crash reporter used for local builds and tests.
 * This avoids compilation errors when referenced from utilities or debug screens.
 */
object CrashReporter {
	fun log(message: String) { /* no-op */ }

	fun setUserId(userId: String) { /* no-op */ }

	fun setCustomKey(key: String, value: String) { /* no-op */ }
	fun setCustomKey(key: String, value: Number) { /* no-op */ }
	fun setCustomKey(key: String, value: Boolean) { /* no-op */ }

	fun recordException(throwable: Throwable) { /* no-op */ }

	fun simulateCrash(): Nothing = throw RuntimeException("Simulated crash from CrashReporter")
}
