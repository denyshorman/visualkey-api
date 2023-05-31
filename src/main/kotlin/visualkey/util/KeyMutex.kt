package visualkey.util

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class KeyMutex<T> {
    private val generalLock = Mutex()
    private val locks = hashMapOf<T, CountedMutex>()

    suspend fun <R> withLock(key: T, action: suspend () -> R): R {
        val lock = withContext(NonCancellable) {
            generalLock.withLock {
                val lock = locks.getOrPut(key) { CountedMutex() }
                lock.count++
                lock
            }
        }

        try {
            return lock.mutex.withLock {
                action()
            }
        } finally {
            withContext(NonCancellable) {
                generalLock.withLock {
                    if (--lock.count == 0L) {
                        locks.remove(key)
                    }
                }
            }
        }
    }

    private data class CountedMutex(
        var count: Long = 0,
        val mutex: Mutex = Mutex(),
    )
}
