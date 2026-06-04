import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-error',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './error.component.html',
  styleUrl: './error.component.scss'
})
export class ErrorComponent implements OnInit {
  private authService = inject(AuthService);
  private router = inject(Router);

  isAuthenticated = this.authService.isAuthenticated;

  errorCode = signal<'404' | '403'>('404');
  errorTitle = signal<string>('Page Not Found');
  errorDescription = signal<string>(
    'The page you are looking for might have been removed, had its name changed, or is temporarily unavailable.'
  );

  ngOnInit(): void {
    const currentUrl = this.router.url;
    if (currentUrl.includes('unauthorized') || currentUrl.includes('403')) {
      this.errorCode.set('403');
      this.errorTitle.set('Access Denied');
      this.errorDescription.set(
        'You do not have permission to access this resource. Please contact your administrator if you believe this is an error.'
      );
    }
  }

  getHomeLink(): string {
    return this.isAuthenticated() ? '/dashboard' : '/auth/login';
  }

  getHomeLabel(): string {
    return this.isAuthenticated() ? 'Return to Dashboard' : 'Back to Login';
  }
}
