# TrustMart — වෙනත් laptop එකක setup කිරීම සහ hosting guide

## 1. Website එක භාවිත කරන්න විතරක් නම්

Browser එකෙන් https://trustmartlk.pythonanywhere.com open කරන්න.
Python, Node.js, Git හෝ commands අවශ්‍ය නැහැ. PC එක off වුණත් server එක වැඩ කරනවා.
Guest කෙනෙකුට `/browse` සහ verified item details බලන්න account එකක් අවශ්‍ය නැහැ.
Buy/save/message වැනි account actions සඳහා website account එකෙන් login වෙන්න.
PythonAnywhere account එක hosting පාලනය සඳහායි; ඒක TrustMart buyer/seller login එකට වෙනස්.

Browse එක හිස් නම්: seller listing submit කරලා moderator approve කරලා තිබිය යුතුයි.
Pending/rejected/draft items public browse එකේ පෙන්වන්නේ නැහැ. Local laptop database
එකේ items hosted database එකට automatically upload වෙන්නේත් නැහැ.

## 2. වෙනත් laptop එකෙන් hosting manage කරන්න

1. https://www.pythonanywhere.com වෙත යන්න.
2. Hosting ownerගේ `trustmartlk` account එකෙන් login වෙන්න.
3. Web tab > `trustmartlk.pythonanywhere.com` තෝරන්න.
4. Code update කළා නම් Reload ඔබන්න.
5. සෑම මාසයකට වරක් **Run until 1 month from today** ඔබන්න.
   දැනට expiry date එක 22 November 2026. Daily renew කරන්න අවශ්‍ය නැහැ.
6. Error එකක් නම් Web tab එකේ error log link බලන්න.

වෙනත් කෙනෙකුගේ laptop එකක් නම් password save නොකරන්න; වැඩ අවසන් කර logout වෙන්න.
Site එක භාවිත කරන හැමෝටම hosting account password දෙන්න අවශ්‍ය නැහැ.

## 3. Full local development setup — Windows PowerShell

Install කරන්න: Git, Python 3.13, Node.js 22.12+ (හෝ compatible newer LTS),
Android apps edit/build කරනවා නම් Android Studio සහ Android SDK 34 / JDK 17.
මෙහි beginner local setup එක SQLite භාවිත කරන නිසා PostgreSQL අවශ්‍ය නැහැ.

### Repository download

```powershell
cd $env:USERPROFILE\Documents
git clone https://github.com/jeramkraskens/TrustMart.git
cd TrustMart
```

### Backend — terminal 1

```powershell
cd backend
py -3.13 -m venv venv
.\venv\Scripts\python.exe -m pip install -r requirements.txt
Copy-Item .env.example .env
notepad .env
```

`.env` එකේ `USE_POSTGRES=False`, `DEBUG=True` දාන්න. Local hosts/CORS values තබන්න.
Random SECRET_KEY එකක් generate කර .env එකට copy කරන්න:

```powershell
.\venv\Scripts\python.exe -c "import secrets; print(secrets.token_urlsafe(64))"
.\venv\Scripts\python.exe manage.py migrate
.\venv\Scripts\python.exe manage.py createsuperuser
.\venv\Scripts\python.exe manage.py runserver
```

Admin username/password ඔබම තෝරන්න. Category/admin setup සඳහා
http://127.0.0.1:8000/admin/ භාවිත කරන්න. Local demo data අවශ්‍ය නම් පමණක්
`python manage.py seed_demo` command එක venv Python එකෙන් run කළ හැකියි;
public server එකේ known demo passwords භාවිත කරන්න එපා.

### Frontend — terminal 2 (repository root එකේ සිට)

```powershell
cd frontend
npm ci
Copy-Item .env.example .env
npm run dev
```

`.env` එකේ `VITE_API_URL=http://127.0.0.1:8000/api` තිබිය යුතුයි.
Terminal එකෙන් දෙන localhost URL එක open කරන්න (සාමාන්‍යයෙන් http://localhost:5173).
Local development සඳහා terminals දෙකම run විය යුතුයි. Local database සහ hosted
SQLite database වෙන වෙනමයි; git push කළාට database/users/photos sync වෙන්නේ නැහැ.

### Android apps

Android Studio > Open හරහා මේ folders වෙන වෙනම open කරන්න:
- `TrustMart App` — buyer/seller app
- `mobile-moderator` — moderator app

Gradle sync ඉවර කර emulator/device select කර Run ඔබන්න. Server settings එකේ
`https://trustmartlk.pythonanywhere.com/api/` දාන්න. Hosted backend භාවිත කරන විට
laptop backend run කරන්න අවශ්‍ය නැහැ. Local emulator backend නම්
`http://10.0.2.2:8000/api/`; physical device නම් same Wi-Fi සහ laptop LAN IP අවශ්‍යයි.
වෙනත් laptop එකක `local.properties` copy නොකර Android Studio එකට SDK path හදන්න දෙන්න.
මේ hosting update එක APK rebuild එකක් නොවේ; installed apps වල server URL වෙනස් කරන්න.
Hosted WSGI එක WebSockets support නොකරන නිසා mobile live-chat fallback වෙනම verify කරන්න.

## 4. අපි host කළ සම්පූර්ණ process එක

Architecture: Browser -> HTTPS PythonAnywhere -> Django WSGI -> SQLite.
React build එකත් Django/WhiteNoise මගින් serve කරනවා. Upload files server disk එකේ;
evidence URLs signed සහ පැයක් පමණක් valid. Payment flow එක demo simulation එකක්.

1. Free Beginner account `trustmartlk` හැදුවා — card එකක් දුන්නේ නැහැ.
2. GitHub code එක PythonAnywhere Bash console එකට clone කළා.
3. Python 3.13 virtualenv එක `--system-site-packages` සමග හැදුවා.
   Host එකේ scientific packages reuse කර 512 MB disk limit එකට ගැළපුවා.
4. `requirements-pythonanywhere.txt` dependencies install කළා.
5. Server එකේ පමණක් `.env` එකක් random SECRET_KEY සමග හැදුවා:
   DEBUG=False, USE_POSTGRES=False, USE_DAPHNE=False,
   SINGLE_PROCESS_CHANNELS=True, ALLOWED_HOSTS=trustmartlk.pythonanywhere.com.
6. `config.pythonanywhere` settings යටතේ migrate සහ collectstatic කළා.
7. Categories initialize කළා. Public demo users/passwords create කළේ නැහැ.
8. React frontend එක `/api` base URL සහ polling chat සමග build කළා.
   Production files `deploy/web` folder එකේ GitHub එකට commit කළා.
9. Web > Add new web app > Manual configuration > Python 3.13 තෝරලා
   virtualenv `/home/trustmartlk/trustmart-venv` දුන්නා.
10. `/var/www/trustmartlk_pythonanywhere_com_wsgi.py` එක project එකට connect කළා:

```python
import sys
sys.path.insert(0, '/home/trustmartlk/TrustMart/backend')
from config.pythonanywhere_wsgi import application
```

11. Force HTTPS enable කර Reload කළා. Homepage/API/categories verify කළා.

ASGI trial එකේ HTTPS redirect loop එකක් හමු වූ නිසා final deployment එක WSGI.
Hosted web chat/notifications තත්පර 15කට refresh වෙනවා. Redis/Supabase මේ final
hosting එකට භාවිත කරන්නේ නැහැ. Hosting account API token එකක් මෙයට අවශ්‍ය නැහැ.

## 5. Edit -> GitHub push -> hosted site update

GitHub එකට push කළ පමණින් PythonAnywhere auto-deploy වෙන්නේ නැහැ.
මුලින් local frontend build කරන්න (PowerShell, repository root):

```powershell
cd frontend
$env:VITE_API_URL='/api'
$env:VITE_LIVE_TRANSPORT='polling'
npm run build
cd ..
Copy-Item frontend/dist/* deploy/web/ -Recurse -Force
git status
git add frontend/src backend deploy/web
git commit -m "Update TrustMart"
git push origin main
```

`git status` බලලා secrets/database/private uploads commit නොවන බව තහවුරු කරන්න.
GitHub password එක command එකේ ලියන්න එපා; Git credential sign-in භාවිත කරන්න.

PythonAnywhere > Consoles > Bash එකේ:

```bash
cd ~/TrustMart
git pull --ff-only origin main
cd backend
export DJANGO_SETTINGS_MODULE=config.pythonanywhere
~/trustmart-venv/bin/python manage.py migrate
~/trustmart-venv/bin/python manage.py collectstatic --noinput
~/trustmart-venv/bin/python manage.py check
```

ඉන්පසු Web tab > Reload. Website එක refresh කර changes බලන්න.
Schema changes කිරීමට පෙර database/media private backup එකක් තබන්න.

## 6. Hosted admin සහ moderator workflow

PythonAnywhere Bash console එකේ:

```bash
cd ~/TrustMart/backend
DJANGO_SETTINGS_MODULE=config.pythonanywhere ~/trustmart-venv/bin/python manage.py createsuperuser
```

ඔබේම strong password එක console prompt එකේ දාන්න. Hosted Django admin:
https://trustmartlk.pythonanywhere.com/django-admin/
React admin dashboard: https://trustmartlk.pythonanywhere.com/admin/overview
Admin user management හරහා moderator account එකක් සකස් කරන්න.
Seller website එකේ register -> New listing -> product photos + evidence -> Submit.
Moderator login -> review evidence -> approve. ඉන්පසු guest Browse එකේ item පෙන්වනවා.

## 7. Limits සහ troubleshooting

- Free site මාසිකව renew කරන්න; capacity/storage සීමිත නිසා viva/demo එකකට සුදුසුයි.
- `.com` domain එකක් අවශ්‍ය නම් domain ownership සහ paid hosting plan අවශ්‍යයි.
- No listings: නිවැරදි hosted database එකේ verified listings තියෙනවාද බලන්න.
- 500 error: PythonAnywhere Web error log බලන්න; secrets share කරන්න එපා.
- Old UI: Git pull + Reload කර browser hard refresh කරන්න.
- Local changes live වෙන්නේ නැහැ: frontend build copy, commit/push, server pull සහ Reload සියල්ල අවශ්‍යයි.
- Database/photos GitHub එකට නොදාන්න; `.env`, venv, local.properties සහ signing keys private තබන්න.
