import { Routes } from '@angular/router';

export const routes: Routes = [
    {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full',
    },
    {
        path: 'dashboard',
        loadComponent: () =>
            import('./pages/dashboard/dashboard.component').then(m => m.DashboardComponent),
        title: 'Alpha Bot — Live Dashboard',
    },
    {
        path: 'signals',
        loadComponent: () =>
            import('./pages/signals/signals.component').then(m => m.SignalsComponent),
        title: 'Alpha Bot — Bullish Signals',
    },
    {
        path: 'settings',
        loadComponent: () =>
            import('./pages/settings/settings.component').then(m => m.SettingsComponent),
        title: 'Alpha Bot — Settings',
    },
    {
        path: '**',
        redirectTo: 'dashboard',
    },
];
