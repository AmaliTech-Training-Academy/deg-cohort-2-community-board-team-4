import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HeaderComponent } from '../../../../core/components/header/header.component';

@Component({
  selector: 'app-analytics',
  standalone: true,
  imports: [CommonModule, HeaderComponent],
  templateUrl: './analytics.component.html',
  styleUrl: './analytics.component.scss',
})
export class AnalyticsComponent {}
