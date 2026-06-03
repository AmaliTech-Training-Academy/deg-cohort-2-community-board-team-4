import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BaseChartDirective } from 'ng2-charts';
import { ChartData, ChartOptions } from 'chart.js';
import { HeaderComponent } from '../../../../core/components/header/header.component';
import { BreadcrumbComponent, BreadcrumbItem } from '../../../../core/components/breadcrumb/breadcrumb.component';

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
export class AnalyticsComponent {
  breadcrumbItems: BreadcrumbItem[] = [
    { label: 'Home', link: '/dashboard', home: true },
    { label: 'Analytics' },
  ];

  totalPosts = 8;
  totalComments = 32;

  contributors: Contributor[] = [
    { rank: 1, name: 'John Smith', posts: 19 },
    { rank: 2, name: 'Brooklyn Simmons', posts: 15 },
    { rank: 3, name: 'Kristin Watson', posts: 14 },
    { rank: 4, name: 'Courtney Henry', posts: 12 },
    { rank: 5, name: 'Leslie Alexander', posts: 11 },
    { rank: 6, name: 'Dianne Russell', posts: 10 },
    { rank: 7, name: 'Dianne Russell', posts: 9 },
    { rank: 8, name: 'Dianne Russell', posts: 7 },
    { rank: 9, name: 'Dianne Russell', posts: 5 },
    { rank: 10, name: 'Dianne Russell', posts: 2 },
  ];

  private readonly barColor = '#3d5567';
  private readonly avgColor = '#3b5bdb';

  categoryData: ChartData<'bar'> = {
    labels: ['Events', 'Help Requests', 'Lost & Found', 'Recommendations'],
    datasets: [
      {
        data: [37, 14, 25, 9],
        backgroundColor: this.barColor,
        borderRadius: 2,
        barPercentage: 0.55,
        categoryPercentage: 0.7,
      },
    ],
  };

  weekdayData: ChartData<'bar'> = {
    labels: ['Mon', 'Tues', 'Wed', 'Thurs', 'Fri', 'Sat', 'Sun'],
    datasets: [
      {
        data: [35, 12, 25, 8, 0, 0, 0],
        backgroundColor: this.barColor,
        borderRadius: 2,
        barPercentage: 0.5,
        categoryPercentage: 0.7,
      },
    ],
  };

  categoryOptions = this.buildOptions();
  weekdayOptions = this.buildOptions();

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
          max: 40,
          ticks: { stepSize: 10, color: '#5c6f81', font: { size: 11 } },
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
      const avg =
        chart.data.datasets[0].data
          .filter((v: number) => v > 0)
          .reduce((a: number, b: number) => a + b, 0) /
        chart.data.datasets[0].data.filter((v: number) => v > 0).length;
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
