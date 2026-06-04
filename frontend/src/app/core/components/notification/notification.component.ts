import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NotificationService } from '../../services/notification.service';

@Component({
  selector: 'app-notification-container',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './notification.component.html',
  styleUrl: './notification.component.scss'
})
export class NotificationComponent {
  notificationService = inject(NotificationService);
  toasts = this.notificationService.toasts;

  onDismiss(id: number): void {
    this.notificationService.dismiss(id);
  }
}
