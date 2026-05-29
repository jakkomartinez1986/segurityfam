package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.TrackingRepository
import com.example.ui.TrackingViewModel
import com.example.ui.TrackingViewModelFactory
import com.example.ui.screens.DashboardScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    
    private lateinit var database: AppDatabase
    private lateinit var repository: TrackingRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize local Room database with automatic migration fallback
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "fami_guard_tracking_db"
        )
        .fallbackToDestructiveMigration()
        .build()

        repository = TrackingRepository(database.trackingDao)

        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val factory = TrackingViewModelFactory(repository)
                    val viewModel: TrackingViewModel = viewModel(factory = factory)
                    
                    DashboardScreen(viewModel = viewModel)
                }
            }
        }
    }
}
