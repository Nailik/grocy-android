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

package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.NFC;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.NfcActionAdapter;
import xyz.zedler.patrick.grocy.databinding.FragmentBottomsheetListSelectionBinding;
import xyz.zedler.patrick.grocy.model.NfcAction;
import xyz.zedler.patrick.grocy.util.UiUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;

public class NfcActionsBottomSheet extends BaseBottomSheetDialogFragment
    implements NfcActionAdapter.NfcActionAdapterListener {

  private final static String TAG = NfcActionsBottomSheet.class.getSimpleName();

  private FragmentBottomsheetListSelectionBinding binding;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState
  ) {
    binding = FragmentBottomsheetListSelectionBinding.inflate(
        inflater, container, false
    );

    MainActivity activity = (MainActivity) requireActivity();

    binding.textListSelectionTitle.setText(getString(R.string.setting_nfc_action));
    ViewUtil.centerText(binding.textListSelectionTitle);

    binding.textListSelectionDescription.setText(getString(R.string.setting_nfc_action_info));
    binding.textListSelectionDescription.setVisibility(View.VISIBLE);

    String selectedId = PreferenceManager.getDefaultSharedPreferences(activity)
        .getString(NFC.ACTION, SETTINGS_DEFAULT.NFC.ACTION);

    binding.recyclerListSelection.setLayoutManager(
        new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
    );
    binding.recyclerListSelection.setAdapter(
        new NfcActionAdapter(NfcAction.getNfcActions(), selectedId, this)
    );

    return binding.getRoot();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    binding = null;
  }

  @Override
  public void onItemRowClicked(NfcAction action) {
    MainActivity activity = (MainActivity) requireActivity();
    performHapticClick();
    PreferenceManager.getDefaultSharedPreferences(activity).edit()
        .putString(NFC.ACTION, action.getId()).apply();
    activity.getCurrentFragment().updateNfcAction();
    dismiss();
  }

  @Override
  public void applyBottomInset(int bottom) {
    binding.recyclerListSelection.setPadding(
        0, UiUtil.dpToPx(requireContext(), 8),
        0, UiUtil.dpToPx(requireContext(), 8) + bottom
    );
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
