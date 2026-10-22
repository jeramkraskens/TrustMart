// Shared state and user actions for the buyer/seller app.
// Compose observes mutableStateOf values and redraws affected UI when those values change.
// Repository performs HTTP requests; this class turns their results into screen state and feedback.
package com.trustmart.app

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.trustmart.app.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Coordinates API actions and keeps form failures visible without discarding user input. */
class AppViewModel(app: Application): AndroidViewModel(app) {
    val session = SessionStore(app)
    var repo = Repository(app, session); private set
    var server by mutableStateOf(session.server); private set
    // Observable screen state: the UI reads these values, while this ViewModel controls updates.
    var user by mutableStateOf<User?>(null); private set
    var ready by mutableStateOf(false); private set
    var busy by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var notice by mutableStateOf<String?>(null); private set
    var categories by mutableStateOf<List<Category>>(emptyList()); private set
    var listings by mutableStateOf<List<Listing>>(emptyList()); private set
    var recommended by mutableStateOf<List<Listing>>(emptyList()); private set
    var detail by mutableStateOf<Listing?>(null); private set
    var reviews by mutableStateOf<List<Review>>(emptyList()); private set
    var merchantListings by mutableStateOf<List<Listing>>(emptyList()); private set
    var merchantReviews by mutableStateOf<List<Review>>(emptyList()); private set
    var wishlist by mutableStateOf<List<SavedListing>>(emptyList()); private set
    var mine by mutableStateOf<List<Listing>>(emptyList()); private set
    var verifications by mutableStateOf<List<Verification>>(emptyList()); private set
    var orders by mutableStateOf<List<Order>>(emptyList()); private set
    var disputes by mutableStateOf<List<Dispute>>(emptyList()); private set
    var myReviews by mutableStateOf<List<Review>>(emptyList()); private set
    var messages by mutableStateOf<List<Message>>(emptyList()); private set
    var notifications by mutableStateOf<List<Notice>>(emptyList()); private set
    var notificationAlert by mutableStateOf<Notice?>(null); private set
    var notificationStatus by mutableStateOf("Connecting…"); private set
    private var latestNotification: Int? = null
    val unreadNotifications: Int get() = notifications.count { !it.isRead }
    fun dismissNotificationAlert() { notificationAlert = null }
    var recovery by mutableStateOf(session.read("checkout")); private set
    var checkoutDestination by mutableStateOf<Int?>(null); private set
    var locations by mutableStateOf<Map<String, List<String>>>(emptyMap()); private set
    var advertisements by mutableStateOf<List<Advertisement>>(emptyList()); private set

    suspend fun loadLocations() {
        locations = repo.get("listings/locations/").asJsonObject.entrySet().associate { (name, cities) ->
            name to cities.asJsonArray.map { it.asString }
        }
    }
    suspend fun loadPromotions() {
        advertisements = repo.all("advertisements/", Advertisement::class.java, mapOf("active_only" to "true"))
    }

    // Restore the saved login when the ViewModel is created, then mark startup as finished.
    init { runAction {
        try { if (session.read("access") != null) user = me() }
        finally { ready = true }
    } }

    fun clearFeedback() { error = null; notice = null }
    fun inform(text: String) { notice = text }
    fun consumeCheckoutDestination() { checkoutDestination = null }
    private suspend fun afterWrite(message: String, done: () -> Unit = {}, refresh: suspend () -> Unit) {
        acknowledgeThenRefresh({ notice = message; done() }, refresh) { failure ->
            // A rejected session still needs the normal logout handling; the write acknowledgement is retained.
            if (failure is ApiFailure && failure.status == 401) throw failure
            notice = "$message The refresh did not finish. Use Refresh to get the latest data; do not submit again."
        }
    }
    private suspend fun me(): User {
        val result = repo.decode(repo.get("auth/me/"), User::class.java)
        if (result.role !in listOf("buyer", "seller")) session.expireTokens()
        require(result.role in listOf("buyer", "seller")) { "Use a buyer or seller account. Moderators have their own app." }
        return result
    }
    /** Serialize taps to prevent duplicate forms and purchases. Cancellation must propagate. */
    fun runAction(block: suspend () -> Unit) {
        if (busy) return
        busy = true; clearFeedback()
        viewModelScope.launch { executeAction(block) }
    }
    /** Screen loads belong to the screen lifecycle, so leaving cancels their HTTP calls. */
    suspend fun runScreenLoad(block: suspend () -> Unit) {
        while (busy) delay(100)
        busy = true; clearFeedback()
        executeAction(block)
    }
    private suspend fun executeAction(block: suspend () -> Unit) {
            val generation = session.generation()
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (session.generation() != generation && session.read("access") != null && user != null) return
                if (e is ApiFailure && e.status == 401) {
                    val pendingCheckout = recovery
                    clearSessionData()
                    recovery = pendingCheckout
                    session.expireTokens()
                }
                error = when (e) {
                    is java.net.ConnectException -> "Cannot connect. Check Server settings, Wi-Fi and Django."
                    is java.net.SocketTimeoutException -> "The server timed out. Refresh before retrying a submission."
                    else -> e.message ?: "Something went wrong. Please retry."
                }
            } finally { busy = false }
    }
    fun configure(url: String, done: () -> Unit) = runAction {
        val normalized = normalizedServer(url, BuildConfig.DEBUG)
        session.clear(); session.server = normalized; server = normalized
        repo = Repository(getApplication(), session)
        clearSessionData(); done()
    }
    private fun clearSessionData() {
        latestNotification = null; notificationAlert = null; notificationStatus = "Connecting…"
        user = null; recovery = null; checkoutDestination = null; listings = emptyList(); recommended = emptyList()
        categories = emptyList(); detail = null; reviews = emptyList(); wishlist = emptyList(); advertisements = emptyList(); locations = emptyMap()
        merchantListings = emptyList(); merchantReviews = emptyList()
        mine = emptyList(); verifications = emptyList(); orders = emptyList(); disputes = emptyList()
        myReviews = emptyList(); messages = emptyList(); notifications = emptyList()
    }
    fun logout() { session.clear(); clearSessionData(); clearFeedback() }
    fun login(username: String, password: String, done: () -> Unit) = runAction {
        val json = repo.post("auth/login/", mapOf("username" to username.trim(), "password" to password)).asJsonObject
        val account = repo.decode(json["user"], User::class.java)
        require(account.role in listOf("buyer", "seller")) { "Sign in with a buyer or seller account." }
        recovery?.let { pending ->
            val buyer = com.google.gson.JsonParser.parseString(pending).asJsonObject["buyer"].asInt
            require(account.id == buyer) { "Sign in with the buyer who started the pending checkout, then resolve it before switching accounts." }
        }
        session.start(json["access"].asString, json["refresh"].asString)
        val pendingCheckout = recovery
        clearSessionData(); recovery = pendingCheckout
        user = account; done()
    }
    fun register(fields: Map<String, String>, done: () -> Unit) = runAction {
        repo.post("auth/register/", fields); notice = "Account created. Sign in with your username."; done()
    }
    suspend fun loadHome(search: String = "", category: String = "", condition: String = "", ordering: String = "-created_at",
        district: String = "", city: String = "") {
        categories = repo.all("categories/", Category::class.java)
        loadLocations()
        try { loadPromotions() } catch (e: CancellationException) { throw e } catch (_: Exception) { advertisements = emptyList() }
        val filters = mutableMapOf("status" to "verified", "ordering" to ordering)
        if (search.isNotBlank()) filters["search"] = search.trim()
        if (category.isNotBlank()) filters["category"] = category
        if (condition.isNotBlank()) filters["condition"] = condition
        if (district.isNotBlank()) filters["district"] = district
        if (city.isNotBlank()) filters["city"] = city
        listings = repo.all("listings/", Listing::class.java, filters)
        if (user?.role == "buyer") {
            wishlist = repo.all("wishlist/", SavedListing::class.java)
            // Recommendations are optional; failure must not hide the marketplace.
            try {
                if (search.isNotBlank()) repo.post("recommendations/log-search/", mapOf("query" to search.trim()))
                recommended = repo.all("recommendations/", Listing::class.java)
            } catch (e: CancellationException) { throw e } catch (_: Exception) { recommended = emptyList() }
        }
    }
    suspend fun loadDetail(id: Int) {
        detail = null
        reviews = emptyList()
        detail = repo.decode(repo.get("listings/$id/"), Listing::class.java)
        reviews = repo.all("reviews/", Review::class.java, mapOf("reviewee" to detail!!.seller.toString()))
        if (user?.role == "buyer") {
            wishlist = repo.all("wishlist/", SavedListing::class.java)
            try { repo.post("recommendations/log-view/", mapOf("listing" to id)) }
            catch (e: CancellationException) { throw e } catch (_: Exception) { /* Analytics never blocks browsing. */ }
        }
    }
    fun toggleWishlist(id: Int) = runAction {
        val saved = repo.post("wishlist/toggle/", mapOf("listing" to id)).asJsonObject["wishlisted"].asBoolean
        afterWrite(if (saved) "Listing saved to your wishlist." else "Listing removed from your wishlist.") { loadWishlist() }
    }
    suspend fun loadWishlist() { wishlist = repo.all("wishlist/", SavedListing::class.java) }
    suspend fun loadSeller() {
        mine = repo.all("listings/", Listing::class.java, mapOf("mine" to "true"))
        verifications = repo.all("verifications/", Verification::class.java)
        categories = repo.all("categories/", Category::class.java)
        loadLocations()
    }
    /** The public API has no seller-directory endpoint; show only public listings and public reviews. */
    suspend fun loadMerchant(id: Int) {
        merchantListings = emptyList(); merchantReviews = emptyList()
        merchantListings = repo.all("listings/", Listing::class.java, mapOf("status" to "verified")).filter { it.seller == id }
        merchantReviews = repo.all("reviews/", Review::class.java, mapOf("reviewee" to id.toString()))
    }
    suspend fun loadSellerHome() {
        loadSeller(); loadOrders()
        merchantReviews = repo.all("reviews/", Review::class.java, mapOf("reviewee" to user!!.id.toString()))
    }
    /** Final website creates the listing, photos and moderation request atomically in one multipart POST. */
    fun createListing(fields: Map<String, String>, photos: List<Uri>, notes: String, evidence: Uri?,
        draftId: Int?, onDraft: (Int) -> Unit, done: () -> Unit) = runAction {
        require(user?.role == "seller") { "A seller account is required." }
        require(notes.isNotBlank() && evidence != null) { "Verification notes and evidence are required." }
        if (draftId == null) repo.decode(repo.upload("listings/", fields + ("evidence_notes" to notes),
            photos.map { "uploaded_images" to it } + listOf("evidence_file" to evidence)), Listing::class.java).id.also(onDraft)
        afterWrite("Listing submitted for moderator verification.", done) { loadSeller() }
    }
    fun editListing(id: Int, fields: Map<String, String>, photos: List<Uri>, done: () -> Unit) = runAction {
        val current = repo.decode(repo.get("listings/$id/"), Listing::class.java)
        require(current.seller == user?.id && current.canManage) { "This listing cannot be edited." }
        repo.upload("listings/$id/", fields, photos.map { "uploaded_images" to it }, patch = true)
        afterWrite("Changes submitted for moderator review.", done) { loadSeller() }
    }
    fun deleteListing(id: Int, done: () -> Unit) = runAction {
        val current = repo.decode(repo.get("listings/$id/"), Listing::class.java)
        require(current.seller == user?.id && current.canManage) { "This listing cannot be deleted." }
        repo.delete("listings/$id/")
        afterWrite("Listing deleted.", done) { loadSeller() }
    }
    fun submitDraft(id: Int) = runAction {
        val pending = repo.all("verifications/", Verification::class.java).any { it.listing == id && it.status == "pending" }
        if (!pending) repo.post("verifications/", mapOf("listing" to id, "notes" to "Submitted from TrustMart App."))
        afterWrite("Submitted for verification.") { loadSeller() }
    }
    suspend fun loadOrders() {
        orders = repo.all("transactions/", Order::class.java)
        disputes = repo.all("disputes/", Dispute::class.java)
        myReviews = repo.all("reviews/", Review::class.java).filter { it.reviewer == user?.id }
    }
    fun confirmDelivery(order: Order) = runAction {
        loadOrders()
        val fresh = orders.first { it.id == order.id }
        require(canConfirm(fresh, user!!, disputes.any { it.transaction == order.id })) { "This order is not eligible for delivery confirmation." }
        repo.post("transactions/${order.id}/confirm_delivery/")
        afterWrite("Delivery confirmed. Test payment released to the seller.") { loadOrders() }
    }
    fun dispute(order: Order, reason: String, done: () -> Unit) = runAction {
        require(reason.isNotBlank()) { "Describe the problem first." }
        loadOrders()
        val fresh = orders.firstOrNull { it.id == order.id } ?: error("Order is no longer available.")
        require(user?.let { canDispute(fresh, it, disputes) } == true) { "This order is not eligible for a new dispute." }
        val created = repo.decode(repo.post("disputes/", mapOf("transaction" to fresh.id, "reason" to reason.trim())), Dispute::class.java)
        disputes = (disputes + created).distinctBy { it.id }
        afterWrite("Dispute sent to a moderator.", done) { loadOrders() }
    }
    fun review(order: Order, rating: Int, comment: String, done: () -> Unit) = runAction {
        require(rating in 1..5) { "Choose a rating from 1 to 5." }
        loadOrders()
        val fresh = orders.firstOrNull { it.id == order.id } ?: error("Order is no longer available.")
        require(user?.let { canReview(fresh, it, myReviews) } == true) { "This order is not eligible for a new review." }
        val created = repo.decode(repo.post("reviews/", mapOf("transaction" to fresh.id, "reviewee" to fresh.sellerId,
            "rating" to rating, "comment" to comment.trim())), Review::class.java)
        myReviews = (myReviews + created).distinctBy { it.id }
        afterWrite("Review submitted.", done) { loadOrders() }
    }
    private val messagesMutex = Mutex()
    suspend fun loadMessages() = messagesMutex.withLock {
        val account = user?.id
        val result = readSession { repo.all("messages/", Message::class.java) }
        if (user?.id == account && account != null) messages = result
    }
    suspend fun observeMessages(status: (String) -> Unit) {
        observeLiveMessages(session.server, { session.read("access") }, { loadMessages() }, status)
    }
    fun send(receiver: Int, text: String, listing: Int?, done: () -> Unit) = runAction {
        require(user != null && receiver > 0 && receiver != user!!.id) { "Choose another user to message." }
        require(text.isNotBlank()) { "Type a message first." }
        val body = mutableMapOf<String, Any>("receiver" to receiver, "content" to text.trim())
        if (listing != null) body["listing"] = listing
        val created = repo.decode(repo.post("messages/", body), Message::class.java)
        messages = (messages + created).distinctBy { it.id }
        afterWrite("Message sent.", done) { loadMessages() }
    }
    private val notificationsMutex = Mutex()
    suspend fun loadNotifications() = notificationsMutex.withLock {
        val account = user?.id ?: return@withLock
        val rows = readSession { repo.all("notifications/", Notice::class.java) }
        if (user?.id != account) return@withLock
        latestNotification?.let { last -> rows.firstOrNull { it.id > last && !it.isRead }?.let { notificationAlert = it } }
        latestNotification = maxOf(latestNotification ?: 0, rows.maxOfOrNull { it.id } ?: 0)
        notifications = rows
    }
    suspend fun observeNotifications() {
        observeLiveMessages(session.server, { session.read("access") }, { loadNotifications() },
            { notificationStatus = it }, resource = "notifications")
    }
    private suspend fun <T> readSession(load: suspend () -> T): T {
        val account = user?.id
        val generation = session.generation()
        try { return load() }
        catch (failure: ApiFailure) {
            if (failure.status == 401 && user?.id == account &&
                (session.generation() == generation || session.read("access") == null)) {
                val pending = recovery
                clearSessionData(); recovery = pending
                session.expireTokens()
                error = "Your session expired. Please sign in again."
            }
            throw failure
        }
    }
    fun readNotification(id: Int) = runAction {
        repo.post("notifications/$id/mark_read/")
        notifications = notifications.map { if (it.id == id) it.copy(isRead = true) else it }
        loadNotifications()
    }
    fun readNotifications() = runAction {
        repo.post("notifications/mark_all_read/")
        notifications = notifications.map { it.copy(isRead = true) }
        afterWrite("Notifications marked as read.") { loadNotifications() }
    }
    fun profile(fields: Map<String, String>) = runAction {
        user = repo.decode(repo.patch("auth/me/", fields), User::class.java); notice = "Profile saved."
    }
    fun password(old: String, new: String, done: () -> Unit) = runAction {
        repo.post("auth/change-password/", passwordChangeFields(old, new))
        done(); notice = "Password changed."
    }

    /** Only clears the local legacy record; this does not cancel an authorization at Stripe. */
    fun cancelCard() { session.write("checkout", null); recovery = null; notice = "Local recovery record cleared. This does not cancel a Stripe authorization." }
    fun completeCard(done: () -> Unit) = runAction {
        val record = com.google.gson.JsonParser.parseString(recovery ?: error("No checkout to recover.")).asJsonObject
        require(record["buyer"].asInt == user?.id) { "Sign in with the account that started checkout." }
        val id = record["listing"].asInt
        // Reconcile a lost response before retrying the non-idempotent legacy endpoint.
        val existing = repo.all("transactions/", Order::class.java).firstOrNull { it.listing == id && it.buyer == user?.id }
        val recorded = existing ?: error("This backend no longer supports Stripe recovery. Ask your administrator to reconcile the old authorization before resetting it. No new order was created.")
        orders = (listOf(recorded) + orders).distinctBy { it.id }
        session.write("checkout", null); recovery = null
        checkoutDestination = recorded.id
        afterWrite("Test purchase recorded. Payment authorization is held until delivery confirmation.", done) { loadOrders() }
    }
    fun demoPurchase(item: Listing, method: String = "payhere", done: () -> Unit) = runAction {
        require(BuildConfig.DEBUG) { "Simulated checkout is available only in debug builds." }
        require(method in listOf("card", "payhere")) { "Choose Card or PayHere." }
        require(recovery == null) { "Resolve your previous card checkout first." }
        require(canBuy(repo.decode(repo.get("listings/${item.id}/"), Listing::class.java), user)) { "Item is no longer available." }
        val created = repo.decode(repo.post("transactions/", mapOf("listing" to item.id, "payment_method" to method)), Order::class.java)
        orders = (listOf(created) + orders).distinctBy { it.id }
        afterWrite("Demo purchase recorded. No real payment was collected.", done) { loadOrders() }
    }
}
