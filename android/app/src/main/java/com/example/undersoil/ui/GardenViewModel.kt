package com.example.undersoil.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.undersoil.data.GardenDao
import com.example.undersoil.data.GardenEntity
import com.example.undersoil.data.Plant
import com.example.undersoil.data.PlantVillageData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GardenViewModel(private val gardenDao: GardenDao) : ViewModel() {

    // Expose the raw catalog
    private val _catalog = MutableStateFlow(PlantVillageData.catalog)
    val catalog: StateFlow<List<Plant>> = _catalog.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Derived state for the Explore Plants screen (Filtered by search query)
    val filteredCatalog: StateFlow<List<Plant>> = combine(catalog, searchQuery) { plants, query ->
        if (query.isBlank()) {
            plants
        } else {
            plants.filter {
                it.name.contains(query, ignoreCase = true) || 
                it.scientificName.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlantVillageData.catalog)

    // Derived state for the "My Garden" screen
    // Maps Room DB entities to catalog plants
    val myGarden: StateFlow<List<Plant>> = gardenDao.getAll().map { gardenEntities ->
        val savedIds = gardenEntities.map { it.plantId }.toSet()
        PlantVillageData.catalog.filter { savedIds.contains(it.id) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addToGarden(plantId: Int) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            gardenDao.insert(GardenEntity(plantId = plantId))
        }
    }

    fun removeFromGarden(plantId: Int) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            gardenDao.delete(GardenEntity(plantId = plantId))
        }
    }
}
