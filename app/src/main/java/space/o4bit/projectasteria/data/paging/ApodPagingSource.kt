package space.o4bit.projectasteria.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import space.o4bit.projectasteria.data.model.EnhancedAstronomyPicture
import space.o4bit.projectasteria.data.repository.SpaceRepository

class ApodPagingSource(
    private val repository: SpaceRepository
) : PagingSource<Int, EnhancedAstronomyPicture>() {

    override fun getRefreshKey(state: PagingState<Int, EnhancedAstronomyPicture>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, EnhancedAstronomyPicture> {
        val position = params.key ?: 1
        return try {
            val response = repository.getPagedHistory(page = position, pageSize = params.loadSize)

            LoadResult.Page(
                data = response,
                prevKey = if (position == 1) null else position - 1,
                nextKey = if (response.isEmpty()) null else position + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
