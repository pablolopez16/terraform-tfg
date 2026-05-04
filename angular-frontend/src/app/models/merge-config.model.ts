import { MergeSource } from './merge-source.model';

export interface MergeConfig {
  mergeId?: string;
  sources: MergeSource[];
  maxResultsPerCalendar?: number;
}