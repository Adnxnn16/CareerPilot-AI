'use client';

// src/components/resume/ResumeSelector.tsx
interface Resume {
  id: string;
  originalFilename: string;
  status: string;
}

interface ResumeSelectorProps {
  resumes: Resume[];
  isLoading: boolean;
  selectedId: string | null;
  onChange: (id: string) => void;
}

export default function ResumeSelector({
  resumes,
  isLoading,
  selectedId,
  onChange,
}: ResumeSelectorProps) {
  if (isLoading) {
    return (
      <div className="animate-pulse">
        <div className="h-4 bg-surface-variant rounded w-32 mb-2" />
        <div className="h-10 bg-surface-variant rounded-lg w-full" />
      </div>
    );
  }

  if (resumes.length === 0) {
    return (
      <div className="rounded-lg bg-tertiary-fixed border border-tertiary-fixed-dim p-4 text-on-tertiary-fixed">
        <p className="font-label-sm text-label-sm font-semibold">No parsed resumes found</p>
        <p className="mt-1 font-label-xs text-label-xs text-on-tertiary-fixed/80">
          Upload and process a resume first before generating a tailored version.
        </p>
      </div>
    );
  }

  return (
    <div>
      <label
        htmlFor="resume-select"
        className="block font-label-sm text-label-sm font-semibold text-on-background mb-2"
      >
        Select source resume
      </label>
      <select
        id="resume-select"
        value={selectedId ?? ''}
        onChange={(e) => onChange(e.target.value)}
        className="block w-full rounded-lg border border-border-subtle bg-surface-bright px-3 py-2 text-body-md font-body-md text-on-background shadow-sm focus:border-primary-container focus:ring-2 focus:ring-primary-container outline-none appearance-none"
      >
        <option value="" disabled>
          Choose a resume…
        </option>
        {resumes.map((r) => (
          <option key={r.id} value={r.id}>
            {r.originalFilename || 'Unnamed resume'}
          </option>
        ))}
      </select>
    </div>
  );
}
