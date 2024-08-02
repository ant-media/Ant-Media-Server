/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.io.object;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Result of paged data request, one page of data.
 *
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 */
public class RecordSetPage {
    /**
     * Recordset cursor
     */
    private int cursor;

    /**
     * Data as List
     */
    private List<List<Object>> data;

    /**
     * Creates recordset page from Input object
     * 
     * @param input
     *            Input object to use as source for data that has to be deserialized
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public RecordSetPage(Input input) {
        Map mapResult = Deserializer.deserialize(input, Map.class);
        cursor = (Integer) mapResult.get("Cursor");
        data = (List<List<Object>>) mapResult.get("Page");
    }

    /**
     * Getter for recordset cursor
     *
     * @return Recordset cursor
     */
    protected int getCursor() {
        return cursor;
    }

    /**
     * Getter for page data
     *
     * @return Page data as unmodifiable list
     */
    protected List<List<Object>> getData() {
        return Collections.unmodifiableList(data);
    }

}
