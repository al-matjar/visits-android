package com.hypertrack.android.ui.common.adapters

import android.view.View
import com.hypertrack.android.ui.base.BaseAdapter
import com.hypertrack.android.ui.common.util.SimpleTextWatcher
import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.item_editable_key_value.view.bDeleteField
import kotlinx.android.synthetic.main.item_editable_key_value.view.etKey
import kotlinx.android.synthetic.main.item_editable_key_value.view.etValue

class EditableKeyValueAdapter() :
    BaseAdapter<EditableKeyValueItem, BaseAdapter.BaseVh<EditableKeyValueItem>>() {

    override val itemLayoutResource: Int = R.layout.item_editable_key_value

    override fun createViewHolder(
        view: View,
        baseClickListener: (Int) -> Unit
    ): BaseVh<EditableKeyValueItem> {
        return object : BaseContainerVh<EditableKeyValueItem>(view, baseClickListener) {
            override fun bind(item: EditableKeyValueItem) {
                containerView.bDeleteField.setOnClickListener {
                    removeItemAndUpdate(bindingAdapterPosition)
                }
                containerView.etKey.addTextChangedListener(object : SimpleTextWatcher() {
                    override fun afterChanged(text: String) {
                        items[bindingAdapterPosition] = items[bindingAdapterPosition].copy(
                            key = text
                        )
                    }
                })
                containerView.etValue.addTextChangedListener(object : SimpleTextWatcher() {
                    override fun afterChanged(text: String) {
                        items[bindingAdapterPosition] = items[bindingAdapterPosition].copy(
                            value = text
                        )
                    }
                })
            }
        }
    }

    fun addNewField() {
        addItemsAndUpdate(listOf(EditableKeyValueItem()))
    }

}

data class EditableKeyValueItem(
    val key: String = "",
    val value: String = "",
)
