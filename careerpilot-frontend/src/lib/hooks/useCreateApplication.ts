'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';
import type { ApplicationDTO } from '@/lib/schemas/application';

interface CreateVars {
  jobId: string;
  resumeId?: string;
  notes?: string;
  appliedDate?: string;
}

interface CreateResult {
  data?: ApplicationDTO;
  conflict?: { message: string; existing: ApplicationDTO };
}

const postApplication = async (vars: CreateVars): Promise<CreateResult> => {
  try {
    const res = await api.post(`/jobs/${vars.jobId}/applications`, {
      resumeId: vars.resumeId,
      notes: vars.notes,
      appliedDate: vars.appliedDate,
    });
    return { data: res.data };
  } catch (err: unknown) {
    if (err && typeof err === 'object' && 'response' in err) {
      const axiosErr = err as { response?: { status?: number, data?: unknown } };
      if (axiosErr.response?.status === 409) {
        return { conflict: axiosErr.response.data as { message: string; existing: ApplicationDTO } };
      }
    }
    throw err;
  }
};

export function useCreateApplication() {
  const queryClient = useQueryClient();

  return useMutation<CreateResult, Error, CreateVars>({
    mutationFn: postApplication,
    onSuccess: (result) => {
      // Only invalidate board if a new record was created (not a duplicate 409)
      if (result.data) {
        queryClient.invalidateQueries({ queryKey: ['applications', 'board'] });
      }
    },
  });
}
