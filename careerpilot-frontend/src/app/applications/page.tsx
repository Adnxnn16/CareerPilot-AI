'use client';

import { useState } from 'react';
import { useAuthStore } from '@/store/authStore';
import KanbanBoard from '@/components/applications/KanbanBoard';
import AddApplicationDialog from '@/components/applications/AddApplicationDialog';

export default function ApplicationsPage() {
  const { isAuthenticated } = useAuthStore();
  const [showAddDialog, setShowAddDialog] = useState(false);
  const [addJobId] = useState('');

  if (!isAuthenticated) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center p-6">
        <div className="text-center space-y-4">
          <p className="text-4xl">🔒</p>
          <h1 className="font-headline-md text-headline-md font-bold text-on-surface">Sign in to view your tracker</h1>
          <p className="font-body-md text-body-md text-on-surface-variant">Your application board is waiting.</p>
          <a
            href="/login"
            className="inline-block mt-2 rounded-lg bg-primary px-6 py-2.5 font-label-sm text-label-sm font-semibold text-on-primary hover:bg-primary-container hover:text-on-primary-container transition-colors"
          >
            Sign in
          </a>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-container-max mx-auto px-4 sm:px-6 lg:px-8 py-8 flex-1 overflow-x-auto">
      {/* Page header */}
      <div className="flex flex-col sm:flex-row sm:items-end sm:justify-between gap-4 mb-6">
        <div>
          <h1 className="font-headline-md text-headline-md font-bold text-on-surface mb-1">Application Tracker</h1>
          <p className="font-body-md text-body-md text-on-surface-variant">
            Drag cards between columns to update your application status.
          </p>
        </div>
      </div>

      {/* Kanban board */}
      <KanbanBoard />

      {/* Add application dialog (triggered from jobs page; can also open here with a job search) */}
      {showAddDialog && addJobId && (
        <AddApplicationDialog
          open={showAddDialog}
          onClose={() => setShowAddDialog(false)}
          jobId={addJobId}
          onCreated={() => setShowAddDialog(false)}
          onDuplicate={() => setShowAddDialog(false)}
        />
      )}
    </div>
  );
}
