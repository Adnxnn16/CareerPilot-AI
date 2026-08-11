'use client';

import { useState } from 'react';
import { useCreateApplication } from '@/lib/hooks/useCreateApplication';
import type { ApplicationDTO } from '@/lib/schemas/application';


interface AddApplicationDialogProps {
  open: boolean;
  onClose: () => void;
  jobId: string;
  jobTitle?: string;
  company?: string;
  /** Called when an application is successfully created */
  onCreated?: (app: ApplicationDTO) => void;
  /** Called when a duplicate is found — passes the existing application */
  onDuplicate?: (existing: ApplicationDTO) => void;
}

export default function AddApplicationDialog({
  open,
  onClose,
  jobId,
  jobTitle,
  company,
  onCreated,
  onDuplicate,
}: AddApplicationDialogProps) {
  const [notes, setNotes] = useState('');
  const [appliedDate, setAppliedDate] = useState('');
  const [error, setError] = useState<string | null>(null);
  const { mutate: create, isPending } = useCreateApplication();

  if (!open) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    create(
      { jobId, notes: notes || undefined, appliedDate: appliedDate || undefined },
      {
        onSuccess: (result) => {
          if (result.conflict) {
            onDuplicate?.(result.conflict.existing);
            onClose();
          } else if (result.data) {
            onCreated?.(result.data);
            onClose();
          }
        },
        onError: (err: unknown) => {
          let msg = 'Failed to save. Please try again.';
          if (err && typeof err === 'object' && 'response' in err) {
            const axiosErr = err as { response?: { data?: { message?: string } } };
            msg = axiosErr.response?.data?.message ?? msg;
          }
          setError(msg);
        },
      }
    );
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label="Save application to tracker"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm"
      onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}
    >
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md p-6 space-y-5 animate-in fade-in zoom-in-95 duration-200">
        {/* Header */}
        <div className="flex items-start justify-between">
          <div>
            <h2 className="text-lg font-bold text-slate-900">Save to Tracker</h2>
            {(jobTitle || company) && (
              <p className="text-sm text-slate-500 mt-0.5">
                {jobTitle}{company ? ` · ${company}` : ''}
              </p>
            )}
          </div>
          <button
            onClick={onClose}
            aria-label="Close dialog"
            className="text-slate-400 hover:text-slate-700 transition-colors mt-0.5"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Applied date (optional) */}
          <div>
            <label htmlFor="applied-date" className="block text-xs font-medium text-slate-700 mb-1">
              Applied date <span className="text-slate-400">(optional)</span>
            </label>
            <input
              id="applied-date"
              type="date"
              value={appliedDate}
              onChange={(e) => setAppliedDate(e.target.value)}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
            />
          </div>

          {/* Notes (optional) */}
          <div>
            <label htmlFor="app-notes" className="block text-xs font-medium text-slate-700 mb-1">
              Notes <span className="text-slate-400">(optional, max 4000 chars)</span>
            </label>
            <textarea
              id="app-notes"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              maxLength={4000}
              rows={3}
              placeholder="Interview prep, recruiter contact, salary range…"
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm resize-none focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
            />
            <p className="text-xs text-slate-400 mt-1 text-right">{notes.length}/4000</p>
          </div>

          {/* Error */}
          {error && (
            <div role="alert" className="rounded-lg bg-red-50 border border-red-200 px-3 py-2 text-xs text-red-700">
              {error}
            </div>
          )}

          {/* Actions */}
          <div className="flex gap-3 pt-1">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 rounded-xl border border-slate-300 px-4 py-2.5 text-sm font-medium text-slate-700 hover:bg-slate-50 transition-colors"
            >
              Cancel
            </button>
            <button
              id="save-to-tracker-submit"
              type="submit"
              disabled={isPending}
              className="flex-1 rounded-xl bg-indigo-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              {isPending ? 'Saving…' : '📌 Save to Tracker'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
