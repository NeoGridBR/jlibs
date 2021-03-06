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

package jlibs.wadl.cli.completors;

import jlibs.wadl.cli.WADLTerminal;
import jline.Completor;

import java.util.List;

/**
 * @author Santhosh Kumar T
 */
public class WADLCompletor implements Completor{
    private WADLTerminal terminal;
    public WADLCompletor(WADLTerminal terminal){
        this.terminal = terminal;
    }

    @Override
    @SuppressWarnings("unchecked")
    public int complete(String buffer, int cursor, List candidates){
        Buffer b = new Buffer(buffer, cursor, candidates);
        new CommandCompletion(terminal).complete(b);
        return b.getFrom();
    }
}
