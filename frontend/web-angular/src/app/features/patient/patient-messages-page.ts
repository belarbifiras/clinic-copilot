import { Component, ElementRef, OnInit, computed, effect, inject, signal, viewChild } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { LucideArrowLeft, LucideMessageCircle, LucideSend } from '@lucide/angular';
import { map } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { ChatService, ChatConversation, ChatMsg } from '../../core/services/chat.service';
import { Icon } from '../../shared/ui/icon';
import { pad } from './appointment-utils';

const hhmm = () => { const d = new Date(); return `${pad(d.getHours())}:${pad(d.getMinutes())}`; };

interface LocalMsg { id: string; from: 'me' | 'doc'; text: string; at: string }

function mapMsg(msg: ChatMsg, myId: string): LocalMsg {
  return {
    id: msg._id,
    from: msg.senderId === myId ? 'me' : 'doc',
    text: msg.text,
    at: (() => {
      try {
        const d = new Date(msg.createdAt);
        return `${pad(d.getHours())}:${pad(d.getMinutes())}`;
      } catch { return hhmm(); }
    })(),
  };
}

interface ConvDisplay {
  id: string;
  contactName: string;
  contactId: string;
  last?: LocalMsg;
}

@Component({
  selector: 'app-patient-messages-page',
  imports: [Icon],
  template: `
    <div class="px-4 py-6 md:px-8 md:py-8">
      <div class="mx-auto max-w-6xl space-y-4">
        <div>
          <h1 class="text-2xl font-bold text-deep sm:text-3xl">Messagerie</h1>
          <p class="mt-1 text-muted-foreground">Échangez en toute sécurité avec vos médecins.</p>
        </div>

        <div class="grid h-[70vh] min-h-[480px] overflow-hidden rounded-3xl border border-border bg-card shadow-soft md:grid-cols-[320px_minmax(0,1fr)]">
          <aside class="flex flex-col border-border md:border-r" [class.hidden]="activeId() && isMobile" [class.md:flex]="true">
            <p class="border-b border-border px-4 py-3 text-sm font-semibold text-deep">Conversations</p>
            @if (loadingConvs()) {
              <p class="p-4 text-sm text-muted-foreground">Chargement…</p>
            }
            <ul class="flex-1 overflow-y-auto">
              @for (c of conversations(); track c.id) {
                <li>
                  <button type="button" (click)="selectConv(c.id)"
                    class="flex w-full items-center gap-3 px-4 py-3 text-left transition-colors duration-200 hover:bg-soft/60"
                    [class]="activeId() === c.id ? 'bg-soft' : ''">
                    <span class="grid h-10 w-10 shrink-0 place-items-center rounded-full bg-primary/15 text-sm font-bold text-primary">
                      {{ initials(c.contactName) }}
                    </span>
                    <span class="min-w-0 flex-1">
                      <span class="flex items-baseline justify-between gap-2">
                        <span class="truncate font-semibold text-deep">{{ c.contactName }}</span>
                        <span class="text-xs text-muted-foreground">{{ c.last?.at }}</span>
                      </span>
                      <span class="block truncate text-sm text-muted-foreground">{{ c.last?.text ?? 'Nouvelle conversation' }}</span>
                    </span>
                  </button>
                </li>
              } @empty {
                @if (!loadingConvs()) {
                  <li class="p-8 text-center text-sm text-muted-foreground">Aucune conversation pour le moment.</li>
                }
              }
            </ul>
          </aside>

          <section class="flex min-h-0 flex-col" [class.hidden]="!activeId()" [class.md:flex]="true">
            @if (activeConv(); as c) {
              <header class="flex items-center gap-3 border-b border-border px-4 py-3">
                <button type="button" class="btn btn-ghost btn-icon md:hidden" (click)="activeId.set(null)" aria-label="Retour"><app-icon [icon]="Back" [size]="18" /></button>
                <span class="grid h-10 w-10 shrink-0 place-items-center rounded-full bg-primary/15 text-sm font-bold text-primary">
                  {{ initials(c.contactName) }}
                </span>
                <div class="min-w-0 flex-1">
                  <p class="truncate font-semibold text-deep">{{ c.contactName }}</p>
                  <p class="text-xs text-muted-foreground">Médecin</p>
                </div>
              </header>
              <div #scroller class="flex-1 space-y-3 overflow-y-auto bg-soft/30 px-4 py-4">
                @if (loadingMsgs()) {
                  <p class="text-center text-sm text-muted-foreground">Chargement…</p>
                }
                @if (thread().length === 0 && !loadingMsgs()) {
                  <p class="mx-auto max-w-xs rounded-xl bg-card p-4 text-center text-sm text-muted-foreground">
                    Écrivez votre premier message à {{ c.contactName }}. Pour une urgence, appelez le 190.
                  </p>
                }
                @for (m of thread(); track m.id) {
                  <div class="flex" [class.justify-end]="m.from === 'me'">
                    <div class="max-w-[80%] rounded-2xl px-4 py-2.5 text-sm shadow-soft"
                      [class]="m.from === 'me' ? 'rounded-br-md bg-gradient-primary text-white' : 'rounded-bl-md border border-border bg-card text-deep'">
                      <p>{{ m.text }}</p>
                      <p class="mt-1 text-right text-[11px] opacity-70">{{ m.at }}</p>
                    </div>
                  </div>
                }
                @if (sending()) { <p class="text-xs text-muted-foreground">Envoi en cours…</p> }
              </div>
              <form class="flex gap-2 border-t border-border p-3" (submit)="$event.preventDefault(); send()">
                <input class="field flex-1 rounded-full" placeholder="Écrire un message…" aria-label="Message" [value]="draft()" (input)="draft.set($any($event.target).value)" />
                <button type="submit" class="btn btn-hero btn-icon" [disabled]="!draft().trim() || sending()" aria-label="Envoyer"><app-icon [icon]="Send" [size]="18" /></button>
              </form>
            } @else {
              <div class="grid flex-1 place-items-center p-8 text-center">
                <div>
                  <span class="mx-auto grid h-14 w-14 place-items-center rounded-2xl bg-soft text-primary"><app-icon [icon]="Chat" [size]="28" /></span>
                  <p class="mt-4 font-semibold text-deep">Sélectionnez une conversation</p>
                  <p class="mt-1 text-sm text-muted-foreground">Ou ouvrez la fiche d'un médecin pour lui écrire.</p>
                </div>
              </div>
            }
          </section>
        </div>
      </div>
    </div>
  `,
})
export class PatientMessagesPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly chatSvc = inject(ChatService);
  private readonly auth = inject(AuthService);

  protected readonly Back = LucideArrowLeft;
  protected readonly Send = LucideSend;
  protected readonly Chat = LucideMessageCircle;
  protected readonly isMobile = false;

  private readonly scroller = viewChild<ElementRef<HTMLElement>>('scroller');

  protected readonly convList = signal<ConvDisplay[]>([]);
  protected readonly threads = signal<Record<string, LocalMsg[]>>({});
  protected readonly activeId = signal<string | null>(null);
  protected readonly draft = signal('');
  protected readonly loadingConvs = signal(true);
  protected readonly loadingMsgs = signal(false);
  protected readonly sending = signal(false);

  private myId = signal('');

  private readonly param = toSignal(this.route.queryParamMap.pipe(map((p) => p.get('conversation'))), { initialValue: null });

  protected readonly conversations = computed(() => this.convList());
  protected readonly activeConv = computed(() => this.convList().find((c) => c.id === this.activeId()) ?? null);
  protected readonly thread = computed(() => this.threads()[this.activeId() ?? ''] ?? []);

  constructor() {
    effect(() => {
      this.thread();
      setTimeout(() => { const el = this.scroller()?.nativeElement; if (el) el.scrollTop = el.scrollHeight; });
    });
    // Si un paramètre de conversation est fourni
    effect(() => {
      const id = this.param();
      if (id && this.convList().some((c) => c.id === id)) {
        this.selectConv(id);
      }
    });
  }

  ngOnInit() {
    // Extrait myId depuis JWT
    const session = this.auth.session();
    if (session) {
      try {
        const payload = JSON.parse(atob(session.token.split('.')[1]));
        this.myId.set(String(payload.sub ?? payload.userId ?? payload.id ?? ''));
      } catch { /* ignore */ }
    }
    this.loadConversations();
  }

  private loadConversations() {
    this.loadingConvs.set(true);
    this.chatSvc.getConversations().subscribe({
      next: (list) => {
        const myId = this.myId();
        this.convList.set(list.map((c) => ({
          id: c._id,
          contactName: c.contactName ?? `Interlocuteur`,
          contactId: c.contactId ?? c.participants.find((p) => p !== myId) ?? '',
          last: undefined,
        })));
        this.loadingConvs.set(false);
      },
      error: () => this.loadingConvs.set(false),
    });
  }

  protected selectConv(id: string) {
    this.activeId.set(id);
    if (!this.threads()[id]) {
      this.loadMessages(id);
    }
  }

  private loadMessages(convId: string) {
    this.loadingMsgs.set(true);
    const myId = this.myId();
    this.chatSvc.getMessages(convId).subscribe({
      next: (msgs) => {
        this.threads.update((t) => ({ ...t, [convId]: msgs.map((m) => mapMsg(m, myId)) }));
        // Met à jour le "last" de la conv
        const last = msgs[msgs.length - 1];
        if (last) {
          this.convList.update((l) => l.map((c) => c.id === convId ? { ...c, last: mapMsg(last, myId) } : c));
        }
        this.loadingMsgs.set(false);
      },
      error: () => this.loadingMsgs.set(false),
    });
  }

  protected send(): void {
    const id = this.activeId();
    const text = this.draft().trim();
    if (!id || !text) return;
    this.sending.set(true);
    this.chatSvc.sendMessage({ conversationId: id, text }).subscribe({
      next: (msg) => {
        const local = mapMsg(msg, this.myId());
        this.threads.update((t) => ({ ...t, [id]: [...(t[id] ?? []), local] }));
        this.convList.update((l) => l.map((c) => c.id === id ? { ...c, last: local } : c));
        this.draft.set('');
        this.sending.set(false);
      },
      error: () => {
        // Fallback : affiche quand même localement
        const local: LocalMsg = { id: crypto.randomUUID(), from: 'me', text, at: hhmm() };
        this.threads.update((t) => ({ ...t, [id]: [...(t[id] ?? []), local] }));
        this.draft.set('');
        this.sending.set(false);
      },
    });
  }

  protected initials(name: string): string {
    return name.split(' ').map((w) => w[0] ?? '').join('').toUpperCase().slice(0, 2);
  }
}
