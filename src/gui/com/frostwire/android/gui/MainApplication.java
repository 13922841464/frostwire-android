/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011, 2012, FrostWire(TM). All rights reserved.
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

package com.frostwire.android.gui;

import java.io.PrintWriter;
import java.io.StringWriter;

import android.app.Application;

import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.gui.search.LocalSearchEngine;
import com.frostwire.android.gui.services.Engine;

/**
 * 
 * @author gubatron
 * @author aldenml
 * 
 */
public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            // important initial setup here
            ConfigurationManager.create(this);
            NetworkManager.create(this);
            Librarian.create(this);
            LocalSearchEngine.create(this);
            Engine.create(this);

            Librarian.instance().syncMediaStore();
            Librarian.instance().syncApplicationsProvider();
        } catch (Throwable e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String stacktrace = sw.toString();
            throw new RuntimeException("MainApplication Create exception: " + stacktrace, e);
        }
    }
}
