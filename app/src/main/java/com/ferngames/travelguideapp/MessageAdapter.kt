package com.ferngames.travelguideapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class Message(
    val content: String,
    val isUser: Boolean
)

class MessageAdapter(
    private val messages: MutableList<Message> = mutableListOf()
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUserMessage: TextView = itemView.findViewById(R.id.tvUserMessage)
        val tvAiMessage: TextView = itemView.findViewById(R.id.tvAiMessage)
        val llAiMessage: LinearLayout = itemView.findViewById(R.id.llAiMessage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]

        if (message.isUser) {
            holder.tvUserMessage.visibility = View.VISIBLE
            holder.llAiMessage.visibility = View.GONE
            holder.tvUserMessage.text = message.content
        } else {
            holder.tvUserMessage.visibility = View.GONE
            holder.llAiMessage.visibility = View.VISIBLE
            holder.tvAiMessage.text = message.content
        }
    }

    override fun getItemCount() = messages.size

    fun addMessage(message: Message) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    fun removeLastMessage() {
        if (messages.isNotEmpty()) {
            messages.removeAt(messages.size - 1)
            notifyItemRemoved(messages.size)
        }
    }
}