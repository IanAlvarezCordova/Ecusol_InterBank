// src/components/common/LogoEcusol.tsx
import React from 'react';
import logoNexus from '@/assets/logo.jpeg';

interface LogoEcusolProps {
    className?: string;
    size?: number; // Tamaño del logo circular
}

export const LogoEcusol: React.FC<LogoEcusolProps> = ({ className = '', size = 50 }) => {
    return (
        <img
            src={logoNexus}
            alt="Logo Ecusol Bank"
            className={`rounded-full object-cover ${className}`}
            style={{
                width: `${size}px`,
                height: `${size}px`
            }}
        />
    );
};
