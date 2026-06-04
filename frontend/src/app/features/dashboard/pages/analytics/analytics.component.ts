import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BaseChartDirective } from 'ng2-charts';
import { ChartData, ChartOptions } from 'chart.js';
import { HeaderComponent } from '../../../../core/components/header/header.component';
import { BreadcrumbComponent, BreadcrumbItem } from '../../../../core/components/breadcrumb/breadcrumb.component';
import { AnalyticsService } from '../../../../core/services/analytics.service';

interface Contributor {
  rank: number;
  name: string;
  posts: number;
}

@Component({
  selector: 'app-analytics',
  standalone: true,
  imports: [CommonModule, HeaderComponent, BreadcrumbComponent, BaseChartDirective],
  templateUrl: './analytics.component.html',
  styleUrl: './analytics.component.scss',
})
export class AnalyticsComponent implements OnInit {
  private analyticsService = inject(AnalyticsService);

  breadcrumbItems: BreadcrumbItem[] = [
    { label: 'Home', link: '/dashboard', home: true },
    { label: 'Analytics' },
  ];

  isLoading = signal<boolean>(true);
  errorMessage = signal<string>('');

  totalPosts = signal<number>(0);
  totalComments = signal<number>(0);
  totalUsers = signal<number>(0);

  contributors = signal<Contributor[]>([]);

  categoryData = signal<ChartData<'bar'> | null>(null);
  weekdayData = signal<ChartData<'bar'> | null>(null);

  private readonly barColor = '#3d5567';
  private readonly avgColor = '#3b5bdb';

  categoryOptions = this.buildOptions();
  weekdayOptions = this.buildOptions();

  ngOnInit(): void {
    const getWeekday = (dateStr: string): string => {
      const days = ['Sun', 'Mon', 'Tues', 'Wed', 'Thurs', 'Fri', 'Sat'];
      const dateParts = dateStr.split('-');
      const d = new Date(Number(dateParts[0]), Number(dateParts[1]) - 1, Number(dateParts[2]));
      return days[d.getDay()];
    };

    this.analyticsService.getAnalytics().subscribe({
      next: (data) => {
        this.totalPosts.set(data.summary?.totalPosts || 0);
        this.totalComments.set(data.summary?.totalComments || 0);
        this.totalUsers.set(data.summary?.totalUsers || 0);

        // Map Category chart data
        const catLabels = (data.categoryCounts || []).map(
          c => c.category === 'Event' ? 'Events' : c.category
        );
        const catValues = (data.categoryCounts || []).map(c => c.postCount);

        this.categoryData.set({
          labels: catLabels,
          datasets: [
            {
              data: catValues,
              backgroundColor: this.barColor,
              borderRadius: 2,
              barPercentage: 0.55,
              categoryPercentage: 0.7,
            }
          ]
        });

        // Map Weekday chart data (sorted chronologically)
        const sortedDaily = [...(data.dailyPostCounts || [])].sort(
          (a, b) => a.day.localeCompare(b.day)
        );
        const dailyLabels = sortedDaily.map(d => getWeekday(d.day));
        const dailyValues = sortedDaily.map(d => d.postCount);

        this.weekdayData.set({
          labels: dailyLabels,
          datasets: [
            {
              data: dailyValues,
              backgroundColor: this.barColor,
              borderRadius: 2,
              barPercentage: 0.5,
              categoryPercentage: 0.7,
            }
          ]
        });

        // Map top contributors (up to 10)
        const topContributorsList = (data.topUsers || []).map((user, idx) => ({
          rank: idx + 1,
          name: user.name || 'Anonymous User',
          posts: user.postCount
        }));
        this.contributors.set(topContributorsList);

        this.isLoading.set(false);
      },
      error: (err) => {
        this.errorMessage.set('Could not load analytics statistics. Please try again later.');
        this.isLoading.set(false);
      }
    });
  }

  private buildOptions(): ChartOptions<'bar'> {
    return {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false },
        tooltip: {
          backgroundColor: '#0f2233',
          padding: 10,
          cornerRadius: 6,
          displayColors: false,
          callbacks: {
            title: () => '',
            label: (ctx) => `Count: ${ctx.parsed.y}`,
          },
        },
      },
      scales: {
        x: {
          grid: { display: false },
          border: { display: false },
          ticks: { color: '#5c6f81', font: { size: 11 } },
        },
        y: {
          beginAtZero: true,
          ticks: { color: '#5c6f81', font: { size: 11 } },
          grid: { color: '#eef1f4' },
          border: { display: false },
        },
      },
    };
  }

  // Average dashed line drawn via a custom inline plugin per chart
  avgLinePlugin = {
    id: 'avgLine',
    afterDatasetsDraw: (chart: any) => {
      const dataArr = chart.data?.datasets?.[0]?.data as number[];
      if (!dataArr || dataArr.length === 0) return;

      const nonZero = dataArr.filter((v: number) => v > 0);
      if (nonZero.length === 0) return;

      const avg = nonZero.reduce((a: number, b: number) => a + b, 0) / nonZero.length;
      const yScale = chart.scales['y'];
      const { left, right } = chart.chartArea;
      const y = yScale.getPixelForValue(avg);
      const ctx = chart.ctx;
      ctx.save();
      ctx.beginPath();
      ctx.setLineDash([5, 4]);
      ctx.strokeStyle = this.avgColor;
      ctx.lineWidth = 1.5;
      ctx.moveTo(left, y);
      ctx.lineTo(right, y);
      ctx.stroke();
      ctx.restore();
    },
  };
}
