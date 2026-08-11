'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';
import type { ApplicationStatus, BoardDTO } from '@/lib/schemas/application';

interface DeleteVars {
  applicationId: string;
  status: ApplicationStatus;
}

const deleteApplication = async ({ applicationId }: DeleteVars): Promise<void> => {
  await api.delete(`/applications/${applicationId}`);
};

export function useDeleteApplication() {
  const queryClient = useQueryClient();

  return useMutation<void, Error, DeleteVars, { previousBoard: BoardDTO | undefined }>({
    mutationFn: deleteApplication,

    // Optimistic removal from board cache
    onMutate: async (variables) => {
      await queryClient.cancelQueries({ queryKey: ['applications', 'board'] });
      const previousBoard = queryClient.getQueryData<BoardDTO>(['applications', 'board']);

      if (previousBoard) {
        const updatedColumns = { ...previousBoard.columns };
        const col = updatedColumns[variables.status];
        if (col) {
          updatedColumns[variables.status] = col.filter(
            (a) => a.id !== variables.applicationId
          );
        }
        queryClient.setQueryData<BoardDTO>(['applications', 'board'], {
          columns: updatedColumns,
        });
      }

      return { previousBoard };
    },

    onError: (_err, _vars, context) => {
      if (context?.previousBoard) {
        queryClient.setQueryData(['applications', 'board'], context.previousBoard);
      }
    },

    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ['applications', 'board'] });
    },
  });
}
