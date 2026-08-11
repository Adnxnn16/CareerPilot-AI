import { Skeleton } from '@/components/ui/Skeleton';

export default function JobDetailSkeleton() {
  return (
    <main className="flex-1 overflow-y-auto p-4 md:p-gutter lg:p-stack-lg bg-background">
      <div className="max-w-container-max mx-auto space-y-stack-lg">
        {/* Back nav skeleton */}
        <Skeleton className="h-4 w-24 mb-6" />

        {/* Hero Section */}
        <div className="bg-surface-container-lowest rounded-xl border border-border-subtle shadow-sm p-6 lg:p-8 flex flex-col lg:flex-row lg:items-start justify-between gap-6 relative overflow-hidden">
          <div className="absolute top-0 left-0 w-1 h-full bg-surface-variant"></div>
          <div className="flex-1 space-y-4">
            <div className="flex flex-col sm:flex-row sm:items-center gap-4">
              <Skeleton className="w-16 h-16 rounded-lg shrink-0" />
              <div className="space-y-2">
                <Skeleton className="h-8 w-48 sm:w-64" />
                <div className="flex gap-2 mt-2">
                  <Skeleton className="h-4 w-24" />
                  <Skeleton className="h-4 w-24" />
                </div>
              </div>
            </div>
          </div>
          <div className="flex flex-col sm:flex-row lg:flex-col gap-3 shrink-0">
            <Skeleton className="h-10 w-32 rounded-lg" />
            <Skeleton className="h-10 w-32 rounded-lg" />
          </div>
        </div>

        {/* Main Content Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 lg:gap-8">
          <div className="lg:col-span-2 space-y-6">
            <div className="bg-surface-container-lowest rounded-xl border border-border-subtle shadow-sm p-6 lg:p-8 space-y-3">
              <Skeleton className="h-6 w-32 mb-4" />
              {[...Array(6)].map((_, i) => (
                <Skeleton key={i} className="h-4 w-full" />
              ))}
              <Skeleton className="h-4 w-3/4" />
            </div>
          </div>
          <div className="space-y-6">
            <div className="bg-surface-container-lowest rounded-xl border border-border-subtle shadow-sm p-6 space-y-4">
              <Skeleton className="h-6 w-48" />
              <Skeleton className="h-10 w-full rounded-lg mt-4" />
              <Skeleton className="h-10 w-full rounded-lg" />
            </div>
          </div>
        </div>
      </div>
    </main>
  );
}
