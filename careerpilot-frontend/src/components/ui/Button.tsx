import React from 'react';

type ButtonVariant = 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger';
type ButtonSize = 'sm' | 'md' | 'lg';

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  size?: ButtonSize;
}

export function Button({ 
  children, 
  variant = 'primary', 
  size = 'md', 
  className = '', 
  ...props 
}: ButtonProps) {
  const baseStyles = 'inline-flex items-center justify-center font-medium rounded-lg transition-colors disabled:opacity-50 disabled:pointer-events-none cursor-pointer';
  
  const variants = {
    primary: 'bg-primary text-on-primary hover:bg-primary-container hover:text-on-primary-container',
    secondary: 'bg-secondary-container text-on-secondary-container hover:bg-secondary hover:text-on-secondary',
    outline: 'border border-outline text-primary hover:bg-surface-container',
    ghost: 'text-secondary hover:bg-surface-container',
    danger: 'bg-error-container text-on-error-container hover:bg-error hover:text-on-error'
  };

  const sizes = {
    sm: 'text-label-sm px-3 py-1.5',
    md: 'text-body-md px-4 py-2',
    lg: 'text-body-lg px-6 py-3'
  };

  return (
    <button 
      className={`${baseStyles} ${variants[variant]} ${sizes[size]} ${className}`} 
      {...props}
    >
      {children}
    </button>
  );
}
