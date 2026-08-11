'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useAuthStore } from '@/store/authStore';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';
import UploadResumeModal from '@/components/resume/UploadResumeModal';
import type { BoardDTO } from '@/lib/schemas/application';
import type { ResumeDTO } from '@/lib/schemas/tailor';

export default function Home() {
  const { isAuthenticated, user } = useAuthStore();
  const [showUploadModal, setShowUploadModal] = useState(false);
  const queryClient = useQueryClient();

  // Fetch applications board
  const { data: board } = useQuery<BoardDTO>({
    queryKey: ['applications', 'board'],
    queryFn: async () => {
      const res = await api.get('/applications/board');
      return res.data;
    },
    enabled: isAuthenticated,
  });

  // Fetch resumes
  const { data: resumes } = useQuery({
    queryKey: ['my-resumes'],
    queryFn: async () => {
      const res = await api.get('/users/me/resumes');
      return res.data;
    },
    enabled: isAuthenticated,
  });

  // Fetch jobs
  const { data: jobs } = useQuery({
    queryKey: ['dashboard-jobs'],
    queryFn: async () => {
      const res = await api.get('/jobs/search');
      const data = res.data;
      return Array.isArray(data) ? data : data.content || [];
    },
    enabled: isAuthenticated,
  });

  if (!isAuthenticated) {
    return (
      <div className="flex-1 p-4 md:p-gutter max-w-container-max mx-auto w-full pb-20 md:pb-8 flex flex-col items-center justify-center min-h-[80vh] text-center">
        <span className="material-symbols-outlined text-primary text-6xl mb-6" style={{ fontVariationSettings: "'FILL' 1" }}>psychology</span>
        <h1 className="font-display-lg text-display-lg text-on-surface mb-4">Welcome to CareerPilot AI</h1>
        <p className="font-body-lg text-body-lg text-text-muted mb-8 max-w-lg">
          The Calm Mentor that helps you navigate your career, tailor your resume for specific jobs, and land interviews.
        </p>
        <Link href="/login" className="bg-primary-container text-on-primary-container px-8 py-3 rounded-lg font-label-md text-label-md font-bold hover:bg-primary-container/80 transition-colors shadow-sm">
          Sign In to Get Started
        </Link>
      </div>
    );
  }

  // Calculate stats
  const activeApplications = board?.columns
    ? Object.entries(board.columns)
        .filter(([status]) => status !== 'REJECTED')
        .reduce((sum, [, apps]) => sum + apps.length, 0)
    : 0;

  const uploadedResumesCount = resumes?.filter((r: ResumeDTO) => r.sourceType === 'UPLOADED' && r.status === 'DONE').length || 0;
  const newJobMatchesCount = jobs?.length || 0;

  return (
    <div className="flex-1 p-4 md:p-gutter max-w-container-max mx-auto w-full pb-20 md:pb-8">
      {/* Desktop Header Area (Search & Profile) */}
      <div className="hidden md:flex justify-between items-center mb-stack-lg w-full mt-4">
        <div className="relative w-96">
          <span className="material-symbols-outlined absolute left-3 top-2.5 text-text-muted">search</span>
          <input 
            className="w-full pl-10 pr-4 py-2 bg-surface-container-lowest border border-border-subtle rounded-lg focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent font-body-md text-body-md" 
            placeholder="Search applications, jobs, or resumes..." 
            type="text" 
          />
        </div>
        <div className="flex items-center gap-4">
          <button className="w-10 h-10 rounded-full border border-border-subtle flex items-center justify-center text-text-muted hover:bg-surface-container transition-colors">
            <span className="material-symbols-outlined">notifications</span>
          </button>
        </div>
      </div>

      {/* Welcome Message */}
      <div className="mb-stack-lg mt-4 md:mt-0">
        <h2 className="font-display-lg-mobile md:font-display-lg text-display-lg-mobile md:text-display-lg text-on-surface mb-2">
          Welcome back{user ? `, ${user.name.split(' ')[0]}` : ''}
        </h2>
        <p className="font-body-lg text-body-lg text-text-muted">
          Here&apos;s an overview of your career progression today.
        </p>
      </div>

      {/* Dashboard Grid */}
      <div className="grid grid-cols-1 md:grid-cols-12 gap-gutter mb-stack-lg">
        
        {/* Summary Cards (Span 8 cols on desktop) */}
        <div className="md:col-span-8 grid grid-cols-1 sm:grid-cols-3 gap-gutter">
          {/* Card 1: Active Applications */}
          <Link href="/applications" className="bg-surface-container-lowest border border-border-subtle rounded-xl p-6 shadow-[0px_4px_12px_rgba(0,0,0,0.05)] hover:shadow-md transition-shadow group">
            <div className="flex justify-between items-start mb-4">
              <div className="w-10 h-10 rounded-lg bg-secondary-container flex items-center justify-center text-primary">
                <span className="material-symbols-outlined group-hover:scale-110 transition-transform">work_history</span>
              </div>
            </div>
            <h3 className="font-label-sm text-label-sm text-text-muted mb-1">Active Applications</h3>
            <p className="font-headline-md text-headline-md text-on-surface">{activeApplications}</p>
            <div className="mt-4 flex items-center gap-1 text-success-green font-label-xs text-label-xs">
              <span className="material-symbols-outlined text-[14px]">trending_up</span>
              <span>Tracking active</span>
            </div>
          </Link>

          {/* Card 2: Resumes on File */}
          <div className="bg-surface-container-lowest border border-border-subtle rounded-xl p-6 shadow-[0px_4px_12px_rgba(0,0,0,0.05)] hover:shadow-md transition-shadow">
            <div className="flex justify-between items-start mb-4">
              <div className="w-10 h-10 rounded-lg bg-surface-container flex items-center justify-center text-on-surface-variant">
                <span className="material-symbols-outlined">description</span>
              </div>
            </div>
            <h3 className="font-label-sm text-label-sm text-text-muted mb-1">Resumes on File</h3>
            <p className="font-headline-md text-headline-md text-on-surface">{uploadedResumesCount}</p>
            <div className="mt-4 font-label-xs text-label-xs text-text-muted">
              Ready for tailoring
            </div>
          </div>

          {/* Card 3: New Job Matches */}
          <Link href="/jobs" className="bg-surface-container-lowest border border-border-subtle rounded-xl p-6 shadow-[0px_4px_12px_rgba(0,0,0,0.05)] hover:shadow-md transition-shadow group">
            <div className="flex justify-between items-start mb-4">
              <div className="w-10 h-10 rounded-lg bg-tertiary-fixed flex items-center justify-center text-tertiary">
                <span className="material-symbols-outlined group-hover:scale-110 transition-transform">auto_awesome</span>
              </div>
            </div>
            <h3 className="font-label-sm text-label-sm text-text-muted mb-1">New Job Matches</h3>
            <p className="font-headline-md text-headline-md text-on-surface">{newJobMatchesCount}</p>
            <div className="mt-4 flex items-center gap-1 text-primary font-label-xs text-label-xs">
              <span>View matches</span>
              <span className="material-symbols-outlined text-[14px]">arrow_forward</span>
            </div>
          </Link>
        </div>

        {/* Quick Actions (Span 4 cols on desktop) */}
        <div className="md:col-span-4 flex flex-col gap-4">
          <div className="bg-surface-container-lowest border border-border-subtle rounded-xl p-6 shadow-[0px_4px_12px_rgba(0,0,0,0.05)] h-full flex flex-col justify-center gap-4">
            <h3 className="font-label-sm text-label-sm font-semibold text-on-surface uppercase tracking-wider mb-2">
              Quick Actions
            </h3>
            <button 
              onClick={() => setShowUploadModal(true)}
              className="w-full py-3 px-4 bg-primary-container text-on-primary font-label-sm text-label-sm rounded-lg hover:bg-[#3f38b8] transition-colors flex items-center justify-center gap-2"
            >
              <span className="material-symbols-outlined">upload_file</span>
              Upload Resume
            </button>
            <Link href="/jobs" className="w-full py-3 px-4 bg-surface-container-lowest text-on-surface border border-border-subtle font-label-sm text-label-sm rounded-lg hover:bg-surface-container-low transition-colors flex items-center justify-center gap-2">
              <span className="material-symbols-outlined">search</span>
              Browse Jobs
            </Link>
          </div>
        </div>
      </div>
      
      {showUploadModal && (
        <UploadResumeModal 
          open={showUploadModal} 
          onClose={() => setShowUploadModal(false)}
          onUploadComplete={() => queryClient.invalidateQueries({ queryKey: ['my-resumes'] })}
        />
      )}
    </div>
  );
}
