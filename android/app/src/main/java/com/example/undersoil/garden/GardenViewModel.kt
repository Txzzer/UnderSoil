package com.example.undersoil.garden

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.undersoil.data.db.GardenDao
import com.example.undersoil.data.db.GardenEntity
import com.example.undersoil.data.model.Plant
import com.example.undersoil.data.model.PlantVillageData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GardenViewModel(private val gardenDao: GardenDao, private val userId: String = "") : ViewModel() {

    private val _catalog = MutableStateFlow(PlantVillageData.catalog)
    val catalog: StateFlow<List<Plant>> = _catalog.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

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

    // Garden rows only store plant ids; look the full plant up in the catalog
    val myGarden: StateFlow<List<Plant>> = gardenDao.getAll(userId).map { gardenEntities ->
        val savedIds = gardenEntities.map { it.plantId }.toSet()
        PlantVillageData.catalog.filter { savedIds.contains(it.id) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addToGarden(plantId: Int) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            gardenDao.insert(GardenEntity(plantId = plantId, userId = userId))
        }
    }

    fun removeFromGarden(plantId: Int) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            gardenDao.deleteByPlantAndUser(plantId = plantId, userId = userId)
        }
    }
}
