/*
 * This file is part of Grocy Android.
 *
 * Grocy Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Grocy Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2020-2024 by Patrick Zedler and Dominic Zedler
 * Copyright (c) 2024-2026 by Patrick Zedler
 */

package xyz.zedler.patrick.grocy.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.databinding.RowNfcActionBinding;
import xyz.zedler.patrick.grocy.model.NfcAction;
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;

public class NfcActionAdapter extends RecyclerView.Adapter<NfcActionAdapter.ViewHolder> {

  private final static String TAG = NfcActionAdapter.class.getSimpleName();

  private final List<NfcAction> actions;
  private final String selectedId;
  private final NfcActionAdapterListener listener;

  public NfcActionAdapter(
      List<NfcAction> actions, String selectedId, NfcActionAdapterListener listener
  ) {
    this.actions = actions;
    this.selectedId = selectedId;
    this.listener = listener;
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {

    private final RowNfcActionBinding binding;

    public ViewHolder(RowNfcActionBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
  }

  @NonNull
  @Override
  public NfcActionAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new ViewHolder(
        RowNfcActionBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false
        )
    );
  }

  @Override
  public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
    NfcAction action = actions.get(holder.getAbsoluteAdapterPosition());

    holder.binding.textNfcActionName.setText(action.getName());
    holder.binding.textNfcActionDescription.setText(action.getDescription());
    holder.binding.imageNfcAction.setImageResource(action.getIcon());

    setSelected(holder, action.getId().equals(selectedId));

    // CONTAINER

    holder.binding.linearNfcActionContainer.setOnClickListener(
        view -> listener.onItemRowClicked(action)
    );
  }

  private void setSelected(ViewHolder holder, boolean selected) {
    Context context = holder.binding.getRoot().getContext();
    int colorSelected = ResUtil.getColor(context, R.attr.colorOnSecondaryContainer);
    if (selected) {
      holder.binding.linearNfcActionContainer
          .setBackground(ViewUtil.getBgListItemSelected(context));
    } else {
      holder.binding.linearNfcActionContainer.setBackground(
          ViewUtil.getRippleBgListItemSurface(context)
      );
    }
    holder.binding.imageNfcAction.setColorFilter(
        selected ? colorSelected : ResUtil.getColor(context, R.attr.colorOnSurfaceVariant)
    );
    holder.binding.textNfcActionName.setTextColor(
        selected ? colorSelected : ResUtil.getColor(context, R.attr.colorOnSurface)
    );
    holder.binding.textNfcActionDescription.setTextColor(
        selected ? colorSelected : ResUtil.getColor(context, R.attr.colorOnSurfaceVariant)
    );
  }

  @Override
  public int getItemCount() {
    return actions.size();
  }

  public interface NfcActionAdapterListener {

    void onItemRowClicked(NfcAction action);
  }
}
