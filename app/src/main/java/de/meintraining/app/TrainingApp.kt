package de.meintraining.app

import android.app.Application
import de.meintraining.app.data.AppDatabase
import de.meintraining.app.data.PhotoStore
import de.meintraining.app.data.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class TrainingApp : Application() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val photos by lazy { PhotoStore(this) }
    val repository by lazy { Repository(AppDatabase.create(this), photos, scope) }
}
