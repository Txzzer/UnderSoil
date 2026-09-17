package com.example.undersoil

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.undersoil.data.GardenDatabase
import com.example.undersoil.data.GardenEntity
import com.example.undersoil.data.Plant
import com.example.undersoil.data.PlantVillageData
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var fabHome: FloatingActionButton
    private lateinit var etSearch: EditText
    private lateinit var rvSavedGardenPlants: RecyclerView
    private lateinit var adapter: SavedGardenAdapter

    private val savedPlantsList = mutableListOf<Plant>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_garden)

        initViews()
        setupBottomNavigation()
    }

    override fun onResume() {
        super.onResume()
        loadSavedPlants()
    }

    private fun initViews() {
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        fabHome = findViewById(R.id.fabHome)
        etSearch = findViewById(R.id.et_search)
        rvSavedGardenPlants = findViewById(R.id.rvSavedGardenPlants)

        etSearch.setOnClickListener {
            startActivity(Intent(this, ExplorePlantsActivity::class.java))
        }

        adapter = SavedGardenAdapter(
            savedPlantsList,
            onPlantClick = { plant ->
                val intent = Intent(this, PlantDetailsActivity::class.java)
                intent.putExtra("plant_id", plant.id)
                startActivity(intent)
            },
            onDeleteClick = { plant ->
                deletePlantFromGarden(plant)
            }
        )

        rvSavedGardenPlants.layoutManager = LinearLayoutManager(this)
        rvSavedGardenPlants.adapter = adapter
    }

    private fun loadSavedPlants() {
        val dao = GardenDatabase.getInstance(this).gardenDao()
        var savedEntities = dao.getAllSync()

        // Seed default plants if empty initially
        if (savedEntities.isEmpty()) {
            dao.insert(GardenEntity(1)) // Tomato
            dao.insert(GardenEntity(2)) // Potato
            savedEntities = dao.getAllSync()
        }

        savedPlantsList.clear()
        for (entity in savedEntities) {
            val plant = getPlantById(entity.plantId)
            if (plant != null) {
                savedPlantsList.add(plant)
            }
        }
        adapter.notifyDataSetChanged()
    }

    private fun deletePlantFromGarden(plant: Plant) {
        val dao = GardenDatabase.getInstance(this).gardenDao()
        dao.delete(GardenEntity(plant.id))
        Toast.makeText(this, "Removed ${plant.name} from My Garden", Toast.LENGTH_SHORT).show()
        loadSavedPlants()
    }

    private fun getPlantById(id: Int): Plant? {
        return PlantVillageData.catalog.find { it.id == id }
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.selectedItemId = R.id.nav_garden
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_iot -> {
                    startActivity(Intent(this, IoTScreen::class.java))
                    true
                }
                R.id.nav_garden -> {
                    true
                }
                else -> false
            }
        }

        fabHome.setOnClickListener {
            startActivity(Intent(this, HomeScreenActivity::class.java))
        }
    }
}

class SavedGardenAdapter(
    private val plants: List<Plant>,
    private val onPlantClick: (Plant) -> Unit,
    private val onDeleteClick: (Plant) -> Unit
) : RecyclerView.Adapter<SavedGardenAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvPlantName: TextView = itemView.findViewById(R.id.tvPlantName)
        val tvPlantStatus: TextView = itemView.findViewById(R.id.tvPlantStatus)
        val ivPlantPhoto: ImageView = itemView.findViewById(R.id.ivPlantPhoto)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_saved_garden_plant, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val plant = plants[position]
        holder.tvPlantName.text = plant.name
        holder.tvPlantStatus.text = "Healthy"
        holder.ivPlantPhoto.setImageResource(plant.imageResId)

        holder.itemView.setOnClickListener { onPlantClick(plant) }
        holder.btnDelete.setOnClickListener { onDeleteClick(plant) }
    }

    override fun getItemCount(): Int = plants.size
}
