'use client';

import { useSortable } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { useState } from 'react';
import type { ApplicationDTO, ApplicationStatus } from '@/lib/schemas/application';
import EditNotesDrawer from './EditNotesDrawer';
import DeleteConfirmDialog from './DeleteConfirmDialog';

interface ApplicationCardProps {
  application: ApplicationDTO;
}

export default function ApplicationCard({ application }: ApplicationCardProps) {
  const [showNotes, setShowNotes] = useState(false);
  const [showDelete, setShowDelete] = useState(false);

  const { attributes, listeners, setNodeRef, transform, isDragging, transition } = useSortable({
    id: application.id,
    data: {
      applicationId: application.id,
      fromStatus: application.status as ApplicationStatus,
      version: application.version,
    },
  });

  const style = {
    transform: CSS.Translate.toString(transform),
    transition,
    opacity: isDragging ? 0.4 : 1,
    touchAction: 'none',
  };

  const snap = application.jobSnapshot;
  const title = snap?.title ?? 'Unknown Role';
  const company = snap?.company ?? 'Unknown Company';
  const location = snap?.location;
  const hasNotes = application.notes && application.notes.trim().length > 0;
  const appliedDate = application.appliedDate
    ? new Date(application.appliedDate).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
    : null;

  return (
    <>
      <div
        ref={setNodeRef}
        style={style}
        className={`
          group bg-surface p-4 rounded-lg border border-border-subtle shadow-[0_2px_4px_rgba(0,0,0,0.02)] cursor-grab active:cursor-grabbing hover:border-primary-container transition-colors kanban-card flex flex-col min-h-[110px]
          ${isDragging ? 'dragging border-primary-container shadow-[0_4px_12px_rgba(0,0,0,0.05)]' : ''}
        `}
        {...listeners}
        {...attributes}
      >
        {/* Header: title + actions */}
        <div className="flex items-start gap-2 justify-between mb-2">
          <div className="flex-1 min-w-0">
            <h4 className="font-label-sm text-label-sm font-semibold text-on-surface leading-tight truncate" title={title}>
              {title}
            </h4>
            <p className="font-label-xs text-label-xs text-on-surface-variant mt-0.5 truncate">{company}</p>
          </div>
          {/* Contextual actions — visible on hover */}
          <div className="opacity-0 group-hover:opacity-100 transition-opacity flex items-center gap-1 shrink-0">
            <button
              id={`edit-notes-${application.id}`}
              onClick={(e) => { e.stopPropagation(); setShowNotes(true); }}
              aria-label="Edit notes"
              className="p-1 rounded hover:bg-slate-100 text-slate-400 hover:text-slate-700 transition-colors"
            >
              <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                  d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
              </svg>
            </button>
            <button
              id={`delete-app-${application.id}`}
              onClick={(e) => { e.stopPropagation(); setShowDelete(true); }}
              aria-label="Delete application"
              className="p-1 rounded hover:bg-red-50 text-slate-400 hover:text-red-500 transition-colors"
            >
              <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                  d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
              </svg>
            </button>
          </div>
        </div>

        {/* Footer: location + date + notes badge */}
        <div className="mt-auto pt-3 border-t border-border-subtle flex justify-between items-center">
          <div className="flex items-center gap-2 flex-wrap">
            {appliedDate && (
              <span className="font-label-xs text-label-xs text-text-muted flex items-center gap-1">
                <span className="material-symbols-outlined text-[14px]">schedule</span>
                {appliedDate}
              </span>
            )}
            {location && (
              <span className="font-label-xs text-label-xs text-text-muted flex items-center gap-1">
                <span className="material-symbols-outlined text-[14px]">location_on</span>
                {location}
              </span>
            )}
          </div>
          {hasNotes && (
            <span className="material-symbols-outlined text-primary-container text-[16px]" style={{ fontVariationSettings: "'FILL' 1" }} title="Notes available">
              sticky_note_2
            </span>
          )}
        </div>
      </div>

      {/* Notes drawer */}
      <EditNotesDrawer
        application={application}
        open={showNotes}
        onClose={() => setShowNotes(false)}
      />

      {/* Delete confirm */}
      <DeleteConfirmDialog
        application={application}
        open={showDelete}
        onClose={() => setShowDelete(false)}
      />
    </>
  );
}
