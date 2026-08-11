'use client';

import {
  DndContext,
  DragEndEvent,
  DragOverlay,
  DragStartEvent,
  PointerSensor,
  KeyboardSensor,
  useSensor,
  useSensors,
  closestCenter,
} from '@dnd-kit/core';
import { sortableKeyboardCoordinates } from '@dnd-kit/sortable';
import { useState } from 'react';
import type { ApplicationDTO, ApplicationStatus } from '@/lib/schemas/application';
import { APPLICATION_STATUSES } from '@/lib/schemas/application';
import { useApplicationBoard } from '@/lib/hooks/useApplicationBoard';
import { useUpdateApplicationStatus } from '@/lib/hooks/useUpdateApplicationStatus';
import KanbanColumn from './KanbanColumn';
import ApplicationCard from './ApplicationCard';

interface KanbanBoardProps {
  onAddClick?: () => void;
}

export default function KanbanBoard({}: KanbanBoardProps) {
  const { data: board, isLoading, error } = useApplicationBoard();
  const updateStatus = useUpdateApplicationStatus();
  const [activeCard, setActiveCard] = useState<ApplicationDTO | null>(null);

  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 8 } }),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
    })
  );

  const handleDragStart = (event: DragStartEvent) => {
    if (!board) return;
    const { applicationId, fromStatus } = event.active.data.current as {
      applicationId: string;
      fromStatus: ApplicationStatus;
      version: number;
    };
    const col = board.columns[fromStatus] ?? [];
    const card = col.find((a) => a.id === applicationId);
    setActiveCard(card ?? null);
  };

  const handleDragEnd = (event: DragEndEvent) => {
    setActiveCard(null);
    const { over, active } = event;
    if (!over || !board) return;

    const { applicationId, fromStatus, version } = active.data.current as {
      applicationId: string;
      fromStatus: ApplicationStatus;
      version: number;
    };
    
    let toStatus: ApplicationStatus | null = null;
    if (APPLICATION_STATUSES.includes(over.id as ApplicationStatus)) {
      toStatus = over.id as ApplicationStatus;
    } else if (over.data.current?.sortable?.containerId) {
      toStatus = over.data.current.sortable.containerId as ApplicationStatus;
    }

    if (!toStatus || fromStatus === toStatus) return;

    updateStatus.mutate({ applicationId, fromStatus, toStatus, version });
  };

  if (error) {
    return (
      <div className="flex items-center justify-center py-20">
        <p className="text-sm text-red-500">Failed to load board. Please refresh.</p>
      </div>
    );
  }

  return (
    <DndContext
      sensors={sensors}
      collisionDetection={closestCenter}
      onDragStart={handleDragStart}
      onDragEnd={handleDragEnd}
    >
      {/* Scrollable horizontal board */}
      <div className="flex gap-4 overflow-x-auto min-w-max pb-8 pt-2 px-1 no-scrollbar" style={{ minHeight: '70vh' }}>
        {APPLICATION_STATUSES.map((status) => (
          <KanbanColumn
            key={status}
            status={status}
            applications={board?.columns[status] ?? []}
            isLoading={isLoading}
          />
        ))}
      </div>

      {/* Floating drag overlay — shows a ghost of the dragged card */}
      <DragOverlay>
        {activeCard ? (
          <div className="rotate-2 scale-105 shadow-2xl">
            <ApplicationCard application={activeCard} />
          </div>
        ) : null}
      </DragOverlay>
    </DndContext>
  );
}
