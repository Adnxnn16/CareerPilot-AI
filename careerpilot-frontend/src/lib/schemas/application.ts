// src/lib/schemas/application.ts
// Zod schemas for F5 — mirrors backend Bean Validation exactly.

import { z } from 'zod';

// ── Enums ─────────────────────────────────────────────────────────────────────

export const applicationStatusSchema = z.enum([
  'SAVED',
  'APPLIED',
  'SCREENING',
  'INTERVIEW',
  'OFFER',
  'REJECTED',
]);

export type ApplicationStatus = z.infer<typeof applicationStatusSchema>;

export const APPLICATION_STATUSES: ApplicationStatus[] = [
  'SAVED',
  'APPLIED',
  'SCREENING',
  'INTERVIEW',
  'OFFER',
  'REJECTED',
];

// ── Column display metadata ───────────────────────────────────────────────────

export const STATUS_META: Record<
  ApplicationStatus,
  { label: string; color: string; bg: string; ring: string; dot: string }
> = {
  SAVED:     { label: 'Saved',     color: 'text-on-surface-variant', bg: 'bg-surface-variant',   ring: 'ring-outline-variant', dot: 'bg-outline' },
  APPLIED:   { label: 'Applied',   color: 'text-primary',            bg: 'bg-primary-container', ring: 'ring-primary-fixed',   dot: 'bg-primary' },
  SCREENING: { label: 'Screening', color: 'text-secondary',          bg: 'bg-secondary-container', ring: 'ring-secondary-fixed', dot: 'bg-secondary' },
  INTERVIEW: { label: 'Interview', color: 'text-tertiary',           bg: 'bg-tertiary-fixed',    ring: 'ring-tertiary-fixed-dim', dot: 'bg-tertiary' },
  OFFER:     { label: 'Offer',     color: 'text-success-green',      bg: 'bg-success-green/10',  ring: 'ring-success-green/20', dot: 'bg-success-green' },
  REJECTED:  { label: 'Rejected',  color: 'text-error',              bg: 'bg-error-container',   ring: 'ring-error-container', dot: 'bg-error' },
};

// ── JobSnapshot ───────────────────────────────────────────────────────────────

export const jobSnapshotSchema = z.object({
  title: z.string().nullable().optional(),
  company: z.string().nullable().optional(),
  location: z.string().nullable().optional(),
});

export type JobSnapshot = z.infer<typeof jobSnapshotSchema>;

// ── ApplicationDTO ────────────────────────────────────────────────────────────

export const applicationDTOSchema = z.object({
  id: z.string().uuid(),
  jobId: z.string().uuid().nullable().optional(),
  resumeId: z.string().uuid().nullable().optional(),
  status: applicationStatusSchema,
  appliedDate: z.string().nullable().optional(),
  notes: z.string().nullable().optional(),
  statusChangedAt: z.string().nullable().optional(),
  createdAt: z.string(),
  updatedAt: z.string(),
  version: z.number(),
  jobSnapshot: jobSnapshotSchema.nullable().optional(),
});

export type ApplicationDTO = z.infer<typeof applicationDTOSchema>;

// ── BoardDTO ──────────────────────────────────────────────────────────────────

export const boardDTOSchema = z.object({
  columns: z.record(applicationStatusSchema, z.array(applicationDTOSchema)),
});

export type BoardDTO = z.infer<typeof boardDTOSchema>;

// ── Request schemas ───────────────────────────────────────────────────────────

export const createApplicationSchema = z.object({
  resumeId: z.string().uuid().optional(),
  notes: z.string().max(4000, 'Notes must not exceed 4000 characters').optional(),
  appliedDate: z.string().optional(),
});

export type CreateApplicationForm = z.infer<typeof createApplicationSchema>;

export const updateStatusSchema = z.object({
  status: applicationStatusSchema,
  version: z.number(),
});

export const updateNotesSchema = z.object({
  notes: z.string().max(4000, 'Notes must not exceed 4000 characters').nullable(),
});
