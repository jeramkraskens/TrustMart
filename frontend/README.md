# TrustMart Frontend

React + Vite website එක. Styling සඳහා Tailwind/custom CSS, API requests සඳහා Axios, navigation සඳහා React Router භාවිතා කරනවා.

## Run / Build

`frontend/` folder එකේ සිට:

```powershell
npm install
if (!(Test-Path .env)) { Copy-Item .env.example .env }
npm run dev
```

- **Website:** http://127.0.0.1:5173/
- **API setting:** `.env` හි `VITE_API_URL` — default `http://127.0.0.1:8000/api`.
- **Production build:** `npm run build`
- **Lint check:** `npm run lint`
- **Session tests:** `npm test`

## Code එක හොයන්න

1. `src/main.jsx` / `src/App.jsx` — startup සහ page routes.
2. `src/context/AuthContext.jsx` / `src/api/session.js` — login සහ guest startup.
3. `src/pages/` — register, profile, listings සහ dashboards.
4. `src/components/` — shared forms, photo upload, search සහ ad popup.

Editor එකේ `Section:` search කළොත් main blocks වල short English comments හොයාගන්න පුළුවන්.

## Seller NIC update

- [AccountFields.jsx](src/components/AccountFields.jsx) — NIC number required, digits පමණක්; digit count limit නැහැ. Last letter optional (`None`, `V`, `X`). Registration / profile / admin create form එකට shared component එකක්.
- [account.js](src/api/account.js) — Leading zeros තබාගන්නවා; `None` තෝරා save කළ විට කලින් තිබූ NIC letter එක clear කරනවා.
- [account-forms.css](src/account-forms.css) — NIC input සහ optional letter එක සමාන උසින් align වෙනවා; mobile එකේ fields එක යට එක පෙන්වනවා.
- Backend validation සහ database migration details සඳහා [main README](../README.md) බලන්න.

Backend එකත් run විය යුතුයි. Technologies, new features, validation rules සහ full setup summary සඳහා [main README](../README.md) බලන්න.
