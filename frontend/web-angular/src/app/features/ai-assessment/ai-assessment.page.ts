import { ChangeDetectionStrategy, Component, ElementRef, effect, inject, signal, viewChild } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { TriageService } from './triage.service';
import { Lang, TriageSession, URGENCY_META, UrgencyLevel } from './triage.models';

// Numéro d'urgence local : à renseigner depuis une source officielle vérifiée (configuration validée).
// Tant qu'il est vide, le bouton d'appel n'est pas affiché.
const EMERGENCY_NUMBER = '';

type Msg =
  | { id: number; role: 'bot' | 'user'; kind: 'text'; text: string; small?: boolean }
  | { id: number; role: 'bot'; kind: 'question'; text: string; options: string[] }
  | { id: number; role: 'bot'; kind: 'result'; session: TriageSession };

function detectLang(): Lang {
  const l = (navigator.language || 'fr').slice(0, 2);
  return l === 'ar' || l === 'en' ? l : 'fr';
}

/** "Où sont les taches ? (visage / bras / dos)" -> question + options cliquables. */
function splitOptions(q: string): { text: string; options: string[] } {
  const m = q.match(/^(.*?)\s*\(([^()]+)\)\s*\??\s*$/);
  const options = m ? m[2].split('/').map(o => o.trim()).filter(Boolean) : [];
  return options.length >= 2 && m ? { text: m[1].trim(), options } : { text: q, options: [] };
}

@Component({
  selector: 'app-ai-assessment',
  templateUrl: './ai-assessment.page.html',
  styleUrl: './ai-assessment.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AiAssessmentPage {
  private readonly api = inject(TriageService);
  private readonly logEl = viewChild<ElementRef<HTMLElement>>('log');

  protected readonly lang = detectLang();
  protected readonly meta = URGENCY_META;
  protected readonly emergencyNumber = EMERGENCY_NUMBER;
  protected readonly messages = signal<Msg[]>([]);
  protected readonly draft = signal('');
  protected readonly busy = signal(false);
  protected readonly location = signal<{ lat: number; lng: number } | null>(null);
  protected readonly locationError = signal(false);
  protected readonly openDoctors = signal<ReadonlySet<number>>(new Set());

  private seq = 0;
  private sessionId: string | null = null;
  private questions: string[] = [];
  private answers: string[] = [];

  constructor() {
    this.bot('Bonjour ! Décrivez vos symptômes, je vous aide à vous orienter.');
    this.bot(
      "L'assistant oriente, il ne pose pas de diagnostic et ne remplace pas un médecin. En cas de danger vital, appelez les secours.",
      true,
    );
    effect(() => {
      this.messages();
      queueMicrotask(() => {
        const el = this.logEl()?.nativeElement;
        if (el) el.scrollTop = el.scrollHeight;
      });
    });
  }

  protected onInput(e: Event): void {
    this.draft.set((e.target as HTMLInputElement).value);
  }

  protected send(text: string = this.draft()): void {
    const t = text.trim();
    if (!t || this.busy()) return;
    this.draft.set('');
    this.push({ id: ++this.seq, role: 'user', kind: 'text', text: t });
    if (this.sessionId && this.answers.length < this.questions.length) {
      this.collect(t);
    } else {
      this.begin(t);
    }
  }

  protected shareLocation(): void {
    this.locationError.set(false);
    navigator.geolocation?.getCurrentPosition(
      p => this.location.set({ lat: p.coords.latitude, lng: p.coords.longitude }),
      () => this.locationError.set(true),
    );
  }

  protected toggleDoctors(id: number): void {
    this.openDoctors.update(s => {
      const n = new Set(s);
      if (n.has(id)) {
        n.delete(id);
      } else {
        n.add(id);
      }
      return n;
    });
  }

  protected pct(v: number): number {
    return Math.round(v * 100);
  }

  protected levelColor(l: UrgencyLevel): string {
    return this.meta[l].color;
  }

  private begin(symptoms: string): void {
    this.sessionId = null;
    this.questions = [];
    this.answers = [];
    const loc = this.location();
    this.busy.set(true);
    this.api
      .start({ symptoms, language: this.lang, ...(loc ? { latitude: loc.lat, longitude: loc.lng } : {}) })
      .subscribe({ next: s => this.handle(s), error: e => this.fail(e) });
  }

  private collect(answer: string): void {
    this.answers.push(answer);
    if (this.answers.length < this.questions.length) {
      this.ask(this.answers.length);
      return;
    }
    const loc = this.location();
    this.busy.set(true);
    this.api
      .answer(this.sessionId!, { answers: this.answers, ...(loc ? { latitude: loc.lat, longitude: loc.lng } : {}) })
      .subscribe({ next: s => this.handle(s), error: e => this.fail(e) });
  }

  private handle(s: TriageSession): void {
    this.busy.set(false);
    if (s.status === 'NEEDS_CLARIFICATION') {
      this.sessionId = s.sessionId;
      this.questions = s.clarificationQuestions;
      this.answers = [];
      if (s.preliminaryExplanation) this.bot(s.preliminaryExplanation);
      this.bot("J'ai besoin de quelques informations pour mieux vous orienter.");
      this.ask(0);
      return;
    }
    this.sessionId = null;
    this.questions = [];
    this.push({ id: ++this.seq, role: 'bot', kind: 'result', session: s });
    this.bot(s.disclaimer, true);
  }

  private ask(i: number): void {
    const { text, options } = splitOptions(this.questions[i]);
    this.push({ id: ++this.seq, role: 'bot', kind: 'question', text: `${i + 1}/${this.questions.length}. ${text}`, options });
  }

  private fail(e: HttpErrorResponse): void {
    this.busy.set(false);
    this.sessionId = null;
    this.questions = [];
    const msg =
      e.status === 0
        ? "Le service d'orientation est injoignable. Réessayez dans un instant. En cas de danger vital, appelez les secours."
        : "Une erreur est survenue. Réessayez, ou contactez un médecin si les symptômes vous inquiètent.";
    this.bot(msg);
  }

  private bot(text: string, small = false): void {
    this.push({ id: ++this.seq, role: 'bot', kind: 'text', text, small });
  }

  private push(m: Msg): void {
    this.messages.update(list => [...list, m]);
  }
}
