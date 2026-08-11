'use client';



export default function ApplicationCardSkeleton() {
  return (
    <div className="bg-white rounded-xl border border-slate-200 p-4 shadow-sm animate-pulse space-y-3">
      <div className="flex items-start justify-between gap-2">
        <div className="space-y-1.5 flex-1">
          <div className="h-4 bg-slate-200 rounded w-3/4" />
          <div className="h-3 bg-slate-100 rounded w-1/2" />
        </div>
        <div className="h-6 w-6 bg-slate-100 rounded-full shrink-0" />
      </div>
      <div className="h-3 bg-slate-100 rounded w-1/3" />
    </div>
  );
}
