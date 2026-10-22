# Moderator Mobile App — Android Studio Setup Guide (සිංහල)

මේක Kotlin + Jetpack Compose වලින් හදපු native Android app එකක්, moderator ට listing verifications approve/reject කරන්නත්, disputes resolve කරන්නත් phone එකෙන්ම කරගන්න පුළුවන්. මේක ඔයාගේ Django backend එකටම (web app එකේම API එකට) connect වෙනවා.

---

## කලින් install කරගන්න ඕන දේවල්

1. **Android Studio** (latest stable — Koala/Ladybug කියලා version) — https://developer.android.com/studio
2. Android Studio එක install කරද්දී, එකේම **Android SDK**, **Android Emulator** automatic විදිහට setup වෙනවා (setup wizard එක follow කරන්න)
3. Backend server එක (Django) දැනටමත් setup කරලා තියෙන්න ඕන — web app එකේ `SETUP_GUIDE.md` file එක බලන්න

---

## Step 1: Project එක Android Studio එකෙන් අරින්න

1. `moderator_mobile_app.zip` file එක extract කරන්න
2. Android Studio අරින්න
3. **File → Open** → extract කරගත්ත `mobile-moderator` folder එක select කරන්න
4. Android Studio එකට "Gradle Sync" එකක් run වෙනවා — මේකට පළවෙනි වතාවට මිනිත්තු කිහිපයක් යන්න පුළුවන් (dependencies download වෙනවා internet එකෙන්)
5. Sync එක fail වුනොත්, පහළ "Sync Now" බැනර් එකක් පෙන්නනවා නම් ඒක click කරන්න

> **Note**: Gradle wrapper jar file එක (`gradle-wrapper.jar`) zip එකේ නෑ (binary file එකක් නිසා පේනවලා). Android Studio එකට project එක open කරද්දී මේක automatic විදිහට download කරගන්නවා. Error එකක් ආවොත් "File → Sync Project with Gradle Files" කරන්න.

---

## Step 2: Backend Server එක සමඟ Connect කරන්න

Mobile app එකේ default configuration එක **Android Emulator** එකට match වෙන්නේ:

```kotlin
// app/build.gradle.kts
buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8000/api/\"")
```

`10.0.2.2` කියන්නේ Android Emulator එකේ, ඔයාගේ computer එකේම `localhost` එකට point කරන special address එකක්. ඒ නිසා:

1. **Backend server එක run කරගෙන ඉන්න ඕන** (`python manage.py runserver`) — web app guide එකේ කියපු විදිහට
2. Emulator එකෙන් test කරනවා නම්, කිසිම වෙනසක් කරන්න ඕන නෑ

### ලේසි phone එකකින් Test කරනවා නම් (Physical Device)

Real phone එකකින් test කරනවා නම් (USB එකෙන් හෝ same Wi-Fi එකේ):

1. ඔයාගේ computer එකේ LAN IP address එක හොයාගන්න:
   - **Windows**: Command Prompt එකේ `ipconfig` → "IPv4 Address" (උදා: `192.168.1.10`)
   - **Mac/Linux**: Terminal එකේ `ifconfig` හෝ `ip addr`
2. `app/build.gradle.kts` file එකේ URL එක වෙනස් කරන්න:
   ```kotlin
   buildConfigField("String", "API_BASE_URL", "\"http://192.168.1.10:8000/api/\"")
   ```
3. `app/src/main/res/xml/network_security_config.xml` file එකේ ඔයාගේ IP එකත් add කරන්න:
   ```xml
   <domain includeSubdomains="true">192.168.1.10</domain>
   ```
4. Django backend එකේ `.env` file එකේ `ALLOWED_HOSTS` එකටත් මේ IP එක add කරන්න:
   ```
   ALLOWED_HOSTS=localhost,127.0.0.1,192.168.1.10
   ```
5. Django server එක restart කරන්න
6. ඔයාගේ phone එකයි computer එකයි **එකම Wi-Fi network එකේ** ඉන්න ඕන

---

## Step 3: Emulator එකක් හදන්න (හෝ Physical Device Connect කරන්න)

### Emulator පාවිච්චි කරනවා නම්:
1. Android Studio එකේ **Device Manager** (right side panel හෝ Tools menu) අරින්න
2. **Create Device** → phone එකක් select කරන්න (උදා: Pixel 7)
3. System Image එකක් download කරන්න (recommended: API 34)
4. Emulator එක start කරන්න

### Physical Phone පාවිච්චි කරනවා නම්:
1. Phone එකේ **Settings → About Phone** → "Build Number" 7 පාරක් click කරන්න (Developer Options enable වෙනවා)
2. **Settings → Developer Options** → "USB Debugging" enable කරන්න
3. Phone එක USB cable එකෙන් computer එකට connect කරන්න
4. "Allow USB debugging?" popup එකට Allow කරන්න

---

## Step 4: App එක Run කරන්න

1. Android Studio එකේ top toolbar එකේ, device එක select කරන්න (emulator හෝ ඔයාගේ phone එක)
2. Green **Run** button එක (▶️) click කරන්න
3. App එක build වෙලා device එකේ install වෙනවා

---

## Step 5: Test කරමු

Web app එකේ `seed_demo` command එකෙන් හදාගත්ත moderator account එකෙන් login කරන්න:

- **Username**: `moderator1`
- **Password**: `Moderator@123`

App එකෙන් login වුනාම:
1. **Verifications** tab එකේ pending listings පේනවා (web app එකෙන් seller කෙනෙක් listing එකක් submit කරලා තියෙනවා නම්)
2. Listing එකක් click කරලා expand කරන්න → feedback එකක් type කරලා **Approve** හෝ **Reject** කරන්න
3. **Disputes** tab එකට switch කරලා, open disputes ඒ විදිහටම resolve කරන්න
4. Top right corner එකේ logout icon එකෙන් log out කරන්න

> Moderator role නැති account එකකින් (buyer/seller/admin) login කරන්න try කළොත්, app එක reject කරනවා — "This app is for moderator accounts only" කියලා.

---

## App එකේ Architecture (Technical Summary)

- **UI**: Jetpack Compose (Material 3) — Login screen, Verification queue, Dispute queue
- **Networking**: Retrofit + OkHttp, JWT access token automatically attach වෙනවා every request එකටම
- **Token refresh**: Access token expire වුනොත් (401 error), app එක automatic විදිහම refresh token එක පාවිච්චි කරලා අලුත් token එකක් ගන්නවා — user ට කිසිම බාධාවක් නෑ
- **Token storage**: Jetpack DataStore (secure, encrypted preferences)
- **State management**: ViewModel + StateFlow (MVVM pattern)

---

## Common Errors

**"Gradle sync failed" / dependencies download වෙන්නේ නෑ** → Internet connection එක check කරන්න. Corporate/university Wi-Fi එකක නම්, VPN එකක් අවශ්‍ය වෙන්න පුළුවන් (Google's Maven repository එකට access ඕන).

**App එකේ "Network error" පේනවා** → Backend server එක run වෙනවද කියලා check කරන්න. Emulator එකෙන් නම් `10.0.2.2`, physical device එකෙන් නම් LAN IP එක හරියටම `.env`, `build.gradle.kts`, සහ `network_security_config.xml` තුනේම match වෙනවද බලන්න.

**"Cleartext HTTP traffic not permitted"** → `network_security_config.xml` file එකේ ඔයා පාවිච්චි කරන IP address එක add කරලා නෑ. එය add කරලා app එක rebuild කරන්න.

**Login කරද්දී "Invalid username or password"** → Backend එකේ `seed_demo` command එක run කරලා තියෙනවද, moderator1 account එක actually තියෙනවද check කරන්න.
