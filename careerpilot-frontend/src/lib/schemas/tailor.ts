// src/lib/schemas/tailor.ts
// Zod schemas for F6 — mirrors backend Bean Validation exactly.

import { z } from 'zod';

export const tailoringRequestSchema = z.object({
  sourceResumeId: z
    .string({ error: 'Please select a resume to tailor from.' })
    .uuid({ message: 'Invalid resume selection.' }),
});

export type TailoringRequestForm = z.infer<typeof tailoringRequestSchema>;

// ── Resume status polling response schema ──────────────────────────────────

export const parseStatusSchema = z.enum(['PENDING', 'PROCESSING', 'DONE', 'FAILED']);
export type ParseStatus = z.infer<typeof parseStatusSchema>;

export const resumeDTOSchema = z.object({
  id: z.string().uuid(),
  userId: z.string().uuid(),
  originalFilename: z.string().nullable().optional(),
  status: parseStatusSchema,
  errorMessage: z.string().nullable().optional(),
  parsedSkills: z.string().nullable().optional(),
  parsedExperience: z.string().nullable().optional(),
  // F6 fields
  sourceType: z.enum(['UPLOADED', 'GENERATED']),
  sourceJobId: z.string().uuid().nullable().optional(),
  fileKeyPdf: z.string().nullable().optional(),
  fileKeyDocx: z.string().nullable().optional(),
  unmatchedKeywords: z.array(z.string()).nullable().optional(),
});

export type ResumeDTO = z.infer<typeof resumeDTOSchema>;
