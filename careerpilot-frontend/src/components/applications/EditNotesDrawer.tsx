'use client';

import { useState, useEffect } from 'react';
import { useUpdateApplicationNotes } from '@/lib/hooks/useUpdateApplicationNotes';
import type { ApplicationDTO } from '@/lib/schemas/application';

interface EditNotesDrawerProps {
  application: ApplicationDTO;
  open: boolean;
  onClose: () => void;
}

export default function EditNotesDrawer({ application, open, onClose }: EditNotesDrawerProps) {
  const [notes, setNotes] = useState(application.notes ?? '');
  const [error, setError] = useState<string | null>(null);
  const { mutate: updateNotes, isPending } = useUpdateApplicationNotes();

  useEffect(() => {
    if (open) {
      setNotes(application.notes ?? '');
      setError(null);
    }
  }, [open, application.notes]);

  if (!open) return null;

  const handleSave = () => {
    if (notes.length > 4000) {
      setError('Notes must not exceed 4000 characters');
      return;
    }
    updateNotes(
      { applicationId: application.id, notes: notes || null },
      {
        onSuccess: () => onClose(),
        onError: (err: unknown) => {
          let msg = 'Failed to save notes. Please try again.';
          if (err && typeof err === 'object' && 'response' in err) {
            const axiosErr = err as { response?: { data?: { message?: string } } };
            msg = axiosErr.response?.data?.message ?? msg;
          }
          setError(msg);
        },
      }
    );
  };

  const snap = application.jobSnapshot;

  return (
    <>
      {/* Backdrop */}
      <div
        className="fixed inset-0 z-40 bg-black/30 backdrop-blur-sm"
        onClick={onClose}
        aria-hidden="true"
      />
      {/* Drawer */}
      <div
        role="dialog"
        aria-label="Edit notes"
        className="fixed right-0 top-0 bottom-0 z-50 w-full max-w-sm bg-white shadow-2xl flex flex-col animate-in slide-in-from-right duration-200"
      >
        {/* Header */}
        <div className="flex items-center justify-between px-5 py-4 border-b border-slate-100">
          <div>
            <h2 className="font-bold text-slate-900 text-base">Edit Notes</h2>
            {snap?.title && (
              <p className="text-xs text-slate-500 mt-0.5 truncate">{snap.title} · {snap.company}</p>
            )}
          </div>
          <button
            onClick={onClose}
            aria-label="Close drawer"
            className="text-slate-400 hover:text-slate-700 transition-colors"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {/* Body */}
        <div className="flex-1 overflow-y-auto p-5">
          <label htmlFor="drawer-notes" className="block text-xs font-medium text-slate-700 mb-1.5">
            Notes <span className="text-slate-400">(max 4000 chars)</span>
          </label>
          <textarea
            id="drawer-notes"
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            maxLength={4000}
            rows={14}
            placeholder="Interview prep, salary info, recruiter contacts, timeline…"
            className="w-full rounded-xl border border-slate-300 px-4 py-3 text-sm resize-none focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent leading-relaxed"
          />
          <p className="text-xs text-slate-400 mt-1.5 text-right">{notes.length}/4000</p>

          {error && (
            <div role="alert" className="mt-3 rounded-lg bg-red-50 border border-red-200 px-3 py-2 text-xs text-red-700">
              {error}
            </div>
          )}
        </div>

        {/* Footer actions */}
        <div className="flex gap-3 px-5 py-4 border-t border-slate-100">
          <button
            onClick={onClose}
            className="flex-1 rounded-xl border border-slate-300 px-4 py-2.5 text-sm font-medium text-slate-700 hover:bg-slate-50 transition-colors"
          >
            Cancel
          </button>
          <button
            id="save-notes-btn"
            onClick={handleSave}
            disabled={isPending}
            className="flex-1 rounded-xl bg-indigo-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {isPending ? 'Saving…' : 'Save Notes'}
          </button>
        </div>
      </div>
    </>
  );
}
