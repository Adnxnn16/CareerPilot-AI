'use client';

import { useState, useRef } from 'react';
import * as Dialog from '@radix-ui/react-dialog';
import api from '@/lib/api';
import { useQueryClient } from '@tanstack/react-query';

interface UploadResumeModalProps {
  open: boolean;
  onClose: () => void;
  onUploadComplete?: () => void;
}

export default function UploadResumeModal({ open, onClose, onUploadComplete }: UploadResumeModalProps) {
  const [file, setFile] = useState<File | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const queryClient = useQueryClient();

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      const selectedFile = e.target.files[0];
      const validTypes = [
        'application/pdf',
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
      ];
      if (!validTypes.includes(selectedFile.type)) {
        setError('Only PDF and DOCX files are allowed.');
        setFile(null);
        return;
      }
      setFile(selectedFile);
      setError(null);
    }
  };

  const handleUpload = async () => {
    if (!file) return;
    setIsUploading(true);
    setError(null);

    const formData = new FormData();
    formData.append('file', file);

    try {
      await api.post('/resumes/upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });
      queryClient.invalidateQueries({ queryKey: ['my-resumes'] });
      if (onUploadComplete) onUploadComplete();
      onClose();
    } catch (err: unknown) {
      if (err && typeof err === 'object' && 'response' in err) {
        const axiosErr = err as { response?: { data?: { error?: string, message?: string } } };
        setError(axiosErr.response?.data?.error || axiosErr.response?.data?.message || 'Upload failed');
      } else if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Upload failed');
      }
    } finally {
      setIsUploading(false);
    }
  };

  return (
    <Dialog.Root open={open} onOpenChange={onClose}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 bg-black/50 z-40 animate-in fade-in" />
        <Dialog.Content className="fixed left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 w-full max-w-md bg-surface-container-lowest rounded-xl shadow-lg z-50 p-6 animate-in fade-in zoom-in-95">
          <Dialog.Title className="font-headline-md text-headline-md text-on-background mb-2">
            Upload Resume
          </Dialog.Title>
          <Dialog.Description className="font-body-md text-body-md text-on-surface-variant mb-6">
            Upload your resume in PDF or DOCX format.
          </Dialog.Description>

          {error && (
            <div className="mb-4 p-3 bg-error-container text-on-error-container text-label-sm font-label-sm rounded-lg border border-error/20">
              {error}
            </div>
          )}

          <div 
            className={`border-2 border-dashed rounded-xl p-8 text-center cursor-pointer transition-colors ${file ? 'border-primary bg-primary/5' : 'border-border-subtle hover:bg-surface-variant hover:border-on-surface-variant/30'}`}
            onClick={() => fileInputRef.current?.click()}
          >
            <input
              type="file"
              ref={fileInputRef}
              onChange={handleFileChange}
              accept=".pdf,.docx,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
              className="hidden"
            />
            {file ? (
              <div className="flex flex-col items-center">
                <span className="material-symbols-outlined text-primary text-4xl mb-2">description</span>
                <p className="font-label-md text-label-md text-on-background font-semibold">{file.name}</p>
                <p className="font-label-xs text-label-xs text-on-surface-variant mt-1">
                  {(file.size / 1024 / 1024).toFixed(2)} MB
                </p>
              </div>
            ) : (
              <div className="flex flex-col items-center text-on-surface-variant">
                <span className="material-symbols-outlined text-4xl mb-2">upload_file</span>
                <p className="font-label-md text-label-md font-semibold">Click to select file</p>
                <p className="font-label-xs text-label-xs mt-1">PDF or DOCX up to 5MB</p>
              </div>
            )}
          </div>

          <div className="mt-8 flex justify-end gap-3">
            <button
              onClick={onClose}
              disabled={isUploading}
              className="px-4 py-2 rounded-lg font-label-sm text-label-sm font-semibold text-on-surface-variant hover:bg-surface-variant transition-colors disabled:opacity-50"
            >
              Cancel
            </button>
            <button
              onClick={handleUpload}
              disabled={!file || isUploading}
              className="px-4 py-2 rounded-lg font-label-sm text-label-sm font-semibold bg-primary-container text-white hover:bg-[#3f38b8] transition-colors disabled:opacity-50 flex items-center gap-2"
            >
              {isUploading ? (
                <>
                  <span className="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent" />
                  Uploading...
                </>
              ) : (
                'Upload'
              )}
            </button>
          </div>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
