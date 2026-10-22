# Second-Hand Goods Verification Marketplace

> **30 September 2026:** The final backend/frontend export has been integrated,
> with compatibility updates for both Android apps. Start with
> [FINAL_PROJECT_INTEGRATION.md](FINAL_PROJECT_INTEGRATION.md) for current behavior,
> backups, verification and daily run instructions. Checkout is now simulated.

Full-stack web application for the HND Software Engineering Final Project (KUHDSE25.2).

**Stack**: Django REST Framework + PostgreSQL/SQLite (backend) · React + Vite + Tailwind CSS (frontend) · JWT auth.

See `SETUP_GUIDE.md` for full Sinhala step-by-step setup instructions.

## Quick start (English)

### Backend
```
cd backend
python -m venv venv
source venv/bin/activate        # Windows: venv\Scripts\activate
pip install -r requirements.txt
cp .env.example .env
python manage.py migrate
python manage.py seed_demo      # optional demo accounts
python manage.py runserver
```

### Frontend
```
cd frontend
npm install
cp .env.example .env
npm run dev
```

Visit http://localhost:5173

### Demo accounts (after `seed_demo`)
| Role | Username | Password |
|---|---|---|
| Admin | admin | Admin@123 |
| Moderator | moderator1 | Moderator@123 |
| Seller | seller1 | Seller@123 |
| Buyer | buyer1 | Buyer@123 |
