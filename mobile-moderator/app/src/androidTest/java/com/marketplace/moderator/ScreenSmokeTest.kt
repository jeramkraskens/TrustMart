package com.marketplace.moderator

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.marketplace.moderator.data.models.Notification
import com.marketplace.moderator.ui.login.LoginScreen
import com.marketplace.moderator.ui.login.LoginViewModel
import com.marketplace.moderator.ui.notifications.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*
import org.junit.runner.RunWith

/** Offline screen checks: no credentials, real API calls or marketplace writes. */
@RunWith(AndroidJUnit4::class)
class ScreenSmokeTest {
    @get:Rule val compose = createComposeRule()
    private val app: Application get() = ApplicationProvider.getApplicationContext()

    @Test fun loginRequiresCredentialsAndExposesServerSettings() {
        val vm = LoginViewModel(app)
        compose.setContent { MaterialTheme { LoginScreen(vm) {} } }
        compose.onNodeWithText("Sign In").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Server settings").performScrollTo().performClick()
        compose.onNodeWithText("Backend URL ending in /api/").performScrollTo().assertIsDisplayed()
    }

    @Test fun emptyNotificationsDoNotOfferMarkAllRead() {
        val vm = NotificationViewModel(app)
        compose.setContent { MaterialTheme { NotificationsScreen(vm) {} } }
        compose.onNodeWithText("Mark all as read").assertIsNotEnabled()
        compose.onNodeWithText("No notifications yet. Updates will appear automatically.").assertIsDisplayed()
    }

    @Test fun notificationShowsUnreadStatusAndOpensRelatedUpdate() {
        val vm = NotificationViewModel(app)
        val item = Notification(901, "new_message", "Offline test message", "/messages?with=5", false, "2026-10-20T08:00:00Z")
        val field = NotificationViewModel::class.java.getDeclaredField("state").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val state = field.get(vm) as MutableStateFlow<NotificationState>
        state.value = NotificationState(items = listOf(item), status = "Live")
        var opened: Int? = null
        compose.setContent { MaterialTheme { NotificationsScreen(vm) { opened = it.id } } }
        compose.onNodeWithText("1 unread · Live").assertIsDisplayed()
        compose.onNodeWithText("Unread").assertIsDisplayed()
        compose.onNodeWithText("View update").performScrollTo().performClick()
        Assert.assertEquals(901, opened)
    }
}
