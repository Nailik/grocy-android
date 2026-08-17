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

package xyz.zedler.patrick.grocy.model;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import java.util.ArrayList;
import java.util.List;
import xyz.zedler.patrick.grocy.Constants.NFC_ACTION;
import xyz.zedler.patrick.grocy.R;

public class NfcAction {

  private final String id;
  @StringRes private final int name;
  @StringRes private final int description;
  @DrawableRes private final int icon;

  public NfcAction(
      String id,
      @StringRes int name,
      @StringRes int description,
      @DrawableRes int icon
  ) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.icon = icon;
  }

  public String getId() {
    return id;
  }

  @StringRes
  public int getName() {
    return name;
  }

  @StringRes
  public int getDescription() {
    return description;
  }

  @DrawableRes
  public int getIcon() {
    return icon;
  }

  public static List<NfcAction> getNfcActions() {
    List<NfcAction> actions = new ArrayList<>();
    actions.add(new NfcAction(
        NFC_ACTION.PRODUCT_OVERVIEW,
        R.string.setting_nfc_action_product_overview,
        R.string.setting_nfc_action_product_overview_description,
        R.drawable.ic_round_preview
    ));
    actions.add(new NfcAction(
        NFC_ACTION.CONSUME,
        R.string.setting_nfc_action_consume,
        R.string.setting_nfc_action_consume_description,
        R.drawable.ic_round_consume_product
    ));
    actions.add(new NfcAction(
        NFC_ACTION.CONSUME_ALL,
        R.string.setting_nfc_action_consume_all,
        R.string.setting_nfc_action_consume_all_description,
        R.drawable.ic_round_inventory
    ));
    actions.add(new NfcAction(
        NFC_ACTION.OPEN,
        R.string.setting_nfc_action_open,
        R.string.setting_nfc_action_open_description,
        R.drawable.ic_round_open
    ));
    actions.add(new NfcAction(
        NFC_ACTION.ADD_TO_SHOPPING_LIST,
        R.string.setting_nfc_action_add_to_shopping_list,
        R.string.setting_nfc_action_add_to_shopping_list_description,
        R.drawable.ic_round_add_shopping_cart
    ));
    actions.add(new NfcAction(
        NFC_ACTION.PURCHASE,
        R.string.setting_nfc_action_purchase,
        R.string.setting_nfc_action_purchase_description,
        R.drawable.ic_round_local_grocery_store
    ));
    actions.add(new NfcAction(
        NFC_ACTION.TRANSFER,
        R.string.setting_nfc_action_transfer,
        R.string.setting_nfc_action_transfer_description,
        R.drawable.ic_round_swap_horiz
    ));
    actions.add(new NfcAction(
        NFC_ACTION.INVENTORY,
        R.string.setting_nfc_action_inventory,
        R.string.setting_nfc_action_inventory_description,
        R.drawable.ic_round_countertops
    ));
    actions.add(new NfcAction(
        NFC_ACTION.NONE,
        R.string.setting_nfc_action_none,
        R.string.setting_nfc_action_none_description,
        R.drawable.ic_round_cancel
    ));
    return actions;
  }

  @Nullable
  public static NfcAction getFromId(List<NfcAction> actions, String id) {
    for (NfcAction action : actions) {
      if (action.getId().equals(id)) {
        return action;
      }
    }
    return null;
  }
}
