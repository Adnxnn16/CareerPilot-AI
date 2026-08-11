'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import api from '@/lib/api';
import { useAuthStore } from '@/store/authStore';
import { useRouter } from 'next/navigation';
import JobDetailSkeleton from '@/components/jobs/JobDetailSkeleton';

interface Job {
  id: string;
  title: string;
  company: string;
  location: string;
  summary: string;
  description: string;
  requiredSkills: string[];
}

export default function JobsPage() {
  const [jobs, setJobs] = useState<Job[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedJobId, setSelectedJobId] = useState<string | null>(null);
  
  const { isAuthenticated } = useAuthStore();
  const router = useRouter();

  useEffect(() => {
    if (!isAuthenticated) {
      router.push('/login');
      return;
    }

    const fetchJobs = async () => {
      try {
        const response = await api.get('/jobs/search');
        const data = response.data;
        const fetchedJobs = Array.isArray(data) ? data : data.content || [];
        setJobs(fetchedJobs);
        if (fetchedJobs.length > 0) {
          setSelectedJobId(fetchedJobs[0].id);
        }
      } catch (err: unknown) {
        setError('Failed to load jobs. Please try again later.');
        console.error(err);
      } finally {
        setLoading(false);
      }
    };

    fetchJobs();
  }, [isAuthenticated, router]);

  const filteredJobs = jobs.filter(job => 
    job.title.toLowerCase().includes(searchQuery.toLowerCase()) || 
    job.company?.toLowerCase().includes(searchQuery.toLowerCase()) ||
    job.location?.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const selectedJob = jobs.find(j => j.id === selectedJobId);

  if (loading) {
    return (
      <div className="flex-1 p-4 md:p-gutter max-w-container-max mx-auto w-full grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-1 space-y-4">
          {[1, 2, 3, 4].map(i => (
            <div key={i} className="animate-pulse bg-surface-container-lowest h-32 rounded-xl border border-border-subtle"></div>
          ))}
        </div>
        <div className="lg:col-span-2 hidden lg:block">
          <JobDetailSkeleton />
        </div>
      </div>
    );
  }

  return (
    <div className="flex-1 flex flex-col p-4 md:p-gutter max-w-container-max mx-auto w-full h-[calc(100vh-64px)] overflow-hidden">
      <div className="mb-4 shrink-0 flex flex-col md:flex-row md:items-end justify-between gap-4">
        <div>
          <h2 className="font-display-lg-mobile md:font-display-lg text-display-lg-mobile md:text-display-lg text-on-surface mb-2">
            Available Jobs
          </h2>
          <p className="font-body-lg text-body-lg text-text-muted">
            Find the perfect match for your career and tailor your resume.
          </p>
        </div>
        <div className="relative w-full md:w-72">
          <span className="material-symbols-outlined absolute left-3 top-2.5 text-text-muted">search</span>
          <input 
            type="text"
            placeholder="Search by title, company, location..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-10 pr-4 py-2 bg-surface-container-lowest border border-border-subtle rounded-lg focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent font-body-md text-body-md"
          />
        </div>
      </div>

      {error && (
        <div className="bg-error-container text-on-error-container p-4 rounded-lg mb-6 shrink-0">
          {error}
        </div>
      )}

      {jobs.length === 0 && !error ? (
        <div className="text-center p-12 bg-surface-container-lowest rounded-xl border border-border-subtle shrink-0">
          <span className="material-symbols-outlined text-4xl text-text-muted mb-4">search_off</span>
          <h3 className="text-lg font-semibold text-on-surface">No jobs found</h3>
          <p className="text-text-muted mt-2">Check back later for new opportunities.</p>
        </div>
      ) : (
        <div className="flex-1 min-h-0 flex gap-6 mt-2">
          {/* Left Pane: Job List */}
          <div className="w-full lg:w-1/3 flex flex-col gap-3 overflow-y-auto pr-2 pb-20 lg:pb-0 custom-scrollbar">
            {filteredJobs.length === 0 ? (
              <p className="text-text-muted text-center py-8">No jobs match your search.</p>
            ) : (
              filteredJobs.map((job) => (
                <button 
                  key={job.id} 
                  onClick={() => {
                    if (window.innerWidth < 1024) {
                      router.push('/jobs/' + job.id);
                    } else {
                      setSelectedJobId(job.id);
                    }
                  }}
                  className={`text-left bg-surface-container-lowest border rounded-xl p-5 hover:shadow-md transition-shadow group flex flex-col flex-shrink-0 ${selectedJobId === job.id ? 'border-primary ring-1 ring-primary' : 'border-border-subtle'}`}
                >
                  <div className="flex-1">
                    <h3 className="font-headline-md text-headline-md text-on-surface mb-1 group-hover:text-primary transition-colors line-clamp-1">
                      {job.title}
                    </h3>
                    <div className="flex items-center gap-2 text-on-surface-variant text-sm mb-3">
                      <span className="font-semibold truncate max-w-[120px]">{job.company}</span>
                      <span>•</span>
                      <span className="truncate">{job.location}</span>
                    </div>
                    <p className="text-sm text-text-muted mb-3 line-clamp-2">
                      {job.summary || job.description}
                    </p>
                  </div>
                  {job.requiredSkills && job.requiredSkills.length > 0 && (
                    <div className="flex flex-wrap gap-2 mt-2">
                      {job.requiredSkills.slice(0, 3).map((skill, index) => (
                        <span 
                          key={index}
                          className="px-2 py-1 bg-surface-container text-on-surface-variant text-xs rounded-md truncate max-w-[100px]"
                        >
                          {skill}
                        </span>
                      ))}
                      {job.requiredSkills.length > 3 && (
                        <span className="px-2 py-1 bg-surface-container text-on-surface-variant text-xs rounded-md">
                          +{job.requiredSkills.length - 3}
                        </span>
                      )}
                    </div>
                  )}
                </button>
              ))
            )}
          </div>

          {/* Right Pane: Job Details */}
          <div className="hidden lg:flex flex-col w-2/3 bg-surface-container-lowest rounded-xl border border-border-subtle shadow-sm overflow-hidden h-full">
            {selectedJob ? (
              <div className="flex-1 overflow-y-auto p-8 custom-scrollbar relative">
                <div className="sticky top-0 bg-surface-container-lowest pb-6 pt-2 z-10 border-b border-border-subtle mb-6">
                  <h1 className="font-display-md text-display-md text-on-background mb-2">
                    {selectedJob.title}
                  </h1>
                  <div className="flex flex-wrap items-center gap-x-4 gap-y-2 font-label-md text-label-md text-on-surface-variant mb-6">
                    <span className="flex items-center gap-1">
                      <span className="material-symbols-outlined text-[20px]">domain</span>
                      {selectedJob.company}
                    </span>
                    {selectedJob.location && (
                      <span className="flex items-center gap-1">
                        <span className="material-symbols-outlined text-[20px]">location_on</span>
                        {selectedJob.location}
                      </span>
                    )}
                  </div>
                  <div className="flex gap-3">
                    <Link
                      href={`/jobs/${selectedJob.id}`}
                      className="bg-primary text-on-primary px-6 py-2.5 rounded-lg font-label-sm text-label-sm font-bold hover:bg-primary-container hover:text-on-primary-container transition-colors shadow-sm flex items-center justify-center gap-2"
                    >
                      <span className="material-symbols-outlined text-[18px]">magic_button</span>
                      Tailor Resume
                    </Link>
                  </div>
                </div>
                
                <h2 className="font-headline-md text-headline-md text-on-background mb-4">About the Role</h2>
                <div className="prose prose-sm md:prose-base max-w-none font-body-md text-body-md text-on-surface-variant space-y-4 whitespace-pre-line mb-8">
                  {selectedJob.description || selectedJob.summary}
                </div>

                {selectedJob.requiredSkills && selectedJob.requiredSkills.length > 0 && (
                  <>
                    <h2 className="font-headline-md text-headline-md text-on-background mb-4">Required Skills</h2>
                    <div className="flex flex-wrap gap-2 pb-8">
                      {selectedJob.requiredSkills.map((skill) => (
                        <span
                          key={skill}
                          className="bg-surface-container-low text-on-surface-variant px-3 py-1.5 rounded-full font-label-xs text-label-xs border border-border-subtle"
                        >
                          {skill}
                        </span>
                      ))}
                    </div>
                  </>
                )}
              </div>
            ) : (
              <div className="flex-1 flex flex-col items-center justify-center text-text-muted p-8">
                <span className="material-symbols-outlined text-6xl mb-4">work</span>
                <p>Select a job from the list to view details</p>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
