package de.meintraining.app

import android.app.Application
import de.meintraining.app.data.AppDatabase
import de.meintraining.app.data.MediaFiles
import de.meintraining.app.data.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class TrainingApp : Application() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val media by lazy { MediaFiles(this) }
    val repository by lazy { Repository(AppDatabase.create(this), media, scope) }
}
