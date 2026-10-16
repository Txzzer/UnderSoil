package com.example.undersoil.garden

import android.content.Intent
import android.content.res.ColorStateList
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
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.undersoil.R
import com.example.undersoil.data.SessionManager
import com.example.undersoil.data.db.GardenDatabase
import com.example.undersoil.data.db.ScanLogDatabaseHelper
import com.example.undersoil.data.model.GrowingCondition
import com.example.undersoil.data.model.Plant
import com.example.undersoil.data.model.PlantVillageData
import com.example.undersoil.data.model.ScanLog
import com.example.undersoil.home.HomeScreenActivity
import com.example.undersoil.iot.IotWateringActivity
import com.example.undersoil.scan.LogListActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class GardenActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var fabHome: FloatingActionButton
    private lateinit var btnSearch: ImageButton
    private lateinit var rvSavedGardenPlants: RecyclerView
    private lateinit var layoutEmptyGarden: View
    private lateinit var btnAddPlant: View
    private lateinit var backButton: ImageButton
    private lateinit var adapter: SavedGardenAdapter

    private val savedPlantsList = mutableListOf<Plant>()
    private val latestScans = mutableMapOf<Int, ScanLog>()

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
        btnSearch = findViewById(R.id.btnSearch)
        rvSavedGardenPlants = findViewById(R.id.rvSavedGardenPlants)
        layoutEmptyGarden = findViewById(R.id.layoutEmptyGarden)
        btnAddPlant = findViewById(R.id.btnAddPlant)
        backButton = findViewById(R.id.backButton)

        backButton.setOnClickListener { finish() }

        val openExplore = View.OnClickListener {
            startActivity(Intent(this, ExplorePlantsActivity::class.java))
        }

        btnSearch.setOnClickListener(openExplore)
        btnAddPlant.setOnClickListener(openExplore)

        adapter = SavedGardenAdapter(
            savedPlantsList,
            latestScans,
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
        try {
            val userId = SessionManager.getInstance(this).getUserIdString()
            val dao = GardenDatabase.getInstance(this).gardenDao()
            val savedEntities = dao.getAllSync(userId)

            val scanLogDb = ScanLogDatabaseHelper.getInstance(this)

            savedPlantsList.clear()
            latestScans.clear()
            for (entity in savedEntities) {
                val plant = getPlantById(entity.plantId)
                if (plant != null) {
                    savedPlantsList.add(plant)
                    scanLogDb.getLatestLogByCropName(plant.name, userId)?.let { latestScans[plant.id] = it }
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
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error loading garden: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deletePlantFromGarden(plant: Plant) {
        try {
            val userId = SessionManager.getInstance(this).getUserIdString()
            val dao = GardenDatabase.getInstance(this).gardenDao()
            dao.deleteByPlantAndUser(plant.id, userId)
            Toast.makeText(this, "Removed ${plant.name} from My Garden", Toast.LENGTH_SHORT).show()
            loadSavedPlants()
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
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
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }
}

class SavedGardenAdapter(
    private val plants: List<Plant>,
    private val latestScans: Map<Int, ScanLog>,
    private val onPlantClick: (Plant) -> Unit,
    private val onDeleteClick: (Plant) -> Unit
) : RecyclerView.Adapter<SavedGardenAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvPlantName: TextView = itemView.findViewById(R.id.tvPlantName)
        val tvPlantStatus: TextView = itemView.findViewById(R.id.tvPlantStatus)
        val ivPlantPhoto: ImageView = itemView.findViewById(R.id.ivPlantPhoto)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
        val ivConditionPrimary: ImageView = itemView.findViewById(R.id.ivConditionPrimary)
        val ivConditionSecondary: ImageView = itemView.findViewById(R.id.ivConditionSecondary)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_saved_garden_plant, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val plant = plants[position]
        holder.tvPlantName.text = plant.name
        bindStatus(holder.tvPlantStatus, latestScans[plant.id])
        holder.ivPlantPhoto.setImageResource(plant.imageResId)
        bindCondition(holder.ivConditionPrimary, plant.favouredConditions.getOrNull(0))
        bindCondition(holder.ivConditionSecondary, plant.favouredConditions.getOrNull(1))

        holder.itemView.setOnClickListener { onPlantClick(plant) }
        holder.btnDelete.setOnClickListener { onDeleteClick(plant) }
    }

    override fun getItemCount(): Int = plants.size

    private fun bindCondition(iconView: ImageView, condition: GrowingCondition?) {
        if (condition == null) {
            iconView.visibility = View.GONE
            return
        }
        iconView.visibility = View.VISIBLE
        iconView.setImageResource(condition.iconResId)
        iconView.setBackgroundResource(condition.backgroundResId)
        iconView.imageTintList =
            ColorStateList.valueOf(ContextCompat.getColor(iconView.context, condition.tintResId))
        iconView.contentDescription = condition.label
    }

    /** Shows the condition from the plant's most recent scan; unscanned plants keep the default "Healthy". */
    private fun bindStatus(statusView: TextView, latestScan: ScanLog?) {
        // Diagnosis is stored as "<Plant> • <Condition>"
        val condition = latestScan?.diagnosis?.substringAfter("•")?.trim()?.takeIf { it.isNotEmpty() }
        val isHealthy = condition == null || condition.contains("healthy", ignoreCase = true)
        val context = statusView.context

        statusView.text = condition ?: "Healthy"
        if (isHealthy) {
            statusView.setTextColor(ContextCompat.getColor(context, R.color.us_bio_green))
            statusView.backgroundTintList = null
        } else {
            statusView.setTextColor(ContextCompat.getColor(context, R.color.us_danger))
            statusView.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(context, R.color.us_danger_container))
        }
    }
}
