import { Component, ElementRef, effect, inject, signal, viewChild } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import {
  LucideActivity, LucideBot, LucideBrain, LucideCircleCheck, LucideCircleHelp, LucideLock, LucidePhone, LucideRotateCcw, LucideSend,
  LucideShieldAlert, LucideStethoscope, LucideThermometer, LucideTriangleAlert, LucideWind,
} from '@lucide/angular';
import { Icon } from '../../shared/ui/icon';
import { Modal } from './overlays';
import { Toasts } from './toasts';
import {
  AssistantService, DoctorSuggestion, TriageLanguage, TriageSession, TriageStartRequest,
} from './assistant.service';

type Level = 'low' | 'mid' | 'high';
type Sex = '' | 'MALE' | 'FEMALE' | 'OTHER';

interface Bubble {
  id: number;
  from: 'me' | 'bot';
  text: string;
  level?: Level;
  code?: string;
  step?: string;
  specialty?: string;
  tips?: string[];
  doctors?: DoctorSuggestion[];
  disclaimer?: string;
  emergency?: boolean;
}

interface Details { age: number | null; sex: Sex; durationDays: number | null; intensity: number | null }
interface Coords { latitude: number; longitude: number }

/** Codes de spécialités du service IA -> libellés affichés. */
const SPECIALTIES: Record<string, string> = {
  general_medicine: 'Médecin généraliste', cardiology: 'Cardiologue', pulmonology: 'Pneumologue',
  neurology: 'Neurologue', gastroenterology: 'Gastro-entérologue', dermatology: 'Dermatologue',
  ent: 'ORL', ophthalmology: 'Ophtalmologue', orthopedics: 'Orthopédiste', gynecology: 'Gynécologue',
  pediatrics: 'Pédiatre', psychiatry: 'Psychiatre', urology: 'Urologue', endocrinology: 'Endocrinologue',
};

/** Niveaux d'urgence du service (N1 le plus urgent) -> style d'affichage. */
const LEVEL_OF: Record<string, Level> = { N1: 'high', N2: 'mid', N3: 'mid', N4: 'low', N5: 'low' };

const LEVELS: Record<Level, { label: string; cls: string; bar: string }> = {
  low: { label: 'Niveau faible', cls: 'bg-success/15 text-success', bar: 'bg-success' },
  mid: { label: 'Niveau modéré', cls: 'bg-warning/25 text-accent-foreground', bar: 'bg-warning' },
  high: { label: 'Urgence possible', cls: 'bg-destructive/15 text-destructive', bar: 'bg-destructive' },
};

@Component({
  selector: 'app-assistant-page',
  imports: [RouterLink, Icon, Modal],
  styles: `
    @keyframes float-a { 0%, 100% { transform: translate(0, 0); } 50% { transform: translate(18px, -10px); } }
    @keyframes float-b { 0%, 100% { transform: translate(0, 0); } 50% { transform: translate(-22px, 8px); } }
    @keyframes msg-in { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: none; } }
    @keyframes dot { 0%, 80%, 100% { transform: scale(0.55); opacity: 0.4; } 40% { transform: scale(1); opacity: 1; } }
    @keyframes ring { 0% { transform: scale(1); opacity: 0.55; } 100% { transform: scale(1.9); opacity: 0; } }
    .float-a { animation: float-a 9s ease-in-out infinite; }
    .float-b { animation: float-b 11s ease-in-out infinite; }
    .msg-in { animation: msg-in 0.35s cubic-bezier(0.2, 0.8, 0.2, 1) both; }
    .dot { animation: dot 1.2s ease-in-out infinite; }
    .ring { animation: ring 1.6s ease-out infinite; }
    @media (prefers-reduced-motion: reduce) { .float-a, .float-b, .msg-in, .dot, .ring { animation: none; } }
  `,
  template: `
    <div class="flex h-[calc(100dvh-6.5rem)] min-h-[520px] flex-col gap-3 px-4 py-4 md:px-6">

      <!-- Barre compacte -->
      <section class="relative shrink-0 overflow-hidden rounded-2xl bg-deep px-4 py-3 text-deep-foreground shadow-soft">
        <div class="pointer-events-none absolute inset-0" aria-hidden="true">
          <div class="float-a absolute -left-8 -top-10 h-28 w-28 rounded-full bg-primary/40 blur-3xl"></div>
          <div class="float-b absolute -bottom-12 right-10 h-32 w-32 rounded-full bg-tertiary/35 blur-3xl"></div>
        </div>
        <div class="relative flex flex-wrap items-center gap-3">
          <span class="relative grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-gradient-primary text-white">
            <span class="ring absolute inset-0 rounded-xl bg-primary/50"></span>
            <app-icon [icon]="Bot" [size]="20" />
          </span>
          <div class="min-w-0 flex-1">
            <h1 class="text-base font-bold leading-tight sm:text-lg">Assistant santé</h1>
            <p class="truncate text-xs text-deep-foreground/70">Orientation prudente, sans diagnostic.</p>
          </div>
          <div class="flex flex-wrap gap-2">
            <button type="button" class="btn rounded-full bg-destructive text-sm text-white hover:opacity-90" (click)="urgenceOpen.set(true)">
              <app-icon [icon]="Shield" [size]="15" />Urgences
            </button>
            <button type="button" class="btn rounded-full bg-white/10 text-sm text-deep-foreground backdrop-blur hover:bg-white/20" (click)="howOpen.set(true)">
              <app-icon [icon]="Help" [size]="15" />Comment ça marche
            </button>
            <button type="button" class="btn rounded-full bg-white/10 text-sm text-deep-foreground backdrop-blur hover:bg-white/20" (click)="privacy()">
              <app-icon [icon]="Lock" [size]="15" />Confidentialité
            </button>
          </div>
        </div>
      </section>

      <!-- Chat : remplit l'écran, seuls les messages défilent -->
      <section class="flex min-h-0 flex-1 flex-col overflow-hidden rounded-2xl border border-border bg-card shadow-soft" aria-label="Conversation avec l'assistant">
        <header class="flex shrink-0 items-center justify-between gap-3 border-b border-border px-5 py-3">
          <div class="flex items-center gap-3">
            <span class="relative grid h-9 w-9 place-items-center rounded-full bg-gradient-primary text-white">
              @if (typing()) { <span class="ring absolute inset-0 rounded-full bg-primary/50"></span> }
              <app-icon [icon]="Bot" [size]="17" />
            </span>
            <div>
              <p class="text-sm font-semibold text-deep">Assistant Clinic Copilot</p>
              <p class="flex items-center gap-1.5 text-xs text-muted-foreground">
                <span class="h-1.5 w-1.5 rounded-full" [class]="typing() ? 'bg-warning' : 'bg-success'"></span>{{ typing() ? 'Analyse en cours' : 'Disponible' }}
              </p>
            </div>
          </div>
          <button type="button" class="btn btn-ghost text-sm" (click)="restart()"><app-icon [icon]="Restart" [size]="15" />Nouvelle conversation</button>
        </header>

        <div #scroller class="bg-mesh min-h-0 flex-1 space-y-4 overflow-y-auto px-4 py-5 sm:px-8">
          @for (m of messages(); track m.id) {
            @if (m.from === 'me') {
              <div class="msg-in flex justify-end">
                <p dir="auto" class="max-w-[70%] rounded-2xl rounded-br-md bg-gradient-primary px-4 py-2.5 text-[15px] leading-snug text-white shadow-soft">{{ m.text }}</p>
              </div>
            } @else {
              <div class="msg-in flex items-end gap-3">
                <span class="grid h-8 w-8 shrink-0 place-items-center rounded-full bg-gradient-primary text-white"><app-icon [icon]="Bot" [size]="15" /></span>
                <div class="glass max-w-[75%] space-y-3 rounded-2xl rounded-bl-md border border-border px-4 py-3 shadow-soft">

                  @if (m.emergency) {
                    <div class="flex items-center gap-2 rounded-xl bg-destructive px-3 py-2 text-white">
                      <app-icon [icon]="Shield" [size]="18" />
                      <p class="text-sm font-bold">Urgence possible : n'attendez pas un rendez-vous.</p>
                    </div>
                  }

                  @if (m.step) {
                    <p class="text-xs font-medium uppercase tracking-wide text-muted-foreground">{{ m.step }}</p>
                  }

                  @if (m.level) {
                    <span class="inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-semibold" [class]="levels[m.level].cls">
                      <app-icon [icon]="levelIcon(m.level)" [size]="13" />{{ m.code ? m.code + ' · ' : '' }}{{ levels[m.level].label }}
                    </span>
                  }

                  <p dir="auto" class="text-[15px] leading-snug text-deep">{{ m.text }}</p>

                  @if (m.tips?.length) {
                    <ul class="space-y-1.5">
                      @for (t of m.tips; track t) {
                        <li dir="auto" class="flex items-start gap-2 text-sm leading-snug text-muted-foreground">
                          <span class="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full" [class]="m.level ? levels[m.level].bar : 'bg-primary'"></span>{{ t }}
                        </li>
                      }
                    </ul>
                  }

                  @if (m.doctors?.length) {
                    <div class="space-y-2">
                      <p class="text-xs font-medium uppercase tracking-wide text-muted-foreground">Médecins proches</p>
                      @for (d of m.doctors; track d.id) {
                        <a [routerLink]="['/medecins', d.id]" class="flex items-center gap-3 rounded-xl bg-soft px-3 py-2.5 transition-colors hover:bg-primary/10">
                          <span class="grid h-9 w-9 shrink-0 place-items-center rounded-lg bg-primary/15 text-primary"><app-icon [icon]="Steth" [size]="18" /></span>
                          <span class="min-w-0 flex-1">
                            <span class="block truncate text-sm font-semibold text-deep">{{ d.displayName }}</span>
                            <span class="block truncate text-xs text-muted-foreground">{{ place(d) }}</span>
                          </span>
                        </a>
                      }
                    </div>
                  }

                  @if (m.disclaimer) {
                    <p dir="auto" class="border-t border-border pt-2 text-xs italic text-muted-foreground">{{ m.disclaimer }}</p>
                  }

                  @if (m.emergency) {
                    <div class="flex flex-wrap gap-2">
                      <a href="tel:190" class="btn rounded-full bg-destructive text-sm text-white hover:opacity-90"><app-icon [icon]="Phone" [size]="15" />Appeler le 190</a>
                      <a href="tel:198" class="btn rounded-full bg-warning/30 text-sm text-accent-foreground hover:bg-warning/40"><app-icon [icon]="Phone" [size]="15" />198 Protection civile</a>
                    </div>
                  } @else if (m.specialty) {
                    <div class="flex flex-wrap items-center gap-3 rounded-xl bg-soft px-3 py-2.5">
                      <span class="grid h-9 w-9 place-items-center rounded-lg bg-primary/15 text-primary"><app-icon [icon]="Steth" [size]="18" /></span>
                      <div class="min-w-0 flex-1">
                        <p class="text-[11px] text-muted-foreground">Professionnel conseillé</p>
                        <p class="text-sm font-semibold text-deep">{{ m.specialty }}</p>
                      </div>
                      <a routerLink="/espace-patient/medecins" class="btn btn-hero text-sm">Trouver un médecin</a>
                    </div>
                  }
                </div>
              </div>
            }
          }

          @if (typing()) {
            <div class="msg-in flex items-end gap-3">
              <span class="grid h-8 w-8 shrink-0 place-items-center rounded-full bg-gradient-primary text-white"><app-icon [icon]="Bot" [size]="15" /></span>
              <div class="glass inline-flex items-center gap-1.5 rounded-2xl rounded-bl-md border border-border px-4 py-3.5" aria-label="L'assistant analyse votre message">
                <span class="dot h-2 w-2 rounded-full bg-primary"></span>
                <span class="dot h-2 w-2 rounded-full bg-primary" style="animation-delay: 0.18s"></span>
                <span class="dot h-2 w-2 rounded-full bg-primary" style="animation-delay: 0.36s"></span>
              </div>
            </div>
          }

          @if (messages().length === 1 && !typing()) {
            <div class="msg-in pt-2">
              <p class="mb-2.5 text-xs font-medium uppercase tracking-wide text-muted-foreground">Suggestions</p>
              <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
                @for (s of suggestions; track s.label) {
                  <button type="button" (click)="ask(s.label)"
                    class="card-lift group flex items-center gap-3 rounded-xl border border-border bg-card/80 p-3 text-left backdrop-blur transition-colors hover:border-primary/40">
                    <span class="grid h-9 w-9 shrink-0 place-items-center rounded-lg bg-soft text-primary transition-colors group-hover:bg-gradient-primary group-hover:text-white"><app-icon [icon]="s.icon" [size]="18" /></span>
                    <span class="text-sm font-medium leading-tight text-deep">{{ s.label }}</span>
                  </button>
                }
              </div>
            </div>
          }
        </div>

        <!-- Avertissement (avant résultat), langue et précisions facultatives -->
        <div class="shrink-0 border-t border-border bg-soft px-4 py-2 sm:px-8">
          <div class="flex flex-wrap items-center justify-between gap-2">
            <p class="text-xs text-muted-foreground">Orientation par IA, sans diagnostic : elle ne remplace pas l'avis d'un médecin.</p>
            <div class="flex items-center gap-1" role="group" aria-label="Langue de la réponse">
              @for (l of langs; track l) {
                <button type="button" class="btn btn-ghost px-2 text-xs" [class.font-bold]="lang() === l" [attr.aria-pressed]="lang() === l" (click)="lang.set(l)">{{ l.toUpperCase() }}</button>
              }
              <button type="button" class="btn btn-ghost px-2 text-xs" [attr.aria-expanded]="detailsOpen()" (click)="detailsOpen.set(!detailsOpen())">Précisions</button>
            </div>
          </div>
          @if (detailsOpen()) {
            <div class="mt-2 grid gap-2 sm:grid-cols-4">
              <label class="space-y-1 text-xs text-muted-foreground">Âge
                <input class="field h-9 rounded-lg px-3" type="number" min="0" max="120" [value]="details().age ?? ''" (input)="setNum('age', $any($event.target).value)" />
              </label>
              <label class="space-y-1 text-xs text-muted-foreground">Sexe
                <select class="field h-9 rounded-lg px-3" [value]="details().sex" (change)="setSex($any($event.target).value)">
                  <option value="">Non précisé</option>
                  <option value="FEMALE">Femme</option>
                  <option value="MALE">Homme</option>
                  <option value="OTHER">Autre</option>
                </select>
              </label>
              <label class="space-y-1 text-xs text-muted-foreground">Depuis (jours)
                <input class="field h-9 rounded-lg px-3" type="number" min="0" max="3650" [value]="details().durationDays ?? ''" (input)="setNum('durationDays', $any($event.target).value)" />
              </label>
              <label class="space-y-1 text-xs text-muted-foreground">Intensité (1 à 10)
                <input class="field h-9 rounded-lg px-3" type="number" min="1" max="10" [value]="details().intensity ?? ''" (input)="setNum('intensity', $any($event.target).value)" />
              </label>
            </div>
            <label class="mt-2 flex items-start gap-2 text-xs text-muted-foreground">
              <input type="checkbox" class="mt-0.5" [checked]="shareLocation()" (change)="shareLocation.set($any($event.target).checked)" />
              <span>Partager ma position pour trouver le médecin le plus proche (facultatif). Elle n'est ni enregistrée ni envoyée au modèle.</span>
            </label>
          }
        </div>

        <form class="flex shrink-0 items-center gap-2 border-t border-border bg-card px-4 py-3 sm:px-8" (submit)="$event.preventDefault(); ask(draft())">
          <input class="field h-11 flex-1 rounded-full px-5" dir="auto" maxlength="2000" [placeholder]="placeholder()" aria-label="Vos symptômes"
            [value]="draft()" (input)="draft.set($any($event.target).value)" />
          <button type="submit" class="btn btn-hero btn-icon h-11 w-11 rounded-full" [disabled]="!draft().trim() || typing()" aria-label="Envoyer">
            <app-icon [icon]="Send" [size]="18" />
          </button>
        </form>
      </section>
    </div>

    <app-modal [open]="urgenceOpen()" title="En cas d'urgence" description="Cet assistant ne remplace pas un médecin. Si votre vie est en danger, appelez sans attendre." (closed)="urgenceOpen.set(false)">
      <div class="grid gap-4 sm:grid-cols-2">
        <a href="tel:190" class="rounded-2xl bg-destructive/10 p-6 text-center transition-colors hover:bg-destructive/15">
          <p class="text-4xl font-bold text-destructive">190</p><p class="mt-1 text-sm text-muted-foreground">SAMU</p>
        </a>
        <a href="tel:198" class="rounded-2xl bg-warning/20 p-6 text-center transition-colors hover:bg-warning/30">
          <p class="text-4xl font-bold text-accent-foreground">198</p><p class="mt-1 text-sm text-muted-foreground">Protection civile</p>
        </a>
      </div>
      <div modalFooter class="contents"><button type="button" class="btn btn-soft" (click)="urgenceOpen.set(false)">Fermer</button></div>
    </app-modal>

    <app-modal [open]="howOpen()" title="Comment ça marche" description="Trois étapes simples pour être orienté." (closed)="howOpen.set(false)">
      <ol class="space-y-5">
        @for (s of steps; track s.title; let i = $index) {
          <li class="flex gap-4">
            <span class="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-gradient-primary font-bold text-white">{{ i + 1 }}</span>
            <div>
              <p class="font-semibold text-deep">{{ s.title }}</p>
              <p class="text-sm text-muted-foreground">{{ s.text }}</p>
            </div>
          </li>
        }
      </ol>
      <div modalFooter class="contents"><button type="button" class="btn btn-hero" (click)="howOpen.set(false)">Compris</button></div>
    </app-modal>
  `,
})
export class AssistantPage {
  private readonly toasts = inject(Toasts);
  private readonly api = inject(AssistantService);

  protected readonly Bot = LucideBot;
  protected readonly Send = LucideSend;
  protected readonly Shield = LucideShieldAlert;
  protected readonly Restart = LucideRotateCcw;
  protected readonly Phone = LucidePhone;
  protected readonly Steth = LucideStethoscope;
  protected readonly Help = LucideCircleHelp;
  protected readonly Lock = LucideLock;
  protected readonly levels = LEVELS;

  protected readonly suggestions = [
    { label: 'Fièvre depuis 2 jours', icon: LucideThermometer },
    { label: 'Mal de tête persistant', icon: LucideBrain },
    { label: 'Toux et mal de gorge', icon: LucideWind },
    { label: 'Douleur au dos', icon: LucideActivity },
  ];
  protected readonly steps = [
    { title: 'Décrivez', text: 'Expliquez vos symptômes avec vos mots, en français, en arabe ou en anglais.' },
    { title: 'Recevez une orientation', text: "Un niveau d'urgence et des conseils prudents. Ce n'est pas un diagnostic." },
    { title: 'Consultez', text: 'Trouvez le bon spécialiste et réservez, sauf en cas d\'urgence vitale.' },
  ];

  // Interface
  protected readonly urgenceOpen = signal(false);
  protected readonly howOpen = signal(false);
  protected readonly detailsOpen = signal(false);

  // Réglages de la demande
  protected readonly langs: TriageLanguage[] = ['fr', 'ar', 'en'];
  protected readonly lang = signal<TriageLanguage>('fr');
  protected readonly details = signal<Details>({ age: null, sex: '', durationDays: null, intensity: null });
  protected readonly shareLocation = signal(false);

  // Conversation
  private readonly scroller = viewChild<ElementRef<HTMLElement>>('scroller');
  private n = 1;
  protected readonly messages = signal<Bubble[]>([this.greeting()]);
  protected readonly draft = signal('');
  protected readonly typing = signal(false);

  // Session de triage en cours (questions de clarification posées une par une)
  private sessionId: string | null = null;
  private questions: string[] = [];
  private answers: string[] = [];

  constructor() {
    effect(() => {
      this.messages();
      this.typing();
      setTimeout(() => {
        const el = this.scroller()?.nativeElement;
        if (el) el.scrollTo({ top: el.scrollHeight, behavior: 'smooth' });
      });
    });
  }

  private greeting(): Bubble {
    return {
      id: 1,
      from: 'bot',
      text: 'Bonjour, je suis votre assistant santé. Décrivez-moi ce que vous ressentez et je vous orienterai vers le bon professionnel.',
      disclaimer: "Cette orientation est fournie par une intelligence artificielle à titre indicatif. Elle ne constitue pas un diagnostic et ne remplace pas l'avis d'un médecin.",
    };
  }

  protected placeholder(): string {
    return this.sessionId ? 'Votre réponse…' : 'Décrivez vos symptômes…';
  }

  protected levelIcon(l: Level) {
    return l === 'low' ? LucideCircleCheck : l === 'mid' ? LucideTriangleAlert : LucideShieldAlert;
  }

  protected place(d: DoctorSuggestion): string {
    const where = [d.city, d.governorate].filter(Boolean).join(', ');
    const km = d.distanceKm == null ? '' : `${d.distanceKm.toFixed(1)} km`;
    return [where, km].filter(Boolean).join(' · ');
  }

  protected privacy(): void {
    this.toasts.success(
      'Vos données sont protégées',
      "Votre nom et votre téléphone ne sont pas transmis au modèle IA. Votre position, si vous la partagez, n'est ni enregistrée ni envoyée au modèle.",
    );
  }

  // ───────── Précisions facultatives ─────────
  protected setNum(key: 'age' | 'durationDays' | 'intensity', raw: string): void {
    const parsed = raw.trim() === '' ? NaN : Number(raw);
    const val = Number.isFinite(parsed) ? Math.trunc(parsed) : null;
    this.details.update((d) =>
      key === 'age' ? { ...d, age: val } : key === 'durationDays' ? { ...d, durationDays: val } : { ...d, intensity: val },
    );
  }

  protected setSex(raw: string): void {
    const sex: Sex = raw === 'MALE' || raw === 'FEMALE' || raw === 'OTHER' ? raw : '';
    this.details.update((d) => ({ ...d, sex }));
  }

  private startRequest(symptoms: string, pos: Coords | null): TriageStartRequest {
    const d = this.details();
    const req: TriageStartRequest = { symptoms, language: this.lang() };
    if (d.age !== null && d.age >= 0 && d.age <= 120) req.age = d.age;
    if (d.sex) req.sex = d.sex;
    if (d.durationDays !== null && d.durationDays >= 0 && d.durationDays <= 3650) req.durationDays = d.durationDays;
    if (d.intensity !== null && d.intensity >= 1 && d.intensity <= 10) req.intensity = d.intensity;
    if (pos) { req.latitude = pos.latitude; req.longitude = pos.longitude; }
    return req;
  }

  /** Position du patient : uniquement s'il a donné son accord. Jamais bloquant. */
  private position(): Promise<Coords | null> {
    if (!this.shareLocation() || !('geolocation' in navigator)) return Promise.resolve(null);
    return new Promise((resolve) => {
      navigator.geolocation.getCurrentPosition(
        (p) => resolve({ latitude: p.coords.latitude, longitude: p.coords.longitude }),
        () => {
          this.shareLocation.set(false);
          this.toasts.error('Position indisponible', "La recherche se fera sans votre position.");
          resolve(null);
        },
        { timeout: 6000, maximumAge: 300000 },
      );
    });
  }

  // ───────── Conversation ─────────
  private resetSession(): void {
    this.sessionId = null;
    this.questions = [];
    this.answers = [];
  }

  private say(b: Omit<Bubble, 'id' | 'from'>): void {
    this.messages.update((l) => [...l, { id: ++this.n, from: 'bot', ...b }]);
  }

  private stepLabel(index: number): string {
    return `Question ${index + 1}/${this.questions.length}`;
  }

  protected restart(): void {
    this.n = 1;
    this.typing.set(false);
    this.resetSession();
    this.messages.set([this.greeting()]);
  }

  protected async ask(text: string): Promise<void> {
    const t = text.trim();
    if (!t || this.typing()) return;
    this.messages.update((l) => [...l, { id: ++this.n, from: 'me', text: t }]);
    this.draft.set('');

    // Clarification : on pose les questions une par une, puis on envoie toutes les réponses d'un coup.
    if (this.sessionId) {
      this.answers.push(t);
      if (this.answers.length < this.questions.length) {
        const i = this.answers.length;
        this.say({ text: this.questions[i], step: this.stepLabel(i) });
        return;
      }
    }

    this.typing.set(true);
    const pos = await this.position();
    const call$ = this.sessionId
      ? this.api.answer(this.sessionId, { answers: this.answers, ...(pos ?? {}) })
      : this.api.start(this.startRequest(t, pos));

    call$.subscribe({
      next: (r) => {
        this.typing.set(false);
        this.onResult(r);
      },
      error: (e: HttpErrorResponse) => {
        this.typing.set(false);
        this.resetSession();
        this.say({ text: this.errorText(e), tips: ['En cas de symptômes graves, appelez le 190 sans attendre.'] });
      },
    });
  }

  private errorText(e: HttpErrorResponse): string {
    if (e.status === 401 || e.status === 403) return 'Votre session a expiré. Reconnectez-vous puis réessayez.';
    if (e.status === 0 || e.status >= 500) return "Le service d'orientation est momentanément indisponible. Réessayez dans un instant.";
    return "Votre message n'a pas pu être traité. Décrivez de nouveau vos symptômes.";
  }

  private onResult(r: TriageSession): void {
    // Le service demande des précisions avant de conclure.
    if (r.status === 'NEEDS_CLARIFICATION' && r.clarificationQuestions?.length) {
      this.sessionId = r.sessionId;
      this.questions = r.clarificationQuestions;
      this.answers = [];
      this.say({
        text: r.preliminaryExplanation || 'Pour mieux vous orienter, répondez à quelques questions.',
        disclaimer: r.disclaimer,
      });
      this.say({ text: this.questions[0], step: this.stepLabel(0) });
      return;
    }

    // Résultat final.
    this.resetSession();
    const a = r.assessment;
    const code = a?.urgency_level ?? 'N3';
    const level: Level = r.emergency ? 'high' : (LEVEL_OF[code] ?? 'mid');
    const conditions = (a?.possible_conditions ?? []).slice(0, 3).map((c) => {
      const pct = c.likelihood <= 1 ? Math.round(c.likelihood * 100) : Math.round(c.likelihood);
      return `${c.label} (${pct} %)`;
    });

    this.say({
      text: a?.patient_explanation || r.disclaimer,
      level,
      code,
      emergency: r.emergency,
      specialty: r.bookingAllowed ? SPECIALTIES[a?.recommended_specialty ?? ''] : undefined,
      tips: r.emergency ? r.interimAdvice : conditions,
      doctors: r.emergency ? undefined : r.suggestedDoctors,
      disclaimer: r.disclaimer,
    });

    // Urgence vitale : écran d'urgence immédiat, sans attendre une réservation.
    if (r.emergency) this.urgenceOpen.set(true);
  }
}