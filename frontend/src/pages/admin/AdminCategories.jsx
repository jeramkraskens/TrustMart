import { useEffect, useState } from 'react';
import client from '../../api/client';
import { getAll } from '../../api/marketplace';
import useRecordSearch from '../../components/useRecordSearch';

export default function AdminCategories() {
  const [categories, setCategories] = useState([]);
  const { filtered, searchControl } = useRecordSearch(categories, ['name', 'description'], 'Search categories');
  const [loading, setLoading] = useState(true);
  const [newName, setNewName] = useState('');
  const [newDesc, setNewDesc] = useState('');
  const [editingId, setEditingId] = useState(null);
  const [editForm, setEditForm] = useState({ name: '', description: '' });
  const [error, setError] = useState('');

  const load = () => {
    setLoading(true);
    getAll('/categories/').then(setCategories).catch(() => setError('Could not load categories. Please refresh.')).finally(() => setLoading(false));
  };

  useEffect(load, []);

  const addCategory = (e) => {
    e.preventDefault();
    setError('');
    if (!newName.trim()) return;
    client.post('/categories/', { name: newName, description: newDesc })
      .then(() => { setNewName(''); setNewDesc(''); load(); })
      .catch((err) => setError(err.response?.data?.name?.[0] || 'Could not add category.'));
  };

  const startEdit = (c) => {
    setEditingId(c.id);
    setEditForm({ name: c.name, description: c.description || '' });
  };

  const saveEdit = (id) => {
    client.patch(`/categories/${id}/`, editForm).then(() => { setEditingId(null); load(); });
  };

  const deleteCategory = (c) => {
    if (!confirm(`Delete category "${c.name}"? Listings in this category will keep their existing data but lose their category link.`)) return;
    client.delete(`/categories/${c.id}/`).then(load);
  };

  return (
    <div className="p-6 md:p-8">
      <h1 className="text-2xl font-bold tracking-tight mb-1">Categories</h1>
      <p className="text-white/40 text-sm mb-6">Manage the categories sellers choose from when listing an item.</p>

      <form onSubmit={addCategory} className="bg-[#1a212e] rounded-xl border border-white/5 p-4 mb-6 flex gap-3 items-start">
        <input
          value={newName}
          onChange={(e) => setNewName(e.target.value)}
          placeholder="Category name"
          className="flex-1 px-4 py-2 rounded-lg border border-white/10 bg-[#1a212e] text-sm focus:outline-none focus:border-teal"
        />
        <input
          value={newDesc}
          onChange={(e) => setNewDesc(e.target.value)}
          placeholder="Description (optional)"
          className="flex-1 px-4 py-2 rounded-lg border border-white/10 bg-[#1a212e] text-sm focus:outline-none focus:border-teal"
        />
        <button type="submit" className="px-4 py-2 rounded-lg bg-teal text-white text-sm font-semibold hover:bg-teal-dark">
          Add category
        </button>
      </form>
      {error && <p className="text-sm text-rust mb-4">{error}</p>}

      {searchControl}
      <div className="bg-[#1a212e] rounded-xl border border-white/5 divide-y divide-white/5">
        {loading ? (
          <p className="p-6 text-center text-white/30">Loading...</p>
        ) : categories.length === 0 ? (
          <p className="p-6 text-center text-white/30">No categories yet.</p>
        ) : (
          filtered.map((c) => (
            <div key={c.id} className="flex items-center gap-4 p-4">
              {editingId === c.id ? (
                <>
                  <input
                    value={editForm.name}
                    onChange={(e) => setEditForm({ ...editForm, name: e.target.value })}
                    className="flex-1 px-3 py-1.5 rounded-lg border border-white/10 text-sm"
                  />
                  <input
                    value={editForm.description}
                    onChange={(e) => setEditForm({ ...editForm, description: e.target.value })}
                    className="flex-1 px-3 py-1.5 rounded-lg border border-white/10 text-sm"
                  />
                  <button onClick={() => saveEdit(c.id)} className="text-xs px-3 py-1 rounded-full bg-teal text-white">Save</button>
                  <button onClick={() => setEditingId(null)} className="text-xs px-3 py-1 text-white/40">Cancel</button>
                </>
              ) : (
                <>
                  <div className="flex-1">
                    <p className="font-medium text-sm">{c.name}</p>
                    {c.description && <p className="text-xs text-white/40">{c.description}</p>}
                  </div>
                  <button onClick={() => startEdit(c)} className="text-xs px-3 py-1 rounded-full border border-white/10 hover:border-teal hover:text-teal">
                    Edit
                  </button>
                  <button onClick={() => deleteCategory(c)} className="text-xs px-3 py-1 rounded-full border border-white/10 text-white/40 hover:border-rust hover:text-rust">
                    Delete
                  </button>
                </>
              )}
            </div>
          ))
        )}
      </div>
    </div>
  );
}
