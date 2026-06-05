import { Injectable, signal } from '@angular/core';

export interface ToastMessage {
  id: number;
  message: string;
  type: 'success' | 'error' | 'info';
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private nextId = 0;
  toasts = signal<ToastMessage[]>([]);

  show(message: string, type: 'success' | 'error' | 'info' = 'success', duration = 3000): void {
    const id = this.nextId++;
    this.toasts.update(all => [...all, { id, message, type }]);
    this.playNotificationSound();
    setTimeout(() => {
      this.dismiss(id);
    }, duration);
  }

  dismiss(id: number): void {
    this.toasts.update(all => all.filter(t => t.id !== id));
  }

  private playNotificationSound(): void {
    try {
      const audio = new Audio('assets/sounds/notification.mp3');
      audio.volume = 0.4;
      audio.play().catch(() => {
        // Silently catch autoplay/interact restrictions
      });
    } catch {
      // Fallback silently if audio context / element is not supported
    }
  }
}

