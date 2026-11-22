package com.clinic.copilot.ui.chat

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.format.DateFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.clinic.copilot.databinding.ItemMessageBulleBinding
import com.clinic.copilot.domain.Message
import com.clinic.copilot.domain.StatutMessage
import java.util.Date

/**
 * Adapte les messages en bulles RecyclerView et aligne les messages de l'utilisateur courant.
 *
 * @param currentUserId Identifiant de l'utilisateur courant.
 */
class MessageAdapter(
    private val currentUserId: String,
) : ListAdapter<Message, MessageAdapter.MessageViewHolder>(MessageDiffCallback) {

    /**
     * Crée un ViewHolder à partir du layout lié par ViewBinding.
     *
     * @param parent Parent RecyclerView.
     * @param viewType Type de vue, inutilisé car il n'existe qu'un layout.
     * @return Le ViewHolder de bulle de message.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemMessageBulleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return MessageViewHolder(binding, currentUserId)
    }

    /**
     * Lie un message à la bulle recyclée.
     *
     * @param holder ViewHolder à mettre à jour.
     * @param position Position du message dans la liste.
     */
    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * Lie les données d'un message à son layout.
     *
     * @property binding Binding généré pour `item_message_bulle.xml`.
     * @property currentUserId Identifiant utilisé pour l'alignement des bulles.
     */
    class MessageViewHolder(
        private val binding: ItemMessageBulleBinding,
        private val currentUserId: String,
    ) : RecyclerView.ViewHolder(binding.root) {

        /**
         * Affiche le contenu, l'heure, le statut et l'alignement d'un message.
         *
         * @param message Message à afficher.
         */
        fun bind(message: Message) {
            val isOutgoing = message.expediteurId == currentUserId
            val context = binding.root.context
            binding.messageContent.text = message.contenu
            binding.messageTime.text = DateFormat.getTimeFormat(context).format(Date(message.dateEnvoi))
            binding.messageStatus.text = when (message.statut) {
                StatutMessage.ENVOYE -> "Envoyé"
                StatutMessage.ECHEC -> "Échec"
                StatutMessage.LU -> "Lu"
            }
            binding.messageStatus.visibility = if (isOutgoing) android.view.View.VISIBLE else android.view.View.GONE
            binding.messageBubble.setBackground(
                GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 16f * context.resources.displayMetrics.density
                    setColor(if (isOutgoing) Color.rgb(216, 233, 255) else Color.rgb(241, 243, 244))
                },
            )

            (binding.messageBubble.layoutParams as LinearLayout.LayoutParams).apply {
                gravity = if (isOutgoing) Gravity.END else Gravity.START
                val margin = (4 * context.resources.displayMetrics.density).toInt()
                setMargins(margin, 0, margin, 0)
                binding.messageBubble.layoutParams = this
            }
        }
    }

    private companion object MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean =
            oldItem == newItem
    }
}
