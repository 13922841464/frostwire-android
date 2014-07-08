/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2014, FrostWire(R). All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.frostwire.android.gui.views.preference;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.frostwire.android.R;
import com.frostwire.android.gui.views.AbstractAdapter;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class StoragePreference extends DialogPreference {

    public StoragePreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setDialogLayoutResource(R.layout.dialog_preference_storage);
    }

    public StoragePreference(Context context) {
        this(context, null);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        ListView list = (ListView) view.findViewById(R.id.dialog_preference_storage_list);

        list.setAdapter(new StoragesAdapter(getContext()));
    }

    private static final class StorageMount {

        public StorageMount(String label, String description, String path) {
            this.label = label;
            this.description = description;
            this.path = path;
        }

        public final String label;
        public final String description;
        public final String path;
    }

    private final class StoragesAdapter extends AbstractAdapter<StorageMount> {

        public StoragesAdapter(Context context) {
            super(context, R.layout.view_preference_storage_list_item);

            addItems(context);
        }

        @Override
        protected void setupView(View view, StorageMount item) {
            ImageView icon = findView(view, R.id.view_preference_storage_list_item_icon);
            TextView label = findView(view, R.id.view_preference_storage_list_item_label);
            TextView description = findView(view, R.id.view_preference_storage_list_item_description);

            icon.setImageResource(R.drawable.app_icon);
            label.setText(item.label);
            description.setText(item.description);
        }

        private void addItems(Context context) {
            add(new StorageMount("a", "a", "a"));
            add(new StorageMount("b", "b", "b"));
            add(new StorageMount("c", "c", "cs"));
        }
    }
}
