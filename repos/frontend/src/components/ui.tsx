import clsx from 'clsx';
import {ReactNode} from 'react';
import {statusLabel, useTranslation} from '../i18n';

/* ---------- Button ---------- */
type BtnVariant = 'default' | 'primary' | 'ai' | 'danger' | 'ghost';
type BtnProps = React.ButtonHTMLAttributes<HTMLButtonElement> & { variant?: BtnVariant; size?: 'sm' | 'md' };
const VARIANT: Record<BtnVariant, string> = {
    default: 'border border-line bg-white text-ink hover:border-slate-300',
    primary: 'border border-brand bg-brand text-white hover:bg-[#0c3d49]',
    ai: 'border border-ai bg-ai text-white hover:bg-[#0e9d8f]',
    danger: 'border border-danger bg-danger text-white hover:bg-[#b91c1c]',
    ghost: 'text-ink2 hover:bg-slate-100',
};

export function Button({variant = 'default', size = 'md', className, ...rest}: BtnProps) {
    return (
        <button
            className={clsx(
                'inline-flex items-center justify-center gap-2 rounded-lg font-semibold transition disabled:cursor-not-allowed disabled:opacity-50',
                size === 'sm' ? 'px-2.5 py-1 text-xs' : 'px-3.5 py-2 text-sm',
                VARIANT[variant],
                className,
            )}
            {...rest}
        />
    );
}

/* ---------- Tag ---------- */
type Tone = 'ok' | 'warn' | 'danger' | 'info' | 'ai' | 'human' | 'bad' | 'mute';
const TONE: Record<Tone, string> = {
    mute: 'bg-slate-100 text-ink2',
    ok: 'bg-green-100 text-ok',
    warn: 'bg-amber-100 text-warn',
    danger: 'bg-red-100 text-danger',
    info: 'bg-blue-100 text-info',
    ai: 'bg-[#D7F5F1] text-[#0E7C70]',
    human: 'bg-[#FDEBDD] text-[#B25E1E]',
    bad: 'bg-red-100 text-red-600',
};

export function Tag({tone = 'mute', children, className}: { tone?: Tone; children: ReactNode; className?: string }) {
    return <span
        className={clsx('inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[11px] font-semibold', TONE[tone], className)}>{children}</span>;
}

/* ---------- Card ---------- */
export function Card({className, children}: { className?: string; children: ReactNode }) {
    return <div className={clsx('rounded-xl border border-line bg-white shadow-card', className)}>{children}</div>;
}

/* ---------- Inputs ---------- */
export const Input = (props: React.InputHTMLAttributes<HTMLInputElement>) => (
    <input {...props}
           className={clsx('w-full rounded-lg border border-line bg-white px-3 py-2 text-sm outline-none focus:border-brand', props.className)}/>
);
export const Textarea = (props: React.TextareaHTMLAttributes<HTMLTextAreaElement>) => (
    <textarea {...props}
              className={clsx('w-full rounded-lg border border-line bg-white px-3 py-2 text-sm outline-none focus:border-brand', props.className)}/>
);
export const Select = (props: React.SelectHTMLAttributes<HTMLSelectElement>) => (
    <select {...props}
            className={clsx('w-full rounded-lg border border-line bg-white px-3 py-2 text-sm outline-none focus:border-brand', props.className)}/>
);

/* ---------- Modal ---------- */
export function Modal({
                          open,
                          onClose,
                          title,
                          children,
                          footer,
                      }: {
    open: boolean;
    onClose: () => void;
    title: ReactNode;
    children: ReactNode;
    footer?: ReactNode;
}) {
    if (!open) return null;
    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4" onClick={onClose}>
            <div className="w-full max-w-lg rounded-2xl bg-white shadow-2xl" onClick={(e) => e.stopPropagation()}>
                <div className="flex items-center justify-between border-b border-line px-5 py-4 font-bold">
                    {title}
                    <button className="text-2xl leading-none text-ink2" onClick={onClose}>
                        ×
                    </button>
                </div>
                <div className="p-5">{children}</div>
                {footer && <div className="flex justify-end gap-2 border-t border-line px-5 py-3.5">{footer}</div>}
            </div>
        </div>
    );
}

/* ---------- Spinner ---------- */
export function Spinner({label}: { label?: string }) {
    return (
        <div className="flex items-center gap-2 text-sm text-ink2">
            <span className="h-3.5 w-3.5 animate-spin rounded-full border-2 border-line border-t-ai"/>
            {label}
        </div>
    );
}

/* ---------- EmptyState ---------- */
export function EmptyState({title, hint}: { title: string; hint?: string }) {
    return (
        <div className="rounded-xl border border-dashed border-line bg-white/60 p-10 text-center">
            <p className="font-semibold text-ink2">{title}</p>
            {hint && <p className="mt-1 text-sm text-ink2/80">{hint}</p>}
        </div>
    );
}

/* ---------- StatusFlow（知识生命周期流程条） ---------- */
const STEPS = ['DRAFT', 'REVIEW', 'APPROVED', 'PUBLISHED', 'DEPRECATED'];

export function StatusFlow({status}: { status: string }) {
    const {t} = useTranslation();
    let idx = STEPS.indexOf(status);
    if (status === 'ARCHIVED') idx = STEPS.length;
    if (idx < 0) return <Tag tone="mute">{statusLabel(t, status)}</Tag>;
    return (
        <div className="flex items-center gap-0">
            {STEPS.map((s, i) => (
                <span key={s} className="flex items-center">
          <span
              className={clsx('rounded-md px-1.5 py-0.5 text-[10px] font-bold', i <= idx ? 'bg-ai text-white' : 'bg-slate-100 text-ink2')}>{statusLabel(t, s)}</span>
                    {i < STEPS.length - 1 && <span className="mx-0.5 h-0.5 w-3 bg-line"/>}
        </span>
            ))}
        </div>
    );
}
