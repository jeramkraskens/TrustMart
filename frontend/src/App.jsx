// Website screen map and shared page layout.
// Routes select pages; ProtectedRoute checks login and role before showing private screens.
import { useEffect } from "react";
import { Route, Routes, useLocation } from "react-router-dom";
import Navbar from "./components/Navbar";
import PromotionProvider from './components/PromotionProvider';
import ProtectedRoute from "./components/ProtectedRoute";

import Home from "./pages/Home";
import Browse from "./pages/Browse";
import Categories from "./pages/Categories";
import { Footer } from "./components/MarketplaceUI";
import Login from "./pages/Login";
import Register from "./pages/Register";
import ListingDetail from "./pages/ListingDetail";
import SellerDashboard from "./pages/SellerDashboard";
import CreateListing from "./pages/CreateListing";
import BuyerDashboard from "./pages/BuyerDashboard";
import Messages from "./pages/Messages";
import Notifications from './pages/Notifications';
import Profile from "./pages/Profile";
import Wishlist from "./pages/Wishlist";
import ModeratorDashboard from "./pages/moderator/ModeratorDashboard";
import AdminLayout from "./pages/admin/AdminLayout";

// Section: Page routes — public pages and protected role dashboards.
export default function App() {
  const location = useLocation();
  const isAdmin = location.pathname.startsWith("/admin");
  const isAuth = ["/login", "/register"].includes(location.pathname);
  const isSeller = location.pathname === "/seller";

  // On navigation, jump to the requested section or reset the page to the top.
  useEffect(() => {
    if (location.hash) {
      document.getElementById(location.hash.slice(1))?.scrollIntoView();
    } else {
      window.scrollTo({ top: 0, left: 0, behavior: "instant" });
    }
  }, [location.pathname, location.hash]);

  return (
    <PromotionProvider><div className="min-h-screen flex flex-col">
      {!isAdmin && !isAuth && !isSeller && <Navbar />}
      <main className="flex-1">
        <Routes>
          <Route path="/notifications" element={<ProtectedRoute><Notifications /></ProtectedRoute>} />
          <Route path="/" element={<Home />} />
          <Route path="/browse" element={<Browse />} />
          <Route path="/categories" element={<Categories />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/listings/:id" element={<ListingDetail />} />

          <Route
            path="/seller"
            element={
              <ProtectedRoute roles={["seller"]}>
                <SellerDashboard />
              </ProtectedRoute>
            }
          />
          <Route
            path="/seller/new"
            element={
              <ProtectedRoute roles={["seller"]}>
                <CreateListing />
              </ProtectedRoute>
            }
          />
          <Route path="/seller/listings/:id/edit" element={<ProtectedRoute roles={["seller"]}><CreateListing /></ProtectedRoute>} />

          <Route
            path="/buyer"
            element={
              <ProtectedRoute roles={["buyer"]}>
                <BuyerDashboard />
              </ProtectedRoute>
            }
          />

          <Route
            path="/wishlist"
            element={
              <ProtectedRoute roles={["buyer"]}>
                <Wishlist />
              </ProtectedRoute>
            }
          />

          <Route
            path="/moderator/*"
            element={
              <ProtectedRoute roles={["moderator"]}>
                <ModeratorDashboard />
              </ProtectedRoute>
            }
          />

          <Route
            path="/admin/*"
            element={
              <ProtectedRoute roles={["admin"]}>
                <AdminLayout />
              </ProtectedRoute>
            }
          />

          <Route
            path="/messages"
            element={
              <ProtectedRoute>
                <Messages />
              </ProtectedRoute>
            }
          />

          <Route
            path="/profile"
            element={
              <ProtectedRoute>
                <Profile />
              </ProtectedRoute>
            }
          />
        </Routes>
      </main>
      {!isAdmin && !isAuth && !isSeller && <Footer />}
    </div></PromotionProvider>
  );
}
