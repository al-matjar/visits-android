package com.hypertrack.android.ui.screens.visits_management.tabs.orders

import android.view.View
import com.hypertrack.android.models.local.Order
import com.hypertrack.android.models.local.OrderStatus
import com.hypertrack.android.ui.base.BaseAdapter
import com.hypertrack.android.ui.common.delegates.address.OrderAddressDelegate
import com.hypertrack.android.ui.common.util.setGoneState
import com.hypertrack.android.ui.common.util.toView
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.formatters.DateTimeFormatter

import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.item_order.view.*

class OrdersAdapter(
    private val dateTimeFormatter: DateTimeFormatter,
    private val addressDelegate: OrderAddressDelegate,
    private val showStatus: Boolean = true
) : BaseAdapter<Order, BaseAdapter.BaseVh<Order>>() {

    override val itemLayoutResource: Int = R.layout.item_order

    override fun createViewHolder(
        view: View,
        baseClickListener: (Int) -> Unit
    ): BaseVh<Order> {
        return object : BaseContainerVh<Order>(view, baseClickListener) {
            override fun bind(item: Order) {
                item.shortAddress?.toView(containerView.tvAddress)
                containerView.tvEta.setGoneState(item.status != OrderStatus.ONGOING)
                if (item.eta != null) {
                    containerView.tvEta.setText(
                        MyApplication.context.getString(
                            R.string.orders_list_eta,
                            dateTimeFormatter.formatTime(item.eta!!)
                        )
                    )
                } else {
                    containerView.tvEta.setText(R.string.orders_list_eta_unavailable)
                }

                containerView.tvStatus.setGoneState(!showStatus)
                item.status.name.toView(containerView.tvStatus)
            }
        }
    }
}
