'use client';

import { useState } from 'react';
import * as Dialog from '@radix-ui/react-dialog';
import api from '@/lib/api';
import { useAuthStore } from '@/store/authStore';
import { useQueryClient } from '@tanstack/react-query';

interface ConfirmSkillsModalProps {
  open: boolean;
  onClose: () => void;
  initialSkills: string[];
  resumeId: string;
}

export default function ConfirmSkillsModal({ open, onClose, initialSkills, resumeId }: ConfirmSkillsModalProps) {
  const [skills, setSkills] = useState<string[]>(initialSkills);
  const [newSkill, setNewSkill] = useState('');
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  
  const { checkAuth } = useAuthStore();
  const queryClient = useQueryClient();

  const handleAddSkill = () => {
    if (newSkill.trim() && !skills.includes(newSkill.trim())) {
      setSkills([...skills, newSkill.trim()]);
      setNewSkill('');
    }
  };

  const handleRemoveSkill = (skillToRemove: string) => {
    setSkills(skills.filter(s => s !== skillToRemove));
  };

  const handleSave = async () => {
    setIsSaving(true);
    setError(null);
    try {
      await api.post(`/resumes/${resumeId}/confirm-skills`, skills);
      await checkAuth(); // Refresh user state
      queryClient.invalidateQueries({ queryKey: ['my-resumes'] });
      onClose();
    } catch (err: unknown) {
      if (err && typeof err === 'object' && 'response' in err) {
        const axiosErr = err as { response?: { data?: { message?: string } } };
        setError(axiosErr.response?.data?.message || 'Failed to save skills');
      } else {
        setError('Failed to save skills');
      }
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <Dialog.Root open={open} onOpenChange={onClose}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 bg-black/50 z-40 animate-in fade-in" />
        <Dialog.Content className="fixed left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 w-full max-w-lg bg-surface-container-lowest rounded-xl shadow-lg z-50 p-6 animate-in fade-in zoom-in-95">
          <Dialog.Title className="font-headline-md text-headline-md text-on-background mb-2">
            Confirm Extracted Skills
          </Dialog.Title>
          <Dialog.Description className="font-body-md text-body-md text-on-surface-variant mb-6">
            We extracted the following skills from your resume. Review and edit them before saving to your profile.
          </Dialog.Description>

          {error && (
            <div className="mb-4 p-3 bg-error-container text-on-error-container text-label-sm font-label-sm rounded-lg border border-error/20">
              {error}
            </div>
          )}

          <div className="space-y-4">
            <div className="flex flex-wrap gap-2 max-h-[200px] overflow-y-auto p-2 border border-border-subtle rounded-lg bg-surface-bright">
              {skills.length === 0 && (
                <span className="text-on-surface-variant text-label-sm font-label-sm italic p-2">No skills found. Add some below.</span>
              )}
              {skills.map(skill => (
                <span key={skill} className="bg-primary-container/10 text-primary-container px-3 py-1.5 rounded-full font-label-sm text-label-sm flex items-center gap-1 border border-primary-container/20">
                  {skill}
                  <button onClick={() => handleRemoveSkill(skill)} className="hover:text-error transition-colors focus:outline-none ml-1">
                    <span className="material-symbols-outlined text-[16px]">close</span>
                  </button>
                </span>
              ))}
            </div>

            <div className="flex gap-2">
              <input
                type="text"
                value={newSkill}
                onChange={(e) => setNewSkill(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleAddSkill()}
                placeholder="Add a new skill..."
                className="flex-1 bg-surface-primary border border-border-subtle rounded-lg px-3 py-2 font-body-md text-body-md focus:border-primary focus:ring-1 focus:ring-primary outline-none"
              />
              <button
                onClick={handleAddSkill}
                className="px-4 py-2 bg-surface-variant text-on-surface hover:bg-surface-container-high rounded-lg font-label-sm text-label-sm font-semibold transition-colors"
              >
                Add
              </button>
            </div>
          </div>

          <div className="mt-8 flex justify-end gap-3">
            <button
              onClick={onClose}
              disabled={isSaving}
              className="px-4 py-2 rounded-lg font-label-sm text-label-sm font-semibold text-on-surface-variant hover:bg-surface-variant transition-colors disabled:opacity-50"
            >
              Skip
            </button>
            <button
              onClick={handleSave}
              disabled={isSaving}
              className="px-4 py-2 rounded-lg font-label-sm text-label-sm font-semibold bg-primary-container text-white hover:bg-[#3f38b8] transition-colors disabled:opacity-50 flex items-center gap-2"
            >
              {isSaving ? (
                <>
                  <span className="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent" />
                  Saving...
                </>
              ) : (
                'Save to Profile'
              )}
            </button>
          </div>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
