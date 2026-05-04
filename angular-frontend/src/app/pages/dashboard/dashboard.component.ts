import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MergeService } from '../../core/services/merge.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent implements OnInit {
  merges: any[] = [];

  constructor(private mergeService: MergeService) {}

  ngOnInit() {}

  delete(mergeId: string) {
    this.mergeService.delete(mergeId).subscribe(() => {
      this.merges = this.merges.filter(m => m.merge_id !== mergeId);
    });
  }

  getIcsUrl(mergeId: string): string {
    return this.mergeService.getIcsUrl(mergeId);
  }
}