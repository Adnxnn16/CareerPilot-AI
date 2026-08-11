'use client';

import { useDeleteApplication } from '@/lib/hooks/useDeleteApplication';
import type { ApplicationDTO, ApplicationStatus } from '@/lib/schemas/application';

interface DeleteConfirmDialogProps {
  application: ApplicationDTO;
  open: boolean;
  onClose: () => void;
}

export default function DeleteConfirmDialog({ application, open, onClose }: DeleteConfirmDialogProps) {
  const { mutate: deleteApp, isPending } = useDeleteApplication();

  if (!open) return null;

  const snap = application.jobSnapshot;
  const title = snap?.title ?? 'this application';

  const handleDelete = () => {
    deleteApp(
      { applicationId: application.id, status: application.status as ApplicationStatus },
      { onSuccess: onClose }
    );
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label="Confirm delete application"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm"
      onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}
    >
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-sm p-6 space-y-4 animate-in fade-in zoom-in-95 duration-200">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-full bg-red-100 flex items-center justify-center shrink-0">
            <svg className="w-5 h-5 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
            </svg>
          </div>
          <div>
            <h2 className="font-bold text-slate-900">Remove Application</h2>
            <p className="text-xs text-slate-500 mt-0.5">This cannot be undone.</p>
          </div>
        </div>

        <p className="text-sm text-slate-700">
          Remove <span className="font-medium">&quot;{title}&quot;</span> from your tracker?
        </p>

        <div className="flex gap-3">
          <button
            onClick={onClose}
            className="flex-1 rounded-xl border border-slate-300 px-4 py-2.5 text-sm font-medium text-slate-700 hover:bg-slate-50 transition-colors"
          >
            Keep it
          </button>
          <button
            id="confirm-delete-btn"
            onClick={handleDelete}
            disabled={isPending}
            className="flex-1 rounded-xl bg-red-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-red-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {isPending ? 'Removing…' : 'Yes, remove'}
          </button>
        </div>
      </div>
    </div>
  );
}
