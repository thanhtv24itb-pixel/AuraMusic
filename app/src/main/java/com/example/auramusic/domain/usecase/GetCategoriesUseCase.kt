package com.example.auramusic.domain.usecase



import com.example.auramusic.domain.model.Category
import com.example.auramusic.domain.repository.SongRepository

class GetCategoriesUseCase(private val songRepository: SongRepository) {
    suspend operator fun invoke(): Result<List<Category>> {
        return songRepository.getCategories()
    }
}