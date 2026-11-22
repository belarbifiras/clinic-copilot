import { DecimalPipe } from '@angular/common';
import { Component, ElementRef, computed, effect, inject, signal, viewChild } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import {
  LucideActivity,
  LucideArrowRight,
  LucideBot,
  LucideBrain,
  LucideCalendar,
  LucideCalendarCheck,
  LucideCheck,
  LucideCircleAlert,
  LucideCircleCheck,
  LucideClock,
  LucideExternalLink,
  LucideMapPin,
  LucidePhone,
  LucideRotateCcw,
  LucideSend,
  LucideShieldAlert,
  LucideStethoscope,
  LucideThermometer,
  LucideTriangleAlert,
  LucideWind,
} from '@lucide/angular';
import { Icon } from '../../shared/ui/icon';
import { Modal } from './overlays';
import { Toasts } from './toasts';
import {
  AssistantService,
  DoctorSuggestion,
  TriageLanguage,
  TriageSession,
  TriageStartRequest,
} from './assistant.service';
import { PatientAppointments } from './patient-appointments';
import { AuthService } from '../../core/auth/auth.service';
import { loadJson, saveJson, sessionEmail } from './patient-storage';
import { DOCTORS, Doctor } from './patient.models';
import {
  daySlots,
  formatLongDate,
  isBookableDay,
  nextAvailable,
  parseLocal,
  toDateKey,
} from './appointment-utils';

type Level = 'low' | 'mid' | 'high';

interface Bubble {
  id: number;
  from: 'me' | 'bot';
  text: string;
  level?: Level;
  code?: string;
  step?: string;
  options?: string[];
  specialty?: string;
  conditions?: { label: string; likelihood: number }[];
  tips?: string[];
  doctors?: DoctorSuggestion[];
  emergency?: boolean;
  bookingSuccess?: {
    doctorName: string;
    specialty?: string;
    date: string;
    time: string;
  };
}

interface Coords {
  latitude: number;
  longitude: number;
}

/** Conversation conservée entre deux sessions/rechargements, par patient. */
interface SavedConversation {
  messages: Bubble[];
  sessionId: string | null;
  lastSymptoms: string;
  specialtyLabel: string;
  n: number;
}

function conversationKey(): string {
  return `cc_assistant_${sessionEmail()}`;
}

/** Conversation archivée dans l'historique — separée de la conversation courante. */
interface ConvHistorique {
  id: string;
  title: string;
  savedAt: string;
  specialtyLabel: string;
  messages: Bubble[];
  expanded?: boolean;
}

function historiqueKey(): string {
  return `cc_assistant_history_${sessionEmail()}`;
}

/** Codes de spécialités du service IA -> libellés affichés. */
const SPECIALTIES: Record<string, string> = {
  general_medicine: 'Médecin généraliste',
  cardiology: 'Cardiologue',
  pulmonology: 'Pneumologue',
  neurology: 'Neurologue',
  gastroenterology: 'Gastro-entérologue',
  dermatology: 'Dermatologue',
  ent: 'ORL',
  ophthalmology: 'Ophtalmologue',
  orthopedics: 'Orthopédiste',
  gynecology: 'Gynécologue',
  pediatrics: 'Pédiatre',
  psychiatry: 'Psychiatre',
  urology: 'Urologue',
  endocrinology: 'Endocrinologue',
};

const LEVEL_CONFIG: Record<string, { label: string; cls: string; bar: string; level: Level }> = {
  N1: { label: 'Urgence vitale immédiate', cls: 'bg-destructive/15 text-destructive border-destructive/30', bar: 'bg-destructive', level: 'high' },
  N2: { label: 'Consultation urgente (sous 24h)', cls: 'bg-amber-500/15 text-amber-700 border-amber-500/30', bar: 'bg-amber-500', level: 'mid' },
  N3: { label: 'Consultation conseillée (sous 48-72h)', cls: 'bg-warning/25 text-accent-foreground border-warning/40', bar: 'bg-warning', level: 'mid' },
  N4: { label: 'Consultation de suivi non urgente', cls: 'bg-success/15 text-success border-success/30', bar: 'bg-success', level: 'low' },
  N5: { label: 'Conseil & surveillance', cls: 'bg-primary/15 text-primary border-primary/30', bar: 'bg-primary', level: 'low' },
};

function parseQuestionAndOptions(q: string): { questionText: string; options: string[] } {
  const match = q.match(/^(.*?)\s*\(([^)]+)\)\s*$/);
  if (match && match[2].includes('/')) {
    const questionText = match[1].trim();
    const options = match[2].split('/').map((s) => s.trim()).filter(Boolean);
    return { questionText, options };
  }
  return { questionText: q, options: [] };
}

@Component({
  selector: 'app-assistant-page',
  imports: [RouterLink, Icon, Modal, DecimalPipe],
  styles: `
    @keyframes msg-in { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: none; } }
    @keyframes dot { 0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; } 40% { transform: scale(1); opacity: 1; } }
    @keyframes pulse-ring { 0% { transform: scale(0.95); opacity: 0.8; } 100% { transform: scale(1.6); opacity: 0; } }
    .msg-in { animation: msg-in 0.3s cubic-bezier(0.2, 0.8, 0.2, 1) both; }
    .dot { animation: dot 1.2s ease-in-out infinite; }
    .pulse-ring { animation: pulse-ring 1.8s ease-out infinite; }
  `,
  template: `
    <div class="flex h-[calc(100dvh-6.5rem)] min-h-[540px] gap-3 p-3 md:p-6 lg:flex-row">
      <!-- Conteneur principal du Chatbot -->
      <section class="flex min-h-0 flex-1 flex-col overflow-hidden rounded-3xl border border-border bg-card shadow-soft" aria-label="Assistant Médical IA">
        
        <!-- En-tête épuré et moderne -->
        <header class="flex shrink-0 items-center justify-between border-b border-border/80 bg-card/90 px-5 py-3.5 backdrop-blur">
          <div class="flex items-center gap-3">
            <div class="relative grid h-10 w-10 place-items-center rounded-2xl bg-gradient-primary text-white shadow-sm">
              @if (typing()) { <span class="pulse-ring absolute inset-0 rounded-2xl bg-primary/40"></span> }
              <app-icon [icon]="Bot" [size]="20" />
            </div>
            <div>
              <div class="flex items-center gap-2">
                <h1 class="text-sm font-bold text-deep sm:text-base">Assistant Santé IA</h1>
                <span class="inline-flex items-center gap-1 rounded-full bg-primary/10 px-2 py-0.5 text-[11px] font-semibold text-primary">Clinic Copilot</span>
              </div>
              <p class="flex items-center gap-1.5 text-xs text-muted-foreground">
                <span class="h-2 w-2 rounded-full" [class]="typing() ? 'bg-amber-500 animate-pulse' : 'bg-emerald-500'"></span>
                {{ typing() ? 'Analyse médicale en cours…' : 'En ligne · Prêt à vous orienter' }}
              </p>
            </div>
          </div>

          <div class="flex items-center gap-2">
            <button type="button" class="btn btn-ghost text-xs text-muted-foreground hover:text-deep" (click)="restart()">
              <app-icon [icon]="Restart" [size]="14" />
              <span class="hidden sm:inline">Nouvelle conversation</span>
            </button>
          </div>
        </header>

        <!-- Fil de discussion des messages -->
        <div #scroller class="bg-mesh min-h-0 flex-1 space-y-4 overflow-y-auto px-4 py-5 sm:px-6">
          @for (m of messages(); track m.id) {
            @if (m.from === 'me') {
              <!-- Message de l'utilisateur -->
              <div class="msg-in flex justify-end">
                <div class="max-w-[85%] rounded-3xl rounded-br-md bg-gradient-primary px-4 py-3 text-[15px] leading-relaxed text-white shadow-soft sm:max-w-[70%]">
                  <p dir="auto">{{ m.text }}</p>
                </div>
              </div>
            } @else {
              <!-- Message du Bot -->
              <div class="msg-in flex items-start gap-3">
                <span class="grid h-8 w-8 shrink-0 place-items-center rounded-full bg-gradient-primary text-white shadow-sm">
                  <app-icon [icon]="Bot" [size]="16" />
                </span>

                <div class="glass max-w-[90%] space-y-3.5 rounded-3xl rounded-tl-md border border-border/80 bg-card/95 p-4 shadow-soft sm:max-w-[75%] sm:p-5">
                  
                  <!-- Cas d'urgence vitale détectée -->
                  @if (m.emergency) {
                    <div class="flex items-start gap-3 rounded-2xl border border-destructive/30 bg-destructive/10 p-3.5 text-destructive">
                      <app-icon [icon]="Shield" [size]="20" class="shrink-0" />
                      <div class="space-y-2">
                        <p class="text-sm font-bold">Urgence vitale possible : n'attendez pas un rendez-vous !</p>
                        <p class="text-xs text-destructive/90">Contactez immédiatement les services de secours d'urgence.</p>
                        <div class="flex flex-wrap gap-2 pt-1">
                          <a href="tel:190" class="btn rounded-full bg-destructive text-xs font-bold text-white hover:opacity-90">
                            <app-icon [icon]="Phone" [size]="14" />Appeler le 190 (SAMU)
                          </a>
                          <a href="tel:198" class="btn rounded-full bg-amber-600 text-xs font-bold text-white hover:opacity-90">
                            <app-icon [icon]="Phone" [size]="14" />198 Protection Civile
                          </a>
                        </div>
                      </div>
                    </div>
                  }

                  <!-- Contenu textuel principal -->
                  <p dir="auto" class="text-[15px] leading-relaxed text-deep whitespace-pre-line">{{ m.text }}</p>

                  <!-- Options rapides en cas de question de clarification -->
                  @if (m.options?.length) {
                    <div class="pt-1">
                      <p class="mb-2 text-xs font-medium text-muted-foreground">Sélectionnez ou tapez votre réponse :</p>
                      <div class="flex flex-wrap gap-2">
                        @for (opt of m.options; track opt) {
                          <button type="button" (click)="ask(opt)"
                            class="rounded-full border border-primary/30 bg-soft px-3.5 py-1.5 text-xs font-medium text-primary transition-colors hover:bg-gradient-primary hover:text-white">
                            {{ opt }}
                          </button>
                        }
                      </div>
                    </div>
                  }

                  <!-- Possibilités diagnostiques avec probabilités -->
                  @if (m.conditions?.length) {
                    <div class="space-y-2.5 rounded-2xl bg-soft/60 p-3.5">
                      <p class="flex items-center gap-1.5 text-xs font-bold uppercase tracking-wide text-muted-foreground">
                        <app-icon [icon]="Brain" [size]="14" class="text-primary" />Causes ou affections possibles
                      </p>
                      <div class="space-y-2">
                        @for (c of m.conditions; track c.label) {
                          <div class="space-y-1">
                            <div class="flex justify-between text-xs font-medium text-deep">
                              <span>{{ c.label }}</span>
                              <span class="font-bold text-primary">{{ c.likelihood <= 1 ? (c.likelihood * 100 | number:'1.0-0') : c.likelihood }} %</span>
                            </div>
                            <div class="h-1.5 w-full overflow-hidden rounded-full bg-border">
                              <div class="h-full rounded-full bg-gradient-primary transition-all duration-500"
                                [style.width.%]="c.likelihood <= 1 ? c.likelihood * 100 : c.likelihood"></div>
                            </div>
                          </div>
                        }
                      </div>
                    </div>
                  }

                  <!-- Consignes et conseils pratiques -->
                  @if (m.tips?.length) {
                    <div class="space-y-2 rounded-2xl bg-soft/40 p-3.5">
                      <p class="flex items-center gap-1.5 text-xs font-bold uppercase tracking-wide text-muted-foreground">
                        <app-icon [icon]="Check" [size]="14" class="text-success" />Consignes et conseils pratiques
                      </p>
                      <ul class="space-y-1.5">
                        @for (t of m.tips; track t) {
                          <li class="flex items-start gap-2 text-xs leading-relaxed text-muted-foreground">
                            <span class="mt-1 h-1.5 w-1.5 shrink-0 rounded-full bg-primary"></span>
                            <span>{{ t }}</span>
                          </li>
                        }
                      </ul>
                    </div>
                  }

                  <!-- Professionnel de santé conseillé (meilleur selon le cas) -->
                  @if (m.specialty && !m.emergency) {
                    <div class="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-primary/20 bg-primary/5 p-3 sm:p-4">
                      <div class="flex items-center gap-2.5">
                        <span class="grid h-9 w-9 shrink-0 place-items-center rounded-xl bg-gradient-primary text-white shadow-sm">
                          <app-icon [icon]="Steth" [size]="18" />
                        </span>
                        <div>
                          <p class="text-[11px] font-medium text-muted-foreground">Meilleur médecin selon votre cas</p>
                          <p class="text-sm font-bold text-deep">{{ m.specialty }}</p>
                        </div>
                      </div>
                      <a routerLink="/espace-patient/medecins" class="btn btn-hero py-1.5 text-xs">
                        Consulter l'annuaire complet
                      </a>
                    </div>
                  }

                  <!-- Médecins recommandés les plus proches avec bouton de prise de rendez-vous immédiat -->
                  @if (m.doctors?.length) {
                    <div class="space-y-2.5 pt-1">
                      <p class="flex items-center gap-1.5 text-xs font-bold uppercase tracking-wide text-muted-foreground">
                        <app-icon [icon]="Pin" [size]="14" class="text-primary" />Médecins recommandés les plus proches
                      </p>
                      <div class="grid gap-2.5 sm:grid-cols-2">
                        @for (d of m.doctors; track d.id; let i = $index) {
                          <div class="flex flex-col justify-between rounded-2xl border border-border/80 bg-card p-3 shadow-xs transition-colors hover:border-primary/40">
                            @if (i === 0) {
                              <span class="mb-1.5 inline-block rounded-full bg-primary/10 px-2 py-0.5 text-[10px] font-bold text-primary">Plus proche</span>
                            }
                            <div class="flex items-start gap-2.5">
                              <span class="grid h-8 w-8 shrink-0 place-items-center rounded-xl bg-primary/10 text-primary">
                                <app-icon [icon]="Steth" [size]="16" />
                              </span>
                              <div class="min-w-0 flex-1">
                                <h4 class="truncate text-xs font-bold text-deep sm:text-sm">{{ d.displayName }}</h4>
                                <p class="flex items-center gap-1 text-[11px] text-muted-foreground">
                                  <app-icon [icon]="Pin" [size]="11" />{{ place(d) }}
                                </p>
                              </div>
                            </div>

                            <div class="mt-3 flex items-center gap-2">
                              <button type="button" class="btn btn-hero flex-1 py-1.5 text-xs font-medium" (click)="openBooking(d)">
                                <app-icon [icon]="Cal" [size]="14" />Prendre RDV
                              </button>
                              <a [href]="googleMapsUrl(d)" target="_blank" rel="noopener noreferrer" class="btn btn-ghost px-2 py-1.5 text-xs text-muted-foreground hover:text-primary" title="Localiser sur Google Maps">
                                <app-icon [icon]="Pin" [size]="14" />
                              </a>
                              <a [routerLink]="['/medecins', d.id]" target="_blank" class="btn btn-ghost px-2 py-1.5 text-xs text-muted-foreground hover:text-deep" title="Voir la fiche complète">
                                <app-icon [icon]="ExtLink" [size]="14" />
                              </a>
                            </div>
                          </div>
                        }
                      </div>
                    </div>
                  }

                  <!-- Confirmation de rendez-vous pris directement dans le chat -->
                  @if (m.bookingSuccess; as bk) {
                    <div class="flex items-start gap-3 rounded-2xl border border-emerald-500/30 bg-emerald-500/10 p-4 text-emerald-950">
                      <span class="grid h-9 w-9 shrink-0 place-items-center rounded-xl bg-emerald-600 text-white shadow-sm">
                        <app-icon [icon]="Check" [size]="20" />
                      </span>
                      <div class="space-y-1">
                        <p class="text-sm font-bold text-emerald-900">Rendez-vous confirmé avec succès !</p>
                        <p class="text-xs text-emerald-800">
                          Consultation avec <strong>{{ bk.doctorName }}</strong> le <strong>{{ bk.date }}</strong> à <strong>{{ bk.time }}</strong>.
                        </p>
                        <div class="pt-2">
                          <a routerLink="/espace-patient/rendez-vous" class="btn rounded-full bg-emerald-600 px-3.5 py-1 text-xs font-bold text-white hover:bg-emerald-700">
                            Voir mes rendez-vous
                          </a>
                        </div>
                      </div>
                    </div>
                  }
                </div>
              </div>
            }
          }

          <!-- Indicateur d'écriture / analyse en cours -->
          @if (typing()) {
            <div class="msg-in flex items-start gap-3">
              <span class="grid h-8 w-8 shrink-0 place-items-center rounded-full bg-gradient-primary text-white shadow-sm">
                <app-icon [icon]="Bot" [size]="16" />
              </span>
              <div class="glass inline-flex items-center gap-1.5 rounded-3xl rounded-tl-md border border-border px-4 py-3.5">
                <span class="dot h-2 w-2 rounded-full bg-primary"></span>
                <span class="dot h-2 w-2 rounded-full bg-primary" style="animation-delay: 0.18s"></span>
                <span class="dot h-2 w-2 rounded-full bg-primary" style="animation-delay: 0.36s"></span>
              </div>
            </div>
          }

          <!-- Suggestions initiales lorsque la conversation débute -->
          @if (messages().length === 1 && !typing()) {
            <div class="msg-in pt-2">
              <p class="mb-2 text-xs font-bold uppercase tracking-wider text-muted-foreground">Suggestions fréquentes</p>
              <div class="grid gap-2 sm:grid-cols-2 lg:grid-cols-4">
                @for (s of suggestions; track s.label) {
                  <button type="button" (click)="ask(s.label)"
                    class="card-lift flex items-center gap-2.5 rounded-2xl border border-border/80 bg-card p-3 text-left shadow-xs transition-colors hover:border-primary/50">
                    <span class="grid h-8 w-8 shrink-0 place-items-center rounded-xl bg-primary/10 text-primary">
                      <app-icon [icon]="s.icon" [size]="16" />
                    </span>
                    <span class="text-xs font-medium text-deep line-clamp-2">{{ s.label }}</span>
                  </button>
                }
              </div>
            </div>
          }
        </div>

        <!-- Barre de saisie simple et épurée -->
        <div class="border-t border-border/80 bg-card p-3 sm:px-6">
          <form class="flex items-center gap-2" (submit)="$event.preventDefault(); ask(draft())">
            <!-- Sélecteur de langue compact -->
            <div class="relative shrink-0">
              <select class="field h-11 rounded-full px-3 text-xs font-semibold uppercase text-deep" [value]="lang()" (change)="lang.set($any($event.target).value)">
                @for (l of langs; track l) {
                  <option [value]="l">{{ l }}</option>
                }
              </select>
            </div>

            <!-- Champ texte -->
            <input class="field h-11 flex-1 rounded-full px-5 text-sm" dir="auto" maxlength="2000"
              [placeholder]="placeholder()" aria-label="Vos symptômes"
              [value]="draft()" (input)="draft.set($any($event.target).value)" />

            <!-- Bouton envoyer -->
            <button type="submit" class="btn btn-hero btn-icon h-11 w-11 shrink-0 rounded-full" [disabled]="!draft().trim() || typing()" aria-label="Envoyer">
              <app-icon [icon]="Send" [size]="18" />
            </button>
          </form>
        </div>
      </section>

      <!-- Historique : conversations passées archivées, à côté du chat -->
      <aside class="hidden lg:block w-72 shrink-0 space-y-2 overflow-y-auto rounded-3xl border border-border bg-card p-3 shadow-soft" aria-label="Historique des conversations">
        <div class="flex items-center justify-between gap-2 pb-2">
          <p class="text-xs font-bold uppercase tracking-wide text-muted-foreground">Historique ({{ historique().length }})</p>
          @if (historique().length) {
            <button type="button" class="text-xs font-semibold text-primary hover:underline" (click)="historique.set([])">Effacer</button>
          }
        </div>
        @for (h of historique(); track h.id) {
          <div class="overflow-hidden rounded-xl border border-border/80 bg-card">
            <div class="flex items-center gap-2 px-2.5 py-2">
              <span class="min-w-0 flex-1 truncate text-xs font-semibold text-deep" [title]="h.title">{{ h.title }}</span>
              <span class="shrink-0 text-[10px] text-muted-foreground">{{ fmtDate(h.savedAt) }}</span>
            </div>
            @if (h.specialtyLabel) {
              <p class="border-t border-border/70 px-2.5 py-1.5 text-[11px] text-muted-foreground">{{ h.specialtyLabel }}</p>
            }
            <div class="overflow-hidden px-2.5 pb-2" [class.h-0]="!(h.expanded ?? false)" [class.max-h-72]="(h.expanded ?? false)">
              <div class="space-y-2">
                @for (m of h.messages; track m.id) {
                  @if (m.from === 'me') {
                    <p class="text-right text-xs font-medium text-deep">{{ m.text }}</p>
                  } @else {
                    <p class="text-xs leading-snug text-deep/80 whitespace-pre-line">{{ m.text }}</p>
                  }
                }
              </div>
            </div>
            <button type="button" class="w-full rounded-lg px-2.5 py-1.5 text-left text-[11px] font-semibold text-muted-foreground transition-colors hover:bg-soft/60" (click)="toggleHist(h.id)">
              {{ (h.expanded ?? false) ? 'Masquer' : 'Voir le détail' }}
            </button>
          </div>
        }
        @if (!historique().length) {
          <p class="px-1 py-3 text-center text-xs text-muted-foreground">Aucune conversation archivée.</p>
        }
      </aside>
    </div>

    <!-- MODAL DE PRISE DE RENDEZ-VOUS DIRECTE -->
    <app-modal [open]="!!bookingDoctor()" [title]="'Prendre rendez-vous'" [description]="bookingDoctor()?.displayName ?? ''" (closed)="bookingDoctor.set(null)">
      @if (bookingDoctor(); as doc) {
        <div class="space-y-4">
          <!-- Carte récapitulative du médecin -->
          <div class="flex items-center gap-3 rounded-2xl bg-soft p-3.5">
            <span class="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-gradient-primary text-white">
              <app-icon [icon]="Steth" [size]="20" />
            </span>
            <div class="min-w-0 flex-1">
              <h4 class="font-bold text-deep">{{ doc.displayName }}</h4>
              <p class="text-xs text-muted-foreground">{{ currentSpecialtyLabel() || 'Médecin spécialiste' }} · {{ place(doc) }}</p>
            </div>
          </div>

          <!-- Choix du jour -->
          <div>
            <label class="mb-1.5 block text-xs font-bold uppercase tracking-wide text-muted-foreground">Choisir la date</label>
            <div class="flex flex-wrap gap-2">
              @for (day of availableDays(); track day.toISOString()) {
                <button type="button" (click)="selectDay(day)"
                  class="rounded-xl border px-3 py-2 text-xs font-semibold transition-colors"
                  [class]="isDaySelected(day) ? 'border-primary bg-gradient-primary text-white shadow-sm' : 'border-border bg-card text-deep hover:bg-soft'">
                  {{ formatDayChip(day) }}
                </button>
              }
            </div>
          </div>

          <!-- Choix du créneau horaire -->
          <div>
            <label class="mb-1.5 block text-xs font-bold uppercase tracking-wide text-muted-foreground">Créneau disponible</label>
            @if (bookingSlots().length === 0) {
              <p class="rounded-xl bg-soft p-3 text-xs text-muted-foreground">Aucun créneau disponible ce jour. Choisissez une autre date.</p>
            } @else {
              <div class="grid grid-cols-4 gap-2">
                @for (s of bookingSlots(); track s.start) {
                  <button type="button" [disabled]="!s.free" (click)="selectedSlot.set(s.start)"
                    class="rounded-xl border py-2 text-xs font-semibold transition-colors"
                    [class]="selectedSlot() === s.start ? 'border-primary bg-gradient-primary text-white' : s.free ? 'border-border bg-card text-deep hover:border-primary/50' : 'cursor-not-allowed bg-muted text-muted-foreground/50 line-through'">
                    {{ s.time }}
                  </button>
                }
              </div>
            }
          </div>

          <!-- Motif du rendez-vous -->
          <div>
            <label class="mb-1.5 block text-xs font-bold uppercase tracking-wide text-muted-foreground">Motif de consultation</label>
            <input class="field h-10 w-full rounded-xl px-3 text-xs" [value]="bookingReason()" (input)="bookingReason.set($any($event.target).value)"
              placeholder="Ex. : Orientation suite à des maux de tête" />
          </div>

          @if (bookingError()) {
            <p class="rounded-xl bg-destructive/10 p-3 text-xs font-medium text-destructive">{{ bookingError() }}</p>
          }
        </div>
      }

      @if (bookingDoctor()) {
        <div modalFooter class="contents">
          <button type="button" class="btn btn-ghost" (click)="bookingDoctor.set(null)">Annuler</button>
          <button type="button" class="btn btn-hero" [disabled]="!selectedSlot() || bookingBusy()" (click)="confirmBooking()">
            {{ bookingBusy() ? 'Réservation en cours…' : 'Confirmer le rendez-vous' }}
          </button>
        </div>
      }
    </app-modal>
  `,
})
export class AssistantPage {
  private readonly toasts = inject(Toasts);
  private readonly api = inject(AssistantService);
  private readonly apptSvc = inject(PatientAppointments);

  protected readonly Bot = LucideBot;
  protected readonly Send = LucideSend;
  protected readonly Shield = LucideShieldAlert;
  protected readonly Restart = LucideRotateCcw;
  protected readonly Phone = LucidePhone;
  protected readonly Steth = LucideStethoscope;
  protected readonly Check = LucideCheck;
  protected readonly Brain = LucideBrain;
  protected readonly Pin = LucideMapPin;
  protected readonly Cal = LucideCalendar;
  protected readonly ExtLink = LucideExternalLink;
  protected readonly Clock = LucideClock;
  protected readonly levelConfig = LEVEL_CONFIG;

  protected readonly suggestions = [
    { label: 'Fièvre et courbatures depuis 2 jours', icon: LucideThermometer },
    { label: 'Maux de tête persistants et nausées', icon: LucideBrain },
    { label: 'Toux sèche avec gêne respiratoire', icon: LucideWind },
    { label: 'Douleur au dos suite à un effort', icon: LucideActivity },
  ];

  // Réglages
  protected readonly langs: TriageLanguage[] = ['fr', 'ar', 'en'];
  protected readonly lang = signal<TriageLanguage>('fr');

  // Conversation (restaurée depuis localStorage si elle existe)
  private readonly scroller = viewChild<ElementRef<HTMLElement>>('scroller');
  private n = 1;
  protected readonly messages = signal<Bubble[]>([]);
  protected readonly draft = signal('');
  protected readonly typing = signal(false);

  // Session de triage
  private sessionId: string | null = null;
  private questions: string[] = [];
  private answers: string[] = [];
  private lastSymptoms = '';
  protected readonly currentSpecialtyLabel = signal<string>('');
  // Historique des conversations passées archivées (separée de la conversation courante).
  protected readonly historique = signal<ConvHistorique[]>(loadJson<ConvHistorique[]>(historiqueKey(), []));
  private readonly auth = inject(AuthService);
  private hadSession = false;

  // Prise de RDV direct
  protected readonly bookingDoctor = signal<DoctorSuggestion | null>(null);
  protected readonly selectedDay = signal<Date>(new Date());
  protected readonly selectedSlot = signal<string | null>(null);
  protected readonly bookingReason = signal('');
  protected readonly bookingBusy = signal(false);
  protected readonly bookingError = signal<string | null>(null);

  protected readonly availableDays = computed(() => {
    const days: Date[] = [];
    const now = new Date();
    for (let i = 0; i < 7; i++) {
      const d = new Date(now.getFullYear(), now.getMonth(), now.getDate() + i);
      if (isBookableDay(d)) days.push(d);
    }
    return days;
  });

  protected readonly bookingSlots = computed(() => {
    const doc = this.bookingDoctor();
    if (!doc) return [];
    return daySlots(this.apptSvc.appointments(), doc.id, this.selectedDay());
  });

  constructor() {
    const saved = loadJson<SavedConversation | null>(conversationKey(), null);
    if (saved && Array.isArray(saved.messages) && saved.messages.length > 0) {
      this.messages.set(saved.messages);
      this.n = typeof saved.n === 'number' ? saved.n : 1;
      this.sessionId = saved.sessionId ?? null;
      this.lastSymptoms = saved.lastSymptoms ?? '';
      this.currentSpecialtyLabel.set(saved.specialtyLabel ?? '');
    } else {
      this.messages.set([this.greeting()]);
    }
    effect(() => {
      this.messages();
      this.typing();
      setTimeout(() => {
        const el = this.scroller()?.nativeElement;
        if (el) el.scrollTo({ top: el.scrollHeight, behavior: 'smooth' });
      }, 50);
    });
    // Historique : au retour d'une déconnexion (transition session -> null), archiver la
    // conversation courante dans l'historique et commencer une nouvelle conversation.
    effect(() => {
      const s = this.auth.session();
      if (this.hadSession && !s) {
        this.archiveCurrent();
        this.startNew();
      }
      this.hadSession = !!s;
    });
  }

  /** Sauvegarde la conversation (messages, session, contexte) : pas de perte à rechargement ni logout. */
  private persist(): void {
    saveJson(conversationKey(), {
      messages: this.messages(),
      sessionId: this.sessionId,
      lastSymptoms: this.lastSymptoms,
      specialtyLabel: this.currentSpecialtyLabel(),
      n: this.n,
    });
  }

  private greeting(): Bubble {
    return {
      id: 1,
      from: 'bot',
      text: 'Bonjour ! Je suis votre assistant médical Clinic Copilot. Décrivez-moi vos symptômes ou ce qui vous préoccupe, et je vais analyser votre état, vous expliquer les possibilités et vous orienter vers les meilleurs spécialistes.',
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

  protected googleMapsUrl(d: DoctorSuggestion): string {
    const q = [d.displayName, d.city, d.governorate, 'Tunisie'].filter(Boolean).join(' ');
    return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(q)}`;
  }

  private resetSession(): void {
    this.sessionId = null;
    this.questions = [];
    this.answers = [];
    this.lastSymptoms = '';
  }

  private say(b: Omit<Bubble, 'id' | 'from'>): void {
    this.messages.update((l) => [...l, { id: ++this.n, from: 'bot', ...b }]);
    this.persist();
  }

  protected restart(): void {
    // Archiver la conversation courante dans l'historique avant de recommencer.
    this.archiveCurrent();
    this.startNew();
  }

  /** Commence une nouvelle conversation (réinitialise la conversation courante). */
  private startNew(): void {
    this.n = 1;
    this.typing.set(false);
    this.resetSession();
    this.messages.set([this.greeting()]);
    this.persist();
    saveJson(historiqueKey(), this.historique());
  }

  /** Archiver la conversation courante dans l'historique si elle contient des échanges. */
  private archiveCurrent(): void {
    const messages = this.messages();
    const userMsgs = messages.filter((m) => m.from === 'me');
    if (userMsgs.length === 0) return;
    this.historique.update((h) => [{
      id: crypto.randomUUID(),
      title: userMsgs[0].text.slice(0, 80),
      savedAt: new Date().toISOString(),
      specialtyLabel: this.currentSpecialtyLabel(),
      messages: [...messages],
    }, ...h]);
    saveJson(historiqueKey(), this.historique());
  }

  protected toggleHist(id: string): void {
    this.historique.update((h) => h.map((e) => (e.id === id ? { ...e, expanded: !e.expanded } : e)));
  }

  protected fmtDate(iso: string): string {
    const d = new Date(iso);
    return d.toLocaleString('fr-FR', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' });
  }

  protected async ask(text: string): Promise<void> {
    const t = text.trim();
    if (!t || this.typing()) return;
    this.messages.update((l) => [...l, { id: ++this.n, from: 'me', text: t }]);
    this.persist();
    this.draft.set('');

    // Si on est dans un cycle de clarification
    if (this.sessionId) {
      this.answers.push(t);
      if (this.answers.length < this.questions.length) {
        const i = this.answers.length;
        const parsed = parseQuestionAndOptions(this.questions[i]);
        this.say({
          text: `Merci pour votre précision.\n\n${parsed.questionText}`,
          options: parsed.options,
        });
        return;
      }
    } else {
      this.lastSymptoms = t;
    }

    this.typing.set(true);
    const coords = await this.getCoords();
    const call$ = this.sessionId
      ? this.api.answer(this.sessionId, { answers: this.answers, ...(coords ?? {}) })
      : this.api.start({ symptoms: t, language: this.lang(), ...(coords ?? {}) });

    call$.subscribe({
      next: (r) => {
        this.typing.set(false);
        this.onResult(r);
      },
      error: (e: HttpErrorResponse) => {
        this.typing.set(false);
        this.resetSession();
        this.say({
          text: this.errorText(e),
          tips: ['En cas de symptômes graves ou d\'urgence vitale, composez le 190 (SAMU) ou le 198.'],
        });
      },
    });
  }

  private getCoords(): Promise<Coords | null> {
    if (!('geolocation' in navigator)) return Promise.resolve(null);
    return new Promise((resolve) => {
      navigator.geolocation.getCurrentPosition(
        (pos) => resolve({ latitude: pos.coords.latitude, longitude: pos.coords.longitude }),
        () => resolve(null),
        { timeout: 2500, maximumAge: 300000 },
      );
    });
  }

  private errorText(e: HttpErrorResponse): string {
    if (e.status === 401 || e.status === 403) return 'Votre session a expiré. Veuillez vous reconnecter pour continuer.';
    if (e.status === 0 || e.status >= 500) return "Le service d'orientation médicale est temporairement indisponible. Veuillez réessayer dans quelques instants.";
    return "Votre message n'a pas pu être traité. Veuillez reformuler vos symptômes.";
  }

  private onResult(r: TriageSession): void {
    // Cas où le modèle LLM demande des précisions
    if (r.status === 'NEEDS_CLARIFICATION' && r.clarificationQuestions?.length) {
      this.sessionId = r.sessionId;
      this.questions = r.clarificationQuestions;
      this.answers = [];

      const parsed = parseQuestionAndOptions(this.questions[0]);
      const intro = r.preliminaryExplanation
        ? `${r.preliminaryExplanation}\n\nPour mieux comprendre votre situation : ${parsed.questionText}`
        : `Pour mieux cerner votre situation et vous orienter au mieux : ${parsed.questionText}`;

      this.say({
        text: intro,
        options: parsed.options,
      });
      return;
    }

    // Résultat final complet
    this.resetSession();
    const a = r.assessment;
    const code = a?.urgency_level ?? 'N3';
    const cfg = this.levelConfig[code] ?? this.levelConfig['N3'];
    const specLabel = SPECIALTIES[a?.recommended_specialty ?? ''] || 'Médecin spécialiste';
    this.currentSpecialtyLabel.set(specLabel);

    this.say({
      text: a?.patient_explanation || 'Voici l\'évaluation de votre situation selon les symptômes décrits :',
      level: cfg.level,
      code,
      emergency: r.emergency,
      conditions: a?.possible_conditions,
      tips: r.emergency ? r.interimAdvice : (a?.red_flags_detected?.length ? a.red_flags_detected : r.interimAdvice),
      specialty: r.bookingAllowed ? specLabel : undefined,
      doctors: r.emergency ? undefined : r.suggestedDoctors,
    });
    // Saisie le médecin recommandé pour le contexte de la conversation.
    this.currentSpecialtyLabel.set(this.messages().at(-1)?.specialty ?? specLabel);
  }

  // ───────── Prise de rendez-vous directe ─────────
  protected openBooking(doctor: DoctorSuggestion): void {
    this.bookingDoctor.set(doctor);
    const days = this.availableDays();
    this.selectDay(days[0] || new Date());
    this.bookingReason.set(`Orientation IA : ${this.lastSymptoms || 'Consultation médicale'}`);
    this.bookingError.set(null);
  }

  protected selectDay(d: Date): void {
    this.selectedDay.set(d);
    const slots = daySlots(this.apptSvc.appointments(), this.bookingDoctor()?.id ?? '', d);
    const firstFree = slots.find((s) => s.free);
    this.selectedSlot.set(firstFree ? firstFree.start : null);
  }

  protected isDaySelected(d: Date): boolean {
    return toDateKey(d) === toDateKey(this.selectedDay());
  }

  protected formatDayChip(d: Date): string {
    const today = new Date();
    if (toDateKey(d) === toDateKey(today)) return "Aujourd'hui";
    const tomorrow = new Date(today.getFullYear(), today.getMonth(), today.getDate() + 1);
    if (toDateKey(d) === toDateKey(tomorrow)) return 'Demain';
    return d.toLocaleDateString('fr-FR', { weekday: 'short', day: 'numeric', month: 'short' });
  }

  protected async confirmBooking(): Promise<void> {
    const doc = this.bookingDoctor();
    const slot = this.selectedSlot();
    const reason = this.bookingReason().trim();
    if (!doc || !slot) return;
    if (!reason) {
      this.bookingError.set('Veuillez préciser le motif de consultation.');
      return;
    }

    this.bookingBusy.set(true);
    this.bookingError.set(null);

    // Enregistrement dynamique dans le catalogue local si absent pour affichage dans "Mes RDV"
    if (!DOCTORS.some((d) => d.id === doc.id)) {
      DOCTORS.push({
        id: doc.id,
        name: doc.displayName,
        specialty: this.currentSpecialtyLabel() || 'Médecin spécialiste',
        rating: 4.8,
        reviewsCount: 42,
        distanceKm: doc.distanceKm ?? 2.5,
        city: doc.city ?? 'Tunis',
        address: `${doc.city || 'Tunis'}, Tunisie`,
        languages: ['Français', 'Arabe'],
        verified: true,
        fee: 60,
        bio: 'Médecin spécialiste recommandé par Clinic Copilot.',
        tone: 'primary',
        online: true,
      });
    }

    const res = await this.apptSvc.book(doc.id, slot, reason);
    this.bookingBusy.set(false);

    if (!res.ok) {
      this.bookingError.set(res.error);
      return;
    }

    this.toasts.success('Rendez-vous réservé !', `Consultation avec ${doc.displayName} confirmée.`);
    this.bookingDoctor.set(null);

    // Ajout d'une bulle de confirmation dans le chat
    const dt = parseLocal(slot);
    this.say({
      text: `Rendez-vous pris avec succès !`,
      bookingSuccess: {
        doctorName: doc.displayName,
        specialty: this.currentSpecialtyLabel(),
        date: dt.toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long' }),
        time: slot.slice(11),
      },
    });
  }
}
