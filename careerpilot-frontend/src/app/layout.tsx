import type { Metadata } from 'next';
import { Inter, Plus_Jakarta_Sans } from 'next/font/google';
import './globals.css';
import QueryProvider from '@/components/providers/QueryProvider';
import AppNav from '@/components/layout/AppNav';

const inter = Inter({
  subsets: ['latin'],
  variable: '--font-inter',
  display: 'swap',
});

const jakarta = Plus_Jakarta_Sans({
  subsets: ['latin'],
  variable: '--font-jakarta',
  display: 'swap',
});

export const metadata: Metadata = {
  title: 'CareerPilot AI — AI-Powered Career Intelligence',
  description:
    'Upload your resume, discover matching jobs, get AI match scores, and generate ATS-optimized tailored resumes instantly.',
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className={`${inter.variable} ${jakarta.variable}`}>
      <head>
        {/* eslint-disable-next-line @next/next/no-page-custom-font */}
        <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:wght,FILL@100..700,0..1&display=swap" rel="stylesheet" />
      </head>
      <body className="antialiased bg-background text-on-background font-sans flex flex-col md:flex-row min-h-screen">
        <QueryProvider>
          <AppNav />
          <main className="flex-1 overflow-x-hidden pb-16 md:pb-0">{children}</main>
        </QueryProvider>
      </body>
    </html>
  );
}
