import { Routes } from '@angular/router';
import { authGuard, publicGuard, adminGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'auth',
    canActivate: [publicGuard],
    loadChildren: () => import('./features/auth/auth.routes').then(m => m.AUTH_ROUTES)
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    children: [
      {
        path: '',
        loadComponent: () => import('./features/dashboard/pages/dashboard/dashboard.component').then(m => m.DashboardComponent)
      },
      {
        path: 'posts/:id',
        loadComponent: () => import('./features/posts/pages/post-detail/post-detail.component').then(m => m.PostDetailComponent)
      },
      {
        path: 'analytics',
        canActivate: [adminGuard],
        loadComponent: () => import('./features/dashboard/pages/analytics/analytics.component').then(m => m.AnalyticsComponent)
      }
    ]
  },
  {
    path: 'unauthorized',
    loadComponent: () => import('./features/error/pages/error/error.component').then(m => m.ErrorComponent)
  },
  {
    path: 'not-found',
    loadComponent: () => import('./features/error/pages/error/error.component').then(m => m.ErrorComponent)
  },
  {
    path: '',
    redirectTo: 'auth/login',
    pathMatch: 'full'
  },
  {
    path: '**',
    redirectTo: 'not-found'
  }
];
