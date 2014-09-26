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

package com.frostwire.bittorrent;

import com.frostwire.bittorrent.libtorrent.LTEngine;
import com.limegroup.gnutella.settings.SharingSettings;
import org.gudy.azureus2.core3.util.protocol.AzURLStreamHandlerFactory;
import org.limewire.util.CommonUtils;

import java.io.File;
import java.net.URL;

/**
 * @author gubatron
 * @author aldenml
 */
public final class BTEngineFactory {

    private static BTEngine instance;

    private BTEngineFactory() {
    }

    public static BTEngine getInstance() {
        if (instance == null) {
            instance = LTEngine.getInstance();
            setup(instance);
        }
        return instance;
    }

    private static void setup(BTEngine engine) {

        // this hack is only due to the remaining vuze TOTorrent code
        URL.setURLStreamHandlerFactory(new AzURLStreamHandlerFactory());

        SharingSettings.initTorrentDataDirSetting();
        SharingSettings.initTorrentsDirSetting();

        engine.setHome(buildHome());
    }

    private static File buildHome() {
        File path = new File(CommonUtils.getUserSettingsDir() + File.separator + "libtorrent" + File.separator);
        if (!path.exists()) {
            path.mkdirs();
        }
        return path;
    }
}
