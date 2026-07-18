package com.example.bulkmessenger

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.bulkmessenger.ui.home.HomeScreen
import com.example.bulkmessenger.ui.theme.BulkMessengerTheme
import com.example.bulkmessenger.viewmodel.SessionViewModel
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Basic structural smoke test: Home renders all four action cards and they're clickable. */
@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeScreen_showsAllFourActionCards_andBroadcastCardIsClickable() {
        var broadcastClicked = false
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
        val owner = object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
        val sessionViewModel = ViewModelProvider(
            owner,
            ViewModelProvider.AndroidViewModelFactory.getInstance(app)
        )[SessionViewModel::class.java]

        composeRule.setContent {
            BulkMessengerTheme {
                HomeScreen(
                    onNewBroadcast = { broadcastClicked = true },
                    onNewPersonalized = {},
                    onDrafts = {},
                    onJobHistory = {},
                    sessionViewModel = sessionViewModel
                )
            }
        }

        composeRule.onNodeWithText("New Broadcast").assertExists()
        composeRule.onNodeWithText("New Personalized Batch").assertExists()
        composeRule.onNodeWithText("Message Drafts").assertExists()
        composeRule.onNodeWithText("Job History").assertExists()

        composeRule.onNodeWithText("New Broadcast").performClick()
        assertTrue(broadcastClicked)
    }
}
