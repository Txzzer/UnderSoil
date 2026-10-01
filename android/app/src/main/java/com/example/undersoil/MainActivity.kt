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

import com.example.undersoil.data.SessionManager

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var fabHome: FloatingActionButton
    private lateinit var etSearch: EditText
    private lateinit var rvSavedGardenPlants: RecyclerView
    private lateinit var layoutEmptyGarden: View
    private lateinit var btnAddPlant: View
    private lateinit var backButton: ImageButton
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
        layoutEmptyGarden = findViewById(R.id.layoutEmptyGarden)
        btnAddPlant = findViewById(R.id.btnAddPlant)
        backButton = findViewById(R.id.backButton)

        backButton.setOnClickListener { finish() }

        val openExplore = View.OnClickListener {
            startActivity(Intent(this, ExplorePlantsActivity::class.java))
        }

        etSearch.setOnClickListener(openExplore)
        btnAddPlant.setOnClickListener(openExplore)

        adapter = SavedGardenAdapter(
            savedPlantsList,
            onPlantClick = { plant ->
                val intent = Intent(this, LogListActivity::class.java)
                intent.putExtra(LogListActivity.EXTRA_CROP_NAME, plant.name)
                intent.putExtra(LogListActivity.EXTRA_PLANT_ID, plant.id)
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
        val userId = SessionManager.getInstance(this).getUserId()
        val dao = GardenDatabase.getInstance(this).gardenDao()
        val savedEntities = dao.getAllSync(userId)

        savedPlantsList.clear()
        for (entity in savedEntities) {
            val plant = getPlantById(entity.plantId)
            if (plant != null) {
                savedPlantsList.add(plant)
            }
        }
        adapter.notifyDataSetChanged()

        if (savedPlantsList.isEmpty()) {
            layoutEmptyGarden.visibility = View.VISIBLE
            rvSavedGardenPlants.visibility = View.GONE
        } else {
            layoutEmptyGarden.visibility = View.GONE
            rvSavedGardenPlants.visibility = View.VISIBLE
        }
    }

    private fun deletePlantFromGarden(plant: Plant) {
        val userId = SessionManager.getInstance(this).getUserId()
        val dao = GardenDatabase.getInstance(this).gardenDao()
        dao.deleteByPlantAndUser(plant.id, userId)
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
                    startActivity(Intent(this, IotWateringActivity::class.java))
                    overridePendingTransition(0, 0)
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
            overridePendingTransition(0, 0)
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
