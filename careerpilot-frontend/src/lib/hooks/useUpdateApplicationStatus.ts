'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';
import type { ApplicationDTO, ApplicationStatus, BoardDTO } from '@/lib/schemas/application';

interface UpdateStatusVars {
  applicationId: string;
  fromStatus: ApplicationStatus;
  toStatus: ApplicationStatus;
  version: number;
}

const patchStatus = async ({ applicationId, toStatus, version }: UpdateStatusVars) => {
  const res = await api.patch(`/applications/${applicationId}/status`, {
    status: toStatus,
    version,
  });
  return res.data as ApplicationDTO;
};

export function useUpdateApplicationStatus() {
  const queryClient = useQueryClient();

  return useMutation<ApplicationDTO, Error, UpdateStatusVars, { previousBoard: BoardDTO | undefined }>({
    mutationFn: patchStatus,

    // ── Optimistic update ──────────────────────────────────────────────────
    onMutate: async (variables) => {
      // Cancel any in-flight board refetches to avoid race conditions
      await queryClient.cancelQueries({ queryKey: ['applications', 'board'] });

      // Snapshot the current board for rollback
      const previousBoard = queryClient.getQueryData<BoardDTO>(['applications', 'board']);

      // Optimistically move the card from fromStatus column to toStatus column
      if (previousBoard) {
        const fromCol = previousBoard.columns[variables.fromStatus] ?? [];
        const toCol = previousBoard.columns[variables.toStatus] ?? [];
        const card = fromCol.find((a) => a.id === variables.applicationId);

        if (card) {
          const updatedCard: ApplicationDTO = {
            ...card,
            status: variables.toStatus,
            version: card.version + 1, // Optimistic version bump
          };

          queryClient.setQueryData<BoardDTO>(['applications', 'board'], {
            columns: {
              ...previousBoard.columns,
              [variables.fromStatus]: fromCol.filter((a) => a.id !== variables.applicationId),
              [variables.toStatus]: [updatedCard, ...toCol],
            },
          });
        }
      }

      return { previousBoard };
    },

    // ── Rollback on error ──────────────────────────────────────────────────
    onError: (_err, _variables, context) => {
      if (context?.previousBoard) {
        queryClient.setQueryData(['applications', 'board'], context.previousBoard);
      }
    },

    // ── Always revalidate after settle ─────────────────────────────────────
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ['applications', 'board'] });
    },
  });
}
