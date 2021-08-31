package com.hypertrack.android.ui.screens.visits_management.tabs.orders

import android.view.View
import com.hypertrack.android.models.local.LocalOrder
import com.hypertrack.android.models.local.OrderStatus
import com.hypertrack.android.ui.base.BaseAdapter
import com.hypertrack.android.ui.common.delegates.OrderAddressDelegate
import com.hypertrack.android.ui.common.util.setGoneState
import com.hypertrack.android.ui.common.util.toView
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.TimeDistanceFormatter
import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.item_order.view.*
import java.time.format.DateTimeFormatter

class OrdersAdapter(
    private val timeDistanceFormatter: TimeDistanceFormatter,
    private val addressDelegate: OrderAddressDelegate,
    private val showStatus: Boolean = true
) : BaseAdapter<LocalOrder, BaseAdapter.BaseVh<LocalOrder>>() {

    override val itemLayoutResource: Int = R.layout.item_order

    override fun createViewHolder(
        view: View,
        baseClickListener: (Int) -> Unit
    ): BaseVh<LocalOrder> {
        return object : BaseContainerVh<LocalOrder>(view, baseClickListener) {
            override fun bind(item: LocalOrder) {
                addressDelegate.shortAddress(item).toView(containerView.tvAddress)
                containerView.tvEta.setGoneState(item.status != OrderStatus.ONGOING)
                if (item.eta != null) {
                    containerView.tvEta.setText(
                        MyApplication.context.getString(
                            R.string.orders_list_eta,
                            timeDistanceFormatter.formatTime(item.eta!!.format(DateTimeFormatter.ISO_INSTANT))
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
