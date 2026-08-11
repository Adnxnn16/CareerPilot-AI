'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useParams, useRouter } from 'next/navigation';
import { useState } from 'react';
import api from '@/lib/api';
import { useAuthStore } from '@/store/authStore';
import { tailoringRequestSchema, type ResumeDTO } from '@/lib/schemas/tailor';
import JobDetailSkeleton from '@/components/jobs/JobDetailSkeleton';
import ResumeSelector from '@/components/resume/ResumeSelector';
import TailoringStatus from '@/components/resume/TailoringStatus';
import KeywordGapReport from '@/components/resume/KeywordGapReport';
import AddApplicationDialog from '@/components/applications/AddApplicationDialog';
import UploadResumeModal from '@/components/resume/UploadResumeModal';
import type { ApplicationDTO } from '@/lib/schemas/application';

// ── Types ─────────────────────────────────────────────────────────────────────

interface Job {
  id: string;
  title: string;
  company: string;
  description: string;
  location: string;
  jobUrl: string;
  requiredSkills: string[];
}

// ── Data fetching helpers ─────────────────────────────────────────────────────

const fetchJob = async (jobId: string): Promise<Job> => {
  const res = await api.get(`/jobs/${jobId}`);
  return res.data;
};

const fetchMyResumes = async (): Promise<ResumeDTO[]> => {
  const res = await api.get('/users/me/resumes');
  return res.data;
};

const fetchResume = async (resumeId: string): Promise<ResumeDTO> => {
  const res = await api.get(`/resumes/${resumeId}`);
  return res.data;
};

const postTailorResume = async ({
  jobId,
  sourceResumeId,
}: {
  jobId: string;
  sourceResumeId: string;
}): Promise<{ id: string; status: string; message: string }> => {
  const res = await api.post(`/jobs/${jobId}/resume/tailor`, { sourceResumeId });
  return res.data;
};

// ── Main Page Component ───────────────────────────────────────────────────────

export default function JobDetailPage() {
  const { id: jobId } = useParams<{ id: string }>();
  const router = useRouter();
  const { isAuthenticated } = useAuthStore();
  const queryClient = useQueryClient();

  const [selectedResumeId, setSelectedResumeId] = useState<string>('');
  const [validationError, setValidationError] = useState<string | null>(null);
  const [generatedResumeId, setGeneratedResumeId] = useState<string | null>(null);
  const [showUploadModal, setShowUploadModal] = useState(false);

  // ── F5: Save to Tracker state ────────────────────────────────────────────
  const [showAddDialog, setShowAddDialog] = useState(false);
  const [existingApplication, setExistingApplication] = useState<ApplicationDTO | null>(null);

  // ── Fetch job details ────────────────────────────────────────────────────
  const {
    data: job,
    isLoading: jobLoading,
    error: jobError,
  } = useQuery({
    queryKey: ['job', jobId],
    queryFn: () => fetchJob(jobId!),
    enabled: !!jobId && isAuthenticated,
  });

  // ── Fetch user's DONE uploaded resumes for selector ──────────────────────
  const { data: uploadedResumes, isLoading: resumesLoading } = useQuery({
    queryKey: ['my-resumes'],
    queryFn: fetchMyResumes,
    enabled: isAuthenticated,
    select: (data: ResumeDTO[]) =>
      data.filter((r) => r.sourceType === 'UPLOADED' && r.status === 'DONE'),
  });

  // ── Poll the generated resume every 3s until DONE or FAILED ─────────────
  const { data: generatedResume } = useQuery({
    queryKey: ['resume', generatedResumeId],
    queryFn: () => fetchResume(generatedResumeId!),
    enabled: !!generatedResumeId,
    refetchInterval: (query) => {
      const s = query.state.data?.status;
      return s === 'DONE' || s === 'FAILED' ? false : 3_000;
    },
  });

  // ── Tailoring mutation ───────────────────────────────────────────────────
  const tailoringMutation = useMutation({
    mutationFn: postTailorResume,
    onSuccess: (data) => {
      setGeneratedResumeId(data.id);
      setValidationError(null);
      queryClient.invalidateQueries({ queryKey: ['my-resumes'] });
    },
    onError: (error: unknown) => {
      let serverMsg = 'Failed to start resume tailoring. Please try again.';
      if (error && typeof error === 'object' && 'response' in error) {
        const axiosErr = error as { response?: { data?: { message?: string } } };
        serverMsg = axiosErr.response?.data?.message ?? serverMsg;
      }
      setValidationError(serverMsg);
    },
  });

  // ── Handle generate click with Zod validation ────────────────────────────
  const handleGenerate = () => {
    const result = tailoringRequestSchema.safeParse({ sourceResumeId: selectedResumeId });
    if (!result.success) {
      setValidationError(result.error.issues[0].message);
      return;
    }
    setValidationError(null);
    tailoringMutation.mutate({ jobId: jobId!, sourceResumeId: result.data.sourceResumeId });
  };

  const handleRetry = () => {
    setGeneratedResumeId(null);
    setValidationError(null);
    tailoringMutation.reset();
  };

  // ── Derived state ─────────────────────────────────────────────────────────
  const isProcessing =
    generatedResume?.status === 'PENDING' || generatedResume?.status === 'PROCESSING';
  const isDone = generatedResume?.status === 'DONE';
  const isFailed = generatedResume?.status === 'FAILED';

  // Compute matched keywords as: requiredSkills that are NOT in unmatchedKeywords
  const unmatchedKws = generatedResume?.unmatchedKeywords ?? [];
  const matchingKws = (job?.requiredSkills ?? []).filter((s) => !unmatchedKws.includes(s));

  // ── Loading / Error states ────────────────────────────────────────────────
  if (jobLoading) return <JobDetailSkeleton />;

  if (jobError || !job) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center p-6 bg-background">
        <div className="text-center space-y-4">
          <p className="text-4xl">🔍</p>
          <h1 className="font-headline-md text-headline-md font-bold text-on-surface">Job not found</h1>
          <p className="font-body-md text-body-md text-on-surface-variant">
            This listing may have expired or been removed.
          </p>
          <button
            onClick={() => router.back()}
            className="mt-2 text-primary hover:underline font-label-sm text-label-sm"
          >
            ← Go back
          </button>
        </div>
      </div>
    );
  }

  return (
    <main className="flex-1 overflow-y-auto p-4 md:p-gutter lg:p-stack-lg bg-background">
      <div className="max-w-container-max mx-auto space-y-stack-lg">

        {/* ── Back nav ─────────────────────────────────────────────────── */}
        <button
          onClick={() => router.back()}
          className="font-label-sm text-label-sm text-on-surface-variant hover:text-on-surface flex items-center gap-1 transition-colors"
        >
          ← Back to jobs
        </button>

        {/* ── Hero Section ───────────────────────────────────────────── */}
        <div className="bg-surface-container-lowest rounded-xl border border-border-subtle shadow-sm p-6 lg:p-8 flex flex-col lg:flex-row lg:items-start justify-between gap-6 relative overflow-hidden">
          <div className="absolute top-0 left-0 w-1 h-full bg-primary-container"></div>
          <div className="flex-1 space-y-4">
            <div className="flex flex-col sm:flex-row sm:items-center gap-4">
              <div className="w-16 h-16 bg-surface-variant rounded-lg border border-border-subtle flex items-center justify-center overflow-hidden shrink-0">
                <span className="font-headline-md text-headline-md text-on-surface-variant font-bold">
                  {job.company ? job.company.charAt(0).toUpperCase() : 'B'}
                </span>
              </div>
              <div>
                <h1 className="font-display-lg-mobile md:font-display-lg text-display-lg-mobile md:text-display-lg text-on-background mb-1">
                  {job.title}
                </h1>
                <div className="flex flex-wrap items-center gap-x-4 gap-y-2 font-label-sm text-label-sm text-on-surface-variant">
                  <span className="flex items-center gap-1">
                    <span className="material-symbols-outlined text-[18px]">domain</span>
                    {job.company}
                  </span>
                  {job.location && (
                    <span className="flex items-center gap-1">
                      <span className="material-symbols-outlined text-[18px]">location_on</span>
                      {job.location}
                    </span>
                  )}
                </div>
              </div>
            </div>
          </div>
          
          <div className="flex flex-col sm:flex-row lg:flex-col gap-3 shrink-0">
            {existingApplication ? (
              <a
                href="/applications"
                className="bg-success-green/10 text-success-green border border-success-green/20 px-4 py-2 rounded-lg font-label-sm text-label-sm font-semibold flex items-center justify-center gap-2 transition-colors hover:bg-success-green/20"
              >
                <span className="material-symbols-outlined text-[18px]">check</span>
                Already Tracking
              </a>
            ) : (
              <button
                id="save-to-tracker-btn"
                onClick={() => setShowAddDialog(true)}
                className="bg-surface-container-lowest border border-border-subtle text-on-background hover:bg-surface-container-low transition-colors px-4 py-2 rounded-lg font-label-sm text-label-sm font-semibold flex items-center justify-center gap-2"
              >
                <span className="material-symbols-outlined text-[18px]">bookmark_add</span>
                Save to Tracker
              </button>
            )}
            
            {job.jobUrl && (
              <a
                href={job.jobUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="bg-primary-container text-on-primary-container hover:bg-[#3f38b8] hover:text-white transition-colors px-4 py-2 rounded-lg font-label-sm text-label-sm font-semibold flex items-center justify-center gap-2 shadow-sm"
              >
                <span className="material-symbols-outlined text-[18px]">open_in_new</span>
                View Posting
              </a>
            )}
          </div>
        </div>

        {/* ── Main Content Grid ────────────────────────────────────────── */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 lg:gap-8">
          
          {/* Left Column: Description & Details */}
          <div className="lg:col-span-2 space-y-6">
            
            {/* Description */}
            <section className="bg-surface-container-lowest rounded-xl border border-border-subtle shadow-sm p-6 lg:p-8">
              <h2 className="font-headline-md text-headline-md text-on-background mb-4">About the Role</h2>
              <div className="prose prose-sm md:prose-base max-w-none font-body-md text-body-md text-on-surface-variant space-y-4 whitespace-pre-line">
                {job.description}
              </div>
            </section>

            {/* Required Skills Chips */}
            {job.requiredSkills && job.requiredSkills.length > 0 && (
              <section className="bg-surface-container-lowest rounded-xl border border-border-subtle shadow-sm p-6 lg:p-8">
                <h2 className="font-headline-md text-headline-md text-on-background mb-4">Required Skills</h2>
                <div className="flex flex-wrap gap-2">
                  {job.requiredSkills.map((skill) => (
                    <span
                      key={skill}
                      className="bg-surface-container-low text-on-surface-variant px-3 py-1.5 rounded-full font-label-xs text-label-xs border border-border-subtle"
                    >
                      {skill}
                    </span>
                  ))}
                </div>
              </section>
            )}
          </div>

          {/* Right Column: ATS Resume Generator Panel */}
          <div className="space-y-6">
            <div className="bg-surface-container-lowest rounded-xl border border-border-subtle shadow-sm p-6 space-y-4 relative overflow-hidden">
              <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-primary-container to-inverse-primary"></div>
              
              <div className="flex items-start gap-3 mt-2">
                <span className="material-symbols-outlined text-primary-container text-[24px]">magic_button</span>
                <div>
                  <h2 className="font-headline-md text-[18px] font-bold text-on-background leading-tight">
                    Generate Tailored Resume
                  </h2>
                  <p className="font-label-xs text-label-xs text-on-surface-variant mt-1">
                    AI weaves missing keywords into your experience without inventing skills.
                    <span className="text-text-muted ml-1">(5/hr limit)</span>
                  </p>
                </div>
              </div>

              {/* ── State: idle — show selector + button ─────────────────────── */}
              {!generatedResumeId && (
                <div className="space-y-4 mt-4">
                  <div className="flex flex-col gap-2">
                    <ResumeSelector
                      resumes={(uploadedResumes ?? []).map((r) => ({
                        id: r.id,
                        originalFilename: r.originalFilename ?? 'Unnamed resume',
                        status: r.status,
                      }))}
                      isLoading={resumesLoading}
                      selectedId={selectedResumeId}
                      onChange={(id) => {
                        setSelectedResumeId(id);
                        setValidationError(null);
                      }}
                    />
                    <button
                      onClick={() => setShowUploadModal(true)}
                      className="text-primary font-label-sm text-label-sm text-left hover:underline w-fit mt-1"
                    >
                      + Upload a new resume
                    </button>
                  </div>

                  {/* Validation / server error */}
                  {validationError && (
                    <div
                      role="alert"
                      className="rounded-lg bg-error-container border border-error px-4 py-3 font-label-sm text-label-sm text-on-error-container"
                    >
                      {validationError}
                    </div>
                  )}

                  <button
                    id="generate-tailored-resume-btn"
                    onClick={handleGenerate}
                    disabled={!selectedResumeId || tailoringMutation.isPending}
                    className="w-full bg-primary-container text-white py-3 rounded-lg font-label-sm text-label-sm font-bold hover:bg-[#3f38b8] transition-colors flex items-center justify-center gap-2 shadow-sm focus:ring-2 focus:ring-primary-container focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    {tailoringMutation.isPending ? (
                      <>
                        <span
                          className="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent"
                          aria-hidden
                        />
                        Starting…
                      </>
                    ) : (
                      <>
                        <span className="material-symbols-outlined text-[20px]">magic_button</span>
                        Generate Tailored Resume
                      </>
                    )}
                  </button>
                </div>
              )}

              {/* ── State: PENDING / PROCESSING — spinner + skeleton ──────────── */}
              {generatedResumeId && isProcessing && (
                <TailoringStatus status={generatedResume?.status ?? 'PENDING'} />
              )}

              {/* ── State: DONE — downloads + keyword gap report ──────────────── */}
              {isDone && generatedResume && (
                <div className="space-y-5 animate-in fade-in slide-in-from-bottom-2 duration-500 mt-4">
                  <div className="flex items-center gap-2">
                    <span className="material-symbols-outlined text-success-green text-[20px]">check_circle</span>
                    <p className="font-label-sm text-label-sm font-bold text-success-green">
                      Your tailored resume is ready!
                    </p>
                  </div>

                  {/* Download row */}
                  <div className="grid grid-cols-2 gap-3">
                    <a
                      id="download-pdf-btn"
                      href={`/api/v1/resumes/${generatedResume.id}/download/pdf`}
                      className="bg-surface-container-lowest border border-border-subtle text-on-surface-variant hover:bg-surface-container-low transition-colors px-3 py-2 rounded-lg font-label-sm text-label-sm font-semibold flex items-center justify-center gap-2"
                    >
                      <span className="material-symbols-outlined text-[18px]">picture_as_pdf</span>
                      PDF
                    </a>
                    <a
                      id="download-docx-btn"
                      href={`/api/v1/resumes/${generatedResume.id}/download/docx`}
                      className="bg-surface-container-lowest border border-border-subtle text-on-surface-variant hover:bg-surface-container-low transition-colors px-3 py-2 rounded-lg font-label-sm text-label-sm font-semibold flex items-center justify-center gap-2"
                    >
                      <span className="material-symbols-outlined text-[18px]">description</span>
                      DOCX
                    </a>
                  </div>
                  <button
                    onClick={handleRetry}
                    className="w-full text-center font-label-xs text-label-xs text-on-surface-variant hover:text-on-surface underline transition-colors mt-2"
                  >
                    Regenerate
                  </button>

                  <div className="pt-4 border-t border-border-subtle">
                    <button
                      onClick={() => setShowAddDialog(true)}
                      className="w-full bg-success-green text-white hover:bg-green-700 transition-colors px-4 py-3 rounded-lg font-label-sm text-label-sm font-bold flex items-center justify-center gap-2 shadow-sm"
                    >
                      <span className="material-symbols-outlined text-[20px]">send</span>
                      Apply for this role
                    </button>
                    {job.jobUrl && (
                      <p className="text-center text-xs text-text-muted mt-2">
                        Don&apos;t forget to use your newly generated resume!
                      </p>
                    )}
                  </div>

                  {/* Keyword gap report */}
                  <KeywordGapReport
                    matchingKeywords={matchingKws}
                    unmatchedKeywords={unmatchedKws}
                  />
                </div>
              )}

              {/* ── State: FAILED — error + retry ─────────────────────────────── */}
              {isFailed && (
                <div className="space-y-4 animate-in fade-in duration-300 mt-4">
                  <div
                    role="alert"
                    className="rounded-lg bg-error-container border border-error px-4 py-3 space-y-1"
                  >
                    <p className="font-label-sm text-label-sm font-bold text-on-error-container">Generation failed</p>
                    <p className="font-label-xs text-label-xs text-on-error-container/80">
                      {generatedResume?.errorMessage ||
                        'An unexpected error occurred. Your quota has not been consumed — please try again.'}
                    </p>
                  </div>
                  <button
                    id="retry-tailoring-btn"
                    onClick={handleRetry}
                    className="w-full bg-surface-container-lowest border border-border-subtle text-on-surface-variant hover:bg-surface-container-low transition-colors px-4 py-2 rounded-lg font-label-sm text-label-sm font-semibold flex items-center justify-center gap-2"
                  >
                    <span className="material-symbols-outlined text-[18px]">refresh</span>
                    Try again
                  </button>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Add application dialog */}
        {showAddDialog && jobId && (
          <AddApplicationDialog
            open={showAddDialog}
            onClose={() => setShowAddDialog(false)}
            jobId={jobId}
            jobTitle={job.title}
            company={job.company}
            onCreated={() => { setShowAddDialog(false); }}
            onDuplicate={(existing) => {
              setExistingApplication(existing);
              setShowAddDialog(false);
            }}
          />
        )}

        {showUploadModal && (
          <UploadResumeModal
            open={showUploadModal}
            onClose={() => setShowUploadModal(false)}
            onUploadComplete={() => {
              // Optionally do something here like selecting the new resume
            }}
          />
        )}
      </div>
    </main>
  );
}
