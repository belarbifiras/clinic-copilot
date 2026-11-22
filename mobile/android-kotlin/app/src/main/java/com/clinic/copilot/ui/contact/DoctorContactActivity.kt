package com.clinic.copilot.ui.contact

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.clinic.copilot.databinding.ActivityDoctorContactBinding

/**
 * Fiche de contact volontairement écrite en Activity classique avec ViewBinding et XML.
 *
 * Ce choix est pédagogique et technique : il démontre la cohabitation Compose / vue classique
 * et rend explicites les trois Intents du barème (un explicite à l'ouverture, deux implicites
 * pour appeler et partager). L'Activity ne contient aucune logique métier : elle affiche les
 * extras reçus et déclenche des Intents, comme une simple couche de présentation.
 */
class DoctorContactActivity : Activity() {

    private lateinit var binding: ActivityDoctorContactBinding

    /**
     * Affiche les extras reçus et branche les deux Intents implicites.
     *
     * @param savedInstanceState État Android restauré le cas échéant.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDoctorContactBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Retour de la Toolbar vers le chat ; le bouton système utilise déjà finish() par défaut.
        binding.toolbar.setNavigationOnClickListener { finish() }

        val nom = intent.getStringExtra(EXTRA_NOM).orEmpty().ifBlank { "Contact" }
        val specialite = intent.getStringExtra(EXTRA_SPECIALITE).orEmpty().ifBlank { "Praticien" }
        val telephone = intent.getStringExtra(EXTRA_TELEPHONE).orEmpty().ifBlank { "01 00 00 00 00" }

        binding.tvNomContact.text = nom
        binding.tvSpecialiteContact.text = specialite
        binding.tvTelephoneContact.text = telephone
        binding.tvAvatarInitiale.text = nom.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"

        // Intent IMPLICITE : ACTION_DIAL ouvre l'application téléphone sans la nommer.
        binding.btnAppeler.setOnClickListener {
            val numeroSansEspaces = telephone.replace(Regex("[\\s.-]"), "")
            val appel = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$numeroSansEspaces"))
            startActivity(appel)
        }

        // Intent IMPLICITE : ACTION_SEND laisse le système proposer les applications de partage.
        binding.btnPartager.setOnClickListener {
            val partage = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Coordonnées : $nom — $specialite — $telephone")
            }
            startActivity(Intent.createChooser(partage, "Partager les coordonnées"))
        }
    }

    /** Clés des extras transportés par l'Intent EXPLICITE depuis ChatScreen. */
    companion object {
        /** Nom du contact affiché en titre de la fiche. */
        const val EXTRA_NOM = "extra_nom_contact"

        /** Spécialité ou rôle du contact. */
        const val EXTRA_SPECIALITE = "extra_specialite_contact"

        /** Numéro de téléphone fictif. */
        const val EXTRA_TELEPHONE = "extra_telephone_contact"
    }
}
