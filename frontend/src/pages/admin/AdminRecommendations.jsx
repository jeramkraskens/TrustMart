import { useEffect, useState } from 'react';
import client from '../../api/client';

export default function AdminRecommendations() {
  const [status, setStatus] = useState(null);
  const [loading, setLoading] = useState(true);
  const [retraining, setRetraining] = useState(false);
  const [notice, setNotice] = useState('');

  const load = () => {
    setLoading(true);
    client.get('/recommendations/status/')
      .then((res) => setStatus(res.data))
      .finally(() => setLoading(false));
  };

  useEffect(load, []);

  const retrain = () => {
    setRetraining(true);
    setNotice('');
    client.post('/recommendations/retrain/')
      .then((res) => {
        setNotice(`Model retrained on ${res.data.model_stats.num_listings} listings.`);
        load();
      })
      .catch(() => setNotice('Retrain failed — check backend logs.'))
      .finally(() => setRetraining(false));
  };

  if (loading) return <div className="p-8 text-white/40">Loading...</div>;

  return (
    <div className="p-6 md:p-8 space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <div className="flex items-center gap-2">
            <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-purple-light text-purple">✦ Machine Learning</span>
          </div>
          <h1 className="text-2xl font-bold tracking-tight mt-1">Recommendation Engine</h1>
          <p className="text-white/40 text-sm">Content-based recommender (TF-IDF + cosine similarity) trained on listings and buyer search/view history.</p>
        </div>
        <button
          onClick={retrain}
          disabled={retraining}
          className="px-5 py-2.5 rounded-lg bg-teal text-white font-semibold hover:bg-teal-dark transition-colors disabled:opacity-50 flex-shrink-0"
        >
          {retraining ? 'Retraining...' : 'Retrain Model Now'}
        </button>
      </div>

      {notice && <p className="text-sm text-sage bg-sage-light rounded-lg px-4 py-2">{notice}</p>}

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="bg-[#1a212e] rounded-xl border border-white/5 p-4">
          <p className="text-2xl font-bold text-teal-dark">{status?.num_listings_indexed ?? 0}</p>
          <p className="text-xs text-white/40 mt-0.5">Listings indexed</p>
        </div>
        <div className="bg-[#1a212e] rounded-xl border border-white/5 p-4">
          <p className="text-2xl font-bold text-purple">{status?.vocabulary_size ?? 0}</p>
          <p className="text-xs text-white/40 mt-0.5">Vocabulary size</p>
        </div>
        <div className="bg-[#1a212e] rounded-xl border border-white/5 p-4">
          <p className="text-2xl font-bold text-amber">{status?.total_search_queries ?? 0}</p>
          <p className="text-xs text-white/40 mt-0.5">Total search queries logged</p>
        </div>
        <div className="bg-[#1a212e] rounded-xl border border-white/5 p-4">
          <p className="text-sm font-bold text-white">
            {status?.trained_at ? new Date(status.trained_at).toLocaleString() : 'Never trained'}
          </p>
          <p className="text-xs text-white/40 mt-0.5">Last trained</p>
        </div>
      </div>

      <div className="bg-[#1a212e] rounded-xl border border-white/5 p-5">
        <h2 className="font-bold text-sm mb-4">Top searched terms</h2>
        {!status?.top_searched_terms || status.top_searched_terms.length === 0 ? (
          <p className="text-sm text-white/30">No searches logged yet — once buyers start searching, this will fill in.</p>
        ) : (
          <div className="space-y-2">
            {status.top_searched_terms.map((t) => {
              const max = status.top_searched_terms[0].count;
              return (
                <div key={t.term} className="flex items-center gap-3">
                  <span className="text-sm text-white/60 w-40 truncate">"{t.term}"</span>
                  <div className="flex-1 h-2 bg-white/5 rounded-full overflow-hidden">
                    <div className="h-full bg-teal rounded-full" style={{ width: `${(t.count / max) * 100}%` }} />
                  </div>
                  <span className="text-xs text-white/40 w-8 text-right">{t.count}</span>
                </div>
              );
            })}
          </div>
        )}
      </div>

      <div className="bg-white/5 rounded-xl p-5 text-sm text-white/55 leading-relaxed">
        <p className="font-semibold text-white mb-1">How this works</p>
        <p>
          Every verified listing's title, description and category are converted into a TF-IDF vector — a numeric
          representation of what the listing is about. When a buyer searches or views a listing, that's logged and
          combined into a "profile" the next time recommendations are requested. Cosine similarity ranks all listings
          against that profile, and the closest matches are shown as "Recommended for you" on the homepage. The model
          automatically retrains shortly after each new search, or can be retrained manually here at any time.
        </p>
      </div>
    </div>
  );
}
