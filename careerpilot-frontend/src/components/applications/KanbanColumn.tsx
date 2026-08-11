'use client';

import { useDroppable } from '@dnd-kit/core';
import { SortableContext, verticalListSortingStrategy } from '@dnd-kit/sortable';
import type { ApplicationDTO, ApplicationStatus } from '@/lib/schemas/application';
import { STATUS_META } from '@/lib/schemas/application';
import ApplicationCard from './ApplicationCard';
import ApplicationCardSkeleton from './ApplicationCardSkeleton';

interface KanbanColumnProps {
  status: ApplicationStatus;
  applications: ApplicationDTO[];
  isLoading?: boolean;
}

export default function KanbanColumn({ status, applications, isLoading }: KanbanColumnProps) {
  const meta = STATUS_META[status];

  const { setNodeRef, isOver } = useDroppable({ id: status });

  return (
    <div className="w-72 flex flex-col bg-surface-container-low rounded-xl border border-border-subtle p-3 kanban-column shrink-0">
      {/* Column header */}
      <div className="flex justify-between items-center mb-4 px-2">
        <h3 className={`font-label-sm text-label-sm font-semibold uppercase tracking-wider ${meta.color || 'text-on-surface'}`}>
          {meta.label}
        </h3>
        <span className={`font-label-xs text-label-xs px-2 py-0.5 rounded-full ${meta.bg || 'bg-surface-variant'} ${meta.color || 'text-on-surface-variant'}`}>
          {applications.length}
        </span>
      </div>

      {/* Drop zone */}
      <div
        ref={setNodeRef}
        data-testid={`dropzone-${status}`}
        className={`
          flex-1 overflow-y-auto space-y-3 no-scrollbar kanban-dropzone min-h-[120px] transition-colors duration-150
          ${isOver ? 'drag-over' : ''}
        `}
      >
        {isLoading ? (
          <>
            <ApplicationCardSkeleton />
            <ApplicationCardSkeleton />
          </>
        ) : applications.length === 0 ? (
          <div className="h-full flex flex-col items-center justify-center opacity-50 border-2 border-dashed border-border-subtle rounded-lg p-4 text-center mt-2 min-h-[120px]">
            <span className="material-symbols-outlined text-[24px] text-on-surface-variant mb-2">drag_indicator</span>
            <p className="font-label-xs text-label-xs text-on-surface-variant">
              {status === 'SAVED' ? 'Drag jobs here to save' : 'Drop cards here'}
            </p>
          </div>
        ) : (
          <SortableContext id={status} items={applications.map(a => a.id)} strategy={verticalListSortingStrategy}>
            {applications.map((app) => (
              <ApplicationCard key={app.id} application={app} />
            ))}
          </SortableContext>
        )}
      </div>
    </div>
  );
}
