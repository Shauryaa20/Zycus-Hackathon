import React, { useEffect, useState, useCallback } from 'react';
import {
  getProducts, getPendingPricing, getPendingReorder,
  simulateSale, updatePricing, updateReorder,
  getStrategy, setStrategy, updateStock,
} from '../api';
import {
  RefreshCw, TrendingUp, Check, X, Package,
  Cpu, BarChart2, ShoppingCart, ArrowUp, ArrowDown,
  Minus, Zap, Bot, Layers, AlertTriangle, PlusCircle,
} from 'lucide-react';

/* ── Types ──────────────────────────────────────────────────────────── */
interface Product {
  id: string; name: string; sku: string;
  currentPrice: number; stockLevel: number; reorderThreshold: number;
  demandVelocity: number;
  status: 'ACTIVE' | 'PRICE_REVIEW_PENDING' | 'OUT_OF_STOCK';
  category: string;
}
interface PS {
  id: number; product: Product;
  currentPrice: number; recommendedPrice: number;
  changeDirection: 'INCREASE' | 'DECREASE' | 'HOLD';
  confidence: number; reasoning: string; triggerReason: string;
}
interface RS {
  id: number; product: Product;
  currentStock: number; recommendedQuantity: number;
  confidence: number; reasoning: string; triggerReason: string;
}

/* ── Constants ──────────────────────────────────────────────────────── */
const T: Record<string, { bg: string; border: string; text: string; dot: string }> = {
  INVENTORY_LOW: { bg: 'rgba(245,158,11,.12)', border: 'rgba(245,158,11,.3)', text: '#fbbf24', dot: '#f59e0b' },
  DEMAND_SPIKE:  { bg: 'rgba(99,102,241,.12)', border: 'rgba(99,102,241,.3)', text: '#818cf8', dot: '#6366f1' },
  MANUAL:        { bg: 'rgba(100,116,139,.1)', border: 'rgba(100,116,139,.2)', text: '#94a3b8', dot: '#64748b' },
};
const tc = (r: string) => T[r] ?? T.MANUAL;

const DIR = {
  INCREASE: { bg: 'rgba(16,185,129,.15)', fg: '#34d399', bd: 'rgba(16,185,129,.25)', icon: <ArrowUp  style={{width:10,height:10}}/> },
  DECREASE: { bg: 'rgba(244,63,94,.15)',  fg: '#fb7185', bd: 'rgba(244,63,94,.25)',  icon: <ArrowDown style={{width:10,height:10}}/> },
  HOLD:     { bg: 'rgba(100,116,139,.15)',fg: '#94a3b8', bd: 'rgba(100,116,139,.2)',icon: <Minus     style={{width:10,height:10}}/> },
} as const;
const dc = (d: string) => DIR[d as keyof typeof DIR] ?? DIR.HOLD;

/* ── Reusable pieces ─────────────────────────────────────────────────── */
const css = String.raw;
const GLOBAL = css`
  @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap');
  *{box-sizing:border-box;margin:0;padding:0}
  body{font-family:'Inter',ui-sans-serif,system-ui,sans-serif;background:#0a0d14;color:#f1f5f9;-webkit-font-smoothing:antialiased}
  ::-webkit-scrollbar{width:5px;height:5px}
  ::-webkit-scrollbar-track{background:transparent}
  ::-webkit-scrollbar-thumb{background:rgba(99,102,241,.35);border-radius:999px}
  @keyframes spin{to{transform:rotate(360deg)}}
  @keyframes pulseA{0%,100%{opacity:1}50%{opacity:.45}}
  @keyframes slideUp{from{opacity:0;transform:translateY(-6px)}to{opacity:1;transform:translateY(0)}}
`;

const Stat = ({ icon: Icon, label, value, accent }: { icon: any; label: string; value: number | string; accent: string }) => (
  <div style={{ background: '#111827', border: '1px solid rgba(255,255,255,0.07)', borderRadius: 14, padding: '16px 18px', display: 'flex', alignItems: 'center', gap: 14 }}>
    <div style={{ background: `${accent}1a`, border: `1px solid ${accent}2a`, borderRadius: 10, padding: 10, display: 'flex' }}>
      <Icon style={{ width: 18, height: 18, color: accent }} />
    </div>
    <div>
      <div style={{ fontSize: 26, fontWeight: 800, color: '#f1f5f9', letterSpacing: '-0.025em', lineHeight: 1 }}>{value}</div>
      <div style={{ fontSize: 10.5, color: '#475569', fontWeight: 600, marginTop: 4, textTransform: 'uppercase', letterSpacing: '.07em' }}>{label}</div>
    </div>
  </div>
);

const Card = ({
  kind, trigger, name, confidence, reasoning, onAccept, onReject, children,
}: {
  kind: 'PRICING' | 'REORDER'; trigger: string; name: string;
  confidence: number; reasoning: string;
  onAccept(): void; onReject(): void;
  children: React.ReactNode;
}) => {
  const t = tc(trigger);
  return (
    <div style={{ background: '#111827', border: `1px solid ${t.border}`, borderRadius: 12, overflow: 'visible' }}>
      {/* header */}
      <div style={{ background: t.bg, padding: '8px 14px', borderRadius: '12px 12px 0 0', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <span style={{ width: 6, height: 6, borderRadius: '50%', background: t.dot, display: 'inline-block' }} />
          <span style={{ fontSize: 10, fontWeight: 700, letterSpacing: '.08em', textTransform: 'uppercase', color: t.text }}>
            {kind} · {trigger.replace('_', ' ')}
          </span>
        </span>
        <span style={{ fontSize: 10, fontWeight: 600, color: t.text, opacity: .85 }}>{(confidence * 100).toFixed(0)}% CONF</span>
      </div>
      {/* body */}
      <div style={{ padding: '12px 14px' }}>
        <div style={{ fontSize: 13, fontWeight: 600, color: '#e2e8f0', marginBottom: 8 }}>{name}</div>
        {children}
        {/* reasoning — capped at 2 lines */}
        <div style={{
          fontSize: 11.5, color: '#64748b', lineHeight: 1.55,
          background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.06)',
          borderRadius: 7, padding: '8px 10px', margin: '10px 0',
          display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden',
        }}>
          {reasoning}
        </div>
        {/* ALWAYS-VISIBLE action buttons */}
        <div style={{ display: 'flex', gap: 8 }}>
          <button onClick={onAccept} style={{
            flex: 1, background: 'linear-gradient(135deg,#10b981,#059669)',
            color: '#fff', border: 'none', borderRadius: 8,
            padding: '10px 0', fontSize: 12.5, fontWeight: 700,
            cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 5,
          }}>
            <Check style={{ width: 13, height: 13 }} /> Accept
          </button>
          <button onClick={onReject} style={{
            flex: 1, background: 'rgba(244,63,94,0.1)', color: '#fb7185',
            border: '1px solid rgba(244,63,94,0.25)', borderRadius: 8,
            padding: '10px 0', fontSize: 12.5, fontWeight: 700,
            cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 5,
          }}>
            <X style={{ width: 13, height: 13 }} /> Reject
          </button>
        </div>
      </div>
    </div>
  );
};

const Toast = ({ msg, accent }: { msg: string; accent: string }) => (
  <div style={{
    position: 'fixed', top: 20, right: 20, zIndex: 9999,
    background: '#111827', border: `1px solid ${accent}40`, borderLeft: `3px solid ${accent}`,
    borderRadius: 10, padding: '11px 16px', color: '#f1f5f9',
    fontSize: 13, fontWeight: 500, maxWidth: 300, lineHeight: 1.4,
    boxShadow: '0 8px 32px rgba(0,0,0,.5)', animation: 'slideUp .25s ease',
  }}>
    {msg}
  </div>
);

/* ── Dashboard ─────────────────────────────────────────────────────── */
export default function Dashboard() {
  const [products, setProducts] = useState<Product[]>([]);
  const [pricing,  setPricing]  = useState<PS[]>([]);
  const [reorder,  setReorder]  = useState<RS[]>([]);
  const [loading,  setLoading]  = useState(true);
  const [spinning, setSpinning] = useState(false);
  const [strategy, setStrategy_] = useState('RULE');
  const [toast, setToast] = useState<{ msg: string; accent: string } | null>(null);

  const notify = (msg: string, accent = '#10b981') => {
    setToast({ msg, accent });
    setTimeout(() => setToast(null), 2600);
  };

  const load = useCallback(async (quiet = false) => {
    if (!quiet) setSpinning(true);
    try {
      const [p, ps, rs, st] = await Promise.all([
        getProducts(), getPendingPricing(), getPendingReorder(), getStrategy(),
      ]);
      setProducts(p.data);
      setPricing(ps.data);
      setReorder(rs.data);
      setStrategy_(st.data.activeStrategy);
    } catch (e) { console.error(e); }
    finally { setLoading(false); setSpinning(false); }
  }, []);

  useEffect(() => { load(); const id = setInterval(() => load(true), 4000); return () => clearInterval(id); }, [load]);

  const switchStrat = async (s: string) => {
    await setStrategy(s);
    setStrategy_(s);
    notify(`Strategy → ${s === 'AI' ? '🤖 AI Advisor' : '📐 Rule Engine'}`, '#818cf8');
  };

  const doAcceptPrice = async (s: PS) => { await updatePricing(s.id, 'ACCEPTED'); notify(`✓ Price → $${s.recommendedPrice.toFixed(2)} for ${s.product.name}`); load(true); };
  const doRejectPrice = async (s: PS) => { await updatePricing(s.id, 'REJECTED'); notify(`Rejected pricing for ${s.product.name}`, '#f43f5e'); load(true); };
  const doAcceptOrder = async (s: RS) => { await updateReorder(s.id, 'ACCEPTED'); notify(`✓ Reorder +${s.recommendedQuantity} units for ${s.product.name}`); load(true); };
  const doRejectOrder = async (s: RS) => { await updateReorder(s.id, 'REJECTED'); notify(`Rejected reorder for ${s.product.name}`, '#f43f5e'); load(true); };

  if (loading) return (
    <div style={{ display: 'flex', height: '100vh', alignItems: 'center', justifyContent: 'center', background: '#0a0d14', flexDirection: 'column', gap: 16 }}>
      <style>{GLOBAL}</style>
      <div style={{ width: 40, height: 40, border: '3px solid rgba(99,102,241,.2)', borderTop: '3px solid #6366f1', borderRadius: '50%', animation: 'spin .8s linear infinite' }} />
      <p style={{ color: '#6366f1', fontSize: 13, fontWeight: 600, letterSpacing: '.06em' }}>LOADING STOCKPULSE</p>
    </div>
  );

  const lowStock = products.filter(p => p.stockLevel < p.reorderThreshold).length;
  const pending  = pricing.length + reorder.length;
  const oos      = products.filter(p => p.status === 'OUT_OF_STOCK').length;
  const HEADER_H = 56;

  return (
    <>
      <style>{GLOBAL}</style>
      {toast && <Toast msg={toast.msg} accent={toast.accent} />}

      {/* ── Fixed top bar ── */}
      <div style={{
        position: 'fixed', top: 0, left: 0, right: 0, height: HEADER_H, zIndex: 200,
        background: 'rgba(10,13,20,.97)', backdropFilter: 'blur(18px)',
        borderBottom: '1px solid rgba(255,255,255,0.07)',
        display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0 28px',
      }}>
        {/* Logo */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <div style={{ background: 'linear-gradient(135deg,#6366f1,#8b5cf6)', borderRadius: 10, padding: '7px 9px', display: 'flex' }}>
            <Zap style={{ width: 18, height: 18, color: '#fff' }} />
          </div>
          <div>
            <div style={{ fontSize: 16, fontWeight: 800, color: '#f1f5f9', letterSpacing: '-0.02em' }}>StockPulse</div>
            <div style={{ fontSize: 9.5, color: '#475569', fontWeight: 600, letterSpacing: '.07em', textTransform: 'uppercase' }}>AI Commerce Engine</div>
          </div>
          <div style={{ marginLeft: 8, display: 'flex', alignItems: 'center', gap: 5, background: 'rgba(16,185,129,0.1)', border: '1px solid rgba(16,185,129,0.2)', borderRadius: 999, padding: '3px 10px' }}>
            <span style={{ width: 6, height: 6, borderRadius: '50%', background: '#10b981', display: 'block', animation: 'pulseA 2s infinite' }} />
            <span style={{ fontSize: 10, color: '#34d399', fontWeight: 700, letterSpacing: '.05em' }}>LIVE</span>
          </div>
        </div>
        {/* Controls */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <div style={{ display: 'flex', background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.08)', borderRadius: 9, padding: 3, gap: 3 }}>
            {(['RULE', 'AI'] as const).map(s => (
              <button key={s} onClick={() => switchStrat(s)} style={{
                padding: '5px 14px', borderRadius: 6, border: 'none', cursor: 'pointer',
                fontSize: 12, fontWeight: 700, letterSpacing: '.03em',
                background: strategy === s ? 'linear-gradient(135deg,#6366f1,#8b5cf6)' : 'transparent',
                color: strategy === s ? '#fff' : '#64748b',
                boxShadow: strategy === s ? '0 2px 8px rgba(99,102,241,.35)' : 'none',
                display: 'flex', alignItems: 'center', gap: 5,
              }}>
                {s === 'AI' ? <Bot style={{ width: 12, height: 12 }} /> : <Cpu style={{ width: 12, height: 12 }} />} {s}
              </button>
            ))}
          </div>
          <button onClick={() => load()} style={{
            display: 'flex', alignItems: 'center', gap: 6,
            background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.08)',
            borderRadius: 8, padding: '7px 14px', color: '#94a3b8',
            fontSize: 12.5, fontWeight: 600, cursor: 'pointer',
          }}>
            <RefreshCw style={{ width: 13, height: 13, animation: spinning ? 'spin .7s linear infinite' : 'none' }} /> Refresh
          </button>
        </div>
      </div>

      {/* ── Page body (scrolls under fixed header) ── */}
      <div style={{ paddingTop: HEADER_H, minHeight: '100vh', background: '#0a0d14' }}>
        <div style={{ maxWidth: 1400, margin: '0 auto', padding: '20px 28px 40px' }}>

          {/* Stats */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4,1fr)', gap: 14, marginBottom: 24 }}>
            <Stat icon={Package}       label="Total Products"  value={products.length} accent="#818cf8" />
            <Stat icon={AlertTriangle} label="Low Stock"        value={lowStock}        accent="#fbbf24" />
            <Stat icon={ShoppingCart}  label="Pending Actions"  value={pending}         accent="#10b981" />
            <Stat icon={BarChart2}     label="Out of Stock"     value={oos}             accent="#fb7185" />
          </div>

          {/* Two-column grid */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 390px', gap: 22, alignItems: 'start' }}>

            {/* ── Product table (normal flow, full height) ── */}
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 12 }}>
                <Layers style={{ width: 15, height: 15, color: '#6366f1' }} />
                <span style={{ fontSize: 14, fontWeight: 700, color: '#f1f5f9' }}>Product Catalog</span>
                <span style={{ fontSize: 11, color: '#475569' }}>{products.length} items</span>
              </div>
              <div style={{ background: '#111827', border: '1px solid rgba(255,255,255,0.07)', borderRadius: 14, overflow: 'hidden' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                  <thead>
                    <tr style={{ background: 'rgba(255,255,255,0.02)' }}>
                      {['Product', 'Price', 'Stock / Min', 'Velocity', 'Status', 'Action'].map((h, i) => (
                        <th key={h} style={{ padding: '11px 14px', textAlign: i === 5 ? 'right' : 'left', fontSize: 10, fontWeight: 700, letterSpacing: '.08em', textTransform: 'uppercase', color: '#334155', borderBottom: '1px solid rgba(255,255,255,0.06)', whiteSpace: 'nowrap' }}>{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {products.map((p, i) => {
                      const low    = p.stockLevel < p.reorderThreshold;
                      const oos_   = p.status === 'OUT_OF_STOCK';
                      const review = p.status === 'PRICE_REVIEW_PENDING';
                      const rowBg  = review ? 'rgba(245,158,11,0.04)' : i % 2 === 0 ? 'transparent' : 'rgba(255,255,255,0.01)';
                      return (
                        <tr key={p.id} style={{ background: rowBg }}>
                          <td style={{ padding: '12px 14px', borderBottom: '1px solid rgba(255,255,255,0.04)' }}>
                            <div style={{ fontSize: 13, fontWeight: 600, color: '#e2e8f0' }}>{p.name}</div>
                            <div style={{ fontSize: 10.5, color: '#475569', marginTop: 2 }}>{p.sku}</div>
                          </td>
                          <td style={{ padding: '12px 14px', borderBottom: '1px solid rgba(255,255,255,0.04)', fontSize: 13.5, fontWeight: 700, color: '#f1f5f9', whiteSpace: 'nowrap' }}>
                            ${p.currentPrice.toFixed(2)}
                          </td>
                          <td style={{ padding: '12px 14px', borderBottom: '1px solid rgba(255,255,255,0.04)', whiteSpace: 'nowrap' }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: 5 }}>
                              <span style={{ fontWeight: 700, fontSize: 13.5, color: low ? '#fb7185' : '#e2e8f0' }}>{p.stockLevel}</span>
                              <span style={{ color: '#334155' }}>/</span>
                              <span style={{ color: '#475569', fontSize: 11.5 }}>{p.reorderThreshold}</span>
                              {low && <span style={{ fontSize: 9.5, background: 'rgba(244,63,94,.15)', color: '#fb7185', padding: '1px 6px', borderRadius: 999, border: '1px solid rgba(244,63,94,.25)', fontWeight: 700 }}>LOW</span>}
                            </div>
                          </td>
                          <td style={{ padding: '12px 14px', borderBottom: '1px solid rgba(255,255,255,0.04)', whiteSpace: 'nowrap' }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: 5, color: '#94a3b8', fontSize: 12.5 }}>
                              <TrendingUp style={{ width: 12, height: 12, color: p.demandVelocity > 8 ? '#818cf8' : '#334155' }} />
                              {p.demandVelocity}/24h
                            </div>
                          </td>
                          <td style={{ padding: '12px 14px', borderBottom: '1px solid rgba(255,255,255,0.04)', whiteSpace: 'nowrap' }}>
                            <span style={{
                              display: 'inline-flex', alignItems: 'center', padding: '3px 10px',
                              borderRadius: 999, fontSize: 10.5, fontWeight: 700, letterSpacing: '.04em',
                              ...(oos_   ? { background: 'rgba(244,63,94,.12)',  color: '#fb7185', border: '1px solid rgba(244,63,94,.2)' }
                                : review ? { background: 'rgba(245,158,11,.12)', color: '#fbbf24', border: '1px solid rgba(245,158,11,.2)' }
                                         : { background: 'rgba(16,185,129,.12)', color: '#34d399', border: '1px solid rgba(16,185,129,.2)' }),
                            }}>
                              ● {oos_ ? 'Out of Stock' : review ? 'Review' : 'Active'}
                            </span>
                          </td>
                          <td style={{ padding: '12px 14px', borderBottom: '1px solid rgba(255,255,255,0.04)', textAlign: 'right', whiteSpace: 'nowrap' }}>
                            <div style={{ display: 'inline-flex', gap: 6, justifyContent: 'flex-end' }}>
                              <button
                                disabled={oos_}
                                onClick={async () => {
                                  await simulateSale(p.id);
                                  notify(`Sale simulated: ${p.name}`, '#6366f1');
                                  load(true);
                                }}
                                style={{
                                  background: oos_ ? 'rgba(255,255,255,.03)' : 'rgba(99,102,241,.15)',
                                  color: oos_ ? '#334155' : '#818cf8',
                                  border: `1px solid ${oos_ ? 'rgba(255,255,255,.06)' : 'rgba(99,102,241,.3)'}`,
                                  borderRadius: 7, padding: '6px 10px', fontSize: 11.5, fontWeight: 600,
                                  cursor: oos_ ? 'not-allowed' : 'pointer',
                                  display: 'inline-flex', alignItems: 'center', gap: 4, whiteSpace: 'nowrap',
                                }}
                              >
                                <ShoppingCart style={{ width: 11, height: 11 }} /> Sale
                              </button>
                              <button
                                onClick={async () => {
                                  const input = window.prompt(`Enter quantity to restock for ${p.name}:`, '50');
                                  if (!input) return;
                                  const qty = parseInt(input.trim(), 10);
                                  if (isNaN(qty) || qty <= 0) return;
                                  await updateStock(p.id, p.stockLevel + qty);
                                  notify(`Restocked +${qty} units for ${p.name}`, '#10b981');
                                  load(true);
                                }}
                                style={{
                                  background: 'rgba(16,185,129,.15)',
                                  color: '#34d399',
                                  border: '1px solid rgba(16,185,129,.3)',
                                  borderRadius: 7, padding: '6px 10px', fontSize: 11.5, fontWeight: 600,
                                  cursor: 'pointer',
                                  display: 'inline-flex', alignItems: 'center', gap: 4, whiteSpace: 'nowrap',
                                }}
                              >
                                <PlusCircle style={{ width: 11, height: 11 }} /> Restock
                              </button>
                            </div>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </div>

            {/* ── Advisor panel (sticky sidebar) ── */}
            <div style={{ position: 'sticky', top: HEADER_H + 20 }}>
              {/* Header */}
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
                <span style={{ display: 'flex', alignItems: 'center', gap: 7, fontSize: 14, fontWeight: 700, color: '#f1f5f9' }}>
                  <Bot style={{ width: 15, height: 15, color: '#818cf8' }} />
                  Agentic Advisor
                  {pending > 0 && (
                    <span style={{ fontSize: 10.5, background: 'rgba(99,102,241,.2)', color: '#818cf8', border: '1px solid rgba(99,102,241,.3)', padding: '2px 8px', borderRadius: 999, fontWeight: 700 }}>
                      {pending}
                    </span>
                  )}
                </span>
                <span style={{ fontSize: 10.5, color: '#475569', background: 'rgba(255,255,255,.04)', border: '1px solid rgba(255,255,255,.07)', padding: '3px 10px', borderRadius: 999 }}>
                  {strategy === 'AI' ? '🤖 AI' : '📐 Rules'}
                </span>
              </div>

              {/* Scrollable suggestion list — max 80vh so it never exceeds viewport */}
              <div style={{ display: 'flex', flexDirection: 'column', gap: 12, maxHeight: `calc(100vh - ${HEADER_H + 120}px)`, overflowY: 'auto', paddingRight: 2, paddingBottom: 8 }}>
                {pricing.length === 0 && reorder.length === 0 ? (
                  <div style={{ background: '#111827', border: '1px solid rgba(255,255,255,0.07)', borderRadius: 14, padding: '44px 24px', textAlign: 'center' }}>
                    <TrendingUp style={{ width: 36, height: 36, color: '#1e2d40', display: 'block', margin: '0 auto 12px' }} />
                    <p style={{ color: '#475569', fontSize: 13.5, fontWeight: 500 }}>No pending suggestions</p>
                    <p style={{ color: '#334155', fontSize: 11.5, marginTop: 6 }}>Simulate a sale to trigger the agentic loop</p>
                  </div>
                ) : (
                  <>
                    {pricing.map(s => {
                      const d = dc(s.changeDirection);
                      const pct = ((s.recommendedPrice - s.currentPrice) / s.currentPrice * 100).toFixed(1);
                      return (
                        <Card key={s.id} kind="PRICING" trigger={s.triggerReason} name={s.product.name}
                          confidence={s.confidence} reasoning={s.reasoning}
                          onAccept={() => doAcceptPrice(s)} onReject={() => doRejectPrice(s)}>
                          <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap', marginBottom: 2 }}>
                            <span style={{ fontSize: 12, color: '#64748b', textDecoration: 'line-through' }}>${s.currentPrice.toFixed(2)}</span>
                            <span style={{ fontSize: 20, fontWeight: 800, color: '#f1f5f9', letterSpacing: '-0.025em' }}>${s.recommendedPrice.toFixed(2)}</span>
                            <span style={{ display: 'inline-flex', alignItems: 'center', gap: 3, fontSize: 10.5, fontWeight: 700, padding: '2px 8px', borderRadius: 999, background: d.bg, color: d.fg, border: `1px solid ${d.bd}` }}>
                              {d.icon} {pct}%
                            </span>
                          </div>
                        </Card>
                      );
                    })}
                    {reorder.map(s => (
                      <Card key={s.id} kind="REORDER" trigger={s.triggerReason} name={s.product.name}
                        confidence={s.confidence} reasoning={s.reasoning}
                        onAccept={() => doAcceptOrder(s)} onReject={() => doRejectOrder(s)}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 2 }}>
                          <span style={{ fontSize: 12, color: '#64748b' }}>Stock: {s.currentStock}</span>
                          <span style={{ fontSize: 18, fontWeight: 800, color: '#10b981', letterSpacing: '-0.02em' }}>+{s.recommendedQuantity} units</span>
                        </div>
                      </Card>
                    ))}
                  </>
                )}
              </div>
            </div>

          </div>
        </div>
      </div>
    </>
  );
}
