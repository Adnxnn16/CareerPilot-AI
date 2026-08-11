'use client';

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useRef } from 'react';

// Create a stable QueryClient instance that is shared across the app.
// We use a ref to avoid re-creating it on re-renders.
function makeQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 60_000,   // 1 minute — avoids refetching on tab focus for fresh data
        retry: 1,
      },
    },
  });
}

let browserQueryClient: QueryClient | undefined;

function getQueryClient() {
  if (typeof window === 'undefined') {
    // Server: always make a new QueryClient (prevents state being shared between requests)
    return makeQueryClient();
  }
  if (!browserQueryClient) {
    browserQueryClient = makeQueryClient();
  }
  return browserQueryClient;
}

export default function QueryProvider({ children }: { children: React.ReactNode }) {
  const queryClientRef = useRef<QueryClient | null>(null);
  if (!queryClientRef.current) {
    queryClientRef.current = getQueryClient();
  }

  return (
    <QueryClientProvider client={queryClientRef.current}>
      {children}
    </QueryClientProvider>
  );
}
