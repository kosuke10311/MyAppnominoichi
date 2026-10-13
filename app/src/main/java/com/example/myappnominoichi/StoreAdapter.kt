package com.example.myappnominoichi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class StoreAdapter(
    private val onItemClick: (Store) -> Unit
) : RecyclerView.Adapter<StoreAdapter.StoreViewHolder>() {

    private var stores: List<Store> = emptyList()
    private var stampedIds: Set<String> = emptySet()

    fun submit(newStores: List<Store>) {
        stores = newStores
        notifyDataSetChanged()
    }

    fun updateStampedIds(ids: Set<String>) {
        stampedIds = ids
        notifyDataSetChanged()
    }

    inner class StoreViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView =
            itemView.findViewById(R.id.storeNameText)
        private val addressText: TextView =
            itemView.findViewById(R.id.storeAddressText)
        private val stampText: TextView? =
            itemView.findViewById(R.id.textStampStatus)

        fun bind(store: Store) {
            nameText.text = store.name
            addressText.text = store.address

            val stamped = stampedIds.contains(store.id)
            if (stampText != null) {
                stampText.visibility = if (stamped) View.VISIBLE else View.GONE
            }

            itemView.setOnClickListener {
                onItemClick(store)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoreViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_store, parent, false)
        return StoreViewHolder(view)
    }

    override fun onBindViewHolder(holder: StoreViewHolder, position: Int) {
        holder.bind(stores[position])
    }

    override fun getItemCount(): Int = stores.size
}
