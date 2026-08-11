'use client';

import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import type { BoardDTO } from '@/lib/schemas/application';

const fetchBoard = async (): Promise<BoardDTO> => {
  const res = await api.get('/applications/board');
  return res.data;
};

export function useApplicationBoard() {
  return useQuery<BoardDTO>({
    queryKey: ['applications', 'board'],
    queryFn: fetchBoard,
    staleTime: 30_000, // 30 s — board is not real-time; React Query polling is sufficient
  });
}
