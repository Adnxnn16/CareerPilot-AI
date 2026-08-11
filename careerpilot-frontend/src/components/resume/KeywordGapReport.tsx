'use client';

// src/components/resume/KeywordGapReport.tsx

interface KeywordGapReportProps {
  matchingKeywords: string[];
  unmatchedKeywords: string[];
}

export default function KeywordGapReport({
  matchingKeywords,
  unmatchedKeywords,
}: KeywordGapReportProps) {
  const total = matchingKeywords.length + unmatchedKeywords.length;
  const matchPct = total > 0 ? Math.round((matchingKeywords.length / total) * 100) : 0;

  return (
    <div className="rounded-xl border border-border-subtle bg-surface-container-low p-5 space-y-5">
      <div className="flex items-center justify-between">
        <h3 className="font-label-sm text-label-sm font-semibold text-on-background">Keyword Coverage Report</h3>
        <span className="font-label-xs text-label-xs font-medium text-on-surface-variant">
          {matchingKeywords.length}/{total} keywords matched
        </span>
      </div>

      {/* Progress bar */}
      <div className="w-full bg-surface-variant rounded-full h-2.5 overflow-hidden">
        <div
          className="h-2.5 rounded-full bg-success-green transition-all duration-700"
          style={{ width: `${matchPct}%` }}
          role="progressbar"
          aria-valuenow={matchPct}
          aria-valuemin={0}
          aria-valuemax={100}
          aria-label={`${matchPct}% of JD keywords matched`}
        />
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        {/* Matched */}
        {matchingKeywords.length > 0 && (
          <div>
            <p className="font-label-xs text-label-xs font-semibold text-success-green uppercase tracking-wide mb-2">
              ✓ Honestly matched ({matchingKeywords.length})
            </p>
            <div className="flex flex-wrap gap-1.5">
              {matchingKeywords.map((kw) => (
                <span
                  key={kw}
                  className="inline-flex items-center rounded-full bg-success-green/10 px-2.5 py-0.5 font-label-xs text-label-xs font-medium text-success-green border border-success-green/20"
                >
                  {kw}
                </span>
              ))}
            </div>
          </div>
        )}

        {/* Unmatched / Gap */}
        {unmatchedKeywords.length > 0 && (
          <div>
            <p className="font-label-xs text-label-xs font-semibold text-tertiary uppercase tracking-wide mb-2">
              ⚠ Skill gap — not fabricated ({unmatchedKeywords.length})
            </p>
            <div className="flex flex-wrap gap-1.5">
              {unmatchedKeywords.map((kw) => (
                <span
                  key={kw}
                  className="inline-flex items-center rounded-full bg-tertiary-fixed px-2.5 py-0.5 font-label-xs text-label-xs font-medium text-on-tertiary-fixed border border-tertiary-fixed-dim"
                >
                  {kw}
                </span>
              ))}
            </div>
            <p className="font-label-xs text-label-xs text-text-muted mt-2">
              Consider acquiring these skills or highlighting adjacent experience.
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
