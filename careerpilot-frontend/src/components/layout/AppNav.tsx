'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useEffect } from 'react';
import { useAuthStore } from '@/store/authStore';

const NAV_LINKS = [
  { href: '/',             label: 'Dashboard',    icon: 'dashboard' },
  { href: '/jobs',         label: 'Job Matches',  icon: 'work' },
  { href: '/applications', label: 'Applications', icon: 'view_kanban' },
];

export default function AppNav() {
  const pathname = usePathname();
  const { isAuthenticated, user, logout, checkAuth } = useAuthStore();

  useEffect(() => {
    checkAuth();
  }, [checkAuth]);

  // Don't render nav on auth pages
  if (pathname === '/login' || pathname === '/register') return null;

  return (
    <>
      {/* Desktop Sidebar (Left) */}
      <nav className="hidden md:flex flex-col w-64 bg-surface-container-lowest border-r border-border-subtle shadow-sm p-4 space-y-stack-md shrink-0">
        <div className="flex items-center gap-3 px-2 mb-8">
          <div className="w-10 h-10 rounded-lg bg-primary-container flex items-center justify-center text-on-primary-container font-headline-md font-bold shrink-0">
            CP
          </div>
          <div className="truncate">
            <h1 className="font-headline-md text-[18px] font-bold text-primary truncate">CareerPilot AI</h1>
            <p className="font-label-xs text-label-xs text-on-surface-variant truncate">The Calm Mentor</p>
          </div>
        </div>
        
        {/* Links */}
        <div className="flex flex-col gap-2 flex-1">
          {NAV_LINKS.map((link) => {
            const isActive = link.href === '/' ? pathname === '/' : pathname.startsWith(link.href);
            return (
              <Link
                key={link.href}
                href={link.href}
                className={`flex items-center gap-3 px-4 py-3 rounded-lg font-label-sm text-label-sm font-semibold transition-colors ${
                  isActive
                    ? 'bg-primary-container text-on-primary-container'
                    : 'text-on-surface-variant hover:bg-surface-container-low hover:text-on-surface'
                }`}
              >
                <span className="material-symbols-outlined text-[20px]" style={{ fontVariationSettings: isActive ? "'FILL' 1" : "'FILL' 0" }}>
                  {link.icon}
                </span>
                {link.label}
              </Link>
            );
          })}
        </div>
        
        {/* User section */}
        {isAuthenticated && user ? (
          <div className="mt-auto pt-4 border-t border-border-subtle">
            <div className="flex items-center justify-between px-2 mb-4">
              <span className="font-label-xs text-label-xs text-on-surface-variant truncate max-w-[140px]">
                {user.email}
              </span>
            </div>
            <button
              onClick={logout}
              className="w-full flex items-center gap-3 px-4 py-3 rounded-lg text-on-surface-variant hover:bg-surface-container-low hover:text-error font-label-sm text-label-sm font-medium transition-colors"
            >
              <span className="material-symbols-outlined text-[20px]">logout</span>
              Sign out
            </button>
          </div>
        ) : (
          <div className="mt-auto pt-4 border-t border-border-subtle">
            <Link
              href="/login"
              className="w-full flex items-center gap-3 px-4 py-3 rounded-lg text-primary hover:bg-primary-container/20 font-label-sm text-label-sm font-medium transition-colors"
            >
              <span className="material-symbols-outlined text-[20px]">login</span>
              Sign in
            </Link>
          </div>
        )}
      </nav>

      {/* Mobile Bottom Navigation & Top Bar */}
      <div className="md:hidden flex flex-col">
        {/* Mobile Top Bar */}
        <header className="sticky top-0 z-30 bg-surface-container-lowest border-b border-border-subtle shadow-sm flex items-center justify-between px-4 h-14">
          <Link href="/" className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-md bg-primary-container flex items-center justify-center text-on-primary-container font-bold text-sm">CP</div>
            <span className="font-headline-md text-[16px] font-bold text-primary">CareerPilot</span>
          </Link>
          
          <div className="flex items-center gap-2">
            {isAuthenticated && user ? (
              <button onClick={logout} className="p-2 text-on-surface-variant hover:text-error" aria-label="Sign out">
                <span className="material-symbols-outlined text-[20px]">logout</span>
              </button>
            ) : (
              <Link href="/login" className="font-label-sm text-label-sm font-semibold text-primary">
                Sign in
              </Link>
            )}
          </div>
        </header>

        {/* Mobile Bottom Tab Bar */}
        <nav className="fixed bottom-0 left-0 right-0 z-40 bg-surface-container-lowest border-t border-border-subtle shadow-[0_-4px_6px_-1px_rgba(0,0,0,0.05)] pb-safe">
          <div className="flex justify-around items-center h-16 px-2">
            {NAV_LINKS.map((link) => {
              const isActive = link.href === '/' ? pathname === '/' : pathname.startsWith(link.href);
              return (
                <Link
                  key={link.href}
                  href={link.href}
                  className={`flex flex-col items-center justify-center w-full h-full gap-1 transition-colors ${
                    isActive ? 'text-primary' : 'text-on-surface-variant'
                  }`}
                >
                  <span 
                    className={`material-symbols-outlined ${isActive ? 'text-[24px]' : 'text-[22px]'}`}
                    style={{ fontVariationSettings: isActive ? "'FILL' 1" : "'FILL' 0" }}
                  >
                    {link.icon}
                  </span>
                  <span className={`font-label-xs text-[10px] ${isActive ? 'font-bold' : 'font-medium'}`}>
                    {link.label}
                  </span>
                </Link>
              );
            })}
          </div>
        </nav>
      </div>
    </>
  );
}
