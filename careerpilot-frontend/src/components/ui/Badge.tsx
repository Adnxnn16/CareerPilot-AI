import React from 'react';

type BadgeVariant = 'default' | 'success' | 'warning' | 'error' | 'info' | 'primary';

interface BadgeProps extends React.HTMLAttributes<HTMLSpanElement> {
  variant?: BadgeVariant;
}

export function Badge({ 
  children, 
  variant = 'default', 
  className = '', 
  ...props 
}: BadgeProps) {
  const baseStyles = 'inline-flex items-center rounded-full px-2.5 py-0.5 text-label-xs font-medium';
  
  const variants = {
    default: 'bg-surface-variant text-on-surface-variant',
    success: 'bg-success-green/10 text-success-green',
    warning: 'bg-tertiary-fixed text-on-tertiary-fixed',
    error: 'bg-error-container text-on-error-container',
    info: 'bg-secondary-container text-on-secondary-container',
    primary: 'bg-primary-container text-on-primary-container'
  };

  return (
    <span className={`${baseStyles} ${variants[variant]} ${className}`} {...props}>
      {children}
    </span>
  );
}
