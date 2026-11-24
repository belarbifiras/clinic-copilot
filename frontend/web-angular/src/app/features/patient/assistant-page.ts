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
  LucideHistory,
  LucideMapPin,
  LucideMessageCircle,
  LucidePhone,
  LucidePlus,
  LucideRotateCcw,
  LucideSend,
  LucideShieldAlert,
  LucideStethoscope,
  LucideThermometer,
  LucideTrash2,
  LucideTriangleAlert,
  LucideWind,
  LucideX,
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
import { detectTriageLanguage } from './assistant-language';
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

/** Conversation courante sauvegardée */
interface SavedConversation {
  convId: string;
  messages: Bubble[];
  sessionId: string | null;
  lastSymptoms: string;
  specialtyLabel: string;
  n: number;
  language?: TriageLanguage;
}

/** Conversation archivée dans l'historique */
export interface ConvHistorique {
  id: string;
  title: string;
  savedAt: string;
  specialtyLabel: string;
  messages: Bubble[];
  sessionId?: string | null;
  lastSymptoms?: string;
  n?: number;
  language?: TriageLanguage;
}

function conversationKey(): string {
  return `cc_assistant_${sessionEmail()}`;
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
        <header class="flex shrink-0 items-center justify-between border-b border-border/80 bg-card/90 px-4 py-3 backdrop-blur sm:px-5 sm:py-3.5">
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
                {{ typing() ? 'Analyse médicale en cours…' : 'En ligne · Orientation clinique intelligente' }}
              </p>
            </div>
          </div>

          <div class="flex items-center gap-2">
            <!-- Bouton pour afficher/masquer l'historique sur tous les écrans -->
            <button type="button" class="btn btn-ghost text-xs text-muted-foreground hover:text-deep flex items-center gap-1.5"
              (click)="showHistory.set(!showHistory())" title="Historique des consultations">
              <app-icon [icon]="History" [size]="15" />
              <span class="hidden sm:inline">Historique</span>
              @if (historique().length) {
                <span class="grid h-4.5 min-w-4.5 place-items-center rounded-full bg-primary/15 px-1 text-[10px] font-bold text-primary">
                  {{ historique().length }}
                </span>
              }
            </button>

            <!-- Bouton Nouvelle conversation -->
            <button type="button" class="btn btn-ghost text-xs text-muted-foreground hover:text-deep flex items-center gap-1" (click)="restart()">
              <app-icon [icon]="Plus" [size]="15" />
              <span class="hidden sm:inline">Nouvelle consultation</span>
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

                  <!-- Badge niveau d'urgence -->
                  @if (m.code && levelConfig[m.code]) {
                    <div class="inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-semibold border"
                      [class]="levelConfig[m.code].cls">
                      <app-icon [icon]="levelIcon(levelConfig[m.code].level)" [size]="13" />
                      <span>{{ levelConfig[m.code].label }}</span>
                    </div>
                  }

                  <!-- Contenu textuel principal -->
                  <p dir="auto" class="text-[15px] leading-relaxed text-deep whitespace-pre-line">{{ m.text }}</p>

                  <!-- Options rapides en cas de question de clarification -->
                  @if (m.options?.length) {
                    <div class="pt-1">
                      <p class="mb-2 text-xs font-medium text-muted-foreground">Sélectionnez une réponse ou écrivez la vôtre :</p>
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

                  <!-- Professionnel de santé conseillé -->
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

                  <!-- Médecins recommandés les plus proches -->
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
            <!-- Champ texte -->
            <input class="field h-11 flex-1 rounded-full px-5 text-sm" dir="auto" maxlength="2000"
              [placeholder]="placeholder()" aria-label="Vos symptômes ou votre question de santé"
              [value]="draft()" (input)="draft.set($any($event.target).value)" />

            <!-- Bouton envoyer -->
            <button type="submit" class="btn btn-hero btn-icon h-11 w-11 shrink-0 rounded-full" [disabled]="!draft().trim() || typing()" aria-label="Envoyer">
              <app-icon [icon]="Send" [size]="18" />
            </button>
          </form>
        </div>
      </section>

      <!-- PANNEAU HISTORIQUE DES CONSULTATIONS (responsive & interactif) -->
      @if (showHistory()) {
        <!-- Backdrop sur mobile -->
        <div class="fixed inset-0 z-40 bg-black/40 backdrop-blur-xs lg:hidden" (click)="showHistory.set(false)"></div>
      }
      <aside [class.translate-x-0]="showHistory()" [class.hidden]="!showHistory()"
        class="fixed inset-y-0 right-0 z-50 flex w-80 flex-col border-l border-border bg-card p-4 shadow-xl transition-transform lg:static lg:z-auto lg:flex lg:w-80 lg:rounded-3xl lg:border lg:shadow-soft"
        aria-label="Historique des consultations">
        
        <!-- En-tête de l'historique -->
        <div class="flex items-center justify-between border-b border-border/80 pb-3">
          <div class="flex items-center gap-2">
            <app-icon [icon]="History" [size]="18" class="text-primary" />
            <h2 class="text-sm font-bold text-deep">Historique</h2>
            <span class="rounded-full bg-primary/10 px-2 py-0.5 text-[11px] font-semibold text-primary">{{ historique().length }}</span>
          </div>
          <div class="flex items-center gap-1">
            <button type="button" class="btn btn-hero py-1 px-2.5 text-xs font-semibold flex items-center gap-1" (click)="restart()" title="Nouvelle consultation">
              <app-icon [icon]="Plus" [size]="14" />
              <span>Nouveau</span>
            </button>
            <button type="button" class="btn btn-ghost p-1 text-muted-foreground lg:hidden" (click)="showHistory.set(false)" aria-label="Fermer">
              <app-icon [icon]="X" [size]="18" />
            </button>
          </div>
        </div>

        <!-- Liste des discussions -->
        <div class="min-h-0 flex-1 space-y-2 overflow-y-auto py-3">
          @for (h of historique(); track h.id) {
            <div class="group relative flex flex-col justify-between rounded-2xl border p-3 transition-all cursor-pointer"
              [class]="currentConvId() === h.id ? 'border-primary bg-primary/5 shadow-xs' : 'border-border/80 bg-card hover:border-primary/40 hover:bg-soft/40'"
              (click)="loadConversation(h)">
              <div class="flex items-start justify-between gap-2">
                <div class="flex items-center gap-2 min-w-0 flex-1">
                  <span class="grid h-7 w-7 shrink-0 place-items-center rounded-lg"
                    [class]="currentConvId() === h.id ? 'bg-gradient-primary text-white' : 'bg-primary/10 text-primary'">
                    <app-icon [icon]="Chat" [size]="14" />
                  </span>
                  <div class="min-w-0 flex-1">
                    <p class="truncate text-xs font-bold text-deep leading-tight" [title]="h.title">{{ h.title }}</p>
                    <p class="text-[10px] text-muted-foreground mt-0.5">{{ fmtDate(h.savedAt) }}</p>
                  </div>
                </div>

                <!-- Bouton supprimer la discussion -->
                <button type="button" class="opacity-60 group-hover:opacity-100 p-1 text-muted-foreground hover:text-destructive transition-opacity"
                  (click)="$event.stopPropagation(); deleteConv(h.id)" title="Supprimer cette consultation">
                  <app-icon [icon]="Trash" [size]="13" />
                </button>
              </div>

              @if (h.specialtyLabel) {
                <div class="mt-2 flex items-center gap-1.5">
                  <span class="inline-block rounded-md bg-soft px-2 py-0.5 text-[10px] font-medium text-deep">
                    {{ h.specialtyLabel }}
                  </span>
                </div>
              }
            </div>
          }
          @if (!historique().length) {
            <div class="flex flex-col items-center justify-center py-12 text-center text-muted-foreground">
              <span class="grid h-12 w-12 place-items-center rounded-2xl bg-soft text-muted-foreground mb-2">
                <app-icon [icon]="Chat" [size]="20" />
              </span>
              <p class="text-xs font-semibold text-deep">Aucun historique pour l'instant</p>
              <p class="text-[11px] text-muted-foreground mt-1 max-w-[200px]">Vos échanges avec l'assistant seront automatiquement conservés ici.</p>
            </div>
          }
        </div>

        @if (historique().length > 1) {
          <div class="border-t border-border/80 pt-2 text-right">
            <button type="button" class="text-[11px] font-medium text-muted-foreground hover:text-destructive" (click)="clearAllHistory()">
              Effacer tout l'historique
            </button>
          </div>
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
  private readonly auth = inject(AuthService);

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
  protected readonly History = LucideHistory;
  protected readonly Plus = LucidePlus;
  protected readonly Trash = LucideTrash2;
  protected readonly Chat = LucideMessageCircle;
  protected readonly X = LucideX;
  protected readonly levelConfig = LEVEL_CONFIG;

  protected readonly suggestions = [
    { label: 'Fièvre et courbatures depuis 2 jours', icon: LucideThermometer },
    { label: 'Maux de tête persistants et nausées', icon: LucideBrain },
    { label: 'Toux sèche avec gêne respiratoire', icon: LucideWind },
    { label: 'Douleur au dos suite à un effort', icon: LucideActivity },
  ];

  // La langue est détectée à partir du dernier message de l'utilisateur.
  protected readonly lang = signal<TriageLanguage>('fr');

  // Identifiant de la discussion en cours
  protected readonly currentConvId = signal<string>(crypto.randomUUID());
  protected readonly showHistory = signal<boolean>(true);

  // Fil de messages
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

  // Historique des conversations
  protected readonly historique = signal<ConvHistorique[]>([]);

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
    // 1. Charger l'historique archivé
    const hist = loadJson<ConvHistorique[]>(historiqueKey(), []);
    this.historique.set(hist);

    // 2. Charger la conversation en cours
    const saved = loadJson<SavedConversation | null>(conversationKey(), null);
    if (saved && Array.isArray(saved.messages) && saved.messages.length > 0) {
      this.currentConvId.set(saved.convId || (hist.length > 0 ? hist[0].id : crypto.randomUUID()));
      this.messages.set(saved.messages);
      this.lang.set(saved.language ?? this.languageFromMessages(saved.messages));
      this.n = typeof saved.n === 'number' ? saved.n : 1;
      this.sessionId = saved.sessionId ?? null;
      this.lastSymptoms = saved.lastSymptoms ?? '';
      this.currentSpecialtyLabel.set(saved.specialtyLabel ?? '');
    } else if (hist.length > 0) {
      // Si aucune en cours mais historique existant, charger la plus récente
      this.loadConversation(hist[0]);
    } else {
      this.messages.set([this.greeting()]);
    }

    // Scroll automatique vers le bas à chaque nouveau message
    effect(() => {
      this.messages();
      this.typing();
      setTimeout(() => {
        const el = this.scroller()?.nativeElement;
        if (el) el.scrollTo({ top: el.scrollHeight, behavior: 'smooth' });
      }, 50);
    });
  }

  /** Sauvegarde l'état courant et met à jour l'historique sans rien perdre */
  private persistCurrent(): void {
    saveJson(conversationKey(), {
      convId: this.currentConvId(),
      messages: this.messages(),
      sessionId: this.sessionId,
      lastSymptoms: this.lastSymptoms,
      specialtyLabel: this.currentSpecialtyLabel(),
      n: this.n,
      language: this.lang(),
    });
    this.syncWithHistory();
  }

  /** Met à jour ou ajoute la conversation courante dans l'historique */
  private syncWithHistory(): void {
    const messages = this.messages();
    const userMsgs = messages.filter((m) => m.from === 'me');
    if (userMsgs.length === 0) return;

    const rawTitle = userMsgs[0].text.trim();
    const title = rawTitle.length > 55 ? rawTitle.slice(0, 55) + '…' : rawTitle;
    const id = this.currentConvId();

    this.historique.update((list) => {
      const idx = list.findIndex((c) => c.id === id);
      const item: ConvHistorique = {
        id,
        title,
        savedAt: new Date().toISOString(),
        specialtyLabel: this.currentSpecialtyLabel(),
        messages: [...messages],
        sessionId: this.sessionId,
        lastSymptoms: this.lastSymptoms,
        n: this.n,
        language: this.lang(),
      };
      if (idx >= 0) {
        const updated = [...list];
        updated[idx] = item;
        return updated;
      }
      return [item, ...list];
    });

    saveJson(historiqueKey(), this.historique());
  }

  /** Charger une discussion passée dans la vue active */
  protected loadConversation(h: ConvHistorique): void {
    this.currentConvId.set(h.id);
    this.messages.set([...h.messages]);
    this.lang.set(h.language ?? this.languageFromMessages(h.messages));
    this.sessionId = h.sessionId ?? null;
    this.lastSymptoms = h.lastSymptoms ?? '';
    this.currentSpecialtyLabel.set(h.specialtyLabel ?? '');
    this.n = h.n ?? h.messages.length;
    this.typing.set(false);

    // Sauvegarde la sélection comme conversation courante
    saveJson(conversationKey(), {
      convId: h.id,
      messages: this.messages(),
      sessionId: this.sessionId,
      lastSymptoms: this.lastSymptoms,
      specialtyLabel: this.currentSpecialtyLabel(),
      n: this.n,
    });

    // Sur mobile, ferme automatiquement le panneau d'historique
    if (window.innerWidth < 1024) {
      this.showHistory.set(false);
    }
  }

  /** Supprime une consultation de l'historique */
  protected deleteConv(id: string): void {
    this.historique.update((list) => list.filter((c) => c.id !== id));
    saveJson(historiqueKey(), this.historique());
    if (this.currentConvId() === id) {
      this.restart();
    }
  }

  /** Efface tout l'historique */
  protected clearAllHistory(): void {
    this.historique.set([]);
    saveJson(historiqueKey(), []);
    this.restart();
  }

  private greeting(): Bubble {
    const l = this.lang();
    let text = 'Bonjour ! Je suis votre assistant médical Clinic Copilot. Décrivez-moi vos symptômes ou posez votre question de santé, et je vais analyser votre état, répondre à vos questions et vous orienter vers les meilleurs spécialistes.';
    if (l === 'ar') {
      text = 'مرحباً بك! أنا مساعدك الطبي الذكي في Clinic Copilot. صف لي أعراضك أو استفسارك الصحي، وسأقوم بتحليل حالتك ومساعدتك وتوجيهك إلى أفضل الأطباء والمختصين.';
    } else if (l === 'en') {
      text = 'Hello! I am your Clinic Copilot medical assistant. Describe your symptoms or ask a health question, and I will analyze your condition, answer your questions, and guide you to the best specialists.';
    }
    return {
      id: 1,
      from: 'bot',
      text,
    };
  }

  protected placeholder(): string {
    const l = this.lang();
    if (this.sessionId) {
      return l === 'ar' ? 'إجابتك على السؤال…' : l === 'en' ? 'Your answer to the question…' : 'Votre réponse à la question…';
    }
    return l === 'ar' ? 'صف أعراضك أو استفسارك الصحي…' : l === 'en' ? 'Describe your symptoms or ask a health question…' : 'Décrivez vos symptômes ou posez une question médicale…';
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

  private languageFromMessages(messages: Bubble[]): TriageLanguage {
    const lastUserMessage = [...messages].reverse().find((message) => message.from === 'me');
    return lastUserMessage
      ? detectTriageLanguage(lastUserMessage.text, this.lang())
      : this.lang();
  }

  private say(b: Omit<Bubble, 'id' | 'from'>): void {
    this.messages.update((l) => [...l, { id: ++this.n, from: 'bot', ...b }]);
    this.persistCurrent();
  }

  /** Lance une nouvelle consultation propre tout en conservant la précédente */
  protected restart(): void {
    this.currentConvId.set(crypto.randomUUID());
    this.n = 1;
    this.typing.set(false);
    this.resetSession();
    this.messages.set([this.greeting()]);
    this.currentSpecialtyLabel.set('');
    saveJson(conversationKey(), {
      convId: this.currentConvId(),
      messages: this.messages(),
      sessionId: null,
      lastSymptoms: '',
      specialtyLabel: '',
      n: 1,
      language: this.lang(),
    });
  }

  protected fmtDate(iso: string): string {
    if (!iso) return '';
    const d = new Date(iso);
    return d.toLocaleString('fr-FR', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' });
  }

  protected async ask(text: string): Promise<void> {
    const t = text.trim();
    if (!t || this.typing()) return;
    this.lang.set(detectTriageLanguage(t, this.lang()));
    this.messages.update((l) => [...l, { id: ++this.n, from: 'me', text: t }]);
    this.persistCurrent();
    this.draft.set('');

    // Si on est dans un cycle de clarification
    if (this.sessionId) {
      this.answers.push(t);
      if (this.answers.length < this.questions.length) {
        const i = this.answers.length;
        const parsed = parseQuestionAndOptions(this.questions[i]);
        const thanks = this.lang() === 'ar'
          ? 'شكراً لإجابتك.'
          : this.lang() === 'en'
          ? 'Thank you for your answer.'
          : 'Merci pour votre précision.';
        this.say({
          text: `${thanks}\n\n${parsed.questionText}`,
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
      ? this.api.answer(this.sessionId, { answers: this.answers, language: this.lang(), ...(coords ?? {}) })
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
    // Cas où le modèle LLM demande des précisions de clarification
    if (r.status === 'NEEDS_CLARIFICATION' && r.clarificationQuestions?.length) {
      this.sessionId = r.sessionId;
      this.questions = r.clarificationQuestions;
      this.answers = [];

      const parsed = parseQuestionAndOptions(this.questions[0]);
      let intro = '';
      if (r.preliminaryExplanation) {
        intro = `${r.preliminaryExplanation}\n\n${parsed.questionText}`;
      } else {
        const promptIntro = this.lang() === 'ar'
          ? 'لتوضيح حالتك وتوجيهك بشكل أفضل : '
          : this.lang() === 'en'
          ? 'To better understand your condition and guide you : '
          : 'Pour mieux cerner votre situation et vous orienter : ';
        intro = `${promptIntro}${parsed.questionText}`;
      }

      this.say({
        text: intro,
        options: parsed.options,
      });
      return;
    }

    // Résultat final complet
    this.resetSession();
    const a = r.assessment;
    const consultationRecommended = a?.consultation_recommended ?? true;
    const code = a?.urgency_level ?? 'N3';
    const cfg = this.levelConfig[code] ?? this.levelConfig['N3'];
    const specLabel = SPECIALTIES[a?.recommended_specialty ?? ''] || 'Médecin spécialiste';
    this.currentSpecialtyLabel.set(r.bookingAllowed ? specLabel : '');

    this.say({
      text: a?.patient_explanation || 'Voici l\'évaluation de votre situation selon les symptômes décrits :',
      level: consultationRecommended ? cfg.level : undefined,
      code: consultationRecommended ? code : undefined,
      emergency: r.emergency,
      conditions: consultationRecommended ? a?.possible_conditions : undefined,
      tips: consultationRecommended
        ? r.emergency ? r.interimAdvice : (a?.red_flags_detected?.length ? a.red_flags_detected : r.interimAdvice)
        : undefined,
      specialty: r.bookingAllowed ? specLabel : undefined,
      doctors: r.bookingAllowed && !r.emergency ? r.suggestedDoctors : undefined,
    });

    this.currentSpecialtyLabel.set(this.messages().at(-1)?.specialty ?? '');
    this.persistCurrent();
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
    const dayLabel = this.formatDayChip(this.selectedDay());
    this.say({
      text: `Rendez-vous confirmé avec ${doc.displayName} le ${dayLabel} à ${slot}.`,
      bookingSuccess: {
        doctorName: doc.displayName,
        specialty: this.currentSpecialtyLabel(),
        date: dayLabel,
        time: slot,
      },
    });
  }
}
