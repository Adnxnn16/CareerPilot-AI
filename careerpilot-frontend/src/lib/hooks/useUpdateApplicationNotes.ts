'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';
import type { ApplicationDTO } from '@/lib/schemas/application';

interface UpdateNotesVars {
  applicationId: string;
  notes: string | null;
}

const patchNotes = async ({ applicationId, notes }: UpdateNotesVars): Promise<ApplicationDTO> => {
  const res = await api.patch(`/applications/${applicationId}/notes`, { notes });
  return res.data;
};

export function useUpdateApplicationNotes() {
  const queryClient = useQueryClient();

  return useMutation<ApplicationDTO, Error, UpdateNotesVars>({
    mutationFn: patchNotes,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['applications', 'board'] });
    },
  });
}
