'use client';

import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import api from '@/lib/api';
import { useAuthStore } from '@/store/authStore';
import { useRouter } from 'next/navigation';

const loginSchema = z.object({
  email: z.string().email(),
  password: z.string().min(1, 'Password is required'),
});

type LoginForm = z.infer<typeof loginSchema>;

export default function LoginPage() {
  const router = useRouter();
  const checkAuth = useAuthStore((state) => state.checkAuth);
  const [error, setError] = useState('');
  
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<LoginForm>({
    resolver: zodResolver(loginSchema)
  });

  const onSubmit = async (data: LoginForm) => {
    try {
      setError('');
      await api.post('/auth/login', data);
      await checkAuth();
      router.push('/');
    } catch (err: unknown) {
      if (err && typeof err === 'object' && 'response' in err) {
        const axiosErr = err as { response?: { data?: { message?: string } } };
        setError(axiosErr.response?.data?.message || 'Login failed');
      } else if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Login failed');
      }
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center p-4 bg-background">
      <div className="w-full max-w-[440px] bg-surface-primary border border-border-subtle rounded-xl shadow-[0px_4px_12px_rgba(0,0,0,0.05)] overflow-hidden flex flex-col">
        <div className="px-8 pt-10 pb-8 text-center flex flex-col items-center">
          <span className="material-symbols-outlined text-primary text-4xl mb-4" style={{ fontVariationSettings: "'FILL' 1" }}>psychology</span>
          <h2 className="font-headline-md text-headline-md text-on-surface">CareerPilot AI</h2>
          <p className="font-body-md text-body-md text-text-muted mt-2">The Calm Mentor</p>
        </div>
        
        <div className="px-8 pb-10">
          {error && (
            <div className="bg-error-container border border-error/20 rounded-lg p-3 mb-6 flex items-start gap-3">
              <span className="material-symbols-outlined text-error mt-0.5" style={{ fontSize: '20px' }}>error</span>
              <p className="font-label-sm text-label-sm text-on-error-container">{error}</p>
            </div>
          )}
          
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
            <div className="space-y-1">
              <label className="block font-label-sm text-label-sm text-on-surface-variant">Email</label>
              <input 
                {...register('email')}
                className="w-full bg-surface-primary border border-border-subtle rounded-md px-3 py-2 font-body-md text-body-md text-on-surface placeholder:text-text-muted focus:border-primary focus:ring-2 focus:ring-primary/20 focus:outline-none transition-colors"
                placeholder="you@example.com"
              />
              {errors.email && <p className="text-error font-label-xs text-label-xs mt-1">{errors.email.message}</p>}
            </div>

            <div className="space-y-1">
              <div className="flex items-center justify-between">
                <label className="block font-label-sm text-label-sm text-on-surface-variant">Password</label>
              </div>
              <input 
                type="password"
                {...register('password')}
                className="w-full bg-surface-primary border border-border-subtle rounded-md px-3 py-2 font-body-md text-body-md text-on-surface placeholder:text-text-muted focus:border-primary focus:ring-2 focus:ring-primary/20 focus:outline-none transition-colors"
                placeholder="••••••••"
              />
              {errors.password && <p className="text-error font-label-xs text-label-xs mt-1">{errors.password.message}</p>}
            </div>

            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full bg-primary-container hover:bg-[#4338CA] text-white font-label-sm text-label-sm py-2.5 rounded-md transition-colors focus:outline-none focus:ring-2 focus:ring-primary-container focus:ring-offset-2 disabled:opacity-50 flex items-center justify-center"
            >
              {isSubmitting ? (
                <>
                  <span className="animate-spin h-4 w-4 border-2 border-white border-t-transparent rounded-full mr-2"></span>
                  Signing in...
                </>
              ) : 'Sign in'}
            </button>
          </form>

          <div className="mt-8 pt-6 border-t border-border-subtle text-center">
            <p className="font-body-md text-body-md text-on-surface-variant">
              Don&apos;t have an account? 
              <a className="text-primary font-medium hover:text-primary-container transition-colors ml-1" href="/register">
                Create an account
              </a>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
