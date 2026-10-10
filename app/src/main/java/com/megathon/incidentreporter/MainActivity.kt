package com.megathon.incidentreporter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import androidx.room.Room
import com.megathon.incidentreporter.ai.FakeAiEngine
import com.megathon.incidentreporter.contract.AIEngine
import com.megathon.incidentreporter.data.IncidentDatabase
import com.megathon.incidentreporter.data.ReportStore
import com.megathon.incidentreporter.ui.*
import com.megathon.incidentreporter.viewmodel.IncidentVmFactory
import com.megathon.incidentreporter.viewmodel.IncidentViewModel

class MainActivity : ComponentActivity() {

    // ===== SINGLE DI POINT (Module A ↔ Module B) =====
    // Demo: FakeAiEngine(). Production: replace with the Module-B adapter
    // implementing the same AIEngine interface. Nothing else changes.
    private fun engine(): AIEngine = FakeAiEngine()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = Room.databaseBuilder(applicationContext, IncidentDatabase::class.java, "incident_reports.db").build()
        val store = ReportStore(db.dao())
        setContent {
            MaterialTheme {
                val vm: IncidentViewModel = viewModel(factory = IncidentVmFactory(engine(), store, cacheDir))
                val nav = rememberNavController()
                NavHost(nav, startDestination = "start") {
                    composable("start") { StartScreen(vm, onNew = { nav.navigate("conversation") }, onHistory = { nav.navigate("history") }) }
                    composable("conversation") { ConversationScreen(vm, onReview = { vm.goReview(); nav.navigate("review") }) }
                    composable("review") { ReportReviewScreen(vm, onSubmitted = { }, onBack = { nav.navigate("start") { popUpTo(0) } }) }
                    composable("history") { ReportHistoryScreen(store, onBack = { nav.popBackStack() }) }
                }
            }
        }
    }
}
