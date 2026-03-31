package space.o4bit.projectasteria.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import space.o4bit.projectasteria.data.api.IssService
import space.o4bit.projectasteria.data.model.iss.IssPosition

class IssRepository(
    private val issService: IssService = IssService.create()
) {
    suspend fun getIssPosition(): IssPosition = withContext(Dispatchers.IO) {
        issService.getSatellitePosition()
    }
}
