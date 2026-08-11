'use client';

// src/components/resume/TailoringStatus.tsx
interface TailoringStatusProps {
  status: 'PENDING' | 'PROCESSING' | string;
}

const statusConfig = {
  PENDING: {
    label: 'Queued…',
    description: 'Your tailoring request is waiting to be processed.',
    color: 'bg-tertiary-fixed text-on-tertiary-fixed border-tertiary-fixed-dim',
    textColor: 'text-on-tertiary-fixed',
  },
  PROCESSING: {
    label: 'Generating your ATS resume…',
    description:
      'Our AI is analysing the job description and rewriting your bullet points. This takes 15–30 seconds.',
    color: 'bg-primary-container text-on-primary-container border-primary-fixed',
    textColor: 'text-on-primary-container',
  },
};

export default function TailoringStatus({ status }: TailoringStatusProps) {
  const cfg = statusConfig[status as keyof typeof statusConfig] ?? statusConfig.PROCESSING;

  return (
    <div
      role="status"
      aria-live="polite"
      className={`rounded-xl border p-6 ${cfg.color} flex flex-col sm:flex-row items-start sm:items-center gap-4`}
    >
      {/* Spinner */}
      <div className="shrink-0">
        <span className="inline-block h-8 w-8 animate-spin rounded-full border-4 border-primary-fixed border-t-primary" />
      </div>

      {/* Text */}
      <div>
        <p className={`font-label-sm text-label-sm font-semibold ${cfg.textColor}`}>{cfg.label}</p>
        <p className={`font-label-xs text-label-xs mt-0.5 ${cfg.textColor} opacity-80`}>{cfg.description}</p>
      </div>

      {/* Skeleton content bars */}
      <div className="w-full sm:ml-auto max-w-xs space-y-2 animate-pulse">
        <div className="h-3 bg-primary-fixed rounded w-full" />
        <div className="h-3 bg-primary-fixed rounded w-4/5" />
        <div className="h-3 bg-primary-fixed rounded w-2/3" />
      </div>
    </div>
  );
}
