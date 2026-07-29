import React, { useEffect, useState } from 'react';
import clsx from 'clsx';
import { useTranslation } from 'react-i18next';

/* ------------------------------------------------------------------ */
/* 基础原子：Button / Input / Textarea / Tag / Spinner / Modal         */
/* ------------------------------------------------------------------ */

type ButtonVariant = 'default' | 'primary' | 'ai' | 'danger' | 'ghost';
const BTN_VARIANT: Record<ButtonVariant, string> = {
  default: 'bg-white border border-line text-ink hover:border-brand-500 hover:text-brand-700 shadow-xs',
  primary: 'bg-brand-700 text-white hover:bg-brand-600 border border-brand-700',
  ai: 'bg-ai-500 text-white hover:bg-ai-600 border border-ai-500',
  danger: 'bg-safety text-white hover:brightness-95 border border-safety',
  ghost: 'bg-transparent border-transparent text-ink2 hover:bg-brand-soft hover:text-brand-700 shadow-none',
};

export function Button({
  variant = 'default',
  size,
  className,
  children,
  ...props
}: React.ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant;
  size?: 'sm';
}) {
  return (
    <button
      className={clsx(
        'inline-flex items-center justify-center gap-1.5 rounded-xl font-semibold transition active:translate-y-px disabled:cursor-not-allowed disabled:opacity-50',
        size === 'sm' ? 'h-8 px-3 text-[13px]' : 'h-10 px-4 text-sm',
        BTN_VARIANT[variant],
        className,
      )}
      {...props}
    >
      {children}
    </button>
  );
}

export function Input({
  className,
  ...props
}: React.InputHTMLAttributes<HTMLInputElement>) {
  return (
    <input
      className={clsx(
        'h-10 w-full rounded-xl border border-line bg-white px-3 text-sm text-ink outline-none transition focus:border-brand-500 focus:ring-2 focus:ring-brand-soft',
        className,
      )}
      {...props}
    />
  );
}

export function Textarea({
  className,
  ...props
}: React.TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return (
    <textarea
      className={clsx(
        'w-full rounded-xl border border-line bg-white px-3 py-2 text-sm text-ink outline-none transition focus:border-brand-500 focus:ring-2 focus:ring-brand-soft',
        className,
      )}
      {...props}
    />
  );
}

type TagTone = 'ai' | 'human' | 'ok' | 'warn' | 'danger' | 'bad' | 'mute' | 'info' | 'safety';
const TAG_TONE: Record<TagTone, string> = {
  ai: 'bg-ai-soft text-ai-600',
  human: 'bg-human-soft text-human-600',
  ok: 'bg-ok-bg text-ok',
  warn: 'bg-warn-bg text-warn',
  danger: 'bg-danger-bg text-danger',
  bad: 'bg-danger-bg text-danger',
  mute: 'bg-slate-100 text-ink2',
  info: 'bg-info-bg text-info',
  safety: 'bg-safety-bg text-safety',
};
const TAG_DOT: Partial<Record<TagTone, string>> = {
  ai: 'bg-ai-600',
  human: 'bg-human-600',
  ok: 'bg-ok',
  warn: 'bg-warn',
  danger: 'bg-danger',
  info: 'bg-info',
  safety: 'bg-safety',
};

export function Tag({
  tone = 'mute',
  className,
  children,
}: {
  tone?: TagTone;
  className?: string;
  children: React.ReactNode;
}) {
  return (
    <span
      className={clsx(
        'inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-bold',
        TAG_TONE[tone],
        className,
      )}
    >
      {TAG_DOT[tone] && <span className={clsx('h-1.5 w-1.5 rounded-full', TAG_DOT[tone])} />}
      {children}
    </span>
  );
}

export function Spinner({ className = 'h-4 w-4' }: { className?: string }) {
  return (
    <span
      className={clsx('inline-block animate-spin rounded-full border-2 border-line border-t-brand-700', className)}
    />
  );
}

export function Modal({
  open,
  title,
  onClose,
  footer,
  children,
  widthClass = 'max-w-md',
}: {
  open: boolean;
  title?: React.ReactNode;
  onClose: () => void;
  footer?: React.ReactNode;
  children: React.ReactNode;
  widthClass?: string;
}) {
  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose();
    document.addEventListener('keydown', onKey);
    document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', onKey);
      document.body.style.overflow = '';
    };
  }, [open, onClose]);

  if (!open) return null;
  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4 enter-pop"
      onMouseDown={onClose}
    >
      <div
        className={clsx('w-full rounded-2xl border border-line bg-white shadow-pop', widthClass)}
        onMouseDown={(e) => e.stopPropagation()}
      >
        {title && (
          <div className="flex items-center justify-between border-b border-line px-5 py-3.5">
            <h3 className="font-display text-base font-bold text-ink">{title}</h3>
            <button
              onClick={onClose}
              aria-label="close"
              className="grid h-8 w-8 place-items-center rounded-lg text-ink3 transition hover:bg-brand-soft hover:text-brand-700"
            >
              ✕
            </button>
          </div>
        )}
        <div className="px-5 py-4">{children}</div>
        {footer && (
          <div className="flex justify-end gap-2 border-t border-line px-5 py-3.5">{footer}</div>
        )}
      </div>
    </div>
  );
}

/* ------------------------------------------------------------------ */
/* 结构与反馈：Card / EmptyState / Banner / Drawer / StatusFlow        */
/* ------------------------------------------------------------------ */

export function Card({
  className,
  interactive,
  onClick,
  children,
}: {
  className?: string;
  interactive?: boolean;
  onClick?: () => void;
  children: React.ReactNode;
}) {
  return (
    <div
      onClick={onClick}
      className={clsx(
        'rounded-2xl border border-line bg-white shadow-card',
        interactive && 'transition hover:-translate-y-0.5 hover:shadow-pop',
        onClick && 'cursor-pointer',
        className,
      )}
    >
      {children}
    </div>
  );
}

export function EmptyState({
  title,
  hint,
  action,
  icon,
}: {
  title: React.ReactNode;
  hint?: React.ReactNode;
  action?: React.ReactNode;
  icon?: React.ReactNode;
}) {
  return (
    <div className="flex flex-col items-center justify-center px-6 py-14 text-center">
      <div className="mb-4 grid h-16 w-16 place-items-center rounded-2xl bg-brand-soft text-brand-600">
        {icon ?? (
          <svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6">
            <rect x="3" y="4" width="18" height="14" rx="2" />
            <path d="M3 9h18M8 14h5" />
          </svg>
        )}
      </div>
      <div className="font-display text-base font-bold text-ink">{title}</div>
      {hint && <div className="mt-1.5 max-w-sm text-sm text-ink2">{hint}</div>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  );
}

type BannerTone = 'info' | 'warn' | 'danger' | 'safety' | 'ok';
const BANNER_TONE: Record<BannerTone, string> = {
  info: 'bg-info-bg text-info border-info/30',
  warn: 'bg-warn-bg text-warn border-warn/30',
  danger: 'bg-danger-bg text-danger border-danger/30',
  safety: 'bg-safety-bg text-safety border-safety/30',
  ok: 'bg-ok-bg text-ok border-ok/30',
};

export function Banner({
  tone = 'info',
  title,
  children,
  className,
}: {
  tone?: BannerTone;
  title?: React.ReactNode;
  children?: React.ReactNode;
  className?: string;
}) {
  return (
    <div className={clsx('flex gap-2.5 rounded-xl border px-4 py-3 text-sm', BANNER_TONE[tone], className)}>
      <span className="mt-0.5 font-bold">⚠</span>
      <div>
        {title && <div className="font-bold">{title}</div>}
        {children && <div className="opacity-90">{children}</div>}
      </div>
    </div>
  );
}

export function Drawer({
  open,
  title,
  onClose,
  children,
  widthClass = 'max-w-lg',
}: {
  open: boolean;
  title?: React.ReactNode;
  onClose: () => void;
  children: React.ReactNode;
  widthClass?: string;
}) {
  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose();
    document.addEventListener('keydown', onKey);
    document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', onKey);
      document.body.style.overflow = '';
    };
  }, [open, onClose]);

  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 flex justify-end bg-black/40 enter-pop" onMouseDown={onClose}>
      <div
        className={clsx('flex h-full w-full flex-col bg-white shadow-pop', widthClass)}
        onMouseDown={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between border-b border-line px-5 py-4">
          <h3 className="font-display text-base font-bold text-ink">{title}</h3>
          <button
            onClick={onClose}
            aria-label="close"
            className="grid h-8 w-8 place-items-center rounded-lg text-ink3 transition hover:bg-brand-soft hover:text-brand-700"
          >
            ✕
          </button>
        </div>
        <div className="flex-1 overflow-auto p-5">{children}</div>
      </div>
    </div>
  );
}

export function StatusFlow({ status }: { status: string }) {
  const { t } = useTranslation();
  const order = ['draft', 'review', 'approved', 'published'];
  const idx = order.indexOf(status);
  if (idx < 0) {
    return <span className="text-xs font-medium text-ink2">{t(`status.${status}`)}</span>;
  }
  return (
    <div className="flex items-center gap-1.5 text-xs">
      {order.map((s, i) => (
        <React.Fragment key={s}>
          <span className={i <= idx ? 'font-semibold text-ai-600' : 'text-ink3'}>{t(`status.${s}`)}</span>
          {i < order.length - 1 && <span className="text-ink3">›</span>}
        </React.Fragment>
      ))}
    </div>
  );
}

/* ------------------------------------------------------------------ */
/* 布局与数据：PageHeader / StatCard / Segmented / Table / Avatar      */
/* ------------------------------------------------------------------ */

export function PageHeader({
  title,
  subtitle,
  actions,
  icon,
}: {
  title: React.ReactNode;
  subtitle?: React.ReactNode;
  actions?: React.ReactNode;
  icon?: React.ReactNode;
}) {
  return (
    <div className="mb-5 flex flex-wrap items-end justify-between gap-3">
      <div className="flex items-center gap-3">
        {icon && (
          <div className="grid h-11 w-11 place-items-center rounded-2xl bg-brand-700 text-white shadow-card">
            {icon}
          </div>
        )}
        <div>
          <h1 className="font-display text-2xl font-bold leading-tight text-ink">{title}</h1>
          {subtitle && <p className="mt-0.5 text-sm text-ink2">{subtitle}</p>}
        </div>
      </div>
      {actions && <div className="flex flex-wrap items-center gap-2">{actions}</div>}
    </div>
  );
}

export function StatCard({
  label,
  value,
  delta,
  tone = 'brand',
  icon,
  spark,
}: {
  label: React.ReactNode;
  value: React.ReactNode;
  delta?: React.ReactNode;
  tone?: 'brand' | 'ai' | 'human' | 'safety' | 'ok';
  icon?: React.ReactNode;
  spark?: React.ReactNode;
}) {
  const toneText: Record<string, string> = {
    brand: 'text-brand-700',
    ai: 'text-ai-600',
    human: 'text-human-600',
    safety: 'text-safety',
    ok: 'text-ok',
  };
  return (
    <Card className="relative overflow-hidden p-4">
      <div className="flex items-start justify-between">
        <div className="text-xs font-semibold text-ink2">{label}</div>
        {icon && <div className="text-ink3">{icon}</div>}
      </div>
      <div className={clsx('mt-2 font-display text-[28px] font-extrabold leading-none', toneText[tone])}>
        {value}
      </div>
      <div className="mt-1 text-xs font-semibold text-ink3">{delta}</div>
      {spark && <div className="pointer-events-none absolute bottom-0 right-0 opacity-90">{spark}</div>}
    </Card>
  );
}

export function Segmented<T extends string>({
  options,
  value,
  onChange,
  className,
}: {
  options: { value: T; label: React.ReactNode }[];
  value: T;
  onChange: (v: T) => void;
  className?: string;
}) {
  return (
    <div className={clsx('flex gap-2', className)}>
      {options.map((o) => (
        <button
          key={o.value}
          onClick={() => onChange(o.value)}
          className={clsx(
            'flex-1 rounded-xl border px-3 py-2 text-sm font-semibold transition',
            value === o.value
              ? 'border-ai-500 bg-ai-500 text-white'
              : 'border-ai-100 bg-white text-brand-700 hover:bg-ai-soft',
          )}
        >
          {o.label}
        </button>
      ))}
    </div>
  );
}

export function Table({
  header,
  children,
  className,
}: {
  header?: React.ReactNode[];
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <div className="overflow-hidden rounded-2xl border border-line bg-white shadow-card">
      <table className={clsx('w-full text-sm', className)}>
        {header && (
          <thead>
            <tr className="bg-brand-soft/50 text-ink2">
              {header.map((h, i) => (
                <th key={i} className="border-b border-line px-4 py-2.5 text-left font-semibold">
                  {h}
                </th>
              ))}
            </tr>
          </thead>
        )}
        <tbody>{children}</tbody>
      </table>
    </div>
  );
}

export function Avatar({ name, size = 32 }: { name: string; size?: number }) {
  const initials = name
    .split(/\s+/)
    .map((s) => s[0])
    .slice(0, 2)
    .join('')
    .toUpperCase();
  return (
    <div
      style={{ width: size, height: size }}
      className="grid place-items-center rounded-xl bg-human-500 font-bold text-white"
    >
      <span style={{ fontSize: size * 0.4 }}>{initials}</span>
    </div>
  );
}

/* ------------------------------------------------------------------ */
/* 诊断进度轨：把状态机可视化（完成 / 进行中 / 风险 / 待执行）         */
/* ------------------------------------------------------------------ */

export type RailState = 'done' | 'current' | 'risk' | 'upcoming';
export interface RailStep {
  key: string;
  label: React.ReactNode;
  state?: RailState;
}

export function DiagnosisRail({
  steps,
  note,
}: {
  steps: RailStep[];
  note?: React.ReactNode;
}) {
  return (
    <div>
      {steps.map((s, i) => {
        const state = s.state ?? 'upcoming';
        const led =
          state === 'done'
            ? 'bg-ok border-ok'
            : state === 'current'
              ? 'bg-ai-500 border-ai-500 animate-pulse-ring'
              : state === 'risk'
                ? 'bg-safety border-safety'
                : 'bg-white border-line';
        return (
          <div key={s.key} className="relative flex gap-3 pb-4 last:pb-0">
            {i < steps.length - 1 && (
              <span className="absolute left-[7px] top-6 bottom-0 w-0.5 bg-line" />
            )}
            <span className={clsx('relative z-10 mt-1 h-4 w-4 flex-none rounded-full border-2', led)} />
            <div className="min-w-0">
              <div
                className={clsx(
                  'font-mono text-[13px] font-semibold',
                  state === 'current' && 'text-ai-600',
                  state === 'risk' && 'text-safety',
                  state === 'upcoming' && 'text-ink3',
                  state === 'done' && 'text-ok',
                )}
              >
                {s.key}
              </div>
              <div className="text-[13px] text-ink2">{s.label}</div>
            </div>
          </div>
        );
      })}
      {note && (
        <div className="mt-2 rounded-xl bg-brand-soft px-3 py-2 text-[11.5px] leading-relaxed text-ink2">
          {note}
        </div>
      )}
    </div>
  );
}
