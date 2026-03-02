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
        title: 'Alpha Bot — News Analysis',
    },
    {
        path: 'intelligence',
        loadComponent: () =>
            import('./pages/intelligence/intelligence.component').then(m => m.IntelligenceComponent),
        title: 'Alpha Bot — Market Intelligence',
    },
    {
        path: 'price-board',
        loadComponent: () =>
            import('./pages/price-board/price-board.component').then(m => m.PriceBoardComponent),
        title: 'Alpha Bot — Bảng Giá',
    },

    {
        path: 'portfolio',
        loadComponent: () =>
            import('./pages/portfolio/portfolio.component').then(m => m.PortfolioComponent),
        title: 'Alpha Bot — AI Portfolio',
    },
    {
        path: '**',
        redirectTo: 'dashboard',
    },
];
