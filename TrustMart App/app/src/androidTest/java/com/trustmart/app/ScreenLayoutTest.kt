package com.trustmart.app

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.trustmart.app.data.*
import com.trustmart.app.ui.*
import org.junit.*
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File

/** Offline layout fixtures only: no credentials, network, uploads or marketplace mutations. */
@RunWith(AndroidJUnit4::class)
class ScreenLayoutTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var vm: AppViewModel
    private val buyer = User(3, "kavindi", "", "buyer", "Kavindi", "Perera", "", 80)
    private val item = Listing(1, 2, "Nimal Karunaratne", 90, 1, "Phones", "iPhone 14 Pro – 256GB",
        "A test fixture used only for offline layout checks.", "145000", "like_new", "verified", "Colombo", emptyList())
    private val order = Order(1, 3, "Kavindi Perera", 1, item.title, 2, item.sellerName,
        "145000", "142100", "held", "card", false, "2026-09-26T10:00:00Z")
    @Before fun fixtures() {
        vm = AppViewModel(FixtureApplication(ApplicationProvider.getApplicationContext<Application>()))
        set("user", buyer); set("ready", true); set("busy", false)
        set("categories", listOf(Category(1, "Phones"), Category(2, "Laptops"), Category(3, "Vehicles"), Category(4, "Furniture")))
        set("listings", listOf(item, item.copy(id = 2, title = "MacBook Air M2", category = 2, categoryName = "Laptops", price = "215000")))
        set("recommended", emptyList<Listing>())
        set("locations", mapOf("Colombo" to listOf("Colombo", "Dehiwala")))
        set("detail", item); set("orders", listOf(order))
        set("mine", listOf(item)); set("merchantListings", listOf(item))
        set("merchantReviews", listOf(Review(1, 2, 3, "Kavindi", 5, "Exactly as described.")))
    }
    // Test-only injection keeps mock marketplace content entirely out of production code.
    @Suppress("UNCHECKED_CAST")
    private fun set(name: String, value: Any?) {
        val field = AppViewModel::class.java.getDeclaredField(name + "$" + "delegate")
        field.isAccessible = true
        (field.get(vm) as MutableState<Any?>).value = value
    }
    private fun render(name: String, content: @Composable () -> Unit) {
        compose.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                TrustMartTheme { Surface(Modifier.fillMaxSize(), color = Paper) { content() } }
            }
        }
        compose.waitForIdle()
        val file = File(vm.getApplication<Application>().filesDir, "ui-screenshots/" + name + ".png")
        file.parentFile?.mkdirs()
        file.outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
        Assert.assertTrue(file.length() > 1000)
    }
    @Test fun splash() {
        var opened = false
        render("01-splash") { WelcomeScreen({ opened = true }, {}, {}, {}) }
        compose.onNodeWithText("Get Started").performScrollTo().performClick()
        Assert.assertTrue(opened)
    }
    @Test fun login() { render("02-login") { LoginScreen(vm, {}, {}) }; compose.onNodeWithText("Sign In").assertIsNotEnabled() }
    @Test fun register() {
        render("03-register") { RegisterScreen(vm) {} }
        // Header and submit share a label; target the actionable node.
        compose.onNode(hasText("Create Account") and hasClickAction()).performScrollTo().assertIsDisplayed().assertIsNotEnabled()
    }
    @Test fun buyerHome() { render("05-buyer-home") { HomeScreen(vm, {}, {}, {}, {}, {}, {}) } }
    @Test fun sellerRegistrationShowsFinalWebsiteIdentityFields() {
        render("25-seller-registration") { RegisterScreen(vm) {} }
        compose.onNodeWithText("Sell").performScrollTo().performClick()
        compose.onNodeWithText("NIC number (digits)").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("NIC suffix (optional)").performScrollTo().assertIsDisplayed()
        compose.onNode(hasText("Create Account") and hasClickAction()).performScrollTo().assertIsNotEnabled()
    }
    @Test fun listingDeletionRequiresConfirmationAndCanBeCancelled() {
        set("user", buyer.copy(id = 2, role = "seller"))
        set("detail", item.copy(canManage = true, district = "Colombo", city = "Colombo", contactPhone = "0771234567"))
        render("26-edit-listing") { EditListingScreen(vm, 1) {} }
        compose.onNodeWithText("Delete listing").performScrollTo().performClick()
        compose.onNodeWithText("Delete listing?").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Delete listing?").assertDoesNotExist()
    }
    @Test fun categories() { render("06-categories") { CategoriesScreen(vm) {} } }
    @Test fun search() { render("07-search") { SearchScreen(vm, {}) } }
    @Test fun product() { render("08-product") { ListingScreen(vm, 1, {}, {}, {}, {}) } }
    @Test fun productWishlistReflectsSavedState() {
        render("27-product-wishlist") { ListingScreen(vm, 1, {}, {}, {}, {}) }
        compose.onNodeWithContentDescription("Save to wishlist").assertExists()
        compose.runOnIdle { set("wishlist", listOf(SavedListing(item.id, item))) }
        compose.onNodeWithContentDescription("Remove from wishlist").assertExists()
        compose.onNodeWithContentDescription("Save to wishlist").assertDoesNotExist()
    }
    @Test fun listingDraftSurvivesDetailReloadAfterPicker() {
        set("user", buyer.copy(id = 2, role = "seller"))
        val editable = item.copy(canManage = true, district = "Colombo", city = "Colombo", contactPhone = "0771234567")
        set("detail", editable)
        render("28-editor-refresh") { EditListingScreen(vm, 1) {} }
        compose.onNode(hasSetTextAction() and hasText(item.title)).performTextReplacement("My unsaved title")
        compose.runOnIdle { set("detail", null); set("busy", true) }
        compose.onNode(hasSetTextAction() and hasText("My unsaved title")).assertExists()
        compose.runOnIdle { set("detail", editable); set("busy", false) }
        compose.onNode(hasSetTextAction() and hasText("My unsaved title")).assertExists()
    }
    @Test fun productWithoutCachedDetailDoesNotCrash() {
        set("detail", null)
        render("22-product-missing") { ListingScreen(vm, 1, {}, {}, {}, {}) }
        compose.onNodeWithText("Unable to load this listing.").assertIsDisplayed()
        compose.onNodeWithText("Retry").assertIsDisplayed()
    }
    @Test fun productLoadingThenFailureDoesNotCrash() {
        set("detail", null); set("busy", true)
        render("23-product-loading") { ListingScreen(vm, 1, {}, {}, {}, {}) }
        compose.runOnIdle { set("busy", false) }
        compose.onNodeWithText("Unable to load this listing.").assertIsDisplayed()
        compose.runOnIdle { set("detail", item) }
        compose.onNodeWithText(item.title).assertExists()
        compose.onNodeWithText("Retry").assertDoesNotExist()
    }
    @Test fun openingDifferentProductDoesNotCrash() {
        // A cached product must not hide the empty state for a different product ID.
        render("24-product-different") { ListingScreen(vm, 2, {}, {}, {}, {}) }
        compose.onNodeWithText("Unable to load this listing.").assertIsDisplayed()
        compose.onNodeWithText(item.title).assertDoesNotExist()
    }
    @Test fun sellerProfile() { render("09-seller-profile") { SellerProfileScreen(vm, 2, {}, {}) } }
    @Test fun checkout() {
        var selected = ""
        render("10-checkout") { CheckoutScreen(vm, 1) { selected = it } }
        compose.onNodeWithText("Proceed to Escrow Payment").performClick()
        Assert.assertEquals("card", selected)
    }
    @Test fun escrow() { render("11-escrow") { EscrowScreen(vm, 1, "card") {} } }
    @Test fun orders() { render("12-orders") { OrdersScreen(vm, { _, _ -> }, {}, {}) } }
    @Test fun tracking() { render("13-tracking") { TrackingScreen(vm, 1, { _, _ -> }, {}) } }
    @Test fun dispute() { render("14-dispute") { DisputeScreen(vm, 1) {} } }
    @Test fun sellerHome() {
        set("user", buyer.copy(id = 2, role = "seller", firstName = "Nimal"))
        render("15-seller-home") { SellerHomeScreen(vm, {}, {}, {}, {}, {}, {}) }
    }
    @Test fun addListing() {
        set("user", buyer.copy(id = 2, role = "seller"))
        render("16-add-listing") { CreateScreen(vm) {} }
    }
    @Test fun guestNavigation() {
        set("user", null)
        render("00-navigation-welcome") { TrustMartApp(vm) }
        compose.onNodeWithText("Explore as a guest").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Search", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Search Results").assertIsDisplayed()
    }
    @Test fun sellerNavigation() {
        set("user", buyer.copy(id = 2, role = "seller", firstName = "Nimal"))
        render("17-seller-navigation") { TrustMartApp(vm) }
        compose.onNodeWithText("Listings").performClick()
        compose.onNodeWithText("My Listings").assertIsDisplayed()
        compose.onNodeWithText("Orders").performClick()
        compose.onNodeWithText("My Sales").assertIsDisplayed()
    }
    @Test fun acknowledgedCardCheckoutNavigatesToOrders() {
        set("checkoutDestination", 1)
        render("20-checkout-complete") { TrustMartApp(vm) }
        compose.onNodeWithText("My Orders").assertIsDisplayed()
        Assert.assertNull(vm.checkoutDestination)
    }
    @Test fun backCannotLeaveRouteDuringAnActiveAction() {
        render("21-back-guard") { TrustMartApp(vm) }
        compose.onNodeWithContentDescription("Search", useUnmergedTree = true).performClick()
        compose.runOnIdle { set("busy", true) }
        compose.waitForIdle()
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.onNodeWithText("Search Results").assertIsDisplayed()
        compose.runOnIdle { set("busy", false) }
        compose.waitForIdle()
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.onNodeWithText("Search Results").assertDoesNotExist()
    }
    @Test fun chatTypingSurvivesBackgroundRefresh() {
        render("18-chat") { CompositionLocalProvider(LocalBusy provides true) { ChatScreen(vm, 2, 1) } }
        compose.onNode(hasSetTextAction()).performTextInput("Keep my draft")
        compose.onNode(hasSetTextAction()).assertTextContains("Keep my draft")
        compose.onNodeWithText("Send").assertIsNotEnabled()
    }
    @Test fun deliveryRequiresExplicitConfirmation() {
        render("19-delivery-confirmation") { TrackingScreen(vm, 1, { _, _ -> }, {}) }
        compose.onNodeWithText("Confirm Delivery & Release Funds").performClick()
        compose.onNodeWithText("Confirm receipt?").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Confirm receipt?").assertDoesNotExist()
        Assert.assertFalse(vm.orders.first().deliveryConfirmed)
    }

    /** Separate preferences ensure tests never load the phone owner's credentials or checkout recovery. */
    private class FixtureApplication(base: Context) : Application() {
        init { attachBaseContext(base) }
        override fun getSharedPreferences(name: String, mode: Int) = super.getSharedPreferences("ui-fixture-" + name, mode)
    }
}
