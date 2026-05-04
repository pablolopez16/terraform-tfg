export interface MergeSource {
  accountId: string;
  provider: 'google' | 'caldav';
  calendarId: string;
  prefix?: string;
}