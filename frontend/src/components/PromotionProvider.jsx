// Loads promotions and shares their state with descendant components.
// It also coordinates which promotion popup should appear as the user navigates.
import { useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getAll } from '../api/marketplace';
import PromotionContext from './PromotionContext';
import AdvertisementPopup from './AdvertisementPopup';

const seenPromotions = new Set();

export default function PromotionProvider({ children }) {
  const { user, loading } = useAuth();
  const { pathname, key: routeKey } = useLocation();
  const navigate = useNavigate();
  const [ads, setAds] = useState([]);
  const [popup, setPopup] = useState(null);
  const sellerVisit = useRef({ route: '', shown: false });
  const identity = user ? `${user.role}:${user.id}` : 'guest';
  const isSeller = user?.role === 'seller';
  // Section: Popup eligibility — signed-in sellers see ads only on the homepage.
  const publicPage = ['/', '/browse', '/categories'].includes(pathname) || pathname.startsWith('/listings/');
  const eligible = !loading && (isSeller ? pathname === '/' : publicPage);
  const key = `trustmart:promotions:${identity}:${ads.map((ad) => `${ad.id}-${ad.updated_at}`).join('|')}`;
  useEffect(() => {
    const controller = new AbortController();
    getAll('/advertisements/', { active_only: 'true' }, controller.signal).then(setAds).catch(() => {});
    return () => controller.abort();
  }, []);
  useEffect(() => {
    // Section: Popup timing — per homepage visit for sellers, per session for others.
    const route = `${identity}:${pathname}`;
    if (sellerVisit.current.route !== route) sellerVisit.current = { route, shown: false };
    if (!eligible || !ads.length) return;
    if (isSeller) {
      if (sellerVisit.current.shown) return;
    } else {
      let seen = seenPromotions.has(key);
      try { seen ||= sessionStorage.getItem(key) === 'seen'; } catch { /* In-memory fallback. */ }
      if (seen) return;
    }
    const timer = setTimeout(() => {
      if (isSeller) {
        if (sellerVisit.current.shown) return;
        sellerVisit.current.shown = true;
      } else {
        if (seenPromotions.has(key)) return;
        seenPromotions.add(key);
        try { sessionStorage.setItem(key, 'seen'); } catch { /* In-memory fallback. */ }
      }
      setPopup({ index: 0, identity, routeKey });
    }, 1100);
    return () => clearTimeout(timer);
  }, [ads.length, eligible, identity, isSeller, key, pathname, routeKey]);
  // Section: Manual offers — open a selected ad only on an eligible page.
  const openOffer = (index) => {
    if (!eligible) return;
    if (isSeller) sellerVisit.current.shown = true;
    else {
      seenPromotions.add(key);
      try { sessionStorage.setItem(key, 'seen'); } catch { /* In-memory fallback. */ }
    }
    setPopup({ index, identity, routeKey });
  };
  return <PromotionContext.Provider value={{ ads, openOffer, eligible }}>
    {children}
    {eligible && popup?.identity === identity && popup?.routeKey === routeKey && <AdvertisementPopup key={`${identity}:${routeKey}:${popup.index}`} ads={ads} initialIndex={popup.index} onDismiss={() => setPopup(null)} onBrowse={isSeller ? () => { setPopup(null); navigate('/browse'); } : undefined} />}
  </PromotionContext.Provider>;
}
