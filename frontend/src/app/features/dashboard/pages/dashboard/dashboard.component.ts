import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent {
  constructor(
    private authService: AuthService, 
    private router: Router
  ) {}

  onLogout(): void {
    this.authService.logout();
    this.router.navigate(['/auth/login']);
  }
}
