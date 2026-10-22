// Category browsing screen that leads users into category-specific product results.
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ArrowRight } from "lucide-react";
import { getAll } from "../api/marketplace";
import { categoryDesigns, categoryHref } from "../api/presentation";

export default function Categories() {
  const [categories, setCategories] = useState([]);
  const [items, setItems] = useState([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  useEffect(() => {
    Promise.all([
      getAll("/categories/"),
      getAll("/listings/", { status: "verified" }),
    ])
      .then(([cats, listings]) => {
        setCategories(cats);
        setItems(listings);
      })
      .catch(() =>
        setError(
          "Could not load listing counts. You can still browse by category.",
        ),
      )
      .finally(() => setLoading(false));
  }, []);
  return (
    <div className="categories-page">
      <div className="page-heading">
        <div className="tm-container">
          <div>
            <h1>Browse Categories</h1>
            <p>Find verified second-hand goods across all categories</p>
          </div>
          <div className="trending">
            <span>Explore:</span>
            {[0, 1, 2, 6].map((i) => (
              <Link
                className="tm-pill tone-teal"
                key={i}
                to={categoryHref(categoryDesigns[i], categories)}
              >
                {categoryDesigns[i].icon} {categoryDesigns[i].short}
              </Link>
            ))}
          </div>
        </div>
      </div>
      <div className="tm-container categories-content">
        <h2>All Categories</h2>
        {error && (
          <p role="alert" className="form-error">
            {error}
          </p>
        )}
        <div className="category-grid">
          {categoryDesigns.map((cat) => {
            const actual = categories.find((c) => cat.match.test(c.name));
            const count = items.filter((item) =>
              actual
                ? item.category === actual.id
                : (item.title + " " + item.description)
                    .toLowerCase()
                    .includes(cat.query),
            ).length;
            return (
              <Link
                className={`category-card accent-${cat.tone}`}
                key={cat.name}
                to={categoryHref(cat, categories)}
              >
                <span className={`category-icon tone-${cat.tone}`}>
                  {cat.icon}
                </span>
                <div>
                  <h3>{cat.name}</h3>
                  <p>{cat.text}</p>
                  <strong>
                    {loading
                      ? "Loading..."
                      : error
                        ? "Explore listings"
                        : `${count.toLocaleString()} listings`}
                  </strong>
                  <span className={`tm-pill tone-${cat.tone}`}>
                    All Verified
                  </span>
                </div>
                <ArrowRight size={17} />
              </Link>
            );
          })}
        </div>
        <div className="popular-searches">
          <h2>Popular Searches</h2>
          <div>
            {[
              "iPhone",
              "MacBook",
              "Honda",
              "PS5",
              "DJI",
              "Guitar",
              "Nike",
              "Samsung",
              "Desk",
              "Yoga",
            ].map((query) => (
              <Link
                className="search-chip"
                key={query}
                to={`/browse?search=${encodeURIComponent(query)}`}
              >
                {query}
              </Link>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
