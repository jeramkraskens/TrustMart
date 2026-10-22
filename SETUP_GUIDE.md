# Second-Hand Goods Verification Marketplace — Setup Guide (සිංහල)

මේ ගයිඩ් එකෙන් ඔයාට zip file එක download කරලා, ඔයාගේ computer එකේ project එක run කරගන්න පුළුවන් වෙනවා step by step.

## අලුත් Features (මේ update එකේ)

- **PostgreSQL Database** — Proposal එකේ කිව්ව විදිහටම, දැන් project එකේ default database එක PostgreSQL.
- **ML Recommendation System** — Buyer කෙනෙක් search කරනකොට, ඒක log වෙලා recommendation model එක train වෙනවා. Buyer browse කරනකොට "Recommended for you" section එකක් පේනවා.
- **Advanced Admin Panel** — Revenue/Commission analytics, ML model monitoring, role-based user management (Buyers/Sellers/Moderators/Admins tabs).

## Project එකේ Structure එක

```
project/
├── backend/          <- Django REST API
├── frontend/         <- React + Tailwind website
├── mobile-moderator/ <- Kotlin Android app (moderator)
├── README.md
└── SETUP_GUIDE.md    <- මේ file එකම
```

---

## කලින් install කරගන්න ඕන දේවල් (Prerequisites)

මේවා ඔයාගේ laptop/PC එකේ install වෙලා තියෙන්න ඕන:

1. **Python 3.11+** — https://www.python.org/downloads/ (install කරද්දී "Add Python to PATH" checkbox එක tick කරන්න)
2. **Node.js 18+** — https://nodejs.org/ (LTS version එක download කරන්න)
3. **PostgreSQL 14+** — https://www.postgresql.org/download/ (⚠️ **අනිවාර්යයෙන් ඕන**, දැන් මේක project එකේ default database එක)

Install වුනාද කියලා check කරන්න, Command Prompt / Terminal එකේ:
```
python --version
node --version
npm --version
psql --version
```

---

## Step 1: Zip File එක Extract කරන්න

Download කරගත්ත zip file එක extract කරලා, ලේසි path එකකට දාගන්න.

---

## Step 2: PostgreSQL Database එක Setup කරමු

PostgreSQL install කරද්දී, `postgres` කියන superuser account එකේ password එකක් set කරන්න කියලා අහනවා — ඒක මතක තියාගන්න.

### 2.1 — Database සහ User එකක් හදන්න

Terminal/Command Prompt එකේ (Windows නම් "SQL Shell (psql)" කියලා Start Menu එකේ search කරන්න):

```sql
psql -U postgres
```

Password එක දෙන්න. ඊට පස්සේ මේ commands ටික එකින් එක run කරන්න:

```sql
CREATE USER marketplace_user WITH PASSWORD 'marketplace_pass';
CREATE DATABASE marketplace_db OWNER marketplace_user;
ALTER USER marketplace_user CREATEDB;
\q
```

(Password එක වෙනස් කරගන්නත් පුළුවන් — ඒත් අනිවාර්යයෙන් Step 3.4 එකේ `.env` file එකේත් ඒම password එකම දාන්න ඕන)

---

## Step 3: Backend එක Run කරමු (Django API)

### 3.1 — Terminal එක අරින්න backend folder එකේ

```
cd path/to/project/backend
```

### 3.2 — Virtual Environment එකක් හදන්න

**Windows:**
```
python -m venv venv
venv\Scripts\activate
```

**Mac/Linux:**
```
python3 -m venv venv
source venv/bin/activate
```

### 3.3 — Dependencies Install කරන්න

```
pip install -r requirements.txt
```

⚠️ මේකට කලින්ට වඩා ටිකක් වැඩිය වෙලා යනවා — scikit-learn, pandas වගේ ML libraries ටිකකුත් install වෙනවා (recommendation system එකට ඕන).

### 3.4 — .env File එක හදන්න

```
cp .env.example .env
```
(Windows: `copy .env.example .env`)

`.env` file එක open කරලා, Step 2.1 එකේදී ඔයා දාපු database password එකම match වෙනවද බලන්න:

```
USE_POSTGRES=True
DB_NAME=marketplace_db
DB_USER=marketplace_user
DB_PASSWORD=marketplace_pass
DB_HOST=localhost
DB_PORT=5432
```

### 3.5 — Database Migrate කරන්න

```
python manage.py migrate
```

### 3.6 — Demo Accounts සහ Categories Seed කරන්න

```
python manage.py seed_demo
```

| Role | Username | Password |
|---|---|---|
| Admin | admin | Admin@123 |
| Moderator | moderator1 | Moderator@123 |
| Seller | seller1 | Seller@123 |
| Buyer | buyer1 | Buyer@123 |

### 3.7 — Server එක Start කරන්න

```
python manage.py runserver
```

🛑 **මේ Terminal window එක open ම තියාගන්න.**

---

## Step 4: Frontend එක Run කරමු

**අලුත් Terminal window එකක්** අරින්න (backend එක run වෙනවම තියෙන්න ඕන):

```
cd path/to/project/frontend
npm install
cp .env.example .env
npm run dev
```

Browser එකේ: **http://localhost:5173**

---

## Step 5: අලුත් Features Test කරමු

### ML Recommendation System

1. `buyer1` / `Buyer@123` එකෙන් login කරන්න
2. Home page එකේ search box එකේ "laptop" වගේ දෙයක් type කරන්න (verified listings තියෙනවා නම්)
3. තත්පර 1ක් ඉන්න (search එක log වෙලා model එක background එකේ retrain වෙනවා)
4. Page එක refresh කරන්න — "✦ ML-Powered — Recommended for you" section එකක් පේනවා
5. Listing එකක් click කරලා බැලුවත් ඒකත් model එකට "training data" විදිහට log වෙනවා

### Advanced Admin Panel

`admin` / `Admin@123` එකෙන් login කරලා `/admin` යන්න:

- **Users** — Buyers/Sellers/Moderators/Admins tabs වලින් වෙන් කරලා manage කරන්න, user කෙනෙක්ගේ නම click කළොත් ඒ කෙනාගේ listings/transactions පේනවා
- **Revenue** — Total commission earned, monthly commission chart, top sellers by revenue, category-wise revenue breakdown
- **ML Recommender** — Model එකේ status (vocabulary size, last trained time), top searched terms, "Retrain Model Now" button එකෙන් manually retrain කරන්නත් පුළුවන්

---

## Common Errors

**"psql: command not found"** → PostgreSQL install කරද්දී PATH එකට add වෙලා නෑ. PostgreSQL bin folder එක (උදා: `C:\Program Files\PostgreSQL\16\bin`) manually PATH එකට add කරන්න.

**"FATAL: password authentication failed for user"** → `.env` file එකේ `DB_PASSWORD` එක, Step 2.1 එකේ දාපු password එකට match වෙනවද බලන්න.

**"could not connect to server"** → PostgreSQL service එක run වෙනවද check කරන්න:
- Windows: Services app එකේ "postgresql-x64-16" service එක Running ද බලන්න
- Mac: `brew services list`
- Linux: `sudo service postgresql status`

**ModuleNotFoundError (sklearn, pandas)** → `pip install -r requirements.txt` හරියට run වුනාද බලන්න, venv activate කරලා තියෙනවද check කරන්න.

**"Recommended for you" section එක නොපෙනීම** → Buyer කෙනෙක් search එකක් කරලා නැත්නම් හෝ listing එකක් view කරලා නැත්නම් පෙන්නන්නේ නෑ (model එකට training data ඕන). අඩුම ගානේ search එකක් කරලා බලන්න.

---

## Optional: SQLite එකට ආපහු මාරු වෙන්න

Quick local testing එකට PostgreSQL setup එක skip කරන්න ඕන නම්, `.env` file එකේ:
```
USE_POSTGRES=False
```
දාන්න. ඒත් **submission එකට PostgreSQL එකම පාවිච්චි කරන්න**, proposal එකේ කිව්ව technology stack එකට match වෙන්න.
