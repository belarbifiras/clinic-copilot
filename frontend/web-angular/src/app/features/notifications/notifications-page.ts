import { Component, OnInit, inject, signal } from '@angular/core';
import { LucideBell, LucideCalendarDays, LucideFileText, LucideStar } from '@lucide/angular';
import { shortDate, timeLabel } from '../../core/mock/format';
import { NotificationItem, NotificationService } from './notification.service';
import { Icon } from '../../shared/ui/icon';
import { PageHeader } from '../../shared/ui/page-header';

type NotifKind = 'RDV' | 'AVIS' | 'DOSSIER' | 'SYSTEME';

function toKind(type: string): NotifKind {
  if (!type) return 'SYSTEME';
  const t = type.toUpperCase();
  if (t.includes('APPOINTMENT') || t.includes('RDV')) return 'RDV';
  if (t.includes('REVIEW') || t.includes('AVIS')) return 'AVIS';
  if (t.includes('RECORD') || t.includes('DOSSIER')) return 'DOSSIER';
  return 'SYSTEME';
}

const KINDS: Record<NotifKind, { icon: unknown; cls: string }> = {
  RDV: { icon: LucideCalendarDays, cls: 'bg-info/15 text-info' },
  AVIS: { icon: LucideStar, cls: 'bg-warning/20 text-accent-foreground' },
  DOSSIER: { icon: LucideFileText, cls: 'bg-primary/15 text-primary' },
  SYSTEME: { icon: LucideBell, cls: 'bg-muted text-muted-foreground' },
};

@Component({
  selector: 'app-notifications-page',
  imports: [Icon, PageHeader],
  template: `
    <div class="px-4 py-6 md:px-8 md:py-8">
      <div class="mx-auto max-w-3xl space-y-6">
        <app-page-header title="Notifications" [subtitle]="unreadCount() + ' non lue(s)'">
          <button type="button" class="btn btn-soft" [disabled]="!unreadCount()" (click)="markAllRead()">Tout marquer comme lu</button>
        </app-page-header>

        @if (loading()) {
          <div class="space-y-3" aria-busy="true">
            @for (i of [0,1,2]; track i) {
              <div class="flex animate-pulse gap-4 rounded-2xl border border-border bg-card p-4">
                <div class="h-11 w-11 rounded-xl bg-muted"></div>
                <div class="flex-1 space-y-2"><div class="h-4 w-1/3 rounded bg-muted"></div><div class="h-3 w-2/3 rounded bg-muted"></div></div>
              </div>
            }
          </div>
        } @else {
          <ul class="space-y-3">
            @for (n of notifications(); track n.id) {
              <li>
                <button type="button" (click)="markRead(n)"
                  class="flex w-full items-start gap-4 rounded-2xl border bg-card p-4 text-left shadow-soft transition-colors duration-200 hover:bg-soft/60"
                  [class]="n.seen ? 'border-border' : 'border-primary/40'">
                  <span class="grid h-11 w-11 shrink-0 place-items-center rounded-xl" [class]="kind(n.type).cls"><app-icon [icon]="kind(n.type).icon" [size]="20" /></span>
                  <div class="min-w-0 flex-1">
                    <p class="font-semibold text-deep">{{ n.title }}</p>
                    <p class="text-sm text-muted-foreground">{{ n.message }}</p>
                    <p class="mt-1 text-xs text-muted-foreground">{{ date(n.createdAt) }} · {{ time(n.createdAt) }}</p>
                  </div>
                  @if (!n.seen) {
                    <span class="mt-1 h-2.5 w-2.5 shrink-0 rounded-full bg-primary" aria-label="Non lue"></span>
                  }
                </button>
              </li>
            } @empty {
              <li class="rounded-2xl border border-dashed border-border bg-card p-10 text-center text-sm text-muted-foreground">
                Aucune notification pour le moment.
              </li>
            }
          </ul>
        }
      </div>
    </div>
  `,
})
export class NotificationsPage implements OnInit {
  private readonly svc = inject(NotificationService);

  protected readonly notifications = signal<NotificationItem[]>([]);
  protected readonly unreadCount = signal(0);
  protected readonly loading = signal(true);

  protected readonly date = shortDate;
  protected readonly time = timeLabel;

  ngOnInit() {
    this.load();
    this.refreshCount();
  }

  private load() {
    this.loading.set(true);
    this.svc.list(0, 50).subscribe((page) => {
      this.notifications.set(page.content);
      this.loading.set(false);
    });
  }

  private refreshCount() {
    this.svc.unreadCount().subscribe((r) => this.unreadCount.set(r.count));
  }

  protected kind(type: string) {
    return KINDS[toKind(type)];
  }

  protected markRead(n: NotificationItem) {
    if (n.seen) return;
    this.svc.markRead(n.id).subscribe(() => {
      this.notifications.update((list) => list.map((x) => (x.id === n.id ? { ...x, seen: true } : x)));
      this.unreadCount.update((c) => Math.max(0, c - 1));
    });
  }

  protected markAllRead() {
    this.svc.markAllRead().subscribe(() => {
      this.notifications.update((list) => list.map((x) => ({ ...x, seen: true })));
      this.unreadCount.set(0);
    });
  }
}
