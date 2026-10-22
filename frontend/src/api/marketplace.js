// Pagination helper for screens that need the complete collection.
// A results page is only part of the data; getAll follows pages before returning the combined list.
import client from "./client";

// Read every API page so totals and client-side filters cover the full result set.
export async function getAll(path, params = {}, signal) {
  let page = 1;
  const items = [];
  while (true) {
    const { data } = await client.get(path, {
      params: { ...params, page },
      signal,
    });
    items.push(...(data.results ?? data));
    if (!data.next) return items;
    page += 1;
  }
}
