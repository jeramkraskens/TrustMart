// Messaging screen for reading conversations and sending messages to another user.
import { useEffect, useMemo, useState, useCallback, useRef } from 'react';
import { connectMessages } from '../api/liveMessages';
import { useSearchParams } from 'react-router-dom';
import client from '../api/client';
import { getAll } from '../api/marketplace';
import useRecordSearch from '../components/useRecordSearch';
import { useAuth } from '../context/AuthContext';

export default function Messages() {
  const { user } = useAuth();
  const [params] = useSearchParams();
  const [messages, setMessages] = useState([]);
  const [activeUserId, setActiveUserId] = useState(() => {
    const receiver = Number(params.get('with'));
    return Number.isSafeInteger(receiver) && receiver > 0 && receiver !== user.id ? receiver : null;
  });
  const [text, setText] = useState('');
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [sending, setSending] = useState(false);
  const [status, setStatus] = useState('Connecting…');
  const end = useRef(null);
  const loadingRef = useRef(false);
  const pending = useRef(false);
  const mounted = useRef(false);

  const load = useCallback(async () => {
    if (loadingRef.current) { pending.current = true; return; }
    loadingRef.current = true;
    do {
      pending.current = false;
      try {
        const data = await getAll('/messages/');
        if (mounted.current) { setMessages(data); setLoadError(''); }
      } catch {
        if (mounted.current) setLoadError('Could not update messages. Retrying automatically.');
      } finally {
        if (mounted.current) setLoading(false);
      }
    } while (pending.current && mounted.current);
    loadingRef.current = false;
  }, []);

  useEffect(() => {
    mounted.current = true;
    load();
    const disconnect = connectMessages(load, setStatus);
    return () => { mounted.current = false; disconnect(); };
  }, [load]);

  useEffect(() => { end.current?.scrollIntoView({ behavior: 'smooth' }); }, [messages.length, activeUserId]);

  const conversations = useMemo(() => {
    const map = new Map();
    messages.forEach((m) => {
      const otherId = m.sender === user.id ? m.receiver : m.sender;
      const otherName = m.sender === user.id ? m.receiver_name : m.sender_name;
      if (!map.has(otherId)) map.set(otherId, { id: otherId, name: otherName, last: m });
      else map.set(otherId, { id: otherId, name: otherName, last: m });
    });
    return Array.from(map.values()).sort((a, b) => b.last.sent_at.localeCompare(a.last.sent_at));
  }, [messages, user]);
  const { filtered: matchingConversations, searchControl } = useRecordSearch(conversations, ['name', 'last.content'], 'Search conversations');

  const thread = messages.filter((m) => m.sender === activeUserId || m.receiver === activeUserId);

  const send = async (e) => {
    e.preventDefault();
    if (!text.trim() || !activeUserId || sending) return;
    const draft = text;
    setSending(true);
    try {
      await client.post('/messages/', { receiver: activeUserId, content: draft.trim() });
      setText(current => current === draft ? '' : current);
      await load();
    } catch {
      setLoadError('Message was not sent. Please try again.');
    } finally { setSending(false); }
  };

  if (loading) return <div className="max-w-5xl mx-auto px-6 py-10 text-ink/50">Loading...</div>;

  return (
    <div className="max-w-5xl mx-auto px-6 py-10">
      <h1 className="font-bold tracking-tight text-3xl mb-6">Messages</h1>
      <p className="text-sm mb-3" role="status">{status}</p>
      {searchControl}
      {loadError && <p className="form-error" role="alert">{loadError}</p>}
      <div className="grid md:grid-cols-3 gap-6 h-[500px]">
        <div className="border border-ink/10 rounded-xl bg-white overflow-y-auto">
          {conversations.length === 0 && <p className="p-4 text-sm text-ink/50">No conversations yet.</p>}
          {matchingConversations.map((c) => (
            <button
              key={c.id}
              onClick={() => setActiveUserId(c.id)}
              className={`w-full text-left px-4 py-3 border-b border-ink/5 hover:bg-paper-warm ${activeUserId === c.id ? 'bg-paper-warm' : ''}`}
            >
              <p className="text-sm font-medium">{c.name}</p>
              <p className="text-xs text-ink/50 truncate">{c.last.content}</p>
            </button>
          ))}
        </div>
        <div className="md:col-span-2 border border-ink/10 rounded-xl bg-white flex flex-col">
          {activeUserId ? (
            <>
              <p className="px-4 pt-3 font-medium">{conversations.find(c => c.id === activeUserId)?.name || 'New conversation'}</p>
              <div className="flex-1 overflow-y-auto p-4 space-y-2">
                {thread.map((m) => (
                  <div key={m.id} className={`max-w-[75%] px-3 py-2 rounded-lg text-sm ${m.sender === user.id ? 'ml-auto bg-teal text-white' : 'bg-paper-warm'}`}>
                    {m.content}
                  </div>
                ))}
                <div ref={end} />
              </div>
              <form onSubmit={send} className="flex gap-2 p-3 border-t border-ink/10">
                <input value={text} onChange={(e) => setText(e.target.value)} placeholder="Type a message..."
                  className="flex-1 px-3 py-2 rounded-lg border border-ink/15 text-sm focus:outline-none focus:border-brass" />
                <button type="submit" disabled={sending || !text.trim()} className="px-4 py-2 rounded-lg bg-teal text-white text-sm">{sending ? 'Sending…' : 'Send'}</button>
              </form>
            </>
          ) : (
            <div className="flex-1 flex items-center justify-center text-ink/40 text-sm">Select a conversation</div>
          )}
        </div>
      </div>
    </div>
  );
}
