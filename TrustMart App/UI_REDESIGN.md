# Screenshot-based UI redesign — 26 September 2026

The 15 supplied PNGs are the visual reference for this native Kotlin/Compose update.
The Figma connector quota prevented design-context access; the user approved using
the screenshots instead. This is an adaptive recreation, not a claim of pixel-perfect
Figma measurements. Native vector icons replace the mockups' emoji/placeholder icons.
Product photos and account values come from the existing Django API.

## What changed

- Shared teal buyer / orange seller palette, gradient headers, white rounded cards,
  softer background, reusable fields, pills, buttons, avatars and bottom action bars.
- Welcome, login and registration layouts with responsive scrolling and password visibility.
- Buyer home, categories, filtered search, product details and public seller profile.
- Separate checkout review and test-payment screens.
- Order list, real-status tracking and dedicated dispute form.
- Seller home dashboard, listing management and three-step listing submission.
- Five role-aware navigation tabs: Home, Search/Listings, Orders, Chat and Profile.
- Existing profile, notification and chat screens inherit the shared components.
- Comments explain source-data, payment, lifecycle, retry and navigation decisions.

## Reference-to-code map

All source paths below are relative to this Android project's
app/src/main/java/com/trustmart/app/ directory.

| PNG | Destination | Source |
| --- | --- | --- |
| 1 Splash | Welcome / guest browsing | ui/AccountScreens.kt |
| 2 Login | Sign In | ui/AccountScreens.kt |
| 3 Register | Create Account | ui/AccountScreens.kt |
| 5 Buyer Home | Buyer Home tab | ui/MarketplaceScreens.kt |
| 6 Categories | Home > Categories > See all | ui/MarketplaceScreens.kt |
| 7 Search Results | Search tab / category tile | ui/MarketplaceScreens.kt |
| 8 Product Detail | Product card | ui/MarketplaceScreens.kt |
| 9 Seller Profile | Product > seller row | ui/MarketplaceScreens.kt |
| 10 Checkout | Product > Buy Safely | ui/OrderScreens.kt |
| 11 Escrow Payment | Checkout > Proceed | ui/OrderScreens.kt |
| 12 My Orders | Orders tab | ui/OrderScreens.kt |
| 13 Order Tracking | Order > Track Order | ui/OrderScreens.kt |
| 14 Raise Dispute | Order > Report a problem | ui/OrderScreens.kt |
| 15 Seller Home | Seller Home tab | ui/SellerScreens.kt |
| 16 Add Listing | Seller Home > Add New Listing | ui/SellerScreens.kt |

## Intentional differences from the mockups

The screenshots contain sample data and features not provided by the backend.
The UI does not pretend those capabilities exist:

- Login uses the website username, not email. Registration retains required username
  and password confirmation. Google/Apple sign-in are disabled; forgot-password
  explains that an administrator must help until a reset endpoint is implemented.
- No fake ratings, listing counts, discounts, sales, view counts, response rates or
  order values. Empty datasets display empty states or a dash, not sample metrics.
- No unconditional "zero losses", "100% safe" or 24/48-hour SLA promises.
- Seller profile supports real listings/reviews and messaging, not following or offers.
- The backend charges commission to the seller. The buyer total is the item price,
  not the screenshot's invented extra fee.
- Native Stripe PaymentSheet handles test-card entry. No raw card-number/CVV form
  is stored or submitted by app code. PayHere remains a clearly marked debug simulation;
  cash on delivery is unavailable.
- Delivery addresses, courier booking, shipping milestones, tracking numbers and ETAs
  require backend work. Tracking shows only recorded order/payment/receipt states.
- Disputes support a reason and description. Evidence-photo upload is explicitly
  unavailable until the dispute API supports attachments.
- Listing submission accepts up to six selected photos and optional existing
  verification evidence. Original-price/discount metadata is not invented.

## Verify on your phone

1. Keep PostgreSQL and Django running; the React frontend is not required.
2. Open this folder in Android Studio, select your connected phone and press Run.
3. On Welcome or Home, use the gear to configure your PC's LAN API address.
4. Try guest browsing, then sign in separately as a buyer and seller.
5. Check all 15 destinations above, small/large text, keyboard, scrolling and rotation.
6. Test actual photos, chat replies and checkout only with demo accounts/listings.
   Creating a demo purchase still changes the shared database.
7. Follow the complete acceptance checklist in README.md.

Android device layout fixtures live in app/src/androidTest/.../ScreenLayoutTest.kt.
Their sample data and isolated session preferences are test-only; they never seed
the shared marketplace. A host-side Robolectric attempt was stopped when its
runtime download stalled; those extra host dependencies were removed.

With a dedicated test phone/emulator connected, run the Gradle connectedDebugAndroidTest
task to exercise the screens, role navigation and UI action guards. This installs the debug app
and test APK. Captures are saved in the app's private files/ui-screenshots directory.
The tests were run on the connected Samsung; see VERIFICATION.md for actual results.
