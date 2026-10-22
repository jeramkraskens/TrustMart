// Shared display helpers for prices, condition labels, categories and chart dates.
export const money = (value) => `Rs. ${Number(value || 0).toLocaleString()}`;
export const conditionLabel = (value) =>
  value === "new" ? "Like New" : (value || "").replaceAll("_", " ");
export const categoryDesigns = [
  {
    name: "Phones & Tablets",
    short: "Phones",
    icon: "📱",
    text: "Smartphones, tablets, accessories",
    query: "phone",
    match: /mobile|phone|tablet/i,
    tone: "teal",
  },
  {
    name: "Computers & Laptops",
    short: "Laptops",
    icon: "💻",
    text: "Laptops, desktops, peripherals",
    query: "laptop",
    match: /computer|laptop/i,
    tone: "blue",
  },
  {
    name: "Vehicles",
    short: "Vehicles",
    icon: "🚗",
    text: "Cars, bikes, scooters",
    query: "vehicle",
    match: /vehicle/i,
    tone: "orange",
  },
  {
    name: "Furniture & Home",
    short: "Furniture",
    icon: "🏠",
    text: "Sofas, beds, appliances",
    query: "furniture",
    match: /furniture/i,
    tone: "green",
  },
  {
    name: "Fashion & Clothing",
    short: "Fashion",
    icon: "👗",
    text: "Clothes, shoes, bags",
    query: "fashion",
    match: /fashion/i,
    tone: "purple",
  },
  {
    name: "Cameras & Optics",
    short: "Cameras",
    icon: "📷",
    text: "DSLRs, lenses, action cams",
    query: "camera",
    match: /camera/i,
    tone: "teal",
  },
  {
    name: "Gaming & Consoles",
    short: "Gaming",
    icon: "🎮",
    text: "PS5, Xbox, Nintendo, games",
    query: "gaming",
    match: /gaming|console/i,
    tone: "red",
  },
  {
    name: "Tools & Equipment",
    short: "Tools",
    icon: "🔧",
    text: "Power tools, hand tools, gear",
    query: "tool",
    match: /tool/i,
    tone: "orange",
  },
  {
    name: "Sports & Outdoors",
    short: "Sports",
    icon: "⚽",
    text: "Bikes, gym gear, camping",
    query: "sport",
    match: /sport/i,
    tone: "green",
  },
  {
    name: "Books & Education",
    short: "Books",
    icon: "📚",
    text: "Textbooks, novels, stationery",
    query: "book",
    match: /book|education/i,
    tone: "blue",
  },
  {
    name: "Musical Instruments",
    short: "Music",
    icon: "🎸",
    text: "Guitars, keyboards, drums",
    query: "guitar",
    match: /music/i,
    tone: "purple",
  },
  {
    name: "Baby & Kids",
    short: "Kids",
    icon: "🧸",
    text: "Toys, clothing, strollers",
    query: "toy",
    match: /baby|kid/i,
    tone: "red",
  },
];
export const categoryHref = (design, categories = []) => {
  const category = categories.find((c) => design.match.test(c.name));
  return category
    ? `/browse?category=${category.id}`
    : `/browse?search=${encodeURIComponent(design.query)}`;
};
export function itemEmoji(listing) {
  const title = listing?.title || "";
  if (/laptop|macbook|computer/i.test(title)) return "💻";
  if (/phone|ipad|tablet/i.test(title)) return "📱";
  if (/camera|canon/i.test(title)) return "📷";
  if (/headphone|sony|jbl/i.test(title)) return "🎧";
  return (
    categoryDesigns.find((c) => c.match.test(listing?.category_name || ""))
      ?.icon || "📦"
  );
}

export function recentMonths(count = 6) {
  const now = new Date();
  return Array.from({ length: count }, (_, i) => {
    const date = new Date(now.getFullYear(), now.getMonth() - count + i + 1, 1);
    return {
      month: `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}`,
      label: date.toLocaleDateString("en", { month: "short" }),
    };
  });
}
