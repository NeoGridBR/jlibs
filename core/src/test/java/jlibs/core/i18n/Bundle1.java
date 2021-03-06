/**
 * JLibs: Common Utilities for Java
 * Copyright (C) 2009  Santhosh Kumar T <santhosh.tekuri@gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */

package jlibs.core.i18n;

import jlibs.core.util.i18n.I18N;
import jlibs.core.util.i18n.Message;
import jlibs.core.util.i18n.ResourceBundle;

/**
 * @author Santhosh Kumar T
 */
@ResourceBundle
public interface Bundle1{
    public static final Bundle1 BUNDLE1 = I18N.getImplementation(Bundle1.class);

    /**
     * @param query
     */
    @Message("executing {0}")
    public String executing(String query);

    /**
     * @param time time taken in seconds
     */
    @Message(key="timeTaken", value="execution took {0, number} seconds")
    public String executionTook(int time);
}
